package com.mmp.sdfs.cdnrpc;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ReadOp implements DataNodeOp, Serializable {
    String blockId;
    int start;
    int len;
}
