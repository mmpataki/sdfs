package com.mmp.sdfs.datanode;

import com.mmp.sdfs.client.SdfsClient;
import com.mmp.sdfs.common.FileStat;
import com.mmp.sdfs.common.TaskDef;
import com.mmp.sdfs.conf.SdfsClientConfig;
import com.mmp.sdfs.utils.IOUtils;
import com.mmp.sdfs.utils.Pair;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
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
    List<String> downloadedFiles = new ArrayList<>();

    public TaskExecutor(TaskDef task, SdfsClientConfig clientConf, Consumer<Integer> statusCallBack) {
        this.task = task;
        this.cliConf = clientConf;
        this.sdfsClient = new SdfsClient(clientConf);
        this.statusCallback = statusCallBack;
        this.workDir = workDir(task.getTaskId());
    }

    public static String workDir(String taskId) {
        return String.format("./tasks/%s", taskId);
    }

    public static String logPath(String taskid) {
        return String.format("%s/logs.txt", workDir(taskid));
    }

    private void executeTask() throws IOException {
        String taskId = task.getTaskId();
        String scriptFile = String.format("%s/script.sh", workDir);

        StringBuilder sb = new StringBuilder();
        sb.append("export TASK_ID=").append(taskId);
        if (task.getEnv() != null)
            task.getEnv().forEach((k, v) -> sb.append("export ").append(k).append('\n').append(v).append('\n'));
        sb.append('\n');
        sb.append("cd ").append(workDir).append('\n');
        task.getCommand().forEach(c -> sb.append(c).append(" "));
        sb.append("> logs.txt 2>&1").append('\n');

        Files.write(Paths.get(scriptFile), sb.toString().getBytes());

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(task.getCommand());
        pb.directory(new File(workDir));
        pb.redirectOutput(new File(String.format("%s/logs.txt", workDir)));
        pb.redirectErrorStream(true);
        new Thread(() -> {
            try {
                Process proc = pb.start();
                int status = proc.waitFor();
                statusCallback.accept(status);
            } catch (Throwable e) {
                log.error("Task {} failed", taskId, e);
                statusCallback.accept(-1);
            } finally {
                cleanUpDownloadedFiles();
            }
        }, taskId + "-process-monitor").start();
    }

    public void start() throws IOException {
        try {
            Files.createDirectories(Paths.get(workDir));
            for (Pair<String, String> file : task.getArtifacts()) {
                downloadedFiles.add(copyFromJobDir(file.getFirst(), file.getSecond()));
            }
        } catch (Exception e) {
            log.error("Error while downloading files of task {}", task.getTaskId(), e);
            cleanUpDownloadedFiles();
        }
        executeTask();
    }

    private void cleanUpDownloadedFiles() {
        for (String downloadedFile : downloadedFiles) {
            try {
                Files.delete(Paths.get(downloadedFile));
            } catch (IOException e) {
                log.error("Couldn't delete {}", downloadedFile, e);
            }
        }
    }

    private String copyFromJobDir(String remoteLoc, String localPath) throws Exception {
        FileStat stat = sdfsClient.get(remoteLoc);
        String localLoc = String.format("%s/%s", workDir, localPath);
        log.info("Downloading {} from DFS to {}", remoteLoc, localLoc);
        Files.createFile(Paths.get(localLoc));
        try (InputStream fis = sdfsClient.open(remoteLoc);
             OutputStream os = new FileOutputStream(localLoc)) {
            IOUtils.copy(fis, os, (int) stat.getSize());
        }
        log.info("download complete");
        return localLoc;
    }

}
