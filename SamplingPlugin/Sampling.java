package sampling.abstraction;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.NormOps;
import org.ejml.simple.SimpleMatrix;

import pt.uminho.ceb.biosystems.mew.core.model.components.IStoichiometricMatrix;
import pt.uminho.ceb.biosystems.mew.core.model.components.ReactionConstraint;
import pt.uminho.ceb.biosystems.mew.core.model.steadystatemodel.ISteadyStateModel;


public abstract class Sampling implements ISampling{
	
	protected  ISteadyStateModel model;
	protected static SimpleMatrix warmupM;
	protected Integer nFiles;
	protected Integer pointsPerFile;
	protected Integer stepsPerPoint;
	
	public Sampling(ISteadyStateModel model, SimpleMatrix warmupM, Integer nFiles, Integer pointsPerFile, Integer stepsPerPoint){
		this.model = model;
		this.warmupM = warmupM;
		this.nFiles = nFiles;
		this.pointsPerFile = pointsPerFile;
		this.stepsPerPoint = stepsPerPoint;
	}
	
	public ISteadyStateModel getSamplerModel(){
		return this.model;//obtain the model
	}
	
	public SimpleMatrix getSamplerWarmupM(){
		return this.warmupM;//obtain warmup points
	}
	
	public Integer getSamplernFiles(){
		return this.nFiles;
	}
	
	public Integer getSamplerPointsPerFile(){
		return this.pointsPerFile;
	}
	
	public Integer getSamplerStepsPerPoint(){
		return this.stepsPerPoint;
	}
	
	/** Converts the file into a matrix **/ 
	public static SimpleMatrix FileToMatrix(String FilePath, String Delimiter) throws IOException {
		HashMap<Integer,ArrayList<Double>> prematrix = new HashMap<Integer,ArrayList<Double>>();
		BufferedReader br = new BufferedReader(new FileReader(FilePath));
		String line;
		int ind=0;
		while((line = br.readLine()) != null){
			String[] column = line.split(Delimiter);
			ArrayList<Double> point = new ArrayList<Double>();
			for (int i=0;i<column.length;i++){
				point.add(Double.parseDouble(column[i]));
				
			}
			prematrix.put(ind,point);
			ind++;
		}
		br.close();
		int matrow = prematrix.size();
		int matcol = prematrix.get(0).size();
		SimpleMatrix mat = new SimpleMatrix(matrow,matcol);
		for (int row=0;row< matrow;row++){
			for (int col=0; col<matcol; col++){
				mat.set(row, col, prematrix.get(row).get(col));
			}
		}
		return mat;
	}
	
	/** Extracts a column from a file **/
	public static ArrayList<Integer> getColfromFile (String FilePath, String Delimiter, int col) throws IOException {
		ArrayList<Integer> randPoint = new ArrayList<Integer>();
		BufferedReader br = new BufferedReader(new FileReader(FilePath));
		String line;
		while((line = br.readLine()) != null){
			String[] columns = line.split(Delimiter);
			randPoint.add(Integer.parseInt(columns[col]));
		}
		br.close();
		return randPoint;
	}

	/** Calculates the mean for each line of the warmup matrix -> centerPoint **/
	public static SimpleMatrix meanRowMatrix(SimpleMatrix centerPoint){
		Integer nRxns = warmupM.numRows();
		Integer nWrmup = warmupM.numCols();
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
	
	
	/** IStoichiometricMatrix to SimpleMatrix **/
	public SimpleMatrix ISToSimple(){
		IStoichiometricMatrix ms = model.getStoichiometricMatrix();
		SimpleMatrix auxSte = new SimpleMatrix(ms.rows(), ms.columns());
		for(int r = 0; r < ms.rows(); r++){
			for(int col = 0; col < ms.columns(); col++){
				   auxSte.set(r, col, ms.getValue(r, col));
			}
		}
		return auxSte;
	}
	
	/** Returns the matrix from the null space **/
	public SimpleMatrix nullMatrix(){
		//Creates matrix N -> matrix of the null space from the stoichiometric matrix
		SimpleMatrix auxSte = ISToSimple();
		SimpleMatrix uSingular = auxSte.svd().nullSpace();
		return uSingular;
	}
	
	/** Converts SimpleMatrix to a DenseMatrix64F format **/
	public DenseMatrix64F simpleToDense(SimpleMatrix u){
		DenseMatrix64F simpleu = new DenseMatrix64F(u.numRows(), u.numCols());
		for(int r2 = 0; r2 < u.numRows(); r2++){
			for(int col2 = 0; col2 < u.numCols(); col2++){
		    	simpleu.set(r2, col2, u.get(r2, col2));
		    }
		}
		return simpleu;
	}
	
	/** Calculates the 2-norm of a matrix **/
	public double twoNormMatrix(DenseMatrix64F simpleu){
		double twonorm = NormOps.normP2(simpleu);
		return twonorm;
	}
	
	/** Divides a matrix by a value **/
	public SimpleMatrix divideMatrix(double twonorm, SimpleMatrix u){
		u = u.divide(twonorm);
		return u;
	}
	
	/** Creates a Map with the bounds of the reactions **/
	public static HashMap<String, Double> criaBounds(ISteadyStateModel model, int f){
        HashMap<String, Double> bounds = new HashMap<String, Double>();
        if(f == 0){
            for(int i = 0; i <model.getNumberOfReactions(); i++){
                String idR = model.getReactionId(i);
                ReactionConstraint rc = model.getReactionConstraint(idR);
                double lb = rc.getLowerLimit();
                bounds.put(idR, lb);
            }
        }
        else{
            for(int i = 0; i <model.getNumberOfReactions(); i++){
                String idR = model.getReactionId(i);
                ReactionConstraint rc = model.getReactionConstraint(idR);
                double ub = rc.getUpperLimit();
                bounds.put(idR, ub);
            }
        }
        return bounds;
}
	
	/** Maps with the calculated distance **/
	public HashMap<String, Double> getDists(SimpleMatrix prevPoint, HashMap<String, Double> bound, int f){
		HashMap<String, Double> dist = new HashMap<String, Double>();
		
		for(int i = 0; i < bound.size(); i++){
			String ind = model.getReactionId(i); 
			double prevPointAux = prevPoint.get(i, 0);
			if(f==1){ //upper bound
				double auxDistUB;
				auxDistUB = bound.get(ind) - prevPointAux;
				dist.put(ind, auxDistUB);
			}
			else{ //lower bound
			double auxDistLB;
			auxDistLB = prevPointAux - bound.get(ind); 
			dist.put(ind, auxDistLB);	
			}					
		}
		return dist;
	}
	
	/** Determine positive and negative reactions
	 * 1 -> positive, 0 -> negative **/
	public ArrayList<Integer> findDirection (double uTol, SimpleMatrix u, ArrayList<Integer> validDir, Integer f){
		ArrayList<Integer> dir = new ArrayList<Integer>();
		if (f==1){
			for (int i=0;i<validDir.size();i++){
				if (validDir.get(i)!=0 && (u.get(i, 0) > uTol)){
					dir.add(i);
				}
			}
		}
		else{
			for (int i=0;i<validDir.size();i++){
				if (validDir.get(i)!=0 && (u.get(i, 0) < -uTol)){
					dir.add(i);
				}
			}
		}
		return dir;
	}
	
	/** Verifies if its too close to the boundary **/
	public ArrayList<Integer>closeBoundary(double dTol, HashMap<String,Double> distUb, HashMap<String, Double> distLb){
		ArrayList<Integer> validDir = new ArrayList<Integer>();
		for (String reaction:distUb.keySet()){
			if ((distUb.get(reaction) > dTol) && (distLb.get(reaction) > dTol)){
				validDir.add(1);
			}
			else {validDir.add(0);}
		}
		return validDir;
	}
	
	/** Determine minimum and maximum steps 
	 * 1 -> max, 0 -> min **/
	public HashMap<String,Double> maxminStep(SimpleMatrix u, ArrayList<Integer> validDir, HashMap<String, Double> dist, Integer f){
		HashMap<String,Double> stepTemp = new HashMap<String, Double>();
		if (f==1){
			for (int i=0;i<validDir.size();i++){
				String ids = model.getReactionId(i);
				if (validDir.get(i)!=0){
					double distvalid = dist.get(ids) / u.get(i, 0);
					stepTemp.put(ids, distvalid);
				}
				
			}
		}
		else{
			for (int i=0;i<validDir.size();i++){
				String ids = model.getReactionId(i);
				if (validDir.get(i)!=0){
					double distvalid = -(dist.get(ids) / u.get(i, 0));
					stepTemp.put(ids, distvalid);
					}
			}
		}
		return stepTemp;
	}
	/** Creates a vector with the distance and direction **/
	public ArrayList<Double> StepVector(HashMap<String,Double> step, ArrayList<Integer> dir){
		ArrayList<Double> vect = new ArrayList<Double>();
		for (int i=0; i<dir.size();i++){
				String ids = model.getReactionId(dir.get(i));
				vect.add(step.get(ids));
		}
		return vect;
	}
	
	/** Adds two arrays **/
	public ArrayList<Double> addArray(ArrayList<Double> first, ArrayList<Double> second){
		ArrayList<Double> full = first;
		for (int i=0;i<second.size();i++){
			full.add(second.get(i));
		}
		return full;
	}
	
	/** Returns the maximum or minimum value of an array **/
	public double minmaxValue(ArrayList<Double> step, int f){
		if (f==0){//obter o minimo
			double value =Double.MAX_VALUE;
			for (int i=0;i<step.size();i++){
				if (step.get(i)<value){
					value = step.get(i);
				}
			}
			return value;
		}
		else{//obter o maximo
			double value =-Double.MAX_VALUE;
			for (int i=0;i<step.size();i++){
				if (step.get(i)>value){
					value = step.get(i);
				}
			}
			return value;
		}
	}
	
	/** Advance to the next point **/
	public SimpleMatrix newPoint(SimpleMatrix curPoint, SimpleMatrix prevPoint, SimpleMatrix stepAux){
		curPoint = new SimpleMatrix(prevPoint.numRows(), 1);
		for(int m = 0; m < prevPoint.numRows(); m++){
			double mAux = prevPoint.get(m, 0) + stepAux.get(m, 0);
			curPoint.set(m, 0, mAux);
		}
		return curPoint;
	}
	
	/** Reproject the current point and go to the next step **/
	public SimpleMatrix reprojectPoint(SimpleMatrix curPoint, SimpleMatrix uSingular){
		SimpleMatrix sCur = ISToSimple().mult(curPoint);
		double maximo = -1000;
		double valueCur = 0;
		for(int scurow = 0; scurow < sCur.numRows(); scurow++){
			for(int scurcol = 0; scurcol < sCur.numCols(); scurcol++){
				valueCur = Math.abs(sCur.get(scurow, scurcol));
				if(valueCur > maximo){ maximo = valueCur; }
			}
		}
		if(valueCur > 1e-9){
			curPoint = uSingular.mult(uSingular.transpose().mult(curPoint));
		}
		return curPoint;
	}
	
	public ArrayList<Double> maxMatMap(SimpleMatrix curPoint, HashMap<String, Double> upperBounds, Integer f){
		ArrayList<Double> value = new ArrayList<Double>();
		for(String k: upperBounds.keySet()){
			Integer indice = model.getReactionIndex(k);
			if(f == 1){
				double aux = curPoint.get(indice, 0) - upperBounds.get(k);
				value.add(aux);
			}
			else{
				double aux = upperBounds.get(k) - curPoint.get(indice, 0);
				value.add(aux);
			}
		}
		return value;
	}
	
	public ArrayList<Integer> findOver (SimpleMatrix curPoint,HashMap<String, Double> bounds, Integer f){
		ArrayList<Integer> over = new ArrayList<Integer>();
		if (f==1){
			for (int i=0;i<bounds.size();i++){
				String ids = model.getReactionId(i);
				if (curPoint.get(i, 0) > bounds.get(ids)){
					over.add(i);
				}
			}
		}
		else{
			for (int i=0;i<bounds.size();i++){
				String ids = model.getReactionId(i);
				if (curPoint.get(i, 0) < bounds.get(ids)){
					over.add(i);
				}
			}
		}
		return over;
	}
	
	public boolean findB(ArrayList<Double> anyRes){
		boolean f = false;
		for(int i = 0; i < anyRes.size() && (f == false); i++){
			if(anyRes.get(i) < 0){ f = true; }
		}
		return f;
	}
	
	public SimpleMatrix alterCurPoint(SimpleMatrix curPoint, ArrayList<Integer> ind, HashMap<String, Double> upperBound){
		for(int i = 0; i < ind.size(); i++){
			String ids = model.getReactionId(i);
			curPoint.set(i, 0, upperBound.get(ids));
		}
		return curPoint;
	}
	
	public SimpleMatrix recalculaCentro(SimpleMatrix curPoint, SimpleMatrix centerPoint, double valueAux){
		
		return centerPoint.scale(valueAux).plus(curPoint).divide(valueAux+1);
			}
	
	/** Convert the points to a map **/
	public Map<String,Double> convertPointsToMap(SimpleMatrix curPoint){
		Map<String, Double> mapa;
			mapa = new HashMap<String, Double>();
			for(int j= 0; j < curPoint.numRows(); j++){
				String ids = model.getReactionId(j);
				mapa.put(ids, curPoint.get(j, 0));
			}
		
		return mapa;
	}	
}
