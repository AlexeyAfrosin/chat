package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class RegistrationController {
    @FXML
    public VBox loginPasswordVbox;

    @FXML
    public Button btnRegister;
    @FXML
    public Button btnChangeNickname;

    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField nickField;
    @FXML
    private TextArea textArea;

    private Controller controller;
    private String login;

    public void setLogin(String login) {
        this.login = login;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void clickOnRegistrationBtn(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = getNickname();
        controller.tryRegistration(login, password, nickname);
    }

    public void addMessageTextArea(String msg) {
        textArea.appendText(msg + "\n");
    }

    public void setChangeNickNameFormStyle(){
        loginPasswordVbox.setVisible(false);
        loginPasswordVbox.setManaged(false);
        btnRegister.setVisible(false);
        btnRegister.setManaged(false);
        btnChangeNickname.setVisible(true);
        btnChangeNickname.setManaged(true);
    }

    private String getNickname() {
        return nickField.getText().trim();
    }

    public void clickOnchangeNicknameBtn(ActionEvent actionEvent) {
        controller.tryChangeNickname(login, getNickname());
    }
}
