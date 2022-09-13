package com.mmp.sdfs.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public abstract class Server {

    int port;
    ServerSocket ssock;

    public Server(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        new Thread(() -> {
            try {
                ssock = new ServerSocket(port);
                log.info("Server started, Listening on {}", port);
                while (true) {
                    try {
                        final Socket sock = ssock.accept();
                        new Thread(() -> {
                            try {
                                process(sock);
                                sock.close();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                    } catch (Exception e) {
                        log.error("Error while processing client connections", e);
                    }
                }
            } catch (IOException e) {
                log.error("Error while setting up server", e);
            }
        }, "Server").start();
    }

    public abstract void process(Socket sock) throws Exception;
}
