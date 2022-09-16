package com.mmp.sdfs.client;

import com.mmp.sdfs.cdnrpc.ReadOp;
import com.mmp.sdfs.cdnrpc.WriteOp;
import com.mmp.sdfs.common.DnAddress;
import com.mmp.sdfs.common.LocatedBlock;
import com.mmp.sdfs.common.ProxyFactory;
import com.mmp.sdfs.common.TaskDef;
import com.mmp.sdfs.conf.SdfsConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class DNClient {

    private final SdfsConfig conf;
    ProxyFactory proxyFactory;

    public DNClient(SdfsConfig conf) {
        this.conf = conf;
        proxyFactory = new ProxyFactory(conf);
    }

    public List<String> deleteBlocks(DnAddress addr, List<String> blocks) throws Exception {
        return proxyFactory.getDNProxy(addr).delete(blocks);
    }

    @SneakyThrows
    public void writeBlock(LocatedBlock block, byte buffer[], int len) {
        ExecutorService pool = Executors.newFixedThreadPool(block.getLocations().size());
        List<Future> futures = new ArrayList<>();
        for (DnAddress addr : block.getLocations()) {
            futures.add(pool.submit(() -> {
                log.info("copying data for {}", "DN-" + addr.getId() + "/" + block.getId());
                try {
                    Socket s = new Socket(addr.getHostname(), addr.getDataPort());
                    OutputStream os = s.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(os);
                    oos.writeObject(new WriteOp(block.getId(), len));
                    os.write(buffer, 0, len);
                    os.flush();
                    int status = s.getInputStream().read();
                    log.debug("write for {} {}", block, status == 0 ? "succeeded" : "failed");
                    s.close();
                } catch (Exception e) {
                    log.error("Error while writing blocks {} to {}", block, addr, e);
                }
            }));
        }
        for (Future future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Error while copying blocks", e);
            }
        }
        pool.shutdownNow();
    }

    public int readBlock(LocatedBlock currentBlock, byte[] buffer) throws Exception {
        DnAddress bestNodeForRead = findBestNodeForRead(currentBlock.getLocations());
        Socket sock = new Socket(bestNodeForRead.getHostname(), bestNodeForRead.getDataPort());
        OutputStream os = sock.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(new ReadOp(currentBlock.getId(), 0, buffer.length));
        InputStream is = sock.getInputStream();
        int len = new ObjectInputStream(is).readInt();
        for (int i = 0; i < len; i += is.read(buffer, i, len - i)) ;
        sock.close();
        return len;
    }

    private DnAddress findBestNodeForRead(List<DnAddress> locations) throws UnknownHostException {
        String myIp = InetAddress.getLocalHost().getHostAddress();
        log.debug("My IP addr: {}", myIp);
        for (DnAddress location : locations) {
            if (location.getHostname().equals(myIp))
                return location;
        }
        return locations.get(0);
    }

    public void startTask(DnAddress addr, TaskDef task) throws Exception {
        proxyFactory.getDNProxy(addr).startTask(task);
    }
}
