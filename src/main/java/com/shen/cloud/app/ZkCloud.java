package com.shen.cloud.app;

import com.shen.cloud.Constant;
import org.apache.log4j.PropertyConfigurator;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * 嵌入式启动zk集群的方法，其实默认搭建一个zk即可。
 * Created by shenluguo on 2015/7/15.
 */
public class ZkCloud {

    private static Logger logger = LoggerFactory.getLogger(ZkCloud.class);

    public static void main(String[] args) {
//        QuorumPeerMain zkServer = new QuorumPeerMain();
        QuorumPeerConfig config = new QuorumPeerConfig();


        boolean production_mode = "true".equals(System.getProperty(Constant.PRODUCTION_MODE)) ? true : false;

        Properties properties = new Properties();
        if(production_mode){
            System.setProperty("hazelcast.logging.type","slf4j");
            properties.put("log4j.rootCategory", "info,R");
            properties.put("log4j.category.com.shen", "debug");
            properties.put("log4j.category.org.apache.zookeeper.server.quorum.FastLeaderElection", "error");
            properties.put("log4j.appender.R", "org.apache.log4j.RollingFileAppender");
            properties.put("log4j.appender.R.layout", "org.apache.log4j.PatternLayout");
            properties.put("log4j.appender.R.layout.ConversionPattern", "[cloud]%d{yyyy-MM-dd HH:mm:ss} %p %c{1}:%L - %m%n");
            properties.put("log4j.appender.R.File", "${server.home}/log/xiu-cloud.log");
            properties.put("log4j.appender.R.MaxFileSize", "10000KB");
            properties.put("log4j.appender.R.MaxBackupIndex", "10");
        }
        PropertyConfigurator.configure(properties);

        InputStream in = null ;
        if(!production_mode){
            in = ZkCloud.class.getResourceAsStream("/servers.properties");
        }else{
            try {
                in = new FileInputStream(System.getProperty("server.home")+ File.separator+"conf"+File.separator+"servers.properties");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

//        InputStream is = ZkCloud.class.getResourceAsStream("/servers.properties");
        Properties props = new Properties();
        try {
            props.load(in);
            config.parseProperties(props);
//            zkServer.runFromConfig(config);
            ServerCnxnFactory e = ServerCnxnFactory.createFactory();
            e.configure(config.getClientPortAddress(), config.getMaxClientCnxns());
            QuorumPeer quorumPeer = new QuorumPeer();
            quorumPeer.setClientPortAddress(config.getClientPortAddress());
            quorumPeer.setTxnFactory(new FileTxnSnapLog(new File(config.getDataLogDir()), new File(config.getDataDir())));
            quorumPeer.setQuorumPeers(config.getServers());
            quorumPeer.setElectionType(config.getElectionAlg());
            quorumPeer.setMyid(config.getServerId());
            quorumPeer.setTickTime(config.getTickTime());
            quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
            quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
            quorumPeer.setInitLimit(config.getInitLimit());
            quorumPeer.setSyncLimit(config.getSyncLimit());
            quorumPeer.setQuorumVerifier(config.getQuorumVerifier());
            quorumPeer.setCnxnFactory(e);
            quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
            quorumPeer.setLearnerType(config.getPeerType());
            quorumPeer.setSyncEnabled(config.getSyncEnabled());
            quorumPeer.setQuorumListenOnAllIPs(config.getQuorumListenOnAllIPs().booleanValue());
            quorumPeer.start();
            quorumPeer.join();
        } catch (Exception e) {
            logger.error("zk start error ",e);
            e.printStackTrace();
        }finally {
            if(null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
