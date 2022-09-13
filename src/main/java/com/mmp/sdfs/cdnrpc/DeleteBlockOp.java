package com.mmp.sdfs.cdnrpc;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class DeleteBlockOp implements DataNodeOp, Serializable {
    List<String> blocks;
}
