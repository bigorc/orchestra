package org.orchestra.coordinate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.orchestra.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateWatcher implements CuratorWatcher {
	private static final transient Logger logger = LoggerFactory.getLogger(StateWatcher.class);
	private CuratorFramework client;
	private String statePath;
	private static Map<String, String> states = new HashMap<String, String>();
	
	public StateWatcher(CuratorFramework client, String statePath) {
		this.client = client;
		this.statePath = statePath;
	}
	
	@Override
	public void process(WatchedEvent event) throws Exception {
		if(event.getType() == EventType.NodeDeleted) return;
//		String statePath = event.getPath();
		String path = statePath.substring(0, statePath.lastIndexOf('/'));
		String lockPath = path  + "/lock";
		InterProcessMutex lock = new InterProcessMutex(client, lockPath);
		logger.info("acuiring lock");
		lock.acquire();
		logger.info("acuired lock");

		String readers = path + "/reader";
		String readerPath = readers + "/" + Client.getName();
		
		byte[] data = client.getData().forPath(statePath);
		states.put(statePath, new String(data));
		
		if(client.checkExists().forPath(readerPath) != null) {
			logger.info("deleting path:" + readerPath);
			client.delete().forPath(readerPath);
		}
		//might need to change this,because the client might not have the permission to delete
		if(client.getChildren().forPath(readers).size() == 0) {
			client.delete().deletingChildrenIfNeeded().forPath(path);
		}
		lock.release();
		logger.info("released lock");
	}

	public static String getState(String statePath) {
		return states.get(statePath);
	}

	public void removeState(String statePath) {
		states.remove(statePath);
	}

	public void setState(String state) {
		states.put(statePath, state);
	}

	public boolean contains(String statePath) {
		return states.keySet().contains(statePath);
	}
}
