package com.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatServer {
    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);
    private ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    private int port;

    public ChatServer() {
        loadConfig();
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            props.load(input);
            port = Integer.parseInt(props.getProperty("server.port"));
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port {}", port);
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this);
                clientHandler.start();
            }
        } catch (IOException e) {
            logger.error("Server error", e);
        }
    }

    public synchronized boolean isNicknameTaken(String nickname) {
        return clients.containsKey(nickname);
    }

    public synchronized void addClient(String nickname, ClientHandler clientHandler) {
        clients.put(nickname, clientHandler);
    }

    public synchronized void removeClient(String nickname) {
        clients.remove(nickname);
    }

    public ConcurrentHashMap<String, ClientHandler> getClients() {
        return clients;
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}