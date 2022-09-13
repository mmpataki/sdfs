package com.mmp.sdfs.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Pair<T1, T2> {
    T1 first;
    T2 second;
}
