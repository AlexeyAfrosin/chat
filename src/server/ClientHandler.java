package server;

import sharedConstants.SharedConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler {
    DataInputStream in;
    DataOutputStream out;
    Server server;
    Socket socket;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Client connected " + socket.getRemoteSocketAddress());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith(SharedConstants.REGISTRATION + " ")) {
                            String[] token = str.split("\\s");
                            if (token.length < 4) {
                                continue;
                            }
                            boolean isRegistered = server.getAuthService()
                                    .registration(token[1], token[2], token[3]);
                            if (isRegistered) {
                                sendMsg(SharedConstants.REGISTRATION_OK);
                            } else {
                                sendMsg(SharedConstants.REGISTRATION_NO);
                            }
                        }

                        if (str.startsWith(SharedConstants.AUTH + " ")) {
                            socket.setSoTimeout(SharedConstants.SOCKET_TIMEOUT);
                            String[] token = str.split("\\s");
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            if (newNick != null) {
                                login = token[1];
                                if (!server.isLoginAuthenticated(login)) {
                                    socket.setSoTimeout(SharedConstants.SOCKET_NO_TIMEOUT);
                                    nickname = newNick;

                                    sendMsg(SharedConstants.AUTH_OK + " " + newNick);
                                    server.subscribe(this);
                                    break;
                                } else {
                                    sendMsg("С этим логином уже вошли в чат");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }
                    }
                    //цикл работы


                    while (true) {
//                        try {
                        String str = in.readUTF();

                        if (str.equals(SharedConstants.END_CONNECTION)) {
                            sendMsg(SharedConstants.END_CONNECTION);
                            break;
                        }
                        server.broadcastMsg(this, str);
//                        }catch (SocketException e){
//                            System.out.println("Client " + nickname + " disconnected");
//                        }
                    }
                } catch (SocketTimeoutException e) {
                    sendMsg(SharedConstants.SOCKET_TIMEOUT_EXCEPTION);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("Client disconnected " + socket.getRemoteSocketAddress());
                    try {
                        socket.close();
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
