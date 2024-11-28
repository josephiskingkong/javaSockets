package com.chat.client;

import java.io.*;
import java.net.Socket;
import java.util.Properties;

public class ChatClient {
    private String host;
    private int port;
    private String nickname;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ChatClient() {
        loadConfig();
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            props.load(input);
            host = props.getProperty("server.host");
            port = Integer.parseInt(props.getProperty("server.port"));
        } catch (IOException e) {
            System.err.println("Error loading configuration");
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            socket = new Socket(host, port);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));

            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                serverMessage = serverMessage.trim();

                if (serverMessage.equals("Enter your nickname:")) {
                    System.out.print("Enter your nickname: ");
                    nickname = userInputReader.readLine().trim();

                    if (nickname.isEmpty()) {
                        System.out.println("Nickname cannot be empty. Please enter a valid nickname.");
                        continue;
                    }

                    out.println(nickname);
                } else if (serverMessage.equals("Nickname already taken. Please choose another one.") ||
                        serverMessage.equals("Nickname cannot be empty.")) {
                    System.out.println(serverMessage);
                    System.out.print("Enter your nickname: ");
                    nickname = userInputReader.readLine().trim();

                    if (nickname.isEmpty()) {
                        System.out.println("Nickname cannot be empty. Please enter a valid nickname.");
                        continue;
                    }

                    out.println(nickname);
                } else if (serverMessage.equals("Nickname accepted.")) {
                    System.out.println("Nickname accepted by server.");
                    break;
                } else {
                    System.out.println("Received unexpected message: " + serverMessage);
                }
            }

            if (serverMessage == null) {
                System.out.println("Server closed the connection.");
                return;
            }

            MessageReceiver receiver = new MessageReceiver(in);
            receiver.start();

            while (true) {
                System.out.println("Choose message type: 1 - Private, 2 - Broadcast");
                String choice = userInputReader.readLine().trim();

                if ("1".equals(choice)) {
                    Message request = new Message(MessageType.USER_LIST_REQUEST, "", "");
                    out.println(request.toJson());

                    Thread.sleep(500);

                    System.out.print("Enter recipient nickname: ");
                    String recipient = userInputReader.readLine().trim();
                    System.out.print("Enter message: ");
                    String content = userInputReader.readLine().trim();
                    Message message = new Message(MessageType.PRIVATE, content, recipient);
                    out.println(message.toJson());
                } else if ("2".equals(choice)) {
                    System.out.print("Enter message: ");
                    String content = userInputReader.readLine().trim();
                    Message message = new Message(MessageType.BROADCAST, content, "");
                    out.println(message.toJson());
                } else {
                    System.out.println("Invalid choice");
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Connection error");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatClient().start();
    }

    private class MessageReceiver extends Thread {
        private BufferedReader in;

        public MessageReceiver(BufferedReader in) {
            this.in = in;
        }

        public void run() {
            String incomingMessage;
            try {
                while ((incomingMessage = in.readLine()) != null) {
                    System.out.println(incomingMessage);
                }
            } catch (IOException e) {
                System.err.println("Connection closed");
            }
        }
    }
}