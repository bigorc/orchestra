package org.oc.orchestra.constraint;

import java.util.List;

import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.coordinate.Coordinator;
import org.oc.orchestra.coordinate.Curator;
import org.oc.orchestra.resource.Resource;


public class RemoteResourceObjectConstraint extends
		DistributedSMConstraint implements Proable {
	private String filename;
	
	@Override
	public String getFilename() {
		return filename;
	}

	@Override
	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public void enforce() {
		coordinator.assignProTask(filename);
	}

	@Override
	public boolean check() {
		List<Resource> resources = ResourceFactory.makeResources(filename);
		for(Resource r : resources) {
			if(!r.getState().equals(coordinator.getProState(filename))) return false;
		}
		return true;
	}
}
