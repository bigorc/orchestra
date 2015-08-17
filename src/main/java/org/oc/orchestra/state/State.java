package org.oc.orchestra.state;

import org.oc.orchestra.resource.Resource;

public interface State {
	void apply(Resource resource) throws StateException;
}
