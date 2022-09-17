package com.mmp.sdfs.namenode;

import com.mmp.sdfs.common.*;
import com.mmp.sdfs.conf.HeadNodeConfig;
import com.mmp.sdfs.nndnrpc.DnHeartbeat;
import com.mmp.sdfs.nndnrpc.DnHeartbeatResponse;
import com.mmp.sdfs.nndnrpc.HeadNode;
import com.mmp.sdfs.rpc.RpcContext;
import com.mmp.sdfs.utils.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class HeadNodeService implements HeadNode {

    private final HeadNodeConfig conf;
    Map<String, DnRef> dns = new HashMap<>();
    NameStore store;
    HttpServer httpServer;
    Scheduler scheduler;

    public HeadNodeService(HeadNodeConfig conf) throws Exception {
        this.conf = conf;
        if (!new File(conf.getNamedir()).exists()) {
            Files.createDirectory(Paths.get(conf.getNamedir()));
        }
        store = (NameStore) Class.forName(conf.getStoreClass()).getConstructor(HeadNodeConfig.class, Map.class).newInstance(conf, dns);

        new BlockDeleter(conf, store, dns).start();

        httpServer = new HttpServer(conf.getInfoPort());
        httpServer.registerController(this);
        httpServer.start();

        scheduler = new Scheduler(dns, conf);
        scheduler.start();
    }

    public DnHeartbeatResponse heartBeat(DnHeartbeat dnRegister) {
        dns.put(dnRegister.getId(), new DnRef(dnRegister, RpcContext.getRpcContext().getSock().getInetAddress().getHostAddress()));
        dnRegister.getTaskStates().forEach((t, s) -> {
            scheduler.taskFinished(t, s);
        });
        return new DnHeartbeatResponse();
    }

    public FileStat create(String path) throws Exception {
        return store.create(path, "anonymous", conf.getReplicationFactor());
    }

    public LocatedBlock addBlock(String path) throws Exception {
        return store.addBlock(path, "anonymous");
    }

    public List<LocatedBlock> getBlocks(String path) throws Exception {
        return store.getBlocks(path);
    }

    public List<FileStat> list(String dirPath) throws Exception {
        return store.list(dirPath);
    }

    public void delete(String path) throws Exception {
        store.delete(path);
    }

    @Override
    public void closeFile(String path, long size) throws Exception {
        store.close(path, size);
    }

    @Override
    public String submitJob(Job job) throws Exception {
        return scheduler.schedule(job);
    }

    @Override
    public List<JobState> getStatusOf(String[] jobIds) throws Exception {
        return Arrays.stream(jobIds).map(id -> scheduler.getJobStates().get(id)).collect(Collectors.toList());
    }

    @Override
    public FileStat get(String path) throws Exception {
        return store.get(path);
    }

    // info api
    @HttpServer.Api("/nodes")
    Object getDataNodeInfo(Map<String, String> params) {
        if (params.containsKey("id"))
            return dns.get(params.get("id"));
        return dns;
    }

    @HttpServer.Api("/jobs")
    Object getJobsStates(Map<String, String> params) {
        Map<String, JobState> jobs = scheduler.getJobStates();
        if (params.containsKey("id"))
            return jobs.get(params.get("id"));
        if (params.containsKey("nodeid")) {
            String node = params.get("nodeid");
            Map<String, JobState> ret = new HashMap<>();
            jobs.values().stream()
                    .filter(j -> j.getTaskStates().values().stream().anyMatch(t -> t.getNode().equals(node)))
                    .forEach(j -> ret.put(j.getJobId(), j));
            return ret;
        }
        return jobs;
    }

}
