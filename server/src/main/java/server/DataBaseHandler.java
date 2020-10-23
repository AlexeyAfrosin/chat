package server;

import java.sql.*;

public class DataBaseHandler {

    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null){
            try {
                connection = connect();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return connection;
    }

    private static Connection connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");

        return DriverManager.getConnection("jdbc:sqlite:chat.db");
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void addUser(String login, String password, String nickname){
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("Insert into users(login, password, nickname) values(?, ?, ?)");
            preparedStatement.setString(1, login);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, nickname);
            preparedStatement.executeUpdate();
            preparedStatement.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static boolean isExistUser(String login, String nickname){
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("select 1 ct from users where upper(login) = ? or upper(nickname) = ? limit 1");
            preparedStatement.setString(1, login.toUpperCase());
            preparedStatement.setString(2, nickname.toUpperCase());
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getInt("ct") == 1;
            }
            preparedStatement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public static String getNicknameByLoginAndPassword(String login, String password){
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("select nickname from users where upper(login) = ? and password = ? limit 1");
            preparedStatement.setString(1, login.toUpperCase());
            preparedStatement.setString(2, password);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                return rs.getString("nickname");
            }
            preparedStatement.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    public static void changeNickname(String login, String nickname) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("Update users set nickname = ? where upper(login) = ?");
            preparedStatement.setString(1, nickname);
            preparedStatement.setString(2, login.toUpperCase());
            preparedStatement.executeUpdate();
            preparedStatement.close();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
