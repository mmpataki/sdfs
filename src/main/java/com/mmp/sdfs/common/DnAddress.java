package com.mmp.sdfs.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class DnAddress implements Serializable {
    String id, hostname;
    int port, dataPort, infoPort;
}
