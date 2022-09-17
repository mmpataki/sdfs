package com.mmp.sdfs.utils;

import com.google.gson.Gson;
import com.mmp.sdfs.server.Server;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HttpServer extends Server {

    boolean __debug = Boolean.getBoolean("DEBUG");

    public HttpServer(int port) {
        super(port);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Api {
        String value();
    }

    interface Handler {
        Object handle(Map<String, String> params) throws Exception;
    }

    Map<String, Handler> handlers = new HashMap<>();
    Gson gson = new Gson();

    public void registerController(Object o) {
        Arrays.stream(o.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Api.class)).forEach(m -> {
            m.setAccessible(true);
            handlers.put(m.getAnnotation(Api.class).value(), (p) -> m.invoke(o, p));
        });
    }

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
                    if (InputStream.class.isAssignableFrom(ret.getClass())) {
                        copyData((InputStream) ret, sock.getOutputStream());
                    } else {
                        OutputStreamWriter osw = new OutputStreamWriter(sock.getOutputStream());
                        gson.toJson(ret, osw);
                        osw.close();
                    }
                } catch (Exception e) {
                    log.error("Error while handling req: {}", api, e);
                    sendHeader(sock.getOutputStream(), 500);
                    sock.getOutputStream().write(e.getMessage().getBytes());
                }
            }
        } else {
            InputStream is = null;
            resource = resource.equals("/") ? "/index.html" : resource;
            try {
                if (__debug) {
                    is = new FileInputStream("C:\\Users\\mpataki\\Desktop\\sdfs\\src\\main\\resources\\public" + resource);
                } else {
                    is = getClass().getResourceAsStream("/public" + resource);
                }
                sendHeader(sock.getOutputStream(), 200);
                copyData(is, sock.getOutputStream());
                is.close();
            } catch (Exception e) {
                log.error("error while processing req: {}", resource, e);
                sendHeader(sock.getOutputStream(), 404);
            }
        }
        sock.getOutputStream().write("\n\n".getBytes());
        sock.getOutputStream().flush();
        sock.close();
    }

    private static void copyData(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

    private void sendHeader(OutputStream os, int code) throws IOException {
        os.write(String.format("HTTP/1.1 %d OK\n\n", code).getBytes());
    }
}
