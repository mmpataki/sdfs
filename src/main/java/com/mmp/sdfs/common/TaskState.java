package com.mmp.sdfs.common;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class TaskState implements Serializable {
    String id;
    int state;
    String node;
}
