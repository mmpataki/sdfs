package com.mmp.sdfs.nndnrpc;

import com.mmp.sdfs.utils.Pair;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class SysInfo implements Serializable {
    int numCores;
    double cpuPercent;
    Map<String, Pair<Long, Long>> disks;
    long memorySize, memoryAvailable;
    double loadAvg[];
}
