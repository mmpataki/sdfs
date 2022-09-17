package com.mmp.sdfs.datanode;

import com.mmp.sdfs.common.ProxyFactory;
import com.mmp.sdfs.conf.WorkerNodeConfig;
import com.mmp.sdfs.nndnrpc.DnHeartbeat;
import com.mmp.sdfs.nndnrpc.SysInfo;
import com.mmp.sdfs.utils.Pair;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class HeartBeater {

    private final WorkerNodeConfig conf;
    private final ProxyFactory proxyFactory;
    private final Map<String, Integer> taskStates;

    File blockDir;
    String myId;

    DnHeartbeat dpl = new DnHeartbeat();
    HardwareAbstractionLayer hal;
    OperatingSystem os;

    public HeartBeater(WorkerNodeConfig conf, String id, Map<String, Integer> taskStates) {
        this.conf = conf;
        this.proxyFactory = new ProxyFactory(conf);
        blockDir = new File(conf.getBlockDir());
        this.myId = id;
        this.taskStates = taskStates;

        dpl.setId(myId);
        dpl.setPort(conf.getPort());
        dpl.setInfoPort(conf.getInfoPort());
        dpl.setDataPort(conf.getDataPort());
        dpl.setTaskStates(taskStates);

        SystemInfo si = new SystemInfo();
        hal = si.getHardware();
        os = si.getOperatingSystem();
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    proxyFactory.getNNProxy().heartBeat(getHeartBeatPayload());
                    List<String> remove = new ArrayList<>();
                    taskStates.forEach((k, v) -> {
                        if (v != -9999)
                            remove.add(k);
                    });
                    remove.forEach(taskStates::remove);
                    Thread.sleep(5 * 1000);
                } catch (Exception e) {
                    log.error("Exception while heartbeating", e);
                }
            }
        }).start();
    }

    private DnHeartbeat getHeartBeatPayload() {
        dpl.setBlocks(blockDir.list().length);
        dpl.setSysInfo(makeSysInfo());
        return dpl;
    }

    private SysInfo makeSysInfo() {
        SysInfo sys = new SysInfo();

        sys.setMemorySize(hal.getMemory().getTotal());
        sys.setMemoryAvailable(hal.getMemory().getAvailable());

        CentralProcessor processor = hal.getProcessor();

        sys.setNumCores(processor.getLogicalProcessorCount());
        sys.setCpuPercent(processor.getSystemCpuLoadBetweenTicks(processor.getSystemCpuLoadTicks()) * 100);
        sys.setLoadAvg(processor.getSystemLoadAverage(3));

        Map<String, Pair<Long, Long>> dMap = new HashMap<>();
        os.getFileSystem().getFileStores().forEach(fs -> dMap.put(fs.getMount(), new Pair<>(fs.getTotalSpace(), fs.getFreeSpace())));
        sys.setDisks(dMap);

        return sys;
    }
}
