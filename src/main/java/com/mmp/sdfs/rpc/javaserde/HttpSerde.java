package com.mmp.sdfs.rpc.javaserde;

import com.google.gson.Gson;
import com.mmp.sdfs.rpc.RpcSerde;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

//WIP
public class HttpSerde extends RpcSerde {

    Gson gson = new Gson();

    @Override
    public Object readFrom(InputStream stream) throws Exception {
        return new ObjectInputStream(stream).readObject();
    }

    @Override
    public void writeTo(OutputStream os, Object obj) throws Exception {
        new ObjectOutputStream(os).writeObject(obj);
    }

}
