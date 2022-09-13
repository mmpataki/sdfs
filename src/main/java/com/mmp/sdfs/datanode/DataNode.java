package com.mmp.sdfs.datanode;

import com.mmp.sdfs.rpc.RpcExposed;
import com.mmp.sdfs.rpc.RpcService;

import java.io.IOException;
import java.util.List;

public interface DataNode extends RpcService {

    @RpcExposed
    List<String> delete(List<String> blocks) throws IOException;

}
