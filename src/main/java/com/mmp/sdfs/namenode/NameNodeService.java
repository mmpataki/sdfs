package com.mmp.sdfs.namenode;

import com.mmp.sdfs.common.FileStat;
import com.mmp.sdfs.common.LocatedBlock;
import com.mmp.sdfs.conf.NameNodeConfig;
import com.mmp.sdfs.nndnrpc.DnHeartbeat;
import com.mmp.sdfs.nndnrpc.DnHeartbeatResponse;
import com.mmp.sdfs.nndnrpc.NameNode;
import com.mmp.sdfs.rpc.RpcContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NameNodeService implements NameNode {

    private final NameNodeConfig conf;
    Map<String, DnRef> dns = new HashMap<>();
    NameStore store;

    public NameNodeService(NameNodeConfig conf) throws Exception {
        this.conf = conf;
        if (!new File(conf.getNamedir()).exists()) {
            Files.createDirectory(Paths.get(conf.getNamedir()));
        }
        store = (NameStore) Class.forName(conf.getStoreClass()).getConstructor(NameNodeConfig.class, Map.class).newInstance(conf, dns);
        new BlockDeleter(conf, store, dns).start();
    }

    public DnHeartbeatResponse heartBeat(DnHeartbeat dnRegister) {
        dns.put(dnRegister.getId(), new DnRef(dnRegister, RpcContext.getRpcContext().getSock().getInetAddress().getHostAddress()));
        return new DnHeartbeatResponse();
    }

    public FileStat create(String path) throws Exception {
        return store.create(path, "anonymous", conf.getReplicationFactor());
    }

    public LocatedBlock addBlock(String path) throws Exception {
        return store.addBlock(path, "anonymous");
    }

    public List<LocatedBlock> getBlocks(String path) throws Exception {
        return store.getBlocks(path);
    }

    public List<FileStat> list(String dirPath) throws Exception {
        return store.list(dirPath);
    }

    public void delete(String path) throws Exception {
        store.delete(path);
    }

}
