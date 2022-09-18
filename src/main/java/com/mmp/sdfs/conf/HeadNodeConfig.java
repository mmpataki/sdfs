package com.mmp.sdfs.conf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeadNodeConfig extends SdfsConfig {

    @Argument(keys = {"-d", "--namedir"}, required = true, help = "namenode directory", defValue = "namenode")
    String namedir;

    @Argument(keys = {"--storeclass"}, required = true, help = "namenode store class", defValue = "com.mmp.sdfs.headnode.SqliteNameStore")
    String storeClass;

    public HeadNodeConfig(String[] args) throws Exception {
        super(args);
    }
}
