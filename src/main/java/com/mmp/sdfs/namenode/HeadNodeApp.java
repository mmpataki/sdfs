package com.mmp.sdfs.namenode;

import com.mmp.sdfs.conf.HeadNodeConfig;
import lombok.extern.slf4j.Slf4j;
import com.mmp.sdfs.rpc.RpcServer;

@Slf4j
public class HeadNodeApp extends RpcServer {

    HeadNodeService headNode;

    public static void main(String[] args) throws Exception {
        HeadNodeConfig conf = new HeadNodeConfig(args);
        if (conf.isHelp()) {
            System.out.println(conf.getHelpString());
            return;
        }
        log.info("Starting headnode...");
        log.info("Config: \n{}", conf);
        new HeadNodeApp(conf).start();
        log.info("Headnode started.");
    }

    public HeadNodeApp(HeadNodeConfig conf) throws Exception {
        super(conf.getNnPort(), conf.getRpcSerde());
        headNode = new HeadNodeService(conf);
        registerRpcProvider(headNode);
    }
}
