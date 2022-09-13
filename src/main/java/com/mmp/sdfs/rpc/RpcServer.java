package com.mmp.sdfs.rpc;

import com.mmp.sdfs.server.Server;

import java.net.Socket;

public class RpcServer extends Server {

    private final RpcRouter rpcRouter;

    public RpcServer(int port, RpcSerde serde) throws Exception {
        super(port);
        this.rpcRouter = new RpcRouter(serde);
    }

    @Override
    public void process(Socket sock) throws Exception {
        rpcRouter.handle(sock);
    }

    protected void registerRpcProvider(RpcService provider) {
        rpcRouter.registerRpcProvider(provider);
    }
}
