package com.mmp.sdfs.nndnrpc;

import com.mmp.sdfs.common.*;
import com.mmp.sdfs.rpc.RpcExposed;
import com.mmp.sdfs.rpc.RpcService;

import java.util.List;

public interface HeadNode extends RpcService {

    @RpcExposed
    DnHeartbeatResponse heartBeat(DnHeartbeat dnRegister);

    @RpcExposed
    FileStat create(String path) throws Exception;

    @RpcExposed
    LocatedBlock addBlock(String path) throws Exception;

    @RpcExposed
    List<LocatedBlock> getBlocks(String path) throws Exception;

    @RpcExposed
    List<FileStat> list(String dirPath) throws Exception;

    @RpcExposed
    void delete(String dirPath) throws Exception;

    @RpcExposed
    void closeFile(String path, long size) throws Exception;

    @RpcExposed
    String submitJob(Job task) throws Exception;

    @RpcExposed
    List<JobState> getStatusOf(String[] jobIds) throws Exception;

    @RpcExposed
    FileStat get(String path) throws Exception;
}
