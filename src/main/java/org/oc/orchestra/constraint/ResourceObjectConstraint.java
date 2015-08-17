package org.oc.orchestra.constraint;

import java.util.List;

import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.resource.Resource;

public class ResourceObjectConstraint implements Constraint {
	private String filename;
	
	public ResourceObjectConstraint(String filename) {
		this.filename = filename;
	}
	
	@Override
	public void enforce() {
		List<Resource> resources = ResourceFactory.makeResources(filename);
		for(Resource r : resources) {
			r.realize();
		}
	}

	@Override
	public boolean check() {
		List<Resource> resources = ResourceFactory.makeResources(filename);
		for(Resource r : resources) {
			if(!r.getCurrentState().equals(r.getState())) {
				return false;
			}
		}
		return true;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
}
