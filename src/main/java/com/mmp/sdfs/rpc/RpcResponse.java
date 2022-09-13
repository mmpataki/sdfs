package com.mmp.sdfs.rpc;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class RpcResponse<T> implements Serializable {
    T ret;
    Exception e;
}
