package server;

import sharedConstants.SharedConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.*;

public class ClientHandler implements Runnable {
    DataInputStream in;
    DataOutputStream out;
    Server server;
    Socket socket;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    public void sendMsg(String msg, Level logLevel, String logMessage) {
        try {
            out.writeUTF(msg);
            server.writeLog(logLevel, logMessage);
        } catch (IOException e) {
            e.printStackTrace();
            server.writeLog(Level.SEVERE, String.valueOf(e.getStackTrace()));
        }
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            server.writeLog(Level.INFO, "Client connected " + socket.getRemoteSocketAddress());

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
                            sendMsg(SharedConstants.REGISTRATION_OK, Level.INFO, String.format("Пользователь '%s' c ником '%s' зарегистрировался", token[1], token[2]));
                        } else {
                            sendMsg(SharedConstants.REGISTRATION_NO, Level.INFO, String.format("Регистрация пользователя '%s' c ником '%s' не прошла", token[1], token[2]));
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

                                sendMsg(SharedConstants.AUTH_OK + " " + newNick, Level.INFO, String.format("Пользователь '%s' авторизовался", newNick));
                                server.subscribe(this);
                                break;
                            } else {
                                sendMsg("С этим логином уже вошли в чат", Level.WARNING, String.format("С этим %s уже вошли в чат", token[1]));
                            }
                        } else {
                            sendMsg("Неверный логин / пароль", Level.WARNING, String.format("Неверный логин / пароль для %s/%s", token[1], token[2]));
                        }
                    }
                }
                //цикл работы


                while (true) {
//                        try {
                    String str = in.readUTF();

                    if (str.startsWith(SharedConstants.CHANGE_NICKNAME + " ")) {

                        String[] token = str.split("\\s");
                        if (token.length < 2) {
                            continue;
                        }

                        String newNick = token[2];

                        if (server.getAuthService().changeNickname(token[1], newNick)) {
                            sendMsg(SharedConstants.CHANGE_NICKNAME_OK + " " + newNick, Level.INFO, String.format("Ник '%s' изменен на '%s'", token[1], newNick));
                            server.updateUserNickNameInList(this, newNick);
                        } else {
                            sendMsg("Ник '" + newNick + "' уже используется", Level.WARNING, "Ник '" + newNick + "' уже используется");
                        }
                    }

                    if (str.equals(SharedConstants.END_CONNECTION)) {
                        sendMsg(SharedConstants.END_CONNECTION, Level.INFO, String.format("Пришла команда завершения сеанса от '%s'", this.getNickname()));
                        break;
                    }
                    server.broadcastMsg(this, str);
//                        }catch (SocketException e){
//                             writeLog(Level.INFO, "Client " + nickname + " disconnected");
//                        }
                }
            } catch (SocketTimeoutException e) {
                sendMsg(SharedConstants.SOCKET_TIMEOUT_EXCEPTION, Level.SEVERE, String.format("Отключение по SOCKET_TIMEOUT_EXCEPTION пользователя '%s'", this.getNickname()));
            } catch (IOException e) {
                e.printStackTrace();
                server.writeLog(Level.SEVERE, String.valueOf(e.getStackTrace()));
            } finally {
                server.unsubscribe(this);
                server.writeLog(Level.INFO, ("Client disconnected " + socket.getRemoteSocketAddress()));
                try {
                    socket.close();
                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    server.writeLog(Level.SEVERE, String.valueOf(e.getStackTrace()));
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
            server.writeLog(Level.SEVERE, String.valueOf(e.getStackTrace()));
        }
    }
}
