package com.mmp.sdfs.headnode;

import com.mmp.sdfs.client.DNClient;
import com.mmp.sdfs.common.*;
import com.mmp.sdfs.conf.HeadNodeConfig;
import com.mmp.sdfs.hnwnrpc.DNState;
import com.mmp.sdfs.utils.Pair;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Data
@Slf4j
public class Scheduler implements Runnable {

    private final HeadNodeConfig conf;
    private final Map<String, DnRef> workerNodes;

    LinkedHashMap<String, Job> jobs = new LinkedHashMap<>();
    LinkedHashMap<String, JobState> jobStates = new LinkedHashMap<>();
    Queue<Pair<TaskDef, Job>> taskQ = new LinkedList<>();
    private Thread thread;


    public Scheduler(Map<String, DnRef> workerNodes, HeadNodeConfig conf) {
        this.workerNodes = workerNodes;
        this.conf = conf;
    }

    public String schedule(Job job) {

        job.setJobId(UUID.randomUUID().toString());
        job.getTasks().forEach(t -> t.setTaskId(String.format("%s/%s", job.getJobId(), UUID.randomUUID())));

        JobState js = new JobState(job);
        job.getTasks().forEach(t -> {
            taskQ.add(new Pair<>(t, job));
        });
        js.queued();
        jobs.put(job.getJobId(), job);
        jobStates.put(job.getJobId(), js);
        return job.getJobId();
    }

    public void start() {
        this.thread = new Thread(this, "Scheduler");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            int loopLimit = taskQ.size();
            while (thereAreTasks() && loopLimit > 0) {
                loopLimit--;
                Pair<TaskDef, Job> taskAndJob = pickATask();
                if (taskAndJob != null) {
                    DnAddress addr = pickANode(taskAndJob.getFirst());
                    if (addr == null) {
                        taskQ.add(taskAndJob);
                    } else {
                        try {
                            taskAndJob.getFirst().getState().picked();
                            runThe(taskAndJob, addr);
                        } catch (Exception e) {
                            log.error("Error submitting a task: {}", taskAndJob, e);
                        }
                    }
                }
            }
            waitForAWhile();
        }
    }

    @SneakyThrows
    private void waitForAWhile() {
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            log.info("I was interrupted for work, will schedule a job");
        }
    }

    private void runThe(Pair<TaskDef, Job> taskAndJob, DnAddress addr) throws Exception {
        TaskDef task = taskAndJob.getFirst();
        Job job = taskAndJob.getSecond();
        JobState js = job.getState();
        log.info("Starting: {} ({}) / {} ({}) on {}", js.getJobId(), js.getJobLabel(), task.getTaskId(), task.getTaskLabel(), addr);
        js.taskAssigned(task.getTaskId(), addr.getId());
        try {
            task.setArtifacts(job.getArtifacts());
            new DNClient(conf).startTask(addr, task);
        } catch (Exception e) {
            js.taskUpdated(task.getTaskId(), -1);
            throw e;
        }
    }

    Random R = new Random();

    private DnAddress pickANode(TaskDef task) {

        for (String prefNode : task.getPreferredNodes()) {
            DnRef node = workerNodes.get(prefNode);
            if (nodeIsOk(node, task)) {
                return node.getAddr();
            }
        }

        List<DnRef> nodes = new ArrayList<>(workerNodes.values());
        for (DnRef node : nodes) {
            if (nodeIsOk(node, task))
                return node.getAddr();
        }

        return null;
    }

    private boolean nodeIsOk(DnRef node, TaskDef task) {
        DNState state = node.getState();
        return state.getMemoryAvailable() > task.getMemNeeded() && (100 - state.getCpuPercent()) > task.getCpuPercentNeeded();
    }

    private synchronized boolean thereAreTasks() {
        while (!taskQ.isEmpty() && taskQ.peek().getFirst().getState().getState() == TaskState.State.ABORTED)
            taskQ.remove();
        log.debug("Tasks in Q: {}", taskQ.size());
        return !taskQ.isEmpty();
    }

    private synchronized Pair<TaskDef, Job> pickATask() {
        if (!taskQ.isEmpty()) {
            return taskQ.remove();
        }
        return null;
    }

    public void taskUpdated(String taskId, Integer status) {
        String jobId = taskId.split("/")[0];
        if (jobStates.containsKey(jobId))
            jobStates.get(jobId).taskUpdated(taskId, status);
        if (status != -9999)
            thread.interrupt();
    }
}
