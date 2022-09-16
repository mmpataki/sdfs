package com.mmp.sdfs.common;

import com.mmp.sdfs.utils.Pair;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TaskDef implements Serializable {

    String id;

    List<String> command;

    List<String> preferredNodes;

    List<Pair<String, String>> artifacts;

    Map<String, String> env;

}
