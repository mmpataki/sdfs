package com.mmp.sdfs.rpc.javaserde;

import com.mmp.sdfs.rpc.RpcSerde;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class JavaSerde extends RpcSerde {

    @Override
    public Object readFrom(InputStream stream) throws Exception {
        return new ObjectInputStream(stream).readObject();
    }

    @Override
    public void writeTo(OutputStream os, Object obj) throws Exception {
        new ObjectOutputStream(os).writeObject(obj);
    }

}
