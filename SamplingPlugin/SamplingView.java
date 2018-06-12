package org.optflux.sampling.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.optflux.core.datatypes.project.Project;
import org.optflux.core.saveloadproject.CorruptProjectFileException;
import org.optflux.core.saveloadproject.SaveLoadManager;
import org.optflux.core.saveloadproject.SerializerNotRegistered;
import org.optflux.core.saveloadproject.serializers.UnsuportedModelTypeException;
import org.optflux.sampling.datatypes.SamplingDataType;
import org.optflux.simulation.datatypes.EnvironmentalConditionsDataType;
import org.optflux.simulation.datatypes.ReferenceFluxDistributionDatatype;
import org.optflux.simulation.datatypes.algorithm.fva.FVASolutionDataType;
import org.optflux.simulation.datatypes.algorithm.fva.FluxLimitsSolutionDataType;
import org.optflux.simulation.datatypes.criticality.CriticalGenesDataType;
import org.optflux.simulation.datatypes.criticality.CriticalReactionsDataType;
import org.optflux.simulation.datatypes.simulation.SteadyStateSimulationResultBox;
import org.optflux.simulation.saveload.serializers.CriticalGenesSerializer;
import org.optflux.simulation.saveload.serializers.CriticalReactionsSerializer;
import org.optflux.simulation.saveload.serializers.EConditionsSerializator;
import org.optflux.simulation.saveload.serializers.FVASolutionSerializer;
import org.optflux.simulation.saveload.serializers.FluxLimitsSolutionSerializer;
import org.optflux.simulation.saveload.serializers.ReferenceFluxDistributionSerializer;
import org.optflux.simulation.saveload.serializers.SimulationResultSerializer;

import com.silicolife.PlotUtilities.gral.XYPlotGral;

import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.axes.Axis;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.axes.LinearRenderer2D;
import de.erichseifert.gral.ui.InteractivePanel;
import de.erichseifert.gral.util.Insets2D;
import pt.uminho.ceb.biosystems.mew.core.model.steadystatemodel.ISteadyStateModel;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;
import pt.uminho.ceb.biosystems.mew.utilities.math.MathUtils;
import serializers.SamplingSerializer;

public class SamplingView extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected SamplingDataType dt;
	protected ISteadyStateModel model = null;
	protected JComboBox<String> reactionX;
	protected JComboBox<String> reactionY;
	protected XYPlotGral plot;
	protected String lastRemovedX;
	protected String lastRemovedY;
	protected Integer nPoints;
	protected ArrayList<Map<String, Double>> points;
	protected ArrayList<Double> pointsX;
	protected ArrayList<Double> pointsY;
	protected Double maxX;
	protected Double maxY;
	protected Double minX;
	protected Double minY;
	protected InteractivePanel panelplot;
	
	public SamplingView(SamplingDataType dt) throws Exception{
		this.dt = dt;
		this.model = (ISteadyStateModel) dt.getModelBox().getModel();
		initPanel();
	}
	
	protected void initPanel() throws Exception{
		reactionX = new JComboBox<String>();
		reactionY = new JComboBox<String>();
		pointsX = new ArrayList<Double>();
		pointsY = new ArrayList<Double>();
		points = dt.getSamplingResult().getPoints();
		
		ArrayList<String> allReactions = new ArrayList<String>(model.getReactions().keySet());
		for (String reaction : allReactions){
			reactionX.addItem(reaction);
			reactionY.addItem(reaction);
		}
		// removes the X first option of the array Y
		String selectedX = reactionX.getSelectedItem().toString();
		DefaultComboBoxModel<String> reactx = (DefaultComboBoxModel<String>)reactionY.getModel();
		reactx.removeElement(selectedX);
		lastRemovedY = selectedX;
		pointsX = makeArray(points, selectedX);
		
		// removes the Y first option of the array X
		String selectedY = reactionY.getSelectedItem().toString();
		DefaultComboBoxModel<String> reacty = (DefaultComboBoxModel<String>)reactionX.getModel();
		reacty.removeElement(selectedY);
		lastRemovedX = selectedY;
		pointsY = makeArray(points, selectedY);
		updateChart(pointsX,pointsY);
		updateCombo();

		GridBagLayout thisLayout = new GridBagLayout();
		thisLayout.rowWeights = new double[] {1.0, 0.0};
		thisLayout.rowHeights = new int[] {1,0};
		thisLayout.columnWeights = new double[] {1.0};
		thisLayout.columnWidths = new int[] {1};
		this.setLayout(thisLayout);
		
		JPanel xy = new JPanel();
		
		JLabel Xlabel = new JLabel("X Reaction:");
		JLabel Ylabel = new JLabel("Y Reaction:");
		
		xy.add(Xlabel);
		xy.add(reactionX);
		xy.add(Ylabel);
		xy.add(reactionY);
		
		this.add(panelplot, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(xy, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		
	}
	
	public ArrayList<Double> makeArray(ArrayList<Map<String, Double>> points, String selected){
		ArrayList<Double> xy = new ArrayList<Double>();
		if (xy.size()>0){
			xy = new ArrayList<Double>();
		}
		for (Map<String, Double> map : points){
				xy.add(map.get(selected));
			}
		return xy;

	}
	
	protected void createPlot(ArrayList<Double> x, ArrayList<Double> y){
		DataTable data = new DataTable(Double.class, Double.class);
		
		nPoints = points.size(); // number of points from the sampling
		
		Double [][] matrix = new Double [2][nPoints];
		
		// add points to the matrix
		for (int p = 0; p<x.size();p++){
			matrix [0][p] = x.get(p);
			matrix [1][p] = y.get(p);
		}
		
		Pair<Double, Double> minmaxX = MathUtils.minMaxT(x);
		minX =minmaxX.getA();
		maxX = minmaxX.getB();
		Pair<Double, Double> minmaxY = MathUtils.minMaxT(y);
		minY =minmaxY.getA();
		maxY = minmaxY.getB();

		//add matrix to the datatable
		for (int i = 0; i < matrix[0].length; i++) {
			data.add(matrix[0][i], matrix[1][i]);
		}
		
		XYPlot plot = new XYPlot(data);
		Insets2D insets = new Insets2D.Double(30.0, 30.0, 30.0, 30.0);
		plot.setInsets(insets);
		Axis axis_x = new Axis(minX-0.25, maxX + 1); //has to be the max value +10
		Axis axis_y = new Axis(minY-0.25, maxY + 1);
		axis_x.addAxisListener(plot);
        axis_y.addAxisListener(plot);
        AxisRenderer axis_x_renderer = new LinearRenderer2D();
        axis_x_renderer.setShapeVisible(true);
        AxisRenderer axis_y_renderer = new LinearRenderer2D();
        axis_y_renderer.setShapeVisible(true);
        plot.setAxis(XYPlot.AXIS_X, axis_x);
        plot.setAxis(XYPlot.AXIS_Y, axis_y);
        plot.setAxisRenderer(XYPlot.AXIS_X, axis_x_renderer);
        plot.setAxisRenderer(XYPlot.AXIS_Y, axis_y_renderer);
        this.panelplot = new InteractivePanel(plot);
        Color color = new Color(128,0,128);
        plot.getPointRenderer(data).setColor(color);
	}

	
	protected void updateChart(ArrayList<Double> x, ArrayList<Double> y){
		if (panelplot != null){
			this.remove(panelplot);
		}
		createPlot(x,y);
		this.add(panelplot, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.updateUI();
	}
	
	public void updateCombo(){
		reactionX.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedX = reactionX.getSelectedItem().toString();
				DefaultComboBoxModel<String> react = (DefaultComboBoxModel<String>) reactionY.getModel();
				react.removeElement(selectedX);
				react.insertElementAt(lastRemovedY, model.getReactionIndex(lastRemovedY));
				lastRemovedY = selectedX;
				pointsX = makeArray(points, selectedX);
				updateChart(pointsX,pointsY);
			}
		});
		reactionY.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedY = reactionY.getSelectedItem().toString();
				DefaultComboBoxModel<String> react = (DefaultComboBoxModel<String>) reactionX.getModel();
				react.removeElement(selectedY);
				react.insertElementAt(lastRemovedX, model.getReactionIndex(lastRemovedX));
				lastRemovedX = selectedY;
				pointsY = makeArray(points, selectedY);
				updateChart(pointsX,pointsY);
			}
		});
	}
	
	public static void main(String[] args) throws Exception {
		JDialog dialog = new JDialog();
		GridBagLayout thisLayout = new GridBagLayout();
		thisLayout.rowWeights = new double[] {1.0};
		thisLayout.rowHeights = new int[] {1};
		thisLayout.columnWeights = new double[] {1.0};
		thisLayout.columnWidths = new int[] {1};
		dialog.setLayout(thisLayout);
        dialog.setMinimumSize(new Dimension(400, 400));

		//test	
		Project p = null;
		EConditionsSerializator ecs = new EConditionsSerializator();
		SimulationResultSerializer srs = new SimulationResultSerializer();
		CriticalGenesSerializer cgs = new CriticalGenesSerializer();
		
		SamplingSerializer ss = new SamplingSerializer();
		
		CriticalReactionsSerializer crs = new CriticalReactionsSerializer();
		FluxLimitsSolutionSerializer fls = new FluxLimitsSolutionSerializer();
		FVASolutionSerializer fvas = new FVASolutionSerializer();
		ReferenceFluxDistributionSerializer fmSer = new ReferenceFluxDistributionSerializer();
		try {
			SaveLoadManager.getInstance().registerBuilder(EnvironmentalConditionsDataType.class, ecs);
			SaveLoadManager.getInstance().registerBuilder(SteadyStateSimulationResultBox.class, srs);
			SaveLoadManager.getInstance().registerBuilder(CriticalGenesDataType.class, cgs);
			SaveLoadManager.getInstance().registerBuilder(CriticalReactionsDataType.class, crs);
			SaveLoadManager.getInstance().registerBuilder(FluxLimitsSolutionDataType.class, fls);
			SaveLoadManager.getInstance().registerBuilder(FVASolutionDataType.class, fvas);
			SaveLoadManager.getInstance().registerBuilder(ReferenceFluxDistributionDatatype.class, fmSer);
			SaveLoadManager.getInstance().registerBuilder(SamplingDataType.class, ss);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			p = SaveLoadManager.getInstance().getProjectFromFolder(new File("/home/niazevedo/of_workspace/teste/"));
//			p = SaveLoadManager.getInstance().getProjectFromFolder(new File("/home/hgiesteira/AllOptFluxWS/OptimizationSerializationWS/EcoliCore"));
		} catch (ClassNotFoundException | IOException | UnsuportedModelTypeException | CorruptProjectFileException | SerializerNotRegistered e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(p.getAnalysisElementListByClass(SamplingDataType.class).getElementList().size());
		SamplingDataType dt = (SamplingDataType)p.getAnalysisElementListByClass(SamplingDataType.class).getElement(0);
		System.out.println(dt);
		
		JPanel panel = new SamplingView(dt);
		
		dialog.add(panel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setVisible(true);
	}
}
