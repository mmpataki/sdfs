package com.mmp.sdfs.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public
class FileStat implements Serializable {
    String path, owner;
    long id, replicas, size;
}