package org.oc.orchestra.client;

import java.util.List;
import java.util.TimerTask;

import org.apache.curator.framework.CuratorFramework;
import org.oc.orchestra.coordinate.ResourceWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceWatcherTimerTask extends TimerTask {
	private static final transient Logger logger = LoggerFactory.getLogger(ResourceWatcherTimerTask.class);
	private CuratorFramework curator;

	public ResourceWatcherTimerTask(CuratorFramework curator) {
		this.curator = curator;
	}

	@Override
	public void run() {
		logger.info("Registering resource watcher");
		try {
			String clientResourcePath = Client.getZkClientResourcePath();
			ResourceWatcher watcher = new ResourceWatcher(curator, clientResourcePath);
			List<String> children = curator.getChildren().usingWatcher(
					watcher).forPath(clientResourcePath);
			//read task children might be added before watchers are registered
			//that's handled here
			watcher.handleResources(children);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
