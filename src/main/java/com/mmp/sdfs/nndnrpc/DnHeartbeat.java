package com.mmp.sdfs.nndnrpc;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DnHeartbeat implements Serializable {
    int port, dataPort, infoPort;
    String id;
    long blocks;
    Map<String, Integer> taskStates;
    SysInfo sysInfo;
}
