package com.mmp.sdfs.datanode;

import com.mmp.sdfs.conf.DataNodeConfig;
import lombok.extern.slf4j.Slf4j;
import com.mmp.sdfs.rpc.RpcServer;

@Slf4j
public class DatanodeApp extends RpcServer {

    public DatanodeApp(DataNodeConfig conf) throws Exception {
        super(conf.getPort(), conf.getRpcSerde());
        DataNodeService dn = new DataNodeService(conf);
        dn.start();
        registerRpcProvider(dn);
    }

    public static void main(String[] args) throws Exception {
        DataNodeConfig conf = new DataNodeConfig(args);
        if (conf.isHelp()) {
            System.out.println(conf.getHelpString());
            return;
        }
        log.info("Starting datanode");
        log.info("Datanode config: \n{}", conf);
        new DatanodeApp(conf).start();
        log.info("Datanode started");
    }
}
