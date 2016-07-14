package com.shen.cloud.utils;

import java.util.List;

import com.shen.common.utils.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

/**
 * 基于curator客户端的示例。比原生zkclient好使
 */
public class ZkUtils {
	
	private static final String ZKHOST = "10.126.60.85:2181,10.126.60.86:2181,10.126.60.87:2181";
	
	public static CuratorFramework getClient(){
		CuratorFramework client = CuratorFrameworkFactory.newClient(ZKHOST,new RetryNTimes(3, 5000));
        client.start();
        System.out.println("zk client start ok ! id="+client);
		return client;
	}
	
	/**
	 * 判断节点是否存在,true:节点存在。false:不存在
	 * @param client
	 * @param path
	 * @return
	 */
	public static boolean checkExists(CuratorFramework client,String path){
		Stat stat = null;
		try {
			stat = client.checkExists().forPath(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null != stat;
	}

	/**
	 * 添加数据监听接口
	 * @param nodeCache
	 * @param listener
	 */
	public static void addNodeChangedListener(NodeCache nodeCache,NodeCacheListener listener){
        try {
			nodeCache.start(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
        nodeCache.getListenable().addListener(listener);
	}
	
	/**
	 * 对连接状态的监听，连接状态只是针对某个客户端的连接状态，和节点path都是没关系的，不要搞混。
	 * @param client
	 */
	public static void addConnectionStateListener(CuratorFramework client, ConnectionStateListener listener){
		client.getConnectionStateListenable().addListener(listener);
	}
    
    /**
     * 创建永久节点
     * @param client
     * @param path
     * @param data
     */
    public static void createPersistentNode(CuratorFramework client,String path, String data){
        try {
        	if(StringUtils.isBlank(data)){
        		client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
        	}else{
        		client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, data.getBytes());
        	}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 创建临时节点
     * @param client
     * @param path
     * @param data
     */
    public static void createEphemeralNode(CuratorFramework client,String path, String data){
        try {
        	if(StringUtils.isBlank(data)){
        		client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);
        	}else{
        		client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, data.getBytes());
        	}
		} catch (Exception e) {
			e.printStackTrace();
		}
        System.out.println("create EphemeralNode = "+path);
    }
    
    /**
     * 获取path下的所有节点
     * @param client
     * @param path
     * @return
     */
    public static List<String> getNodes(CuratorFramework client, String path){
    	List<String> nodes = null;
        try {
        	nodes = client.getChildren().forPath(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nodes;
    }
    
    /**
     * 取node节点数据
     * @param client
     * @param path
     * @return 
     */
    public static byte[] getNodeData(CuratorFramework client, String path){
    	byte[] datas = null;
        try {
        	datas = client.getData().forPath(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return datas;
    }
    
    /**
     * 取node节点数据string格式
     * @param client
     * @param path
     * @return 
     */
    public static String getNodeDataString(CuratorFramework client, String path){
    	String result = null;
    	byte[] datas = getNodeData(client,path);
    	if(null != datas){
    		result = new String((byte[]) datas);
    	}
		return result;
    }
    
    /**
     * 更新节点内容
     * @param client
     * @param path
     * @param data
     */
    public static void setNodeData(CuratorFramework client, String path, String data){
        try {
        	client.setData().forPath(path, data.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 级联删除节点
     * @param client
     * @param path
     */
    public static void deleteNode(CuratorFramework client, String path){
        try {
			client.delete().guaranteed().deletingChildrenIfNeeded().forPath(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	public static void main(String[] args) {
//		CuratorFramework client = ZkUtils.getClient();
//		ZkUtils.createEphemeralNode(client, "/dsp/linshi", "");
//		ZkUtils.testNodeChangedListener();
		ZkUtils.testStateListener();
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 创建一个临时节点，对该节点进行监控，如果该临时节点消失了，不会收到任何提示，只会在node数据更改时才会有提示。
	 */
	public static void testNodeChangedListener(){
		CuratorFramework client = ZkUtils.getClient();
		//建本地缓存
		final NodeCache nodeCache = new NodeCache(client, "/dsp/linshi", false);
		//注册监听
		ZkUtils.addNodeChangedListener(nodeCache, new NodeCacheListener(){
			@Override
			public void nodeChanged() throws Exception {
				System.out.println("Node data is changed, new data: " + new String(nodeCache.getCurrentData().getData()));
			}});
	}
	
	
	public static void testStateListener(){
		final CuratorFramework client = ZkUtils.getClient();
		ZkUtils.addConnectionStateListener(client, new ConnectionStateListener(){
			@Override
			public void stateChanged(CuratorFramework curatorFramework, ConnectionState newState) {
				System.out.println("new state = "+newState);
				if(newState == ConnectionState.LOST){
					while(true){
						try {
							System.err.println("我来了，嘿嘿");
							if(curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut()){
								System.out.println("rebuild node !!!");
								ZkUtils.createEphemeralNode(client, "/dsp/linshi", "newa");
								//TODO 重建临时节点
//								curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(zkRegPathPrefix, regContent.getBytes("UTF-8"));
								break;
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						} catch (Exception e){
							e.printStackTrace();
						}
					}
				}
			}});
	}
	
}

