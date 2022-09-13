package com.mmp.sdfs.datanode;

import com.mmp.sdfs.conf.DataNodeConfig;
import lombok.extern.slf4j.Slf4j;
import com.mmp.sdfs.nndnrpc.DnHeartbeat;
import com.mmp.sdfs.common.ProxyFactory;

import java.io.File;

@Slf4j
public class HeartBeater {

    private final DataNodeConfig conf;
    private final ProxyFactory proxyFactory;

    File blockDir;
    String myId;

    public HeartBeater(DataNodeConfig conf, String id) {
        this.conf = conf;
        this.proxyFactory = new ProxyFactory(conf);
        blockDir = new File(conf.getBlockDir());
        this.myId = id;
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    proxyFactory.getNNProxy().heartBeat(getHeartBeatPayload());
                    Thread.sleep(5 * 1000);
                } catch (Exception e) {
                    log.error("Exception while heartbeating", e);
                }
            }
        }).start();
    }

    private DnHeartbeat getHeartBeatPayload() {
        DnHeartbeat dpl = new DnHeartbeat();
        dpl.setBlocks(blockDir.list().length);
        dpl.setId(myId);
        dpl.setPort(conf.getPort());
        dpl.setDataPort(conf.getDataPort());
        return dpl;
    }

}
