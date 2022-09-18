package com.mmp.sdfs.client;

import com.mmp.sdfs.common.*;
import com.mmp.sdfs.conf.SdfsClientConfig;
import com.mmp.sdfs.utils.IOUtils;
import com.mmp.sdfs.utils.Pair;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

@Slf4j
public class SdfsClient {

    @Getter
    private final SdfsClientConfig conf;

    @Getter
    ProxyFactory proxyFactory;

    public interface JobUpdateCallBack {
        void jobUpdated(JobState js);

        void taskUpdated(TaskState status);
    }

    private Thread taskMonitor = null;
    private Map<String, Pair<Job, JobUpdateCallBack>> jobs = new HashMap<>();
    private Map<String, JobState> jobStates = new HashMap<>();

    public SdfsClient(SdfsClientConfig conf) {
        this.conf = conf;
        proxyFactory = new ProxyFactory(conf);
    }

    public SdfsOutputStream create(String path, int replicas) throws Exception {
        return new SdfsOutputStream(path, this);
    }

    public SdfsOutputStream create(String path) throws Exception {
        return create(path, conf.getReplicationFactor());
    }

    public void delete(String path) throws Exception {
        try {
            FileStat fs = get(path);
            proxyFactory.getHNProxy().delete(path);
            delete(fs.getPath());
        } catch (FileNotFoundException fnfe) {
            List<FileStat> list = list(path);
            for (FileStat fileStat : list) {
                proxyFactory.getHNProxy().delete(fileStat.getPath());
            }
        }
    }

    public SdfsInputStream open(String path) {
        return new SdfsInputStream(path, this);
    }

    public List<FileStat> list(String path) throws Exception {
        return proxyFactory.getHNProxy().list(path);
    }

    public List<LocatedBlock> getBlocks(String path) throws Exception {
        return proxyFactory.getHNProxy().getBlocks(path);
    }

    public String submit(Job job, JobUpdateCallBack callBack) throws Exception {

        String uuid = UUID.randomUUID().toString();
        List<Pair<String, String>> artifacts = job.getArtifacts();
        List<Pair<String, String>> remoteArtifacts = new ArrayList<>();
        job.setArtifacts(remoteArtifacts);
        try {
            for (Pair<String, String> artifact : artifacts) {
                String remotePath = String.format("/jobs/%s/%s", uuid, artifact.getSecond());
                log.info("Uploading {} -> {}", artifact.getFirst(), remotePath);
                upload(artifact.getFirst(), remotePath);
                remoteArtifacts.add(new Pair<>(remotePath, artifact.getSecond()));
            }
            job.setArtifacts(remoteArtifacts);
            String ret = proxyFactory.getHNProxy().submitJob(job);
            if (taskMonitor == null) {
                jobs.put(ret, new Pair<>(job, callBack));
                taskMonitor = new Thread(new TaskMonitor(), "task-monitor");
                taskMonitor.start();
            }
            return ret;
        } catch (Exception e) {
            log.error("Error while starting task", e);
            cleanupTask(job);
            throw e;
        }
    }

    void doTry(Runnable f) {
        try {
            f.run();
        } catch (Exception e) {
            log.error("error updating job & task status: ", e);
        }
    }

    class TaskMonitor implements Runnable {

        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                try {
                    String[] ids = jobs.keySet().toArray(new String[0]);
                    if (ids.length != 0) {
                        List<JobState> statuses = getStatusOf(ids);
                        statuses.forEach(js -> {
                            JobState oldJob = jobStates.get(js.getJobId());
                            Pair<Job, JobUpdateCallBack> jobAndCallback = jobs.get(js.getJobId());
                            if (oldJob == null || oldJob.getState() != js.getState())
                                doTry(() -> jobAndCallback.getSecond().jobUpdated(js));
                            for (TaskState ts : js.getTaskStates().values()) {
                                if ((oldJob == null || oldJob.getTaskState(ts.getTaskId()).getExitCode() != ts.getExitCode())) {
                                    doTry(() -> jobAndCallback.getSecond().taskUpdated(ts));
                                }
                            }
                            if (js.hasCompleted()) {
                                Pair<Job, JobUpdateCallBack> taskAndCall = jobs.get(js.getJobId());
                                doTry(() -> taskAndCall.getSecond().jobUpdated(js));
                                jobs.remove(js.getJobId());
                                jobStates.remove(js.getJobId());
                                cleanupTask(taskAndCall.getFirst());
                            }
                            jobStates.put(js.getJobId(), js);
                        });
                    }
                } catch (Exception e) {
                    log.error("Error while fetching task states", e);
                }
                Thread.sleep(5000);
            }
        }
    }

    private void cleanupTask(Job job) {
        for (Pair<String, String> artifact : job.getArtifacts()) {
            try {
                delete(artifact.getFirst());
            } catch (Exception e) {
                log.error("Deleting {} failed", artifact.getFirst(), e);
            }
        }
    }

    public List<JobState> getStatusOf(String... taskIds) throws Exception {
        return proxyFactory.getHNProxy().getStatusOf(taskIds);
    }

    public void upload(String localPath, String remotePath) throws Exception {
        File file = new File(localPath);
        try (SdfsOutputStream dos = create(remotePath);
             FileInputStream fis = new FileInputStream(file)) {
            IOUtils.copy(fis, dos, (int) file.length());
        }
    }

    public static void main(String[] args) throws Exception {
        SdfsClient fsc = new SdfsClient(new SdfsClientConfig(args));
        fsc.delete("/tmp/a/1.txt");
        File file = new File("C:\\Users\\mpataki\\Downloads\\tmp\\e1.txt");

        try (SdfsOutputStream dos = fsc.create("/tmp/a/1.txt")) {
            FileInputStream fis = new FileInputStream(file);
            IOUtils.copy(fis, dos, (int) file.length());
        }

        try (SdfsInputStream is = fsc.open("/tmp/a/1.txt")) {
            FileOutputStream fos = new FileOutputStream("C:\\Users\\mpataki\\Downloads\\tmp\\e2.txt");
            IOUtils.copy(is, fos, (int) file.length());
        }
    }

    public FileStat get(String path) throws Exception {
        return proxyFactory.getHNProxy().get(path);
    }
}
