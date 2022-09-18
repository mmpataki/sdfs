package com.mmp.sdfs.hnwnrpc;

import com.mmp.sdfs.utils.Pair;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DNState implements Serializable {
    String id;
    long blocks;
    double cpuPercent;
    long memoryAvailable;
    double[] loadAvg;
    Map<String, Pair<Long, Long>> disks;
    Map<String, Integer> taskStates;
}
