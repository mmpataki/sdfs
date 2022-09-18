package com.mmp.sdfs.workernode;

import com.mmp.sdfs.common.ProxyFactory;
import com.mmp.sdfs.conf.WorkerNodeConfig;
import com.mmp.sdfs.hnwnrpc.DNState;
import com.mmp.sdfs.hnwnrpc.DnProfile;
import com.mmp.sdfs.hnwnrpc.UnknownDnException;
import com.mmp.sdfs.utils.Pair;
import lombok.SneakyThrows;
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

    long[] prevTicks;

    DnProfile profile;
    DNState state = new DNState();
    HardwareAbstractionLayer hal;
    OperatingSystem os;
    CentralProcessor processor;

    public HeartBeater(WorkerNodeConfig conf, String id, Map<String, Integer> taskStates) throws Exception {
        this.conf = conf;
        this.proxyFactory = new ProxyFactory(conf);
        blockDir = new File(conf.getBlockDir());
        this.myId = id;
        this.taskStates = taskStates;

        SystemInfo si = new SystemInfo();
        hal = si.getHardware();
        os = si.getOperatingSystem();
        processor = hal.getProcessor();
        prevTicks = processor.getSystemCpuLoadTicks();

        profile = new DnProfile();
        profile.setPort(conf.getPort());
        profile.setInfoPort(conf.getInfoPort());
        profile.setDataPort(conf.getDataPort());
        profile.setCores(processor.getLogicalProcessorCount());
        profile.setOs(si.getOperatingSystem().getFamily() + " " + si.getOperatingSystem().getVersionInfo());
        profile.setMemorySize(hal.getMemory().getTotal());
        profile.setId(id);

        register();

        state.setId(myId);
        state.setTaskStates(taskStates);
    }

    private void register() throws Exception {
        log.info("Registering with headnode with profile: {}", profile);
        proxyFactory.getHNProxy().register(profile);
        log.info("Registered!");
    }

    public void start() {
        new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    try {
                        proxyFactory.getHNProxy().heartBeat(getHeartBeatPayload());
                        List<String> remove = new ArrayList<>();
                        taskStates.forEach((k, v) -> {
                            if (v != -9999)
                                remove.add(k);
                        });
                        remove.forEach(taskStates::remove);
                    } catch (UnknownDnException ukdn) {
                        try {
                            register();
                        } catch (Exception e) {
                            log.error("Registering failed, going to try indefinitely");
                        }
                    } catch (Exception e) {
                        log.error("Exception while heartbeating", e);
                    }
                    Thread.sleep(conf.getHeartBeatInterval());
                }
            }
        }).start();
    }

    private DNState getHeartBeatPayload() {
        state.setBlocks(blockDir.list().length);
        state.setMemoryAvailable(hal.getMemory().getAvailable());
        state.setCpuPercent(processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100);
        prevTicks = processor.getSystemCpuLoadTicks();
        state.setLoadAvg(processor.getSystemLoadAverage(3));
        Map<String, Pair<Long, Long>> dMap = new HashMap<>();
        os.getFileSystem().getFileStores().forEach(fs -> dMap.put(fs.getMount(), new Pair<>(fs.getTotalSpace(), fs.getFreeSpace())));
        state.setDisks(dMap);
        return state;
    }

}
