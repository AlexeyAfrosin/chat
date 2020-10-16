package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
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

    private String nickname;

    public void setAuthenticated(boolean authenticated) {
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);

        if (!authenticated) {
            nickname = "";
            setTitle(Constants.CHAT_TITLE);
        } else {
            setTitle(String.format("[ %s ] - %s", nickname, Constants.CHAT_TITLE));
        }
        taChatMessages.clear();

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) tfMessageText.getScene().getWindow();
        });
        tfMessageText.requestFocus();
        btnSendMessage.setDisable(true);
        setAuthenticated(false);
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

                        if (str.startsWith(SharedConstants.AUTH_OK + " ")) {
                            nickname = str.split("\\s")[1];
                            setAuthenticated(true);
                            break;
                        }

                        taChatMessages.appendText(str + "\n");
                    }

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals(SharedConstants.END_CONNECTION)) {
                            break;
                        }

                        taChatMessages.appendText(str + "\n");
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

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        String msg = String.format("%s %s %s",
                SharedConstants.AUTH, loginField.getText().trim(), passwordField.getText().trim());
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
}
