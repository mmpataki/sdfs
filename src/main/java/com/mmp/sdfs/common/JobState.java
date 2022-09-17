package com.mmp.sdfs.common;

import com.mmp.sdfs.utils.Pair;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class JobState implements Serializable {


    enum State implements Serializable {
        ACCEPTED,
        QUEUED,
        RUNNING,
        FAILED,
        SUCCEEDED,
        ABORTED;

    }

    State state;
    String jobId, jobLabel;

    List<Pair<State, Long>> stateChanges;
    Map<String, TaskState> taskStates;

    transient int completed = 0;

    public JobState(Job job) {
        jobId = job.getJobId();
        jobLabel = job.getJobLabel();

        stateChanges = new LinkedList<>();
        stateChanged(State.ACCEPTED);

        taskStates = new HashMap<>();
        job.getTasks().forEach(t -> {
            TaskState ts = new TaskState(t);
            t.setState(ts);
            taskStates.put(t.getTaskId(), ts);
        });

        job.setState(this);
    }

    public void taskCompleted(String taskId, Integer s) {
        getTaskStates().get(taskId).completed(s);
        if (s != 0 && s != -9999) {
            for (TaskState ts : taskStates.values()) {
                if (!ts.hasRun()) {
                    ts.abort();
                }
            }
            stateChanged(State.FAILED);
        } else {
            completed++;
            if (completed == taskStates.size())
                stateChanged(State.SUCCEEDED);
        }
    }

    public void taskStarted(String taskId, String nodeId) {
        running();
        getTaskStates().get(taskId).started(nodeId);
    }

    public void queued() {
        stateChanged(State.QUEUED);
        getTaskStates().values().forEach(ts -> ts.queued());
    }

    public void running() {
        stateChanged(State.RUNNING);
    }

    private void stateChanged(State state) {
        this.state = state;
        if (stateChanges.isEmpty() || stateChanges.get(stateChanges.size() - 1).getFirst() != state)
            stateChanges.add(new Pair<>(state, System.currentTimeMillis()));
    }

    public TaskState getTaskState(String taskId) {
        return getTaskStates().get(taskId);
    }

    public boolean hasCompleted() {
        return getState().ordinal() >= State.FAILED.ordinal();
    }
}
