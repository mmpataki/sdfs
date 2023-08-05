package com.mmp.sdfs.rpc;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;

//WIP
@Slf4j
@AllArgsConstructor
public class HttpRpcInvocationHandler implements InvocationHandler {
    Class<?> clazz;
    String host;
    int port;
    RpcSerde serde;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try (Socket sock = new Socket(host, port)) {
            RpcCall rpcCall = new RpcCall(UUID.randomUUID().toString(), clazz.getCanonicalName() + ":" + method.getName(), args);
            log.trace("Rpc({}) -> {} {}({})", rpcCall.getTraceId(), sock.getRemoteSocketAddress(), rpcCall.getName(), Arrays.toString(rpcCall.getArgs()));
            serde.writeTo(sock.getOutputStream(), rpcCall);
            RpcResponse rpcResponse = (RpcResponse) serde.readFrom(sock.getInputStream());
            log.trace("RPC({}) <- {}", rpcResponse.getTraceId(), rpcResponse);
            if (rpcResponse.e != null) {
                throw rpcResponse.e.getCause();
            } else {
                return rpcResponse.ret;
            }
        }
    }
}
