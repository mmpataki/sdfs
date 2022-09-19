package com.mmp.sdfs.hnwnrpc;

import lombok.Data;

import java.io.Serializable;

@Data
public class DnProfile implements Serializable {
    int port, dataPort, infoPort;
    String id, os;
    int cores;
    long memorySize;
    long diskTotal;
}
