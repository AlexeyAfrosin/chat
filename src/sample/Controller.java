package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Controller {
    @FXML
    Button btnSendMessage;

    @FXML
    TextField tfMessageText;

    @FXML
    TextArea taChatMessages;

    SimpleDateFormat formatter;

    @FXML
    public void initialize() {
        tfMessageText.requestFocus();
        formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        btnSendMessage.setDisable(true);
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

    public void sendMessage(){
        String text = tfMessageText.getText().trim();
        if (text.length() > 0) {
            taChatMessages.appendText(String.format("%s\n%s\n", formatter.format(new Date()), tfMessageText.getText()));
            tfMessageText.clear();
            tfMessageText.requestFocus();
            btnSendMessage.setDisable(true);
        }
    }

}
