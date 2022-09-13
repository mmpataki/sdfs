package com.mmp.sdfs.nndnrpc;

import lombok.Data;

import java.io.Serializable;

@Data
public class DnHeartbeat implements Serializable {
    int port, dataPort;
    String id;
    long blocks, availableDisk, usedDisk;
}
