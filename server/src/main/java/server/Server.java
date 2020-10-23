package server;

import sharedConstants.SharedConstants;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.text.SimpleDateFormat;

public class Server {
    private List<ClientHandler> clients;
    private AuthService authService;
    SimpleDateFormat formatter;

    public Server() {
        clients = new CopyOnWriteArrayList<>();
//        authService = new SimpleAuthService();
        authService = new DatabaseAuthService();
        formatter = new SimpleDateFormat(SharedConstants.DATE_FORMAT);
        ServerSocket server = null;
        Socket socket = null;

        try {
            server = new ServerSocket(SharedConstants.SERVER_PORT);
            System.out.println("Server started");

            while (true) {
                socket = server.accept();
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (authService instanceof DatabaseAuthService) {
                ((DatabaseAuthService) authService).disconnect();
            }

            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(ClientHandler sender, String msg) {
        String message;
        String senderNickname = sender.getNickname();
        if (msg.indexOf(SharedConstants.PERSONAL_MESSAGE) == 0) {
            String destinationUserNickname = msg.split("\\s")[1];
            int spaceCount = 2;
            int messageStartIdx = SharedConstants.PERSONAL_MESSAGE.length() + destinationUserNickname.length() + spaceCount;
            message = prepareMessage(sender.getNickname(), msg.substring(messageStartIdx));
            for (ClientHandler c : clients) {
                if (c.getNickname().equals(senderNickname) || c.getNickname().equals(destinationUserNickname)) {
                    c.sendMsg(message);
                }
            }
        } else {
            message = prepareMessage(senderNickname, msg);
            for (ClientHandler c : clients) {
                c.sendMsg(message);
            }
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    private String prepareMessage(String nickName, String message) {
        return String.format("%s\n[%s]: %s\n", formatter.format(new Date()), nickName, message);
    }

    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder(SharedConstants.CLIENT_LIST + " ");

        for (ClientHandler c : clients) {
            sb.append(c.getNickname()).append(" ");
        }
        String message = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public void updateUserNickNameInList(ClientHandler clientHandler, String newNick) {
        for (ClientHandler c : clients) {
            if (c.equals(clientHandler)) {
                c.setNickname(newNick);
                break;
            }
        }
        broadcastClientList();
    }
}

