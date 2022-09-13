package com.mmp.sdfs.namenode;

import com.mmp.sdfs.common.DnAddress;
import com.mmp.sdfs.common.FileStat;
import com.mmp.sdfs.common.LocatedBlock;
import com.mmp.sdfs.conf.NameNodeConfig;
import com.mmp.sdfs.utils.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class NameStore {

    private final NameNodeConfig conf;
    protected final Map<String, DnRef> dataNodes;

    public NameStore(NameNodeConfig conf, Map<String, DnRef> dataNodes) {
        this.conf = conf;
        this.dataNodes = dataNodes;
    }

    protected List<DnAddress> getDnLocations(int replicas) {
        List<DnRef> nodes = new ArrayList<>(dataNodes.values());
        nodes.sort(Comparator.comparingLong(o -> o.availableDisk));
        return nodes.subList(Math.max(0, nodes.size() - replicas), nodes.size()).stream().map(dr -> dr.getAddr()).collect(Collectors.toList());
    }

    public abstract FileStat create(String path, String owner, int replicationFactor) throws Exception;

    public abstract void delete(String path) throws Exception;

    public abstract LocatedBlock addBlock(String path, String owner) throws Exception;

    public abstract List<LocatedBlock> getBlocks(String path) throws Exception;

    public abstract List<FileStat> list(String dirPath) throws Exception;

    public abstract List<Pair<String, String>> getDeletedBlocks() throws Exception;

    public abstract void blocksDeleted(List<Pair<String, String>> deleted) throws Exception;
}
