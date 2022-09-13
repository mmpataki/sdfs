package com.mmp.sdfs.conf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SdfsClientConfig extends SdfsConfig {

    public SdfsClientConfig(String[] args) throws Exception {
        super(args);
    }
}
