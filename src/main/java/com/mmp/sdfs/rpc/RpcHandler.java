package com.mmp.sdfs.rpc;

public interface RpcHandler<Return> {
    Return handle(Object[] t) throws Exception;
}
