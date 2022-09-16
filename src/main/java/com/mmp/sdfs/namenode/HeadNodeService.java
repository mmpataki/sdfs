package com.mmp.sdfs.namenode;

import com.google.gson.Gson;
import com.mmp.sdfs.client.DNClient;
import com.mmp.sdfs.common.*;
import com.mmp.sdfs.conf.HeadNodeConfig;
import com.mmp.sdfs.nndnrpc.DnHeartbeat;
import com.mmp.sdfs.nndnrpc.DnHeartbeatResponse;
import com.mmp.sdfs.nndnrpc.HeadNode;
import com.mmp.sdfs.rpc.RpcContext;
import com.mmp.sdfs.server.Server;
import com.mmp.sdfs.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class HeadNodeService extends Server implements HeadNode {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Api {
        String value();
    }

    interface Handler {
        Object handle(Map<String, String> params) throws Exception;
    }

    Map<String, Handler> handlers = new HashMap<>();
    Gson gson = new Gson();


    private final HeadNodeConfig conf;
    Map<String, DnRef> dns = new HashMap<>();
    Map<String, TaskState> taskState = new HashMap<>();
    NameStore store;

    public HeadNodeService(HeadNodeConfig conf) throws Exception {
        super(conf.getInfoPort());
        this.conf = conf;
        if (!new File(conf.getNamedir()).exists()) {
            Files.createDirectory(Paths.get(conf.getNamedir()));
        }
        store = (NameStore) Class.forName(conf.getStoreClass()).getConstructor(HeadNodeConfig.class, Map.class).newInstance(conf, dns);
        new BlockDeleter(conf, store, dns).start();
        Arrays.stream(getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Api.class)).forEach(m -> {
            handlers.put(m.getAnnotation(Api.class).value(), (p) -> m.invoke(this, p));
        });
    }

    @Override
    public void start() throws IOException {
        super.start();
    }

    public DnHeartbeatResponse heartBeat(DnHeartbeat dnRegister) {
        dns.put(dnRegister.getId(), new DnRef(dnRegister, RpcContext.getRpcContext().getSock().getInetAddress().getHostAddress()));
        dnRegister.getTaskStates().forEach((t, s) -> {
            if (taskState.containsKey(t))
                taskState.get(t).setState(s);
        });
        return new DnHeartbeatResponse();
    }

    public FileStat create(String path) throws Exception {
        return store.create(path, "anonymous", conf.getReplicationFactor());
    }

    public LocatedBlock addBlock(String path) throws Exception {
        return store.addBlock(path, "anonymous");
    }

    public List<LocatedBlock> getBlocks(String path) throws Exception {
        return store.getBlocks(path);
    }

    public List<FileStat> list(String dirPath) throws Exception {
        return store.list(dirPath);
    }

    public void delete(String path) throws Exception {
        store.delete(path);
    }

    @Override
    public void closeFile(String path, long size) throws Exception {
        store.close(path, size);
    }

    @Override
    public String startTask(TaskDef task) throws Exception {
        String taskId = task.getId();
        String nodeId = pickABestNode(task);
        DnAddress addr = dns.get(nodeId).getAddr();
        new DNClient(conf).startTask(addr, task);
        taskState.put(taskId, TaskState.builder().state(-9999).node(nodeId).id(taskId).build());
        return taskId;
    }

    @Override
    public List<TaskState> getStatusOf(String[] taskIds) throws Exception {
        return Arrays.stream(taskIds).map(id -> taskState.get(id)).collect(Collectors.toList());
    }

    @Override
    public FileStat get(String path) throws Exception {
        return store.get(path);
    }

    private String pickABestNode(TaskDef task) {
        return task.getPreferredNodes().get(0);
    }


    // info api
    @Api("/nodes")
    Object getDataNodeInfo(Map<String, String> params) {
        if (params.containsKey("id"))
            return dns.get(params.get("id"));
        return dns;
    }

    @Api("/tasks")
    Object getTaskState(Map<String, String> params) {
        if (params.containsKey("id"))
            return taskState.get(params.get("id"));
        if(params.containsKey("nodeid")){
            String node = params.get("nodeid");
            Map<String, TaskState> tasks = new HashMap<>();
            taskState.values().stream().filter(t -> t.getNode().equals(node)).forEach(t -> tasks.put(t.getId(), t));
            return tasks;
        }
        return taskState;
    }

    boolean __debug = true;

    @Override
    public void process(Socket sock) throws Exception {
        // simple HTTP server
        String resource = new BufferedReader(new InputStreamReader(sock.getInputStream())).readLine().split(" ")[1];
        Map<String, String> params = new HashMap<>();
        if (resource.contains("?")) {
            Arrays.stream(resource.split("\\?")[1].split("&")).forEach(kvp -> {
                if (kvp.contains("="))
                    params.put(kvp.substring(0, kvp.indexOf('=')), kvp.substring(kvp.indexOf('=') + 1));
            });
            resource = resource.substring(0, resource.indexOf('?'));
        }
        if (resource.startsWith("/api")) {
            String api = resource.substring(4);
            if (!handlers.containsKey(api)) {
                sendHeader(sock.getOutputStream(), 404);
            } else {
                try {
                    Object ret = handlers.get(api).handle(params);
                    sendHeader(sock.getOutputStream(), 200);
                    String s = gson.toJson(ret);
                    sock.getOutputStream().write(s.getBytes());
                } catch (Exception e) {
                    log.error("Error while handling req: {}", api, e);
                    sendHeader(sock.getOutputStream(), 500);
                    sock.getOutputStream().write(e.getMessage().getBytes());
                }
            }
        } else {
            File f = new File((__debug ? "C:\\Users\\mpataki\\Desktop\\sdfs\\src\\main\\resources\\public" : "public") + (resource.equals("/") ? "/index.html" : resource));
            sendHeader(sock.getOutputStream(), f.exists() ? 200 : 404);
            try (FileInputStream fis = new FileInputStream(f)) {
                IOUtils.copy(fis, sock.getOutputStream(), (int) f.length());
            }
        }
        sock.getOutputStream().write("\n\n".getBytes());
        sock.getOutputStream().flush();
        sock.close();
    }

    private void sendHeader(OutputStream os, int code) throws IOException {
        os.write(String.format("HTTP/1.1 %d OK\n\n", code).getBytes());
    }
}
