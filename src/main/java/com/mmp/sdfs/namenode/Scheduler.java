package com.mmp.sdfs.namenode;

import com.mmp.sdfs.client.DNClient;
import com.mmp.sdfs.common.*;
import com.mmp.sdfs.conf.HeadNodeConfig;
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

    Map<String, Job> jobs = new HashMap<>();
    Map<String, JobState> jobStates = new HashMap<>();
    Queue<Pair<TaskDef, Job>> taskQ = new LinkedList<>();


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
        new Thread(this, "Scheduler").start();
    }

    @Override
    public void run() {
        while (true) {
            if (thereAreResources()) {
                if (thereAreTasks()) {
                    Pair<TaskDef, Job> taskAndJob = pickATask();
                    if (taskAndJob != null) {
                        try {
                            runThe(taskAndJob);
                        } catch (Exception e) {
                            log.error("Error submitting a task: {}", taskAndJob);
                        }
                    }
                }
            }
            waitForAWhile();
        }
    }

    @SneakyThrows
    private void waitForAWhile() {
        Thread.sleep(5000);
    }

    private void runThe(Pair<TaskDef, Job> taskAndJob) throws Exception {
        TaskDef task = taskAndJob.getFirst();
        Job job = taskAndJob.getSecond();
        JobState js = job.getState();
        DnAddress addr = pickANode(task);
        log.info("Starting: {} ({}) / {} ({}) on {}", js.getJobId(), js.getJobLabel(), task.getTaskId(), task.getTaskLabel(), addr);
        js.taskStarted(task.getTaskId(), addr.getId());
        try {
            task.setArtifacts(job.getArtifacts());
            new DNClient(conf).startTask(addr, task);
        } catch (Exception e) {
            js.taskCompleted(task.getTaskId(), -1);
            throw e;
        }
    }

    Random R = new Random();

    private DnAddress pickANode(TaskDef first) {
        return workerNodes.get(first.getPreferredNodes().get(R.nextInt(first.getPreferredNodes().size()))).getAddr();
    }

    private boolean thereAreResources() {
        return true;
    }

    private synchronized boolean thereAreTasks() {
        while (!taskQ.isEmpty() && taskQ.peek().getFirst().getState().getState() == TaskState.State.ABORTED)
            taskQ.remove();
        log.debug("Tasks in Q: {}", taskQ.size());
        return !taskQ.isEmpty();
    }

    private synchronized Pair<TaskDef, Job> pickATask() {
        if (!taskQ.isEmpty()) {
            Pair<TaskDef, Job> next = taskQ.remove();
            next.getFirst().getState().picked();
            return next;
        }
        return null;
    }

    public void taskFinished(String taskId, Integer status) {
        String jobId = taskId.split("/")[0];
        jobStates.get(jobId).taskCompleted(taskId, status);
    }
}
