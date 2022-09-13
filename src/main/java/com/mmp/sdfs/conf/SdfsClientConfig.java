package com.mmp.sdfs.conf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SdfsClientConfig extends SdfsConfig {

    @Argument(keys = {"--nnhost"}, required = true, help = "Namenode hostname", defValue = "localhost")
    String nameNodeHost;

    @Argument(keys = {"--nnport"}, required = true, help = "Namenode nnPort", defValue = "5001")
    int nnPort;

    public SdfsClientConfig(String[] args) throws Exception {
        super(args);
    }
}
