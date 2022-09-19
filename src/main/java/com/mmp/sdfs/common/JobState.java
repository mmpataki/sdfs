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


    public boolean hasRunOnNode(String node) {
        return getTaskStates().values().stream().anyMatch(t -> t.getNode() != null && t.getNode().equals(node));
    }

    public enum State implements Serializable {
        ACCEPTED,
        QUEUED,
        RUNNING,
        FAILED,
        SUCCEEDED,
        ABORTED;

        public boolean isCompleted() {
            return ordinal() > RUNNING.ordinal();
        }
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

    public JobState(JobState js, String node) {
        state = js.state;
        jobId = js.jobId;
        jobLabel = js.jobLabel;
        stateChanges = js.stateChanges;
        taskStates = new HashMap<>();
        js.taskStates.values().stream().filter(ts -> ts.getNode().equals(node)).forEach(ts -> taskStates.put(ts.getTaskId(), ts));
    }

    public void taskUpdated(String taskId, Integer s) {
        if (s == -9999) {
            getTaskStates().get(taskId).running();
            running();
        } else {
            // task has completed
            getTaskStates().get(taskId).completed(s);
            completed++;
            if (s == 0) {
                if (completed >= taskStates.size())
                    stateChanged(State.SUCCEEDED);
            } else {
                for (TaskState ts : taskStates.values()) {
                    if (!ts.hasRun()) {
                        ts.abort();
                    }
                }
                stateChanged(State.FAILED);
            }
        }
    }

    public void taskAssigned(String taskId, String nodeId) {
        getTaskStates().get(taskId).nodeAssigned(nodeId);
    }

    public void queued() {
        stateChanged(State.QUEUED);
        getTaskStates().values().forEach(TaskState::queued);
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
