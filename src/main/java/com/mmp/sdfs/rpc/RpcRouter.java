package com.mmp.sdfs.rpc;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class RpcRouter {

    private Map<String, RpcHandler> handlers = new HashMap<>();
    private final RpcSerde serde;

    public RpcRouter(RpcSerde serde) throws Exception {
        this.serde = serde;
    }

    public void registerRpcProvider(RpcService obj) {
        Class<?> oClz = obj.getClass();
        for (Class<?> iClz : obj.getClass().getInterfaces()) {
            String iName = iClz.getCanonicalName();
            for (Method method : iClz.getMethods()) {
                if (method.isAnnotationPresent(RpcExposed.class)) {
                    String meth = iName + ":" + method.getName();
                    log.trace("Registering RPC method - {}", meth);
                    handlers.put(meth, args -> oClz.getMethod(method.getName(), method.getParameterTypes()).invoke(obj, args));
                }
            }
        }
    }

    public void handle(Socket sock) throws Exception {
        RpcCall rpcCall = (RpcCall) serde.readFrom(sock.getInputStream());
        String id = UUID.randomUUID().toString();
        log.trace("RPC({}) ->  {} {}({})", id, sock.getRemoteSocketAddress(), rpcCall.getName(), rpcCall.getArgs());
        Object ret = null;
        Exception e = null;
        try {
            RpcContext.setRpcContext(new RpcContext(sock));
            ret = handlers.get(rpcCall.getName()).handle(rpcCall.getArgs());
        } catch (Exception e1) {
            log.error("RPC(" + id + ") failed", e1);
            e = e1;
        } finally {
            RpcContext.unsetRpcContext();
        }
        RpcResponse resp = new RpcResponse(ret, e);
        log.trace("RPC({}) <- {}", id, resp);
        serde.writeTo(sock.getOutputStream(), resp);
    }

}
