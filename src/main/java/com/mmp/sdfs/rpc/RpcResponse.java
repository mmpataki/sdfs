package com.mmp.sdfs.rpc;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class RpcResponse<T> implements Serializable {
    @ToString.Exclude
    String traceId;
    T ret;
    Exception e;
}
