package com.mmp.sdfs.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class LocatedBlock implements Serializable {
    String id;
    List<DnAddress> locations;
}
