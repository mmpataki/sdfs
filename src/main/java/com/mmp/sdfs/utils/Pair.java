package com.mmp.sdfs.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class Pair<T1, T2> implements Serializable {
    T1 first;
    T2 second;
}
