package com.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private Socket socket;
    private ChatServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String nickname;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getNickname() {
        return nickname;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                out.println("Enter your nickname:");
                nickname = in.readLine();

                if (nickname == null || nickname.isEmpty()) {
                    out.println("Nickname cannot be empty.");
                } else if (server.isNicknameTaken(nickname)) {
                    out.println("Nickname already taken. Please choose another one.");
                } else {
                    server.addClient(nickname, this);
                    out.println("Nickname accepted.");
                    logger.info("User connected: {}", nickname);
                    break;
                }
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                handleMessage(inputLine);
            }
        } catch (IOException e) {
            logger.error("Error in client handler", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error closing socket", e);
            }
            if (nickname != null) {
                server.removeClient(nickname);
                logger.info("User disconnected: {}", nickname);
            }
        }
    }

    private void handleMessage(String input) {
        Message message = Message.fromJson(input);

        switch (message.getType()) {
            case PRIVATE:
                handlePrivateMessage(message);
                break;
            case BROADCAST:
                handleBroadcastMessage(message);
                break;
            case USER_LIST_REQUEST:
                handleUserListRequest();
                break;
            default:
                logger.warn("Unknown message type from {}: {}", nickname, message.getType());
        }
    }

    private void handlePrivateMessage(Message message) {
        String recipient = message.getRecipient();
        ClientHandler recipientHandler = server.getClients().get(recipient);
        if (recipientHandler != null) {
            recipientHandler.sendMessage("Private message from " + nickname + ": " + message.getContent());
            logger.info("Private message from {} to {}: {}", nickname, recipient, message.getContent());
        } else {
            sendMessage("User not found: " + recipient);
        }
    }

    private void handleBroadcastMessage(Message message) {
        for (Map.Entry<String, ClientHandler> entry : server.getClients().entrySet()) {
            if (!entry.getKey().equals(nickname)) {
                entry.getValue().sendMessage("Broadcast from " + nickname + ": " + message.getContent());
            }
        }
        logger.info("Broadcast message from {}: {}", nickname, message.getContent());
    }

    private void handleUserListRequest() {
        StringBuilder userList = new StringBuilder("Connected users:");
        for (String user : server.getClients().keySet()) {
            if (!user.equals(nickname)) {
                userList.append("\n").append(user);
            }
        }
        sendMessage(userList.toString());
    }
}