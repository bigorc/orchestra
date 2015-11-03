package org.orchestra.constraint;

import java.util.List;

import org.orchestra.ResourceFactory;
import org.orchestra.coordinate.Coordinator;
import org.orchestra.resource.Resource;

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
