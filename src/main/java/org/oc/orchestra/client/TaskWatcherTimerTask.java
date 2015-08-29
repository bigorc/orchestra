package org.oc.orchestra.client;

import java.util.List;
import java.util.TimerTask;

import org.apache.curator.framework.CuratorFramework;
import org.oc.orchestra.coordinate.TaskWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskWatcherTimerTask extends TimerTask {
	private static final transient Logger logger = LoggerFactory.getLogger(TaskWatcherTimerTask.class);
	private CuratorFramework curator;

	public TaskWatcherTimerTask(CuratorFramework curator) {
		this.curator = curator;
	}

	@Override
	public void run() {
		logger.info("Registering task watcher");
		String taskClientPath = Client.getZkTaskClientPath();
		TaskWatcher watcher = new TaskWatcher(curator, taskClientPath);
		List<String> children;
		try {
			children = curator.getChildren().usingWatcher(
					watcher).forPath(taskClientPath);
			//tasks children might be added before watchers are registered
			//that's handled here
			watcher.handleTasks(children);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
