package server;

import sharedConstants.SharedConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.util.logging.*;

public class Server {
    private List<ClientHandler> clients;
    private AuthService authService;
    SimpleDateFormat formatter;
    Logger logger = Logger.getLogger(this.getClass().getName());
    ExecutorService clientHandlerServices = Executors.newFixedThreadPool(SharedConstants.MAX_USER_COUNT);

    public Server() {
        clients = new CopyOnWriteArrayList<>();
//        authService = new SimpleAuthService();
        authService = new DatabaseAuthService();
        formatter = new SimpleDateFormat(SharedConstants.DATE_FORMAT);
        ServerSocket server = null;
        Socket socket = null;

        prepareLogger();

        try {
            server = new ServerSocket(SharedConstants.SERVER_PORT);

            writeLog(Level.INFO, "Server started");
            while (true) {
                try {
                    socket = server.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    writeLog(Level.SEVERE, String.valueOf(e.getStackTrace()));
                }
                clientHandlerServices.execute(new ClientHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
            writeLog(Level.SEVERE, String.valueOf(e.getStackTrace()));
        } finally {
            try {
                socket.close();
                writeLog(Level.INFO, "Соединение закрыто");
            } catch (IOException e) {
                e.printStackTrace();
                writeLog(Level.SEVERE, String.valueOf(e.getStackTrace()));
            }

            if (authService instanceof DatabaseAuthService) {
                ((DatabaseAuthService) authService).disconnect();
                writeLog(Level.INFO, "Соединение с базой данных закрыто");
            }

            try {
                server.close();
                clientHandlerServices.shutdown();
                writeLog(Level.INFO, "Сервер остановлен");
            } catch (IOException e) {
                e.printStackTrace();
                writeLog(Level.SEVERE, String.valueOf(e.getStackTrace()));
            }
        }
    }

    private void prepareLogger() {
        Handler fileHandler = null;

        try {
            LogManager logManager = LogManager.getLogManager();
            logManager.readConfiguration(new FileInputStream("server/logging.properties"));

            prepareLogDir();
            fileHandler = new FileHandler(Constants.LOG_FILE_PATH + "/log_%g.txt", true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);


        } catch (IOException e) {
            e.printStackTrace();
            writeLog(Level.SEVERE, String.valueOf(e.getStackTrace()));
        }
        logger.addHandler(fileHandler);
    }

    private void prepareLogDir() {
        File dir = new File(Constants.LOG_FILE_PATH);
        if (!dir.exists()) {
            dir.mkdir();
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
                    c.sendMsg(message, Level.FINEST, String.format("Личное сообщение от '%s' к '%s': %s", senderNickname, destinationUserNickname, message));
                }
            }
        } else {
            message = prepareMessage(senderNickname, msg);
            for (ClientHandler c : clients) {
                c.sendMsg(message, Level.FINEST, String.format("Сообщение от '%s' к '%s': %s", senderNickname, c.getNickname(), message));
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
            c.sendMsg(message, Level.FINEST, String.format("Список пользователей: %s", message));
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

    public void writeLog(Level level, String message) {
        if (logger.isLoggable(level)) {
            logger.log(level, message);
        }
    }
}

