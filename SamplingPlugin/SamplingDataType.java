package org.optflux.sampling.datatypes;

import java.io.Serializable;

import org.optflux.core.datatypes.model.ModelBox;
import org.optflux.core.datatypes.project.AbstractOptFluxDataType;
import org.optflux.core.datatypes.project.Project;
import org.optflux.core.datatypes.project.interfaces.IAnalysisResult;

import es.uvigo.ei.aibench.core.datatypes.annotation.Datatype;
import es.uvigo.ei.aibench.core.datatypes.annotation.Structure;
import sampling.abstraction.SamplingResult;

@Datatype(structure=Structure.SIMPLE,namingMethod="getName",setNameMethod="setName", renamed=true, removeMethod="remove")
public class SamplingDataType extends AbstractOptFluxDataType implements IAnalysisResult, Serializable{
	
	private static final long serialVersionUID = 1L;
	
	protected ModelBox<?> modelBox;
	protected SamplingResult sampling;
	
	public SamplingDataType(ModelBox<?> modelBox,SamplingResult sampling, String name){
		super (name);
		this.modelBox = modelBox;
		this.sampling = sampling;
	}
	
	@Override
	public Class<?> getByClass() {
		return getClass();
	}

	public SamplingResult getSamplingResult() {
		return sampling;
	}
	
	public ModelBox<?> getModelBox(){
		return modelBox;
	}
	
	@Override
	public String toString(){
		return getName();
	}

	@Override
	public Project getOwnerProject() {
		return modelBox.getOwnerProject();
	}

}
