package com.example.chatonjavafx.client;

import com.example.chatonjavafx.Command;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ChatController {
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextField loginField;
    @FXML
    private HBox authBox;
    @FXML
    private HBox messageBox;
    @FXML
    private PasswordField passField;
    @FXML
    private TextArea messageArea;
    @FXML
    private TextField messageField;
    private final ChatClient client;

    private String selectedNick;


    public ChatController() {
        this.client = new ChatClient(this);
        while (true) {
            try {
                client.openConnection();
                break;
            } catch (IOException e) {
                showMotification();
            }
        }
    }

    private void showMotification() {
        Alert alert =
                new Alert(Alert.AlertType.ERROR, "Не могу подключиться к серверу.\n"
                        + "Проверьте что сервер доступен и запущен",
                        new ButtonType("Попробовать снова", ButtonBar.ButtonData.OK_DONE),
                        new ButtonType("Выйти", ButtonBar.ButtonData.CANCEL_CLOSE));
        alert.setTitle("Ошибка подключения!");
        final Optional<ButtonType> answer = alert.showAndWait();
        Boolean isExit = answer
                .map(select -> select.getButtonData().isCancelButton())
                .orElse(false);
        if (isExit) {
            System.exit(0);
        }
    }

    public void clickSendButton() {
        final String message = messageField.getText();
        if (message.isBlank()) return;
        if (selectedNick != null ){
            client.sendMessage(Command.PRIVATE_MESSAGE,selectedNick, message);
            selectedNick = null;
        }
        client.sendMessage(Command.MESSAGE, message);
        messageField.clear();
        messageField.requestFocus();
    }

    public void addMessage(String message) {
        messageArea.appendText(message + '\n');
    }

    public void setAuth(boolean sucsess) {
        authBox.setVisible(!sucsess);
        messageBox.setVisible(sucsess);
    }

    public void signInBtnClick(ActionEvent actionEvent) {
        client.sendMessage(Command.AUTH, loginField.getText(), passField.getText());
    }

    public void showError(String errorMessage) {
        final Alert alert = new Alert(Alert.AlertType.ERROR, errorMessage,
                new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        alert.setTitle("ERROR!");
        alert.showAndWait();
    }

    public void selectClient(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            final ObservableList<String> selectedNick = clientList.getSelectionModel().getSelectedItems();
            if (selectedNick!= null && !selectedNick.isEmpty()){
                this.selectedNick = selectedNick.toString();
            }
        }
    }

    public void updateClientsList(String[] clients) {
        clientList.getItems().clear();
        clientList.getItems().addAll(clients);
    }

    public void signOutClick() {
        client.sendMessage(Command.END);
    }

    public ChatClient getClient() {
        return client;
    }
}