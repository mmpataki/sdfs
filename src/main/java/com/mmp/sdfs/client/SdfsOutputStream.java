package com.mmp.sdfs.client;

import com.mmp.sdfs.common.LocatedBlock;
import com.mmp.sdfs.conf.SdfsClientConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.mmp.sdfs.common.ProxyFactory;

import java.io.IOException;
import java.io.OutputStream;

@Slf4j
public class SdfsOutputStream extends OutputStream {

    private final SdfsClient client;
    private final String path;
    SdfsClientConfig conf;
    ProxyFactory proxyFactory;

    int offset = 0;
    byte buffer[];
    LocatedBlock currentBlock;

    SdfsOutputStream(String path, SdfsClient client) throws Exception {
        this.path = path;
        this.client = client;
        conf = client.getConf();
        proxyFactory = client.getProxyFactory();

        buffer = new byte[conf.getBlockSize()];
        proxyFactory.getNNProxy().create(path);
    }

    @SneakyThrows
    @Override
    public void write(int b) throws IOException {
        if(currentBlock == null) {
            currentBlock = proxyFactory.getNNProxy().addBlock(path);
            offset = 0;
        }
        buffer[offset++] = (byte) b;
        if (offset == buffer.length) {
            flush();
            currentBlock = null;
        }
    }

    @Override
    public void flush() throws IOException {
        DNClient dnc = new DNClient(conf);
        dnc.writeBlock(currentBlock, buffer, offset);
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
