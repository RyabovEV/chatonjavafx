package com.example.chatonjavafx.server;

import com.example.chatonjavafx.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatServer {
    private final Map<String, ClientHandler> clients;

    public ChatServer() {
        this.clients = new HashMap<>();
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(8189);
             AuthService authService = new inMemoryAuthService()) {
            while (true) {
                System.out.println("Ожидается подключение");
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(socket, this, authService);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(ClientHandler client) {
        clients.put(client.getNick(), client);
        broadcastClientsList();
    }

    public boolean isNickBusy(String nick) {
        return clients.get(nick) != null;
    }

    private void broadcastClientsList() {
        String nicks = clients.values().stream()
                .map(ClientHandler::getNick)
                .collect(Collectors.joining(" "));
        broadcast(Command.CLIENTS, nicks);
    }

    public void broadcast(Command command, String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(Command.MESSAGE, message);
        }
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client.getNick());
        broadcastClientsList();
    }

    public void sendPrivateMessage(ClientHandler from, String nickTo, String message) {
        ClientHandler clientTo = clients.get(nickTo);
        if (clientTo == null) {
            from.sendMessage(Command.ERROR, "Пользователь не авторизован!");
            return;
        }
        clientTo.sendMessage(Command.MESSAGE, "От " + from.getNick() + ": " + message);
        from.sendMessage(Command.MESSAGE, "Участнику " + nickTo + ": " + message);
    }
}
