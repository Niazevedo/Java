package sampling.abstraction;
import java.util.ArrayList;
import java.util.Map;


public class SamplingResult {
	
	private ArrayList<Map<String, Double>> points;
	
	public SamplingResult(){
		this.points = new ArrayList<Map<String, Double>>();
	}
	
	public SamplingResult(SamplingResult p){
		this.points = p.getPoints();
	}
	
	public SamplingResult(ArrayList<Map<String, Double>> points){
		this.points = points;
	}

	public ArrayList<Map<String, Double>> getPoints() {
		return points;
	}

	public void setPoints(ArrayList<Map<String, Double>> points) {
		this.points = points;
	}
	
	public void addPoint(Map<String,Double> map){
		this.points.add(map);
	}
}
