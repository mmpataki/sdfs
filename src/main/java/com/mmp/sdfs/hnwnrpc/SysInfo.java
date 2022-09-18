package com.mmp.sdfs.hnwnrpc;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class SysInfo implements Serializable {
    long blocks;
    double cpuPercent;
    long memoryAvailable;
    double[] loadAvg;
    Map<String, Integer> taskStates;
}
