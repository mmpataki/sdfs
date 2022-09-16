package com.mmp.sdfs.client;

import com.mmp.sdfs.common.*;
import com.mmp.sdfs.conf.SdfsClientConfig;
import com.mmp.sdfs.utils.IOUtils;
import com.mmp.sdfs.utils.Pair;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class SdfsClient {

    @Getter
    private final SdfsClientConfig conf;

    @Getter
    ProxyFactory proxyFactory;

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
            proxyFactory.getNNProxy().delete(path);
            delete(fs.getPath());
        } catch (FileNotFoundException fnfe) {
            List<FileStat> list = list(path);
            for (FileStat fileStat : list) {
                proxyFactory.getNNProxy().delete(fileStat.getPath());
            }
        }
    }

    public SdfsInputStream open(String path) {
        return new SdfsInputStream(path, this);
    }

    public List<FileStat> list(String path) throws Exception {
        return proxyFactory.getNNProxy().list(path);
    }

    public List<LocatedBlock> getBlocks(String path) throws Exception {
        return proxyFactory.getNNProxy().getBlocks(path);
    }

    public String startTask(TaskDef taskDef) throws Exception {
        taskDef.setId(UUID.randomUUID().toString());
        List<Pair<String, String>> remoteArtifacts = new ArrayList<>();
        try {
            for (Pair<String, String> artifact : taskDef.getArtifacts()) {
                String remotePath = String.format("/jobs/%s/%s", taskDef.getId(), artifact.getSecond());
                log.info("Uploading {} -> {}", artifact.getFirst(), remotePath);
                upload(artifact.getFirst(), remotePath);
                remoteArtifacts.add(new Pair<>(remotePath, artifact.getSecond()));
            }
            taskDef.setArtifacts(remoteArtifacts);
            return proxyFactory.getNNProxy().startTask(taskDef);
        } catch (Exception e) {
            log.error("Error while starting task", e);
            for (Pair<String, String> remoteArtifact : remoteArtifacts) {
                try {
                    delete(remoteArtifact.getFirst());
                } catch (Exception fde) {
                    log.error("Error while deleting file: {}", remoteArtifact.getFirst(), e);
                }
            }
            throw e;
        }
    }

    public List<TaskState> getStatusOf(String... taskIds) throws Exception {
        return proxyFactory.getNNProxy().getStatusOf(taskIds);
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
        return proxyFactory.getNNProxy().get(path);
    }
}
