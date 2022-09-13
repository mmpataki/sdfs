package com.mmp.sdfs.conf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NameNodeConfig extends SdfsConfig {

    @Argument(keys = {"-d", "--namedir"}, required = true, help = "namenode directory", defValue = "namenode")
    String namedir;

    @Argument(keys = {"--storeclass"}, required = true, help = "namenode store class", defValue = "com.mmp.sdfs.namenode.SqliteNameStore")
    String storeClass;

    public NameNodeConfig(String[] args) throws Exception {
        super(args);
    }
}
