package com.mmp.sdfs.rpc;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class RpcSerde {
    public abstract Object readFrom(InputStream is) throws Exception;

    public abstract void writeTo(OutputStream os, Object ret) throws Exception;
}
