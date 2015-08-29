package org.oc.orchestra.coordinate;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.oc.orchestra.ResourceFactory;
import org.oc.orchestra.client.Client;
import org.oc.orchestra.resource.GeneralResource;
import org.oc.orchestra.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class Curator implements Coordinator {
	private static final transient Logger logger = LoggerFactory.getLogger(Curator.class); 
	protected static String connectString = "localhost:2181";
	public static int baseSleepTimeMs = 1000;
	public static int maxRetries = 3;
	public static String basePath = "/orchestra";
	public static byte[] baseData = "Orchestra Root Node".getBytes();
	private static Curator instance;
	protected CuratorFramework curator;
	protected String client;
	protected static String baseTaskPath = basePath + "/tasks";
	protected static String baseResourcePath = basePath + "/resources";
	protected String clientTaskPath;
	protected String clientResourcePath;
	private int timeout;
	
	public static Curator getInstance() {
		if(instance == null) {
			instance = new Curator(connectString);
		}
		return instance;
	}
	
	public Curator(String connectString) {
		RetryPolicy retryPolicy =  new ExponentialBackoffRetry(baseSleepTimeMs , maxRetries );
		curator = CuratorFrameworkFactory.newClient(connectString , retryPolicy );
		curator.start();
		
		try {
			if(curator.checkExists().forPath(basePath) == null) {
				curator.create().forPath(basePath, baseData);
			}
			if(curator.checkExists().forPath(baseTaskPath) == null) {
				curator.create().forPath(baseTaskPath, "todo node".getBytes());
			}
			if(curator.checkExists().forPath(baseResourcePath) == null) {
				curator.create().forPath(baseResourcePath, "resource node".getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Curator(String connectString, String client) {
		this(connectString);
		this.client = client;
		this.clientResourcePath = baseResourcePath + "/" + client;
		this.clientTaskPath = baseTaskPath + "/" + client;
	}
	
	@Override
	public String getSMState(String type, List<String> args) {
		GeneralResource ge = new GeneralResource();
		ge.setStateMachine(type);
		ge.setArgs(getArg(args));
		return getState(ge.uri(), ge.toRO());
	}

	@Override
	public String getProState(String pro) {
		List<Resource> resources = ResourceFactory.makeResources(pro);
		return getState(resources.get(0).uri(), resources.get(0).toRO());
	}

	protected String zkPathize(String pro) {
		return '\"' + pro.replace('/', '\\') + '\"';
	}

	protected String unZkPathize(String path) {
		return path.substring(1, path.length()-1).replace('\\', '/');
	}
	
	@Override
	public Future<String> asyncGeState(String uri, String config) {
		try {
			String resourcePath = baseResourcePath + "/" + client + "/" + uri;
			
			curator.create().forPath(resourcePath, config.getBytes());
			return readState(uri, config);
		} catch (KeeperException.NodeExistsException e) {
			return readState(uri, config);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Future<String> readState(String uri, String config) {
		String resourcePath = baseResourcePath + "/" + client + "/" + uri;
		String statePath = resourcePath + "/state";
		try {
			String lockPath = resourcePath + "/lock";
			InterProcessMutex lock = new InterProcessMutex(curator, lockPath);
			
			logger.info("acuiring lock");
			lock.acquire();
			logger.info("acuired lock");
			StateWatcher watcher = new StateWatcher(curator);
			//corner cases. 
			//1) The state path is added before the watcher is set
			//2) The uri path is deleted before it is locked
			if(curator.checkExists().usingWatcher(watcher).forPath(statePath) != null) {
				//handle case 1)
				logger.info("Corner case 1");
				byte[] data = curator.getData().forPath(statePath);
				watcher.set(new String(data));
			} else {
				//handle case 2)
				byte[] data = curator.getData().forPath(resourcePath);
				String str = new String(data);
				if(!str.trim().endsWith("}")) {
//					System.out.println("Got data:" + str + "from " + resourcePath);
					curator.setData().forPath(resourcePath, config.getBytes());
				}
				String countPath = resourcePath + "/count";
				SharedCount count = new SharedCount(curator, countPath , 0);
				count.start();
				int counter = count.getCount();
				counter = counter + 1;
				logger.info("Counter:" + counter);
				count.setCount(counter);
				count.close();
			}
			lock.release();
			logger.info("released lock");
			return watcher;
		} catch (Exception e) {
			
		}
		return null;
	}
	
	@Override
	public String getState(String uri, String config) {
		Future<String> result = asyncGeState(uri, config);
		try {
			if(result != null) {
				if(timeout == 0) return result.get();
				return result.get(timeout, TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Future<String> asyncGetSMState(String type, List<String> args) {
		GeneralResource ge = new GeneralResource();
		ge.setStateMachine(type);
		ge.setArgs(getArg(args));
		return asyncGeState(ge.uri(), ge.toRO());
	}

	private String getArg(List<String> args) {
		return getArg(args, ' ').toString();
	}
	
	private StringBuffer getArg(List<String> args, char link) {
		StringBuffer arg = new StringBuffer();
		for(String a : args) {
			arg.append(a);
			arg.append(link);
		}
		arg.deleteCharAt(arg.length() - 1);
		return arg;
	}

	@Override
	public Future<String> asyncGetProState(String pro) {
		List<Resource> resources = ResourceFactory.makeResources(pro);
		return asyncGeState(resources.get(0).uri(), resources.get(0).toRO());
	}
	
	@Override
	public boolean assignSMTask(String type, List<String> args, String state) {
		GeneralResource gs = new GeneralResource();
		gs.setArgs(getArg(args));
		gs.setState(state);
		gs.setStateMachine(type);
		String task = generateTask(gs.uri());
		return assignTask(task, gs.toRO());
	}
	
	@Override
	public void assignResourceTask(Resource resource) {
		String task = generateTask(resource.uri());
		assignTask(task, resource.toRO());
	}

	@Override
	public void asyncAssignResourceTask(Resource resource) {
		String task = generateTask(resource.uri());
		asyncAssignTask(task, resource.toRO());
	}
	
	@Override
	public boolean assignProTask(String pro) {
		List<Resource> resources = ResourceFactory.makeResources(pro);
		return assignTask(generateTask("task_" + resources.get(0).getClass().getName()), 
				resources.get(0).toRO());
	}

	private String generateTask(String taskType) {
		DateTime current= new DateTime();
		String timestamp = current.toString("yyyyMMddHHmmss");
		return taskType + "_" + timestamp;
	}

	public boolean assignTask(String task, String config) {
		asyncAssignTask(task, config);
		String result = null;
		try {
			String taskPath = clientTaskPath + "/" + task;
			String statusPath = taskPath + "/taskStatus";
			while(curator.checkExists().forPath(statusPath) == null) {
				Thread.sleep(5);
			}
			byte[] data = curator.getData().forPath(statusPath);
			result = new String(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		cleanupTask(task);
		return "done".equals(result);
	}
	
	private void cleanupTask(String task) {
		try {
			String taskPath = clientTaskPath + "/" + task;
			curator.delete().deletingChildrenIfNeeded().forPath(taskPath);
		} catch(NoNodeException e) {
			//
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String asyncAssignTask(String task, String config) {
		return addTask(config.getBytes(), clientTaskPath + "/" + task);
	}
	
	@Override
	public String asyncAssignSMTask(String type, List<String> args, String state) {
		GeneralResource gr = new GeneralResource();
		gr.setStateMachine(type);
		gr.setArgs(getArg(args));
		gr.setState(state);
		return asyncAssignTask(generateTask("task_" + type), gr.toRO());
	}

	@Override
	public String asyncAssignProTask(String pro) {
		List<Resource> resources = ResourceFactory.makeResources(pro);
		return asyncAssignTask(generateTask("task_" + resources.get(0).getClass().getName()), 
				resources.get(0).toRO());
	}

	public String addTask(byte[] data, String taskPath) {
		try {
			if(curator.checkExists().forPath(taskPath) == null) {
				curator.create().forPath(taskPath, data);
				return taskPath;
			} else {
				int i = taskPath.lastIndexOf("_");
				int ind;
				String root;
				if(i < taskPath.length() - 14) {
					ind = 1;
					root = taskPath;
				} else {
					String index = taskPath.substring(i, taskPath.length());
					root = taskPath.substring(0, i);
					ind = Integer.parseInt(index.substring(1)) + 1;
				}
				return addTask(data, taskPath + "_" + ind);
			}
		} catch (KeeperException.NodeExistsException e) {
//			logger.debug("Node exists when assigning task + " + taskPath);
			return addTask(data, taskPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getClient() {
		return client;
	}

	@Override
	public void setClient(String client) {
		this.client = client;
		this.clientResourcePath = baseResourcePath + "/" + client;
		this.clientTaskPath = baseTaskPath + "/" + client;
	}

	public static String getConnectString() {
		return connectString;
	}

	public static void setConnectString(String connectString) {
		Curator.connectString = connectString;
	}

}
