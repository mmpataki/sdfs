package com.mmp.sdfs.workernode;

import com.mmp.sdfs.conf.SdfsClientConfig;
import com.mmp.sdfs.conf.WorkerNodeConfig;
import lombok.extern.slf4j.Slf4j;
import com.mmp.sdfs.rpc.RpcServer;

@Slf4j
public class WorkerNodeApp extends RpcServer {

    public WorkerNodeApp(WorkerNodeConfig conf, SdfsClientConfig clientConfig) throws Exception {
        super(conf.getPort(), conf.getRpcSerde());
        WorkerNodeService dn = new WorkerNodeService(conf, clientConfig);
        dn.start();
        registerRpcProvider(dn);
    }

    public static void main(String[] args) throws Exception {
        WorkerNodeConfig conf = new WorkerNodeConfig(args);
        SdfsClientConfig cliConf = new SdfsClientConfig(args);
        if (conf.isHelp()) {
            System.out.println(conf.getHelpString());
            return;
        }
        log.info("Starting datanode");
        log.info("Datanode config: \n{}", conf);
        new WorkerNodeApp(conf, cliConf).start();
        log.info("Datanode started");
    }
}
