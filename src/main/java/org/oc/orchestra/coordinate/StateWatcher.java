package org.oc.orchestra.coordinate;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateWatcher implements CuratorWatcher, Future<String> {
	private static final transient Logger logger = LoggerFactory.getLogger(StateWatcher.class);
	private CuratorFramework client;
	private String wid;
	private static Map<String, String> states = new HashMap<String, String>();
	
	public StateWatcher(CuratorFramework client) {
		this.client = client;
		this.wid = UUID.randomUUID().toString();
	}
	
	@Override
	public void process(WatchedEvent event) throws Exception {
		if(event.getType() == EventType.NodeDeleted) return;
		String statePath = event.getPath();
		String path = statePath.substring(0, statePath.lastIndexOf('/'));
		String lockPath = path  + "/lock";
		InterProcessMutex lock = new InterProcessMutex(client, lockPath);
		logger.info("acuiring lock");
		lock.acquire();
		logger.info("acuired lock");
		
		String countPath = path + "/count";
		SharedCount count = new SharedCount(client, countPath , 0);
		count.start();
		
		byte[] data = client.getData().forPath(statePath);
		states.put(wid, new String(data));
		int counter = count.getCount();
		if(counter > 1) {
			counter = counter - 1;
			count.setCount(counter);
			count.close();
			lock.release();
			logger.info("released lock");
		} else {
			logger.info("deleting path:" + path);
			count.close();
			client.delete().deletingChildrenIfNeeded().forPath(path);
			lock.release();
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public String get() throws InterruptedException, ExecutionException {
		while(states.get(wid) == null) {
			Thread.sleep(5);
		}
		String result = states.get(wid);
		states.remove(wid);
		return result;
	}

	@Override
	public String get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		long millis = unit.toMillis(timeout);
		long time = 0;
		while(states.get(wid) == null && time < millis) {
			time = time + 5;
			Thread.sleep(5);
		}
		String result = states.get(wid);
		states.remove(wid);
		return result;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return states.keySet().contains(wid);
	}

	public void set(String state) {
		states.put(wid, state);
	}
}
