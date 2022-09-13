package com.mmp.sdfs.namenode;

import com.mmp.sdfs.conf.NameNodeConfig;
import lombok.extern.slf4j.Slf4j;
import com.mmp.sdfs.rpc.RpcServer;

@Slf4j
public class NamenodeApp extends RpcServer {

    NameNodeService namenode;

    public static void main(String[] args) throws Exception {
        NameNodeConfig conf = new NameNodeConfig(args);
        if (conf.isHelp()) {
            System.out.println(conf.getHelpString());
            return;
        }
        log.info("Starting namenode...");
        log.info("Config: \n{}", conf);
        new NamenodeApp(conf).start();
        log.info("Namenode started.");
    }

    public NamenodeApp(NameNodeConfig conf) throws Exception {
        super(conf.getNnPort(), conf.getRpcSerde());
        namenode = new NameNodeService(conf);
        registerRpcProvider(namenode);
    }
}
