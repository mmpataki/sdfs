package com.mmp.sdfs.namenode;

import com.mmp.sdfs.common.DnAddress;
import com.mmp.sdfs.nndnrpc.SysInfo;
import lombok.Data;
import com.mmp.sdfs.nndnrpc.DnHeartbeat;

import java.util.Map;

@Data
public class DnRef {
    DnAddress addr;
    long blocks;
    SysInfo sysInfo;

    public DnRef(DnHeartbeat rpl, String hostname) {
        this.addr = new DnAddress(rpl.getId(), hostname, rpl.getPort(), rpl.getDataPort());
        this.blocks = rpl.getBlocks();
        this.sysInfo = rpl.getSysInfo();
    }
}
