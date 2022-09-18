package com.mmp.sdfs.common;

import com.mmp.sdfs.utils.Pair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskState implements Serializable {

    public enum State implements Serializable {
        QUEUED,
        PICKED,
        ASSIGNED,
        RUNNING,
        FAILED,
        ABORTED,
        SUCCEEDED;

        public boolean isCompleted() {
            return ordinal() > RUNNING.ordinal();
        }

    }

    String taskId;

    String taskLabel;

    int exitCode;
    String node;
    State state;
    List<Pair<TaskState.State, Long>> stateChanges;

    public TaskState(TaskDef t) {
        taskId = t.getTaskId();
        taskLabel = t.getTaskLabel();
        exitCode = -9999;
        stateChanges = new LinkedList<>();
    }

    void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    void stateChanged(State state) {
        if (stateChanges.isEmpty() || stateChanges.get(stateChanges.size() - 1).getFirst() != state)
            stateChanges.add(new Pair<>(state, System.currentTimeMillis()));
        this.state = state;
    }

    public void queued() {
        setState(State.QUEUED);
    }

    public void picked() {
        stateChanged(State.PICKED);
    }

    public void running() {
        stateChanged(State.RUNNING);
    }

    public void nodeAssigned(String nodeId) {
        this.node = nodeId;
        setState(State.ASSIGNED);
    }

    public void completed(Integer s) {
        stateChanged(s == 0 ? State.SUCCEEDED : s != -9999 ? State.FAILED : state);
        setExitCode(s);
    }

    public void abort() {
        setState(State.ABORTED);
    }

    public boolean hasRun() {
        return getState().ordinal() >= State.RUNNING.ordinal();
    }
}
