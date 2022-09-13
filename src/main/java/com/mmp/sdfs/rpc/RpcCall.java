package com.mmp.sdfs.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class RpcCall implements Serializable {
    String name;
    Object[] args;
}
