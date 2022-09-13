package com.mmp.sdfs.client;

import com.mmp.sdfs.common.FileStat;
import com.mmp.sdfs.common.ProxyFactory;
import com.mmp.sdfs.conf.SdfsClientConfig;
import lombok.Getter;
import com.mmp.sdfs.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

public class SdfsClient {

    @Getter
    private final SdfsClientConfig conf;

    @Getter
    ProxyFactory proxyFactory;

    public SdfsClient(SdfsClientConfig conf) {
        this.conf = conf;
        proxyFactory = new ProxyFactory(conf);
    }

    public SdfsOutputStream create(String path, int replicas) throws Exception {
        return new SdfsOutputStream(path, this);
    }

    public SdfsOutputStream create(String path) throws Exception {
        return create(path, conf.getReplicationFactor());
    }

    public void delete(String path) throws Exception {
        proxyFactory.getNNProxy().delete(path);
    }

    public SdfsInputStream open(String path) {
        return new SdfsInputStream(path, this);
    }

    public static void main(String[] args) throws Exception {
        SdfsClient fsc = new SdfsClient(new SdfsClientConfig(args));
        fsc.delete("/tmp/a/1.txt");
        File file = new File("C:\\Users\\mpataki\\Downloads\\tmp\\e1.txt");

        try (SdfsOutputStream dos = fsc.create("/tmp/a/1.txt")) {
            FileInputStream fis = new FileInputStream(file);
            IOUtils.copy(fis, dos, (int) file.length());
        }

        try (SdfsInputStream is = fsc.open("/tmp/a/1.txt")) {
            FileOutputStream fos = new FileOutputStream("C:\\Users\\mpataki\\Downloads\\tmp\\e2.txt");
            IOUtils.copy(is, fos, (int) file.length());
        }
    }

    public List<FileStat> list(String path) throws Exception {
        return proxyFactory.getNNProxy().list(path);
    }
}
