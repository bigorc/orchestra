package org.orchestra.coordinate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.orchestra.ResourceFactory;
import org.orchestra.client.Client;
import org.orchestra.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceWatcher implements CuratorWatcher {
	private static final transient Logger logger = LoggerFactory.getLogger(ResourceWatcher.class);
	private CuratorFramework curator;
	private String clientResourcePath;
	static Map<String, Timer> timers = new HashMap<String, Timer>();
	static Map<String, Object> locks = new HashMap<String, Object>();
	static int CLEANUP_TIME = 10000;
	
	public ResourceWatcher(CuratorFramework curator, String clientResourcePath) {
		this.curator = curator;
		this.clientResourcePath = clientResourcePath;
	}
	
	public void handleResources(List<String> children) {
		if(children == null || children.size() == 0) return;
		synchronized(timers) {
			for(String k : timers.keySet()) {
				if(!children.contains(k)) {
					timers.get(k).cancel();
					timers.remove(k);
					logger.info("timer canceled " + k);
				}
			}
		}
		
		for(String uri : children) {
			byte[] data = null;
			String uriPath = clientResourcePath + "/" + uri;
			try {
				synchronized(locks) {
					if(!locks.containsKey(uriPath)) {
						locks.put(uriPath, null);
					} else {
						return;
					}
				}
				data = curator.getData().forPath(uriPath);
				String statusPath = uriPath + "/" + "state";
				if(curator.checkExists().forPath(statusPath) == null) {
					String config = new String(data);
					logger.info("Config:" + config);
					Resource resource = ResourceFactory.makeResource(
							(JSONObject) JSONValue.parse(config));
					String state = resource.getCurrentState();
					curator.create().forPath(statusPath , state.getBytes());
					Timer timer = new Timer();
					timer.schedule(new CleanupTask(curator, uriPath), CLEANUP_TIME);
					timers.put(uriPath, timer);
				}
			} catch(KeeperException.NodeExistsException e) {
				//
			} catch(KeeperException.NoNodeException e) {
				//
			}catch (Exception e) {
				e.printStackTrace();
			} finally {
				locks.remove(uriPath);
			}
		}
	}

	@Override
	public void process(WatchedEvent event) throws Exception {
		logger.info("received watch event " + event.getType() + " for " + event.getPath());
		List<String> children = curator.getChildren().usingWatcher(
				this).forPath(clientResourcePath);
		handleResources(children);
	}

	class CleanupTask extends TimerTask {
		
		private CuratorFramework curator;
		private String uriPath;

		public CleanupTask(CuratorFramework curator, String uriPath) {
			this.curator = curator;
			this.uriPath = uriPath;
		}

		@Override
		public void run() {
			try {
				logger.info("Cleanup task triggered for " + uriPath);
				timers.remove(uriPath);
				curator.delete().deletingChildrenIfNeeded().forPath(uriPath);
			} catch (KeeperException.NoNodeException e) {
				//
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
		
	}
}
