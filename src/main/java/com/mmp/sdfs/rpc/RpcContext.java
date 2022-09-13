package com.mmp.sdfs.rpc;

import lombok.Data;

import java.net.Socket;

@Data
public class RpcContext {

    static ThreadLocal<RpcContext> rpcContext = new ThreadLocal<>();

    public static void setRpcContext(RpcContext rpcContext) {
        RpcContext.rpcContext.set(rpcContext);
    }

    Socket sock;
    public RpcContext(Socket sock) {
        this.sock = sock;
    }

    public static void unsetRpcContext() {
        rpcContext.remove();
    }

    public static RpcContext getRpcContext() {
        return rpcContext.get();
    }
}
