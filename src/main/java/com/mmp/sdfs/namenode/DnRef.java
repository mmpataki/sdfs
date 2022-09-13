package com.mmp.sdfs.namenode;

import com.mmp.sdfs.common.DnAddress;
import lombok.Data;
import com.mmp.sdfs.nndnrpc.DnHeartbeat;

@Data
public class DnRef {
    DnAddress addr;
    long blocks, availableDisk, usedDisk;

    public DnRef(DnHeartbeat rpl, String hostname) {
        this.addr = new DnAddress(rpl.getId(), hostname, rpl.getPort(), rpl.getDataPort());
        this.blocks = rpl.getBlocks();
        this.availableDisk = rpl.getAvailableDisk();
        this.usedDisk = rpl.getUsedDisk();
    }
}
