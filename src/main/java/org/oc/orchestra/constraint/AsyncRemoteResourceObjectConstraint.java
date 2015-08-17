package org.oc.orchestra.constraint;

import java.util.List;

import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.coordinate.Coordinator;
import org.oc.orchestra.resource.Resource;

public class AsyncRemoteResourceObjectConstraint extends
		DistributedSMConstraint implements Proable {
	private String filename;
	
	@Override
	public void enforce() {
		coordinator.asyncAssignProTask(filename);
	}

	@Override
	public boolean check() {
		List<Resource> resources = ResourceFactory.makeResources(filename);
		for(Resource r : resources) {
			if(!coordinator.getProState(filename).equals(r.getState())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getFilename() {
		return filename;
	}

	@Override
	public void setFilename(String filename) {
		this.filename = filename;
	}

}
