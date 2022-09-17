package com.mmp.sdfs.datanode;

import com.mmp.sdfs.cdnrpc.DataNodeOp;
import com.mmp.sdfs.cdnrpc.ReadOp;
import com.mmp.sdfs.cdnrpc.WriteOp;
import com.mmp.sdfs.common.TaskDef;
import com.mmp.sdfs.conf.SdfsClientConfig;
import com.mmp.sdfs.conf.WorkerNodeConfig;
import com.mmp.sdfs.server.Server;
import com.mmp.sdfs.utils.HttpServer;
import com.mmp.sdfs.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class WorkerNodeService extends Server implements WorkerNode {

    String myId;
    HttpServer httpServer;
    private final WorkerNodeConfig conf;
    private final SdfsClientConfig clientConfig;
    private final Map<String, Integer> taskStates = new HashMap<>();

    public WorkerNodeService(WorkerNodeConfig conf, SdfsClientConfig clientConfig) throws Exception {
        super(conf.getDataPort());
        this.conf = conf;
        this.clientConfig = clientConfig;
        if (!new File(conf.getDnDir()).exists()) {
            formatDn();
        } else {
            myId = getMyId();
        }
        new HeartBeater(conf, getMyId(), taskStates).start();
        httpServer = new HttpServer(conf.getInfoPort());
        httpServer.registerController(this);
        httpServer.start();
    }

    private void formatDn() throws Exception {
        Files.createDirectory(Paths.get(conf.getDnDir()));
        Files.createDirectory(Paths.get(conf.getBlockDir()));
        FileWriter fw = new FileWriter(getIdFile());
        myId = UUID.randomUUID().toString();
        fw.write(myId.toCharArray());
        fw.close();
    }

    @Override
    public void process(Socket sock) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
        DataNodeOp op = (DataNodeOp) ois.readObject();
        log.trace("Datanode op from {} -> {}", sock.getRemoteSocketAddress(), op);
        if (op instanceof ReadOp) {
            ReadOp rop = (ReadOp) op;
            read(rop.getBlockId(), rop.getStart(), rop.getLen(), sock);
        } else if (op instanceof WriteOp) {
            WriteOp wop = (WriteOp) op;
            write(wop.getBlockId(), wop.getSize(), sock);
        } else {
            log.error("Unknown operation {}", op);
        }
    }

    @Override
    public List<String> delete(List<String> blocks) {
        List<String> passed = new ArrayList<>();
        for (String block : blocks) {
            try {
                Files.delete(Paths.get(getBlockFile(block)));
                passed.add(block);
            } catch (NoSuchFileException nfe) {
                log.warn("block {} was already deleted", block);
                passed.add(block);
            } catch (Exception e) {
                log.error("Error while deleting block {}", block, e);
            }
        }
        return passed;
    }

    @Override
    public void startTask(TaskDef task) throws Exception {
        String taskId = task.getTaskId();
        try {
            taskStates.put(taskId, -9999);
            new TaskExecutor(task, clientConfig, status -> taskStates.put(taskId, status)).start();
        } catch (Throwable e) {
            log.error("Error while starting task: {}", taskId, e);
            taskStates.put(taskId, -1);
            throw e;
        }
    }

    public String getIdFile() {
        return conf.getDnDir() + "/id";
    }

    public String getMyId() throws IOException {
        return (new BufferedReader(new FileReader(getIdFile()))).readLine();
    }

    public String getBlockFile(String blockId) {
        return String.format("%s/%s", conf.getBlockDir(), blockId);
    }

    public int write(String blockId, int len, Socket sock) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(getBlockFile(blockId))) {
            IOUtils.copy(sock.getInputStream(), fos, len);
        }
        sock.getOutputStream().write(0);
        return len;
    }

    public int read(String blockId, int start, int numBytes, Socket sock) throws IOException {
        File f = new File(getBlockFile(blockId));
        if (!f.exists()) {
            sock.close();
            throw new FileNotFoundException("Block " + blockId + " not found");
        }
        int len = (int) Math.min(numBytes, f.length() - start), retlen = len;
        OutputStream os = sock.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeInt(len);
        oos.flush();
        try (FileInputStream fis = new FileInputStream(f)) {
            IOUtils.copy(fis, sock.getOutputStream(), len);
        }
        os.close();
        return retlen;
    }

    @HttpServer.Api("/task/logs")
    InputStream getLogs(Map<String, String> params) throws FileNotFoundException {
        if(!params.containsKey("taskid")) {
            throw new RuntimeException("Specify taskid as query parameter");
        }
        return new FileInputStream(TaskExecutor.logPath(params.get("taskid")));
    }

}
