package com.mmp.sdfs.rpc;

import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Socket;

@AllArgsConstructor
public class RpcInvocationHandler implements InvocationHandler {
    Class<?> clazz;
    String host;
    int port;
    RpcSerde serde;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try (Socket sock = new Socket(host, port)) {
            serde.writeTo(sock.getOutputStream(), new RpcCall(clazz.getCanonicalName() + ":" + method.getName(), args));
            RpcResponse rpcResponse = (RpcResponse) serde.readFrom(sock.getInputStream());
            if (rpcResponse.e != null) {
                throw rpcResponse.e.getCause();
            } else {
                return rpcResponse.ret;
            }
        }
    }
}
