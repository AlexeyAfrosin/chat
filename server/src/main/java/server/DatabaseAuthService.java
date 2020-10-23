package server;

import java.sql.*;

public class DatabaseAuthService implements AuthService {

    private Connection connection;

    public DatabaseAuthService() {
        connection = DataBaseHandler.getConnection();
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        return DataBaseHandler.getNicknameByLoginAndPassword(login, password);
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        boolean isExistUser = DataBaseHandler.isExistUser(login, nickname);
        if (!isExistUser){
            DataBaseHandler.addUser(login, password, nickname);
        }
        return !isExistUser;
    }

    @Override
    public boolean changeNickname(String login, String nickname) {
        boolean isExistUser = DataBaseHandler.isExistUser(login, nickname);
        if (isExistUser){
            DataBaseHandler.changeNickname(login, nickname);
        }
        return isExistUser;
    }

    public void disconnect(){
        DataBaseHandler.disconnect();
    }

}
