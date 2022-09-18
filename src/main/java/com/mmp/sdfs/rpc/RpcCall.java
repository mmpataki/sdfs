package com.mmp.sdfs.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class RpcCall implements Serializable {
    @ToString.Exclude
    String traceId;
    String name;
    Object[] args;
}
