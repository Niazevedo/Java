package org.optflux.sampling.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.optflux.core.datatypes.model.ModelBox;
import org.optflux.core.datatypes.project.Project;
import org.optflux.core.gui.genericpanel.okcancel.OkCancelMiniPanel;
import org.optflux.core.gui.genericpanel.projectandmodelselection.ProjectAndModelSelectionAibench;
import org.optflux.core.gui.genericpanel.projectandmodelselection.ProjectAndModelSelectionMiniPanel;
import org.optflux.core.populate.AbstractOperationGUIOptflux;
import org.xml.sax.SAXException;

import es.uvigo.ei.aibench.core.ParamSpec;
import es.uvigo.ei.aibench.core.operation.OperationDefinition;
import es.uvigo.ei.aibench.workbench.InputGUI;
import es.uvigo.ei.aibench.workbench.ParamsReceiver;
import es.uvigo.ei.aibench.workbench.Workbench;
import es.uvigo.ei.aibench.workbench.utilities.Utilities;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.ErrorsException;
import pt.uminho.ceb.biosystems.mew.biocomponents.validation.io.JSBMLValidationException;
import pt.uminho.ceb.biosystems.mew.core.model.exceptions.InvalidSteadyStateModelException;
import pt.uminho.ceb.biosystems.mew.core.model.steadystatemodel.ISteadyStateModel;

public class SamplingGUI extends AbstractOperationGUIOptflux  implements ActionListener, InputGUI{
	private static final long serialVersionUID = 1L;
	protected ProjectAndModelSelectionAibench projectModelSelectionPanel;
	protected SamplingOptions samplingPanel;
	protected ParamsReceiver rec;
	
	public SamplingGUI(){
		super();
		pack();
	}
	
	@Override
	public void init(ParamsReceiver receiver, OperationDefinition<?> operation) {
		rec = receiver;
		setTitle(operation.getName());
	    Utilities.centerOnOwner(this);
	    setVisible(true);
	    pack();
	}

	@Override
	public void onValidationError(Throwable t) {
		Workbench.getInstance().error(t);
	}

	@Override
	public void finish() {
		setVisible(false);
		dispose();
	}

	boolean valid = true;
	@Override
	public void actionPerformed(ActionEvent event) {
		String actionCommand = event.getActionCommand();
		if(actionCommand.equals(ProjectAndModelSelectionMiniPanel.PROJECT_ACTION_COMMAND)) {
			updateSamplingPanel();
		}else if(actionCommand.equals(OkCancelMiniPanel.OK_BUTTON_ACTION_COMMAND))
			termination();
		else if(actionCommand.equals(OkCancelMiniPanel.CANCEL_BUTTON_ACTION_COMMAND))
			finish();
	}

	private void termination() {
		ModelBox<?> modelBox = projectModelSelectionPanel.getModelBox();
		int warmup = samplingPanel.getWarmupPoints();
		int nfiles = samplingPanel.getnFiles();
		int pointcount = samplingPanel.getPointCount();
		int stepcount = samplingPanel.getStepCount();
		String biasoption = samplingPanel.getBiasOption();
		Double[][] biasvalues = null;
		ArrayList<String> biasindex = null;
		if(!biasoption.equals("No Bias")){
			biasvalues = samplingPanel.getBiasValuesParam();
			biasindex = samplingPanel.getBiasIndex();
		}
		
		rec.paramsIntroduced(new ParamSpec[] {
				new ParamSpec("Project", Project.class,
							modelBox.getOwnerProject(), null),
					new ParamSpec("modelBox", ModelBox.class,
							modelBox, null),
					new ParamSpec("WarmupPoints",
							Integer.class,warmup, null),
					new ParamSpec("NumberFiles",
							Integer.class, nfiles, null),
					new ParamSpec("PointCount",
							Integer.class, pointcount,null),
					new ParamSpec("StepPoints", 
							Integer.class, stepcount, null),
					new ParamSpec("BiasOption",
							Integer.class, biasoption, null),
					new ParamSpec("BiasValuesParam",
							Double.class, biasvalues, null),
					new ParamSpec("BiasIndex",
							ArrayList.class, biasindex, null)});
	}

	private void updateSamplingPanel() {
		if(projectModelSelectionPanel.getSelectedProjectId() != null){
			ModelBox<?> modelBox = projectModelSelectionPanel.getModelBox();
			if(modelBox!= null)
			samplingPanel.updateModel((ISteadyStateModel) modelBox.getModel());
			}
	}

	@Override
	public JPanel buildContentPanel() {
		JPanel thisPanel = new JPanel();
		
		GridBagLayout thisLayout = new GridBagLayout();
		thisLayout.rowWeights = new double[] {0.0, 1.0, 0.0};
		thisLayout.rowHeights = new int[] {0, 0, 0};
		thisLayout.columnWeights = new double[] {1.0};
		thisLayout.columnWidths = new int[] {10};
		thisPanel.setLayout(thisLayout);

		projectModelSelectionPanel = new ProjectAndModelSelectionAibench();
		projectModelSelectionPanel.addProjectActionListener(this);
		
		try {
			samplingPanel = new SamplingOptions();
		} catch (XMLStreamException | ErrorsException | IOException | ParserConfigurationException | SAXException
				| JSBMLValidationException | InvalidSteadyStateModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		samplingPanel.addButtonsActionListener(this);
		
		okCancelPanel.setEnabledOkButton(true);
		updateSamplingPanel();
		
		thisPanel.add(projectModelSelectionPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		thisPanel.add(samplingPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
		return thisPanel;
	}

	@Override
	protected LinkedHashSet<? extends JComponent> getOptfluxPanels() {
		LinkedHashSet<JPanel> allPanels = new LinkedHashSet<JPanel>();
		allPanels.add(projectModelSelectionPanel);
		allPanels.add(samplingPanel);
		
		return allPanels;
	}

	@Override
	public String getGUISubtitle() {
		return "Create Sampling Options";
	}
}
