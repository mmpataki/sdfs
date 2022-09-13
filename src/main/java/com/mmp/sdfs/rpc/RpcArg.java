package com.mmp.sdfs.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.Socket;

@Data
@AllArgsConstructor
public class RpcArg<T> {
    Socket sock;
    T arg;
}
