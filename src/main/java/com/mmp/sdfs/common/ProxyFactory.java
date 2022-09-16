package com.mmp.sdfs.common;

import com.mmp.sdfs.conf.SdfsConfig;
import com.mmp.sdfs.datanode.WorkerNode;
import lombok.extern.slf4j.Slf4j;
import com.mmp.sdfs.nndnrpc.HeadNode;
import com.mmp.sdfs.rpc.RpcInvocationHandler;
import com.mmp.sdfs.rpc.RpcSerde;

import java.lang.reflect.Proxy;

@Slf4j
public class ProxyFactory {

    private final SdfsConfig conf;

    public ProxyFactory(SdfsConfig conf) {
        this.conf = conf;
    }

    public <T> T makeProxy(Class<T> clz, String host, int port, RpcSerde serde) {
        return (T) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{clz}, new RpcInvocationHandler(clz, host, port, serde));
    }

    public HeadNode getNNProxy() {
        return makeProxy(HeadNode.class, conf.getNnHost(), conf.getNnPort(), conf.getRpcSerde());
    }

    public WorkerNode getDNProxy(DnAddress addr) {
        return makeProxy(WorkerNode.class, addr.getHostname(), addr.getPort(), conf.getRpcSerde());
    }

}
