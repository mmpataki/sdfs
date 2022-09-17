package com.mmp.sdfs.client;

import com.mmp.sdfs.common.LocatedBlock;
import com.mmp.sdfs.common.ProxyFactory;
import com.mmp.sdfs.conf.SdfsClientConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
public class SdfsInputStream extends InputStream {

    private final SdfsClient client;
    private final String path;
    private final ProxyFactory proxyFactory;
    private final SdfsClientConfig conf;

    List<LocatedBlock> blocks;
    LocatedBlock currentBlock;

    byte[] buffer;
    int len, offset, curBlock = 0;

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
            if (blocks == null) {
                blocks = proxyFactory.getNNProxy().getBlocks(path);
            }
            if (curBlock >= blocks.size())
                return -1;
            currentBlock = blocks.get(curBlock++);
            buffer = new byte[conf.getBlockSize()];
            len = new DNClient(conf).readBlock(currentBlock, buffer);
            if (len == 0) return -1;
            offset = 0;
        }
        int ret = buffer[offset++];
        if (offset == len) {
            currentBlock = null;
        }
        if (ret == -1)
            ret = 0xff;
        return ret;
    }

    public void seek(long pos) {
        curBlock = (int) (pos / conf.getBlockSize());
        offset = (int) (pos % conf.getBlockSize());
        currentBlock = null;
    }

}
