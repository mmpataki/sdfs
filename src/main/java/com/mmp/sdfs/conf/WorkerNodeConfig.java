package com.mmp.sdfs.conf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkerNodeConfig extends SdfsConfig {

    @Argument(keys = {"--dnport"}, help = "Datanode port", defValue = "5003")
    int port;

    @Argument(keys = {"--dndport"}, help = "Datanode data transfer port", defValue = "5004")
    int dataPort;

    @Argument(keys = {"--dndir"}, help = "Datanode dir", defValue = "./datanode")
    String dnDir;

    public String getBlockDir() {
        return String.format("%s/blocks", getDnDir());
    }

    public WorkerNodeConfig(String args[]) throws Exception {
        super(args);
    }
}
