package org.orchestra.coordinate;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.orchestra.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadStateTimerTask extends TimerTask implements Future<String> {
	private static final transient Logger logger = LoggerFactory.getLogger(ReadStateTimerTask.class);
	private CuratorFramework curator;
	private String statePath;
	private String config;
	private StateWatcher watcher;

	public ReadStateTimerTask(CuratorFramework curator, String statePath, String config) {
		this.curator = curator;
		this.statePath = statePath;
		this.config = config;
	}

	@Override
	public void run() {
		StateWatcher watcher = new StateWatcher(curator, statePath);
		this.watcher = watcher;
		//corner cases. 
		//1) The state path is added before the watcher is set
		//2) The uri path is deleted before it is locked
		try {
			logger.info("Registering state watcher");
			if(curator.checkExists().usingWatcher(watcher).forPath(statePath) != null) {
				//handle case 1)
				byte[] data = curator.getData().forPath(statePath);
				watcher.setState(new String(data));
			} else {
				String resourcePath = statePath.substring(0, statePath.lastIndexOf('/'));
				//handle case 2)
				byte[] data = curator.getData().forPath(resourcePath);
				String str = new String(data);
				if(!str.trim().endsWith("}")) {
//				System.out.println("Got data:" + str + "from " + resourcePath);
					curator.setData().forPath(resourcePath, config.getBytes());
				}
				String lockPath = resourcePath  + "/lock";
				InterProcessMutex lock = new InterProcessMutex(curator, lockPath);
				logger.info("acuiring lock");
				lock.acquire();
				logger.info("acuired lock");
				
				String readerPath = resourcePath + "/reader/" + Client.getName();
				if(curator.checkExists().forPath(readerPath) == null) {
					logger.info("creating path:" + readerPath);
					curator.create().creatingParentsIfNeeded().forPath(readerPath);
				}
				lock.release();
				logger.info("released lock");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean cancel(boolean arg0) {
		return cancel();
	}

	@Override
	public String get() throws InterruptedException, ExecutionException {
		while(watcher == null || watcher.getState(statePath) == null) {
			Thread.sleep(5);
		}
		String result = watcher.getState(statePath);
		watcher.removeState(statePath);
		return result;
	}

	@Override
	public String get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		long millis = unit.toMillis(timeout);
		long time = 0;
		while(watcher == null || watcher.getState(statePath) == null && time < millis) {
			time = time + 5;
			Thread.sleep(5);
		}
		String result = watcher.getState(statePath);
		watcher.removeState(statePath);
		return result;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled();
	}

	@Override
	public boolean isDone() {
		if(watcher == null) return false;
		return watcher.contains(statePath);
	}

}
