package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sharedConstants.SharedConstants;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;


import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public ListView clientList;
    @FXML
    public VBox clientListVbox;

    @FXML
    HBox msgPanel;

    @FXML
    HBox authPanel;

    @FXML
    TextField loginField;

    @FXML
    TextField passwordField;

    @FXML
    Button btnSendMessage;

    @FXML
    TextField tfMessageText;

    @FXML
    TextArea taChatMessages;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Stage stage;
    private Stage registrationStage;
    private RegistrationController registrationController;

    private String nickname;
    private String login;

    private static boolean isSocketTimeoutException;

    private static void setIsSocketTimeoutException(boolean pIsSocketTimeoutException) {
        isSocketTimeoutException = pIsSocketTimeoutException;
    }

    public static boolean isIsSocketTimeoutException() {
        return isSocketTimeoutException;
    }

    public void setAuthenticated(boolean authenticated) {
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);

        clientListVbox.setVisible(authenticated);
        clientListVbox.setManaged(authenticated);

        if (!authenticated) {
            nickname = "";
            setTitle(Constants.CHAT_TITLE);
        } else {
            setTitle(String.format("[ %s ] - %s", nickname, Constants.CHAT_TITLE));
        }
        if (isIsSocketTimeoutException()) {
            taChatMessages.appendText("Превышено время ожидания авторизации");
        } else {
            taChatMessages.clear();
        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) tfMessageText.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (socket != null && !socket.isClosed()) {
                    try {
                        out.writeUTF(SharedConstants.END_CONNECTION);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });

        setIsSocketTimeoutException(false);
        tfMessageText.requestFocus();
        btnSendMessage.setDisable(true);
        setAuthenticated(false);
        createRegistrationWindow();
    }

    public void btnSendMessage(ActionEvent actionEvent) {
        sendMessage();
    }

    public void tfMessageTextOnKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            sendMessage();
        }
    }

    public void tfMessageTextOnKeyReleased(KeyEvent event) {
        btnSendMessage.setDisable(tfMessageText.getText().trim().length() == 0);
    }

    public void sendMessage() {
        String text = tfMessageText.getText().trim();
        if (text.length() > 0) {
            try {
                out.writeUTF(tfMessageText.getText());
                tfMessageText.clear();
                tfMessageText.requestFocus();
                btnSendMessage.setDisable(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connect() {
        try {
            socket = new Socket(SharedConstants.SERVER_HOST, SharedConstants.SERVER_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        setIsSocketTimeoutException(str.equals(SharedConstants.SOCKET_TIMEOUT_EXCEPTION));

                        if (str.startsWith(SharedConstants.AUTH_OK + " ")) {
                            nickname = str.split("\\s")[1];
                            setAuthenticated(true);
                            break;
                        }

                        if (str.startsWith(SharedConstants.REGISTRATION_OK)) {
                            registrationController.addMessageTextArea("Регистрация прошла успешно");
                        }
                        if (str.startsWith(SharedConstants.REGISTRATION_NO)) {
                            registrationController.addMessageTextArea("Зарегистрироватся не удалось\n" +
                                    "возможно такой логин или никнейм уже заняты");
                        }

                        if (!isIsSocketTimeoutException()) {
                            taChatMessages.appendText(str + "\n");
                        }
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith(SharedConstants.CHANGE_NICKNAME_OK)) {
                            nickname = str.split("\\s")[1];
                            setTitle(String.format("[ %s ] - %s", nickname, Constants.CHAT_TITLE));
                        }

                        if (str.equals(SharedConstants.END_CONNECTION)) {
                            break;
                        } else if (str.startsWith(SharedConstants.CLIENT_LIST + " ")) {
                            refreshClientList(str.split("\\s"));
                        } else {
                            taChatMessages.appendText(str + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    setAuthenticated(false);
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

    private void refreshClientList(String[] clientListArr) {
        Platform.runLater(() -> {
            clientList.getItems().clear();
            for (int i = 1; i < clientListArr.length; i++) {
                clientList.getItems().add(clientListArr[i]);
            }
        });
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        login = loginField.getText().trim();
        String msg = String.format("%s %s %s",
                SharedConstants.AUTH, login, passwordField.getText().trim());
        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String title) {
        Platform.runLater(() -> {
            stage.setTitle(title);
        });
    }

    private void createRegistrationWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/regisration.fxml"));
            Parent root = fxmlLoader.load();
            registrationStage = new Stage();
            registrationStage.setTitle("Регистрация в чате " + Constants.CHAT_TITLE);
            registrationStage.setScene(new Scene(root, 400, 300));
            registrationStage.initModality(Modality.APPLICATION_MODAL);

            registrationController = fxmlLoader.getController();
            registrationController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToRegister(ActionEvent actionEvent) {
        registrationStage.show();
    }

    public void tryRegistration(String login, String password, String nickname) {
        String msg = String.format("%s %s %s %s", SharedConstants.REGISTRATION, login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void clickClientList(MouseEvent mouseEvent) {
        if (clientList.getSelectionModel().getSelectedIndex() > -1) {
            tfMessageText.setText(String.format("%s %s ", SharedConstants.PERSONAL_MESSAGE, clientList.getSelectionModel().getSelectedItem()));
        }
    }

    public void tryToChangeNick(ActionEvent actionEvent) {
        registrationController.setChangeNickNameFormStyle();
        registrationController.setLogin(login);
        registrationStage.show();
    }

    public void tryChangeNickname(String login, String nickname) {
        String msg = String.format("%s %s %s", SharedConstants.CHANGE_NICKNAME, login, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
