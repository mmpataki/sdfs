package com.mmp.sdfs.hncrpc;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class CreateFilePL implements Serializable {
    String name;
    int replication;
}
