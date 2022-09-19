package com.mmp.sdfs.hnwnrpc;

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
    long diskAvailable;
    Map<String, Integer> taskStates;
}
