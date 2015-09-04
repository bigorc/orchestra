package org.oc.orchestra.coordinate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskWatcher implements CuratorWatcher {
	private static final transient Logger logger = LoggerFactory.getLogger(TaskWatcher.class);
	private CuratorFramework curator;
	private String taskClientPath;
	static Map<String, Object> locks = new HashMap<String, Object>();
	
	public TaskWatcher(CuratorFramework curator, String taskClientPath) {
		this.curator = curator;
		this.taskClientPath = taskClientPath;
	}
	
	@Override
	public void process(WatchedEvent event) throws Exception {
		List<String> tasks;
		logger.info("received task event for " + event.getType() + event.getPath());
		tasks = curator.getChildren().usingWatcher(this).forPath(taskClientPath);
		handleTasks(tasks);
	}

	public void handleTasks(List<String> tasks) {
		if(tasks == null || tasks.size() == 0) return;
		for(String task : tasks) {
			String taskPath = taskClientPath + "/" + task;
			synchronized(locks) {
				if(locks.containsKey(taskPath)) {
					return;
				} else {
					locks.put(taskPath, null);
				}
			}
			//check if the task is done
			try {
				if(curator.checkExists().forPath(taskPath + "/" + "taskStatus") == null) {
					String config = new String(curator.getData().forPath(taskPath));
					logger.info("Config:" + config);
					JSONObject json = (JSONObject) JSONValue.parse(config);
					Resource resource = ResourceFactory.makeResource(json);
					resource.realize();
					curator.create().forPath(taskPath + "/" + "taskStatus", "done".getBytes());
				}
			} catch(NoNodeException | NodeExistsException e) {
				//
			} catch (Exception e) {
				String errmsg = "";
				for( StackTraceElement stack : e.getStackTrace()) {
					errmsg += stack.toString() + "\n";
				}
				logger.error(errmsg);
				try {
					curator.create().forPath(taskPath + "/" + "taskStatus", ("error:" + errmsg).getBytes());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			} finally {
				locks.remove(taskPath);
			}
		}
	}
}