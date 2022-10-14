package com.example.chatonjavafx.server;

import com.example.chatonjavafx.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private ChatServer server;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;
    private AuthService authService;

    private Clock clock;
    private Thread clockThread;

    class Clock implements Runnable {
        private boolean statTimer = true;

        public boolean isStatTimer() {
            return this.statTimer;
        }

        public void run() {
            Thread current = Thread.currentThread();
            while (!current.isInterrupted()) {
                try {
                    Thread.sleep(120000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    current.interrupt();
                }
                this.statTimer = false;
                closeConnection();
            }
        }
    }

    public ClientHandler(Socket socket, ChatServer server, AuthService authService) {
        try {
            this.server = server;
            this.socket = socket;
            this.authService = authService;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            this.clock = new Clock();
            this.clockThread = new Thread(clock);
            this.clockThread.start();

            if (this.clock.isStatTimer() != true) {
                sendMessage(Command.ERROR, "таймаут");
                sendMessage(Command.END);
            }
            System.out.println("дальше");

            new Thread(() -> {
                try {
                    authenticte();
                    readMessages();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (
                IOException e) {
            e.printStackTrace();
        }

    }

    private void authenticte() {
        while (true) {
            Command command = null;
            String message = null;
            try {
                message = in.readUTF();
                command = Command.getCommand(message);
                if (command == Command.AUTH) {
                    final String[] params = command.parse(message);
                    final String login = params[0];
                    final String password = params[1];
                    final String nick = authService.getNickByLoginAndPassword(login, password);
                    if (nick != null) {
                        if (server.isNickBusy(nick)) {
                            sendMessage(Command.ERROR, "Пользователь уже авторизован");
                            continue;
                        }
                        sendMessage(Command.AUTHOK, nick);
                        this.nick = nick;
                        server.broadcast(Command.MESSAGE, "Пользователь " + nick + " зашел в чат!");
                        server.subscribe(this);
                        break;
                    } else sendMessage(Command.ERROR, "Неверные логин или пароль");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(Command command, String... params) {
        sendMessage(command.collectMessage(params));
    }

    private void closeConnection() {
        sendMessage(Command.END);
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (socket != null) {
            server.unsubscribe(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMessages() {
        while (true) {
            try {
                final String message = in.readUTF();
                final Command command = Command.getCommand(message);
                if (command == Command.END) {
                    break;
                }
                if (command == Command.PRIVATE_MESSAGE) {
                    final String[] params = command.parse(message);
                    server.sendPrivateMessage(this, params[0], params[1]);
                    continue;
                }
                server.broadcast(Command.MESSAGE, nick + ": " + command.parse(message)[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getNick() {
        return nick;
    }
}
