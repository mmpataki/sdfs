package com.mmp.sdfs.common;

import com.mmp.sdfs.utils.Pair;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Job implements Serializable {
    String jobId;
    String jobLabel;
    List<TaskDef> tasks;
    List<Pair<String, String>> artifacts;

    transient JobState state;
}
