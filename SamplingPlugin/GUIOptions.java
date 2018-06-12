package org.optflux.sampling.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.optflux.core.gui.genericpanel.tablesearcher.TableSearchPanel;
import org.xml.sax.SAXException;

import pt.uminho.ceb.biosystems.mew.biocomponents.container.io.readers.ErrorsException;
import pt.uminho.ceb.biosystems.mew.biocomponents.validation.io.JSBMLValidationException;
import pt.uminho.ceb.biosystems.mew.core.model.exceptions.InvalidSteadyStateModelException;
import pt.uminho.ceb.biosystems.mew.core.model.steadystatemodel.ISteadyStateModel;

public class SamplingOptions extends JPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	ISteadyStateModel model = null;
	SpinnerNumberModel nwarm;
	JTextField nf;
	JTextField pc;
	JTextField sc;
	JButton cal;
	JLabel total;
	TableSearchPanel searchPanel;
	JTextField selreaction;
	JComboBox<String> combo;
	JLabel minmu;
	JLabel maxsigma;
	TableModel modeltab;
	JTextField opminmu;
	JTextField opmax;
	JButton button;
	TableModel tableModel;
	TableModel reactionTableModel;
	JTable table2;
	JButton bremove;
	int addedreaction = -1;
	
	public SamplingOptions() throws FileNotFoundException, XMLStreamException, ErrorsException, IOException, ParserConfigurationException, SAXException, JSBMLValidationException, InvalidSteadyStateModelException {
		
		// layout
		GridBagLayout thisLayout = new GridBagLayout();
		thisLayout.rowWeights= new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		thisLayout.rowHeights= new int[]{};
		thisLayout.columnWeights = new double[] {0.0, 0.0};
		thisLayout.columnWidths=new int[]{};
		this.setLayout(thisLayout);
		
		JPanel panelw = new JPanel();//left panel, warmup points
		JPanel panelf = new JPanel(); // left panel, nfiles
		JPanel panelpc = new JPanel(); //left panel. point count
		JPanel panelsc = new JPanel(); // left panel, step count
		JPanel panelt = new JPanel(); // left panel, total
		JPanel panelcomb = new JPanel(); //right panel, combo box
		JPanel paneltab = new JPanel(); //right panel, table
		JPanel panelopt = new JPanel(); //right panel, options
		JPanel panelbut = new JPanel();
		JPanel paneltab2 = new JPanel(); // right panel, table of the choices
		
		//left panel
		// warmup points
		JLabel nWarmp = new JLabel();
		nWarmp.setText("Warmup Points:");
		nwarm = new SpinnerNumberModel(5000, 0, 50000, 1);
		JSpinner spinner = new JSpinner(nwarm);
		panelw.add(nWarmp);
		panelw.add(spinner);
		
		//number of files
		JLabel nFiles = new JLabel("Number of Files:");
		nf = new JTextField();
		nf.setText("10");
		nf.setPreferredSize( new Dimension(75, 20));
		panelf.add(nFiles);
		panelf.add(nf);
		
		//points per file
		JLabel pCount = new JLabel("Points per File:");
		pc = new JTextField("1000");
		pc.setPreferredSize(new Dimension(75, 20));
		panelpc.add(pCount);
		panelpc.add(pc);
		
		//steps per point
		JLabel sCount = new JLabel("Steps per Point");
		sc = new JTextField("200");
		sc.setPreferredSize( new Dimension(75, 20));
		panelsc.add(sCount);
		panelsc.add(sc);
		
		// calculate button
		cal = new JButton("Calculate");
		cal.setPreferredSize( new Dimension(150, 25));
		total = new JLabel();
		this.changelabel();
		
		panelt.add(new JLabel("Total Points:"));
		panelt.add(total);
		
		//Search and reaction Table
		JLabel title = new JLabel("Bias Configuration");
		JLabel choice = new JLabel("Bias:");
		combo = new JComboBox<String>();
		combo.addItem("No Bias");
		combo.addItem("Uniform");
		combo.addItem("Normal");
		
		panelcomb.add(choice);
		panelcomb.add(combo);
		
		tableModel = new MyTable();
		JTable table = new JTable(tableModel);
		modeltab = new choiceTable();
		JScrollPane jScrollPane2 = new JScrollPane();
		jScrollPane2.setPreferredSize(new Dimension(0, 20));
		jScrollPane2.setViewportView(table);
		
		searchPanel = new TableSearchPanel(false);
		reactionTableModel = new MyTable();
		searchPanel.setModel(reactionTableModel);
		paneltab.add(searchPanel);
		
		// selected reaction table
		JLabel reaction = new JLabel("Reaction ID");
		selreaction = new JTextField();
		selreaction.setPreferredSize(new Dimension(75, 20));
		tableselection();
		
		selreaction.setEnabled(false);
		minmu = new JLabel("Min");
		maxsigma = new JLabel("Max");
		opminmu = new JTextField();
		opminmu.setPreferredSize(new Dimension(60, 20));
		opmax = new JTextField();
		opmax.setPreferredSize(new Dimension(60, 20));
		opminmu.setEnabled(false);
		opmax.setEnabled(false);
		
		selectfromCombo();
		
		panelopt.add(reaction);
		panelopt.add(selreaction);
		panelopt.add(minmu);
		panelopt.add(opminmu);
		panelopt.add(maxsigma);
		panelopt.add(opmax);
		
		button = new JButton();
		button.setText("Set");
		button.setPreferredSize(new Dimension(100, 20));
		setInformation();
		button.setEnabled(false);
		table2 = new JTable(modeltab);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(table2);
		table2.setFillsViewportHeight(true);
		bremove = new JButton("Remove");
		bremove.setPreferredSize(new Dimension(100, 20));
		bremove.setEnabled(false);
		setRemoveButton();
		removeFromTable();
		scrollPane.setPreferredSize(new Dimension(300, 100));
		paneltab2.add(scrollPane);
		panelbut.add(button);
		panelbut.add(bremove);
		
		this.add(panelw, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(panelf, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(panelpc, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(panelsc, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(cal, new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(panelt, new GridBagConstraints(0, 5, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(title, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(panelcomb, new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(paneltab, new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(panelopt, new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(panelbut, new GridBagConstraints(1, 4, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));
		this.add(paneltab2, new GridBagConstraints(1, 5, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 3, 3, 3), 0, 0));

	}
	
	public void updateModel(ISteadyStateModel model){
		
		ArrayList<String> allReactions = new ArrayList<String>(model.getReactions().keySet());
		ArrayList<String> allReactionsNames = new ArrayList<String>();
		for (String id : allReactions) {
			allReactionsNames.add(model.getReaction(id).getName());
			}
		tableModel = new MyTable(allReactions, allReactionsNames);
		reactionTableModel = new MyTable(allReactions, allReactionsNames);
		searchPanel.setModel(reactionTableModel);
		nwarm = new SpinnerNumberModel(5000, 2*model.getNumberOfReactions(), 50000, 1);
		this.model = model;
	}
	
	// action listener of the calculate button
	protected void updateComponents(){
		//
	}
	
	protected void changelabel(){
		cal.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				calculate();
			}
		});	
	}
	
	protected void calculate(){
		Integer fil = Integer.parseInt(nf.getText());
		Integer pointc = Integer.parseInt(pc.getText());
		Integer stepc = Integer.parseInt(sc.getText());
		Integer count = fil * (pointc * stepc);
		total.setText(count.toString());
	}
	
	// action listener to insert the selected reaction in a JTextField
	protected void tableselection(){
		searchPanel.getSelectionModel().addListSelectionListener(new ListSelectionListener() {	
			@Override
			public void valueChanged(ListSelectionEvent e) {
				updateReaction(e);
			}
		});
	}
	
	protected void updateReaction(ListSelectionEvent e){
		ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();
		int index = listSelectionModel.getMinSelectionIndex();
		String reactionId = (String) searchPanel.getValueAt(index,0);
		selreaction.setText(reactionId);
	}
	
	// action listener to update the Bias panel for each change in the JComboBox
	protected void selectfromCombo(){
		combo.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				 chooseBias();
			}
		});
	}
	
	protected void chooseBias(){
		String selected = combo.getSelectedItem().toString();
		 boolean state = false;
		 
		 if (selected.equals("No Bias")){
			 state = false;
		 }
		 else if (selected.equals("Normal")){
			 state = true;
			 minmu.setText("Mu");
			 maxsigma.setText("Sigma");
			 changeHeader("Mu", "Sigma");
			 modeltab = new choiceTable();
		 }
		 else{
			 state = true;
			 minmu.setText("Min");
			 maxsigma.setText("Max");
			 changeHeader("min", "max");
			 modeltab = new choiceTable();
		 }
		 searchPanel.setEnabled(state);
		 opminmu.setEnabled(state);
		 opmax.setEnabled(state);
		 button.setEnabled(state);
	}
	
	protected void changeHeader(String fName, String sName){
		JTableHeader th = table2.getTableHeader();
		TableColumnModel tcm = th.getColumnModel();
		TableColumn tc1 = tcm.getColumn(2);
		tc1.setHeaderValue( fName );
		TableColumn tc2 = tcm.getColumn(3);
		tc2.setHeaderValue( sName );
		th.repaint();
	}
	
	// action listener of the set button. Will add information to the table
	protected void setInformation(){
		button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				updateInformation();
			}
		});
	}
	
	protected void updateInformation(){
		if (opminmu != null && opmax 
				!=null && selreaction.getText().isEmpty() == false && ((choiceTable) modeltab).variableNames.contains(selreaction.getText()) == false){
			double minmu = Double.parseDouble(opminmu.getText());
			double maxsigma = Double.parseDouble(opmax.getText());
			((choiceTable) modeltab).setLine(selreaction.getText(), model.getReaction(selreaction.getText()).getName(), minmu, maxsigma);
		}
	}
	
	//action listener of the table. If a row is selected, the remove button is enabled
	protected void setRemoveButton(){
		table2.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();
				addedreaction = listSelectionModel.getMinSelectionIndex();
				bremove.setEnabled(true);
			}
		});
	}
	
	// action listener of the remove button. Will remove selected row
	protected void removeFromTable(){
		bremove.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				removeInformation();
			}
		});
	}
	
	protected void removeInformation(){
		if (addedreaction != -1){
			((choiceTable) modeltab).removeLine(addedreaction);
		}
	}
	
	protected static class MyTable extends AbstractTableModel{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		protected String[] columnNames = {"Reaction Id", "Reaction Name"};
		protected ArrayList<String> variableNames;
		protected ArrayList<String> variableExtendedNames;

		public MyTable() {
			variableNames = new ArrayList<String>();
			variableExtendedNames = new ArrayList<String>();
		}
		
		public MyTable(ArrayList<String> variableNames, ArrayList<String> variableExtendedNames) {
			this.variableNames = variableNames;
			this.variableExtendedNames = variableExtendedNames;
		}
		
		public String getColumnName(int col){
	        return columnNames[col];
	    }
		
		public void setVariableNames(ArrayList<String> variableNames) {
			this.variableNames = variableNames;
		}
		
		public void setVariableExtendedNames(ArrayList<String> variableExtendedNames) {
			this.variableExtendedNames = variableExtendedNames;
		}
		
		public int getRowCount() {
			return variableNames.size();
		}

		public int getColumnCount() {
			return 2;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if(columnIndex == 0)
				return  variableNames.get(rowIndex);
			
			if(columnIndex == 1)
				return variableExtendedNames.get(rowIndex);
			
			return null;
		}
	}
	
	protected static class choiceTable extends AbstractTableModel{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		protected String[] columnNames = {"Reaction Id", "Reaction Name","min","max"};
		protected ArrayList<String> variableNames;
		protected ArrayList<String> variableExtendedNames;
		protected ArrayList<Double> minmu;
		protected ArrayList<Double> maxsigma;

		public choiceTable() {
			variableNames = new ArrayList<String>();
			variableExtendedNames = new ArrayList<String>();
			minmu = new ArrayList<Double>();
			maxsigma = new ArrayList<Double>();
		}
		
		public choiceTable(String min, String max) {
			columnNames[2] = min;
			columnNames[3] = max;
			variableNames = new ArrayList<String>();
			variableExtendedNames = new ArrayList<String>();
			minmu = new ArrayList<Double>();
			maxsigma = new ArrayList<Double>();
		}
		
		public choiceTable(ArrayList<String> variableNames, ArrayList<String> variableExtendedNames, ArrayList<Double> minmu, ArrayList<Double> maxsigma){
			this.variableNames = variableNames;
			this.variableExtendedNames = variableExtendedNames;
			this.minmu = minmu;
			this.maxsigma = maxsigma;
		}
		
		public void setColumnName(String option){
			if (option.equals("Normal")){
				String[] norm = {"Reaction Id", "Reaction Name","mu","sigma"};
				this.columnNames = norm;
			}
			else {
				String[] uni = {"Reaction Id", "Reaction Name","min","max"};
				this.columnNames = uni;
			}
			fireTableDataChanged();
		}
		
		public String getColumnName(int col){
	        return columnNames[col];
	    }
		
		public void setVariableName(String variableNames) {
			this.variableNames.add(variableNames);
		}
		
		public void setVariableExtendedNames(String variableExtendedNames) {
			this.variableExtendedNames.add(variableExtendedNames);
		}
		
		public void setminmuValue(double minmu){
			this.minmu.add(minmu);
		}
		
		public void setmaxsigmaValue(double maxsigma){
			this.maxsigma.add(maxsigma);
		}
		
		public int getRowCount() {
			return variableNames.size();
		}

		public int getColumnCount() {
			return 4;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if(columnIndex == 0)
				return  variableNames.get(rowIndex);
			
			if(columnIndex == 1)
				return variableExtendedNames.get(rowIndex);
			
			if (columnIndex == 2)
				return minmu.get(rowIndex);
			
			if (columnIndex == 3)
				return maxsigma.get(rowIndex);
			
			return null;
		}
		 public void setLine (String reaction, String name, double min, double max){
			 this.variableNames.add(reaction);
			 this.variableExtendedNames.add(name);
			 this.minmu.add(min);
			 this.maxsigma.add(max);
			 fireTableDataChanged();
		 }
		 
		 public void removeLine (int index){
			 this.variableNames.remove(index);
			 this.variableExtendedNames.remove(index);
			 this.minmu.remove(index);
			 this.maxsigma.remove(index);
			 fireTableDataChanged();
		 }
	}
	
	public int getWarmupPoints(){
		String warm = nwarm.getValue().toString();
		return Integer.parseInt(warm);
	}
	
	public int getnFiles(){
		String n = nf.getText();
		return Integer.parseInt(n);
	}
	
	public int getPointCount(){
		String p = pc.getText();
		return Integer.parseInt(p);
	}
	
	public int getStepCount(){
		String s = sc.getText();
		return Integer.parseInt(s);
	}
	
	public String getBiasOption(){
		return combo.getSelectedItem().toString();
	}
	
	public Double[][] getBiasValuesParam(){
		int size = ((choiceTable) modeltab).getRowCount();
		Double[][] minmax = new Double[size][size];
		for (int row=0;row<size;row++){
			double min = ((choiceTable) modeltab).minmu.get(row);
			double max = ((choiceTable) modeltab).maxsigma.get(row);
			minmax[row][0]= min;
			minmax[row][1] = max;
		}
		return minmax;
	}
	
	public ArrayList<String> getBiasIndex(){
		ArrayList<String> index = new ArrayList<String>();
		int size = ((choiceTable) modeltab).getRowCount();
		for (int row=0;row<size;row++){
			String reaction = ((choiceTable) modeltab).variableNames.get(row);
			index.add(reaction);
		}
		return index;
	}
	
	public void addButtonsActionListener(ActionListener actionListener) {
		button.addActionListener(actionListener);
    	bremove.addActionListener(actionListener);
	}
}
