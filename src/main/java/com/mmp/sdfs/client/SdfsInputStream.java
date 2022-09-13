package com.mmp.sdfs.client;

import com.mmp.sdfs.common.LocatedBlock;
import com.mmp.sdfs.conf.SdfsClientConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.mmp.sdfs.common.ProxyFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Slf4j
public class SdfsInputStream extends InputStream {

    private final SdfsClient client;
    private final String path;
    private final ProxyFactory proxyFactory;
    private final SdfsClientConfig conf;

    Iterator<LocatedBlock> blocks;
    LocatedBlock currentBlock;

    byte[] buffer;
    int len, offset;

    public SdfsInputStream(String path, SdfsClient client) {
        this.path = path;
        this.client = client;
        this.proxyFactory = client.getProxyFactory();
        this.conf = client.getConf();
    }

    @SneakyThrows
    @Override
    public int read() throws IOException {
        if (currentBlock == null) {
            if(blocks == null)
                blocks = proxyFactory.getNNProxy().getBlocks(path).iterator();
            if (!blocks.hasNext())
                return -1;
            currentBlock = blocks.next();
            buffer = new byte[conf.getBlockSize()];
            len = new DNClient(conf).readBlock(currentBlock, buffer);
            if (len == 0) return -1;
            offset = 0;
        }
        int ret = buffer[offset++];
        if (offset == len)
            currentBlock = null;
        if(ret == -1)
            ret = 0xff;
        return ret;
    }

}
