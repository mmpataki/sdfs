package com.mmp.sdfs.headnode;

import com.mmp.sdfs.common.FileStat;
import com.mmp.sdfs.common.Job;
import com.mmp.sdfs.common.JobState;
import com.mmp.sdfs.common.LocatedBlock;
import com.mmp.sdfs.conf.HeadNodeConfig;
import com.mmp.sdfs.hnwnrpc.*;
import com.mmp.sdfs.rpc.RpcContext;
import com.mmp.sdfs.utils.HttpServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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

        new DnMonitor().start();

        httpServer = new HttpServer(conf.getInfoPort());
        httpServer.registerController(this);
        httpServer.start();

        scheduler = new Scheduler(dns, conf);
        scheduler.start();
    }

    @Override
    public void register(DnProfile profile) {
        dns.put(profile.getId(), new DnRef(profile, RpcContext.getRpcContext().getSock().getInetAddress().getHostAddress()));
    }

    @Override
    public DnHeartbeatResponse heartBeat(DNState dnState) throws UnknownDnException {
        if (!dns.containsKey(dnState.getId()))
            throw new UnknownDnException();
        DnRef dnRef = dns.get(dnState.getId());
        dnRef.setState(dnState);
        dnRef.setLastHeartbeat(System.currentTimeMillis());

        dnState.getTaskStates().forEach((t, s) -> {
            scheduler.taskUpdated(t, s);
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
        return Arrays.stream(jobIds).map(id -> scheduler.getJobStates().get(id)).filter(Objects::nonNull).collect(Collectors.toList());
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
        LinkedHashMap<String, JobState> jobs = scheduler.getJobStates();
        if (params.containsKey("id"))
            return jobs.get(params.get("id"));
        int offset = params.containsKey("from") ? Integer.parseInt(params.get("from")) : 0;
        int size = params.containsKey("from") ? Integer.parseInt(params.get("size")) : 20;
        LinkedHashMap<String, JobState> allJobs = getAllJobs(jobs, params);
        String[] keys = allJobs.keySet().toArray(new String[0]);
        Map<String, JobState> ret = new LinkedHashMap<>();
        for (int i = offset; i < Math.min(offset + size, keys.length); i++) {
            ret.put(keys[i], allJobs.get(keys[i]));
        }
        return ret;
    }

    private LinkedHashMap<String, JobState> getAllJobs(LinkedHashMap<String, JobState> jobs, Map<String, String> params) {
        if (params.containsKey("nodeid")) {
            String node = params.get("nodeid");
            LinkedHashMap<String, JobState> ret = new LinkedHashMap<>();
            jobs.values().stream()
                    .filter(j -> j.hasRunOnNode(node))
                    .forEach(j -> ret.put(j.getJobId(), new JobState(j, node)));
            return ret;
        }
        return jobs;
    }

    private class DnMonitor implements Runnable {

        void start() {
            new Thread(this, "Dn-Monitor").start();
        }

        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                try {
                    dns.values().forEach(dn -> {
                        if (dn.getLastHeartbeat() + conf.getHeartBeatInterval() + (5 * 1000) < System.currentTimeMillis()) {
                            log.info("{} is inactive", dn.getAddr());
                            dn.setActive(false);
                        } else {
                            dn.setActive(true);
                        }
                    });
                } catch (Exception e) {
                    log.error("Error: ", e);
                }
                Thread.sleep(1000 * 10);
            }
        }
    }
}