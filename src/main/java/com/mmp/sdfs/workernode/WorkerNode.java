package com.mmp.sdfs.workernode;

import com.mmp.sdfs.common.TaskDef;
import com.mmp.sdfs.rpc.RpcExposed;
import com.mmp.sdfs.rpc.RpcService;

import java.io.IOException;
import java.util.List;

public interface WorkerNode extends RpcService {

    @RpcExposed
    List<String> delete(List<String> blocks) throws IOException;

    @RpcExposed
    void startTask(TaskDef task) throws Exception;
}
