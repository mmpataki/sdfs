package com.mmp.sdfs.headnode;

import com.mmp.sdfs.common.DnAddress;
import com.mmp.sdfs.hnwnrpc.DnProfile;
import lombok.Data;
import com.mmp.sdfs.hnwnrpc.DNState;

import java.io.Serializable;

@Data
public class DnRef implements Serializable {
    DnAddress addr;
    DnProfile profile;
    volatile DNState state;

    volatile long lastHeartbeat;
    volatile boolean active;

    public DnRef(DnProfile profile, String hostname) {
        this.addr = new DnAddress(profile.getId(), hostname, profile.getPort(), profile.getDataPort(), profile.getInfoPort());
        this.profile =profile;
    }
}
