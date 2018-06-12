
public class CreateWarmupPoints {
	
	private ISteadyStateModel model;
	private Integer nPoints;
	private Bias bias; //method, index and parameters
	private SimpleMatrix warmupPts;
	
	public CreateWarmupPoints(ISteadyStateModel model,Integer nPoints, Bias bias){
		this.model = model;
		this.nPoints = nPoints;
		this.bias = bias;
		Integer n = model.getNumberOfReactions();
		this.warmupPts = new SimpleMatrix(n, nPoints);
	}
	
	public ISteadyStateModel getModel(){
		return this.model;
	}
	
	public Integer getNPoints(){
		return this.nPoints;
	}
	
	public Bias getBias(){
		return this.bias;
	}
	
	public SimpleMatrix getWarmup(){
		return this.warmupPts;
	}
	
	public void setModel(ISteadyStateModel newModel){
		this.model = newModel;
	}
	
	public void setNPoints(Integer newPoints){
		this.nPoints = newPoints;
	}
	
	public void setBias(Bias newBias){
		this.bias = newBias;
	}
	
	/** Verifies the number of points(need to be at least 2*number of reactions **/
	public Integer verifyPoints(Integer nRxns){
		if(nPoints < (nRxns*2)){
			nPoints = nRxns*2;
		}
		return nPoints;
	}
	
	/** Applies the Bias method 'Uniform'**/
	public void methodUniform(Integer nb, EnvironmentalConditions ec, String idReact){
		double diff = bias.getValueParam(nb, 1) - bias.getValueParam(nb, 0);
		Random ra = new Random();
		double rand = ra.nextDouble();
		double fluxVal = diff*rand + bias.getValueParam(nb, 0);
		
		//System.out.println(diff + "\t" + rand + "\t" + bias.getValueParam(nb, 1) + "\t" + bias.getValueParam(nb, 0));
		ec.put(idReact, new ReactionConstraint(0.99999999*fluxVal, 1.00000001*fluxVal));
		
	}
	
	/** Applies the Bias method 'Normal'**/
	public void methodNormal(Integer nb, EnvironmentalConditions ec, String idReact){
		Boolean valOK = false;
		while(!valOK){
			Random ra = new Random();
			double rand = ra.nextDouble();
			double fluxVal = rand*bias.getValueParam(nb, 1) + bias.getValueParam(nb, 0);
			//System.out.println(fluxVal);
			if(fluxVal <= 20 && fluxVal > 20){
				valOK = true;
			}
			ec.put(idReact, new ReactionConstraint(0.99999999*fluxVal, 1.00000001*fluxVal));
		}
	}
	
	/** Calculates the mean for each line of the warmup matrix -> centerPoint **/
	public SimpleMatrix meanWarmup(Integer nRxns, Integer nWrmup, SimpleMatrix warmupM){
		SimpleMatrix centerPoint = new SimpleMatrix(nRxns,1);
		for(int l = 0; l < nRxns; l++){
			double rowSum = 0;
			for(int c = 0; c < nWrmup; c++){
				rowSum += warmupM.get(l, c);
			}
			double aux = rowSum/nWrmup;
			centerPoint.set(l, 0, aux);
		}
		return centerPoint;
	}
	
	/** Verifies if values are within the bounds **/
	private SimpleMatrix verifyBounds(Map<String, Double> simulation,HashMap<String, Double> ub, HashMap<String, Double> lb){
		int nRxns = simulation.size();
		SimpleMatrix x = new SimpleMatrix(nRxns,1);
		for (int bound=0;bound<nRxns;bound++){
			String rID = model.getReactionId(bound);
			Double xd = simulation.get(rID);
			x.set(bound, 0, xd);
			if (xd>ub.get(rID)){
				x.set(bound, 0, ub.get(rID));
			}
			if (xd<lb.get(rID)){
				x.set(bound, 0, lb.get(rID));
			}
		}
		return x;
	}
	
	public void run() throws Exception{
		Integer nRxns = model.getNumberOfReactions();
		SimulationSteadyStateControlCenter cc = new SimulationSteadyStateControlCenter(null, null, model, SimulationProperties.FBA);
		cc.setSolver(SolverType.CPLEX);
		verifyPoints(nRxns);
		ArrayList<Double> biasFluxMin = new ArrayList<Double>();
		ArrayList<Double> biasFluxMax = new ArrayList<Double>();
		HashMap<String, Double> lb = sampling.abstraction.Sampling.criaBounds(model,0);
		HashMap<String, Double> ub = sampling.abstraction.Sampling.criaBounds(model,1);
		
		if (bias!=null){
			// verifies if the parameters are within the bounds
			for (int k = 0; k<bias.getIndex().size();k++){
				String ind = bias.getReaction(k);
				//change objective objective
				cc.setFBAObjSingleFlux(ind, 1.0);
				cc.setMaximization(true);
				double maxFlux = cc.simulate().getOFvalue();
				cc.setMaximization(false);
				double minFlux = cc.simulate().getOFvalue();
				if(bias.getMethod().equals("Uniform")){
					double upperBias = bias.getValueParam(k, 1);
					double lowerBias = bias.getValueParam(k, 0);
					if (upperBias > maxFlux || upperBias < minFlux){
						upperBias = maxFlux;
					}
					if (lowerBias < minFlux || lowerBias > maxFlux){
						lowerBias = minFlux;
					}
					bias.setValueParam(k, 0, lowerBias);
					bias.setValueParam(k, 1, upperBias);
				}
				
				if (bias.getMethod().equals("Normal")){
					double biasMean = bias.getValueParam(k, 0);
					if (biasMean > maxFlux || biasMean < minFlux){
						double val = (minFlux + maxFlux)/2;
						bias.setValueParam(k, 0, val);
					}
					biasFluxMin.add(k, minFlux);
					biasFluxMax.add(k, maxFlux);
				}
			}
		}
		
		int point = 0;
		while (point< nPoints/2){
			if (bias!=null){
				// applies the bias to the reactions
				EnvironmentalConditions ec = new EnvironmentalConditions("bias");
				for (int i=0;i<bias.getIndex().size();i++){
					String idReact = bias.getReaction(i);
					if(bias.getMethod().equals("Uniform")){
						methodUniform(i, ec, idReact);
						cc = new SimulationSteadyStateControlCenter(ec, null, model, SimulationProperties.FBA);
					}
					if(bias.getMethod().equals("Normal")){
						methodNormal(i, ec, idReact);
						cc = new SimulationSteadyStateControlCenter(ec, null, model, SimulationProperties.FBA);
					}
				}
			}
			
			if (point < nRxns){
				// the objective function is the reaction
				cc.setFBAObjSingleFlux(model.getReactionId(point), 1.0);
			}else{
				// creates a random objective reaction
				HashMap<String,Double> reactions = new HashMap<String,Double>();
				Random ra= new Random();
				SimpleMatrix randVector = SimpleMatrix.random(nRxns, 1, 0, 1, ra);
				for (int reac = 0; reac < nRxns; reac++){
					reactions.put(model.getReactionId(reac), randVector.get(reac, 0)-0.5);
				}
				cc.setFBAObj(reactions);
			}
			
			// for maxmin = [-1,1] executes the cicle tow time, maxMin = 1 and then =-1
			for (int maxmin=0; maxmin<=1;maxmin++){
				
				if (maxmin ==0){
					// maximizes the reaction and inserts it into the matrix
					cc.setMaximization(true);
					FluxValueMap maxValue = cc.simulate().getFluxValues();
					SimpleMatrix x = verifyBounds(maxValue, ub, lb);
					
					warmupPts.insertIntoThis(0,2*point, x);
				}
				else{
					// minimizes the reaction and inserts it into the matrix
					cc.setMaximization(false);
					FluxValueMap minValue = cc.simulate().getFluxValues();
					SimpleMatrix x = verifyBounds(minValue, ub, lb);
					warmupPts.insertIntoThis(0,2*point+1, x);
				}
			
			SteadyStateSimulationResult res = cc.simulate();
			if (!res.getSolutionType().equals(LPSolutionType.OPTIMAL)){
				System.out.println("stuck");
				continue;
			}	
			}
			point +=1;
		}
		SimpleMatrix centerPoint = meanWarmup(nRxns, nPoints, warmupPts);
		SimpleMatrix one = new SimpleMatrix(1, nPoints);
		for (int col = 0; col<nPoints; col++){
			one.set(0, col, 1.0);
		}
		if (bias == null){
			SimpleMatrix mult = centerPoint.scale(0.67);
			warmupPts = warmupPts.scale(0.33).plus(mult.mult(one));
		}
		else{
			SimpleMatrix mult = centerPoint.scale(0.01);
			warmupPts = warmupPts.scale(0.99).plus(mult.mult(one));
		}
	}
	
	public static void main(String[] args){

	}
}
