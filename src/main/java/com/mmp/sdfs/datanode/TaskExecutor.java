package com.mmp.sdfs.datanode;

import com.mmp.sdfs.client.SdfsClient;
import com.mmp.sdfs.common.FileStat;
import com.mmp.sdfs.common.TaskDef;
import com.mmp.sdfs.conf.SdfsClientConfig;
import com.mmp.sdfs.utils.IOUtils;
import com.mmp.sdfs.utils.Pair;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class TaskExecutor {

    private final TaskDef task;
    private final Consumer<Integer> statusCallback;
    private final SdfsClientConfig cliConf;
    private final SdfsClient sdfsClient;
    String workDir;

    public TaskExecutor(TaskDef task, SdfsClientConfig clientConf, Consumer<Integer> statusCallBack) {
        this.task = task;
        this.cliConf = clientConf;
        this.sdfsClient = new SdfsClient(clientConf);
        this.statusCallback = statusCallBack;
        this.workDir = String.format("./tasks/%s", task.getId());
    }

    private void executeTask() throws IOException {
        String taskId = task.getId();
        String scriptFile = String.format("%s/script.sh", workDir);

        StringBuilder sb = new StringBuilder();
        sb.append("export TASK_ID=").append(taskId);
        task.getEnv().forEach((k, v) -> sb.append("export ").append(k).append('\n').append(v).append('\n'));
        sb.append('\n');
        sb.append("cd ").append(workDir).append('\n');
        task.getCommand().forEach(c -> sb.append(c).append(" "));
        sb.append("> stdout 2> stderr").append('\n');

        Files.createDirectories(Paths.get(workDir));
        Files.write(Paths.get(scriptFile), sb.toString().getBytes());

        ProcessBuilder pb = new ProcessBuilder();
        pb.command("bash", scriptFile);
        new Thread(() -> {
            try {
                Process proc = pb.start();
                int status = proc.waitFor();
                statusCallback.accept(status);
            } catch (Throwable e) {
                log.error("Task {} failed", taskId, e);
                statusCallback.accept(-1);
            }
        }, taskId + "-process-monitor").start();
    }

    public void start() throws IOException {
        List<String> downloadedFiles = new ArrayList<>();
        try {
            for (Pair<String, String> file : task.getArtifacts()) {
                downloadedFiles.add(copyFromJobDir(file.getFirst(), file.getSecond()));
            }
            executeTask();
        } catch (Exception e) {
            log.error("Error while executng task", e);
        } finally {
            for (String downloadedFile : downloadedFiles) {
                Files.delete(Paths.get(downloadedFile));
            }
        }
    }

    private String copyFromJobDir(String remoteLoc, String localPath) throws Exception {
        FileStat stat = sdfsClient.get(remoteLoc);
        String localLoc = String.format("%s/%s", workDir, localPath);
        log.info("Downloading {} from DFS to {}", remoteLoc, localPath);
        Files.createFile(Paths.get(localLoc));
        try (InputStream fis = sdfsClient.open(remoteLoc);
             OutputStream os = new FileOutputStream(localPath)) {
            IOUtils.copy(fis, os, (int) stat.getSize());
        }
        log.info("upload complete");
        return localLoc;
    }

}
