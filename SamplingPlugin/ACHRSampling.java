
public class ACHRSampling extends Sampling{
	
		private double maxMinTol = 1e-9;
		private double uTol = 1e-9; 
		private double dTol = 1e-14;
		private Integer totalStepCount = 0;
		private Integer totalCount = pointsPerFile*stepsPerPoint;
		private SimpleMatrix centerPoint = new SimpleMatrix(model.getNumberOfReactions(),1);
		private SimpleMatrix prevPoint = new SimpleMatrix(model.getNumberOfReactions(),1);
		private SimpleMatrix curPoint = new SimpleMatrix(prevPoint.numRows(), 1);
		private SamplingResult pointsMatrix = new SamplingResult();
		
		public ACHRSampling(ISteadyStateModel model, SimpleMatrix warmupM, Integer nFiles,Integer pointsPerFile, Integer stepsPerPoint){
			super(model, warmupM, nFiles, pointsPerFile, stepsPerPoint);
			
		}
		
		public void setSamplerModel(ISteadyStateModel m){
			this.model = m;
		}
		
		public void setSamplerWarmupM(SimpleMatrix s){
			this.warmupM = s;
		}
		
		public void setSamplernFiles(Integer n){
			this.nFiles = n;
		}
		public void setSamplerPointsPerFile(Integer point){
			this.pointsPerFile = point;
		}
		
		public void setSamplerStepsPerPoint(Integer perpoint){
			this.stepsPerPoint = perpoint;
		}
		
		public double getMaxMinTol() {
			return this.maxMinTol;
		}

		public void setMaxMinTol(double maxMinTol) {
			this.maxMinTol = maxMinTol;
		}

		public double getuTol() {
			return this.uTol;
		}

		public void setuTol(double uTol) {
			this.uTol = uTol;
		}
		
		public double getdTol() {
			return this.dTol;
		}

		public void setdTol(double dTol) {
			this.dTol = dTol;
		}
		
		public SimpleMatrix getCenterPoint() {
			return this.centerPoint;
		}

		public void setCenterPoint(SimpleMatrix centerPoint) {
			this.centerPoint = centerPoint;
		}
		
		public SimpleMatrix getPrevPoint() {
			return this.prevPoint;
		}

		public void setPrevPoint(SimpleMatrix prevPoint) {
			this.prevPoint = prevPoint;
		}
		
		public Integer getTotalStepCount() {
			return this.totalStepCount;
		}

		public void setTotalStepCount(Integer totalStepCount) {
			this.totalStepCount = totalStepCount;
		}

		public Integer getTotalCount() {
			return this.totalCount;
		}

		public void setTotalCount(Integer totalCount) {
			this.totalCount = totalCount;
		}

		public SimpleMatrix getCurPoint() {
			return this.curPoint;
		}

		public void setCurPoint(SimpleMatrix curPoint) {
			this.curPoint = curPoint;
		}
		
		public SamplingResult getPointsMatrix() {
			return this.pointsMatrix;
		}

		public void setPointsMatrix(SamplingResult pointsMatrix) {
			this.pointsMatrix = pointsMatrix;
		}
		
		public SamplingResult run() throws IOException{
			Integer nWrmup = warmupM.numCols();
			
			setCenterPoint(meanRowMatrix(centerPoint));
			SimpleMatrix uSingular = nullMatrix();
			setPrevPoint(getCenterPoint());
			
			for (int file=0;file<getSamplernFiles();file++){
				
				int pointCount = 0;
				while (pointCount < getSamplerPointsPerFile()){
					Random ra = new Random();
					SimpleMatrix randVector = SimpleMatrix.random(getSamplerPointsPerFile(), 1, 0, 1, ra);
					//SimpleMatrix randV = FileToMatrix("randvector_file.txt", ",").extractVector(true, file*(getSamplerPointsPerFile())+pointCount);
					//SimpleMatrix randVector = randV.transpose();
					
					int stepCount=0;
					while (stepCount < getSamplerStepsPerPoint()){
						Double ran = ra.nextDouble();
						//Neste caso usamos o floor ao contrario do ceil porque o java começa em 0 e não em 1
						Integer randPointID = (int) Math.floor(nWrmup*ran);
	
						//Integer randPointID = getColfromFile("alldata.txt", ",", 2).get(file*(getSamplerPointsPerFile()+getSamplerStepsPerPoint())+stepCount+pointCount)-1;
						
						//Get a direction from the center point to the warmup point
						SimpleMatrix randPoint = warmupM.extractVector(false, randPointID);
						SimpleMatrix d = randPoint.minus(centerPoint);
						SimpleMatrix u;
						DenseMatrix64F simpleu = simpleToDense(d);
						double twonorm = twoNormMatrix(simpleu);
						u = divideMatrix(twonorm, d); // a partir deste passo começa a sofrer mais com os arredondamentos
						
						
						// 0 -> lower bound, 1 -> upper bound
						// creates a map with the bounds of each reaction
						HashMap<String, Double> lowerBounds = criaBounds(model, 0); 
						HashMap<String, Double> upperBounds = criaBounds(model, 1);
						
						//Figure out the distances to upper and lower bounds
						HashMap<String,Double> distLb = getDists(prevPoint, lowerBounds, 0);
						HashMap<String,Double> distUb = getDists(prevPoint, upperBounds, 1);
						
						// Figure out if we are too close to a boundary
						ArrayList<Integer> validDir = closeBoundary(dTol,distUb,distLb);
						
						// Figure out positive and negative directions
						// Figure out positive direction -> returns reaction ids
						ArrayList<Integer> posDir = findDirection(uTol, u, validDir, 1);
						// Figure out negative direction -> returns reaction ids
						ArrayList<Integer> negDir = findDirection(uTol, u, validDir, 0);
						
						//Figure out all the possible maximum and minimum step sizes
						HashMap<String,Double> maxstep = maxminStep(u, validDir, distUb, 1);
						HashMap<String,Double> minstep = maxminStep(u, validDir, distLb, 0);
						
						ArrayList<Double> maxStepVecPos = StepVector(maxstep, posDir);
						ArrayList<Double> minStepVecNeg = StepVector(minstep, negDir);
						ArrayList<Double> maxStepVecNeg = StepVector(maxstep, negDir);
						ArrayList<Double> minStepVecPos = StepVector(minstep, posDir);
						
						ArrayList<Double> maxStepVec = addArray(maxStepVecPos, minStepVecNeg);
						ArrayList<Double> minStepVec = addArray(minStepVecPos, maxStepVecNeg);
						
						//Figure out the true max & min step sizes
						// 1 for maximum, 0 for minimum
						double MaxM=minmaxValue(maxStepVec, 0);
						double MinM=minmaxValue(minStepVec, 1);
						
						//Find new direction if we're getting too close to a constraint
						if((Math.abs(MinM) < getMaxMinTol()) && (Math.abs(MaxM) < getMaxMinTol()) || (MinM > MaxM)){
							continue; // returns to the top of the cicle, next iteration
						}
						
						//Pick a rand out of list_of_rands and use it to get a random step distance
						double valueRandVector  = randVector.get(stepCount, 0);
						double stepDist = valueRandVector * (MaxM-MinM) + MinM;
						
						//Advance to the next point
						SimpleMatrix stepAux = u.scale(stepDist);
						setCurPoint(newPoint(curPoint, prevPoint, stepAux));
						
						//Reproject the current point and go to the next step
						if((getTotalStepCount() % 10) == 0){
							setCurPoint(reprojectPoint(curPoint, uSingular));
						}
						
						ArrayList<Integer> overInd = findOver(curPoint, upperBounds, 1);
						ArrayList<Integer> underInd = findOver(curPoint, lowerBounds, 0);
						
						ArrayList<Double> anyResUB = maxMatMap(curPoint, upperBounds, 0);
						ArrayList<Double> anyResLB = maxMatMap(curPoint, lowerBounds, 1);
						if(findB(anyResUB)==true || findB(anyResLB)==true){ 
							setCurPoint(alterCurPoint(curPoint, overInd, upperBounds));
							setCurPoint(alterCurPoint(curPoint, underInd, lowerBounds));
						}
						
						setPrevPoint(getCurPoint());
						
						//recalculate the center point
						setTotalStepCount(totalStepCount+1);
						double valueAux = nWrmup + getTotalStepCount();
						setCenterPoint(recalculaCentro(curPoint, centerPoint, valueAux));
						
						
						/** Saves points to the structure **/
						Map<String,Double> map = convertPointsToMap(curPoint);
						pointsMatrix.addPoint(map);
						stepCount+=1;
						System.out.println("end");
					}
					pointCount += 1;
				}
			}
			return pointsMatrix;
		}
		
		 public static void main(String [] args) throws Exception{
			 ///Test
			String filePath = "/home/niazevedo/Documents/ecoli_core_model.xml";
			Container cont = new Container(new JSBMLReader(filePath, "", false));
			Set<String> b = cont.identifyMetabolitesIdByPattern(Pattern.compile(".*_b"));
			cont.removeMetabolites(b);
			ISteadyStateModel model = ContainerConverter.convert(cont);
			
			EnvironmentalConditions envcond = new EnvironmentalConditions();
			envcond.addReactionConstraint(model.getBiomassFlux(), new ReactionConstraint(0.1, 1000.0));
			
			SimulationSteadyStateControlCenter cc = new SimulationSteadyStateControlCenter(envcond, null, model, SimulationProperties.FBA);
			cc.setMaximization(true);
			cc.setSolver(SolverType.CPLEX3);
			
			SteadyStateSimulationResult result = cc.simulate();
				
			
			System.out.println(result.getOFvalue());
						
			//SimpleMatrix warmupM = FileToMatrix("new_file.tsv","\t");
			
			Integer num = 250;
			//EnvironmentalConditions ec = null;
			Bias bias = null;
			//OverrideSteadyStateModel override = new OverrideSteadyStateModel(model, envcond);
			CreateWarmupPoints point = new CreateWarmupPoints(model, num, bias);
			point.run();
			SimpleMatrix warmupM = point.getWarmup();
			warmupM.print(15, 9);
			
			ACHRSampling s = new ACHRSampling(model, warmupM, 10, 50, 5);
			SamplingResult res = s.run();
			for (Map<String,Double> resl : res.getPoints()){
				System.out.println(resl);
			}
	 }
}
