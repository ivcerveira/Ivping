package com.nuapps.ivping;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingAnyIpDialogController {
    private static final String PING_ = "@ping ";
    private static final String PING_T = "@ping -t ";

    @FXML
    private Button cancelButton;
    @FXML
    private TextField enterIpAddressTextField;
    @FXML
    private CheckBox t2CheckBox;
    @FXML
    public void initialize() {
        Platform.runLater(() -> enterIpAddressTextField.requestFocus());
    }

    @FXML
    private void handlePing() {
        String string = enterIpAddressTextField.getText();
        if (string != null && !string.isEmpty()) {
            String zeroTo255 = "(\\d{1,2}|([01])\\" + "d{2}|2[0-4]\\d|25[0-5])";
            String regex = zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255;
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(string);
            boolean isIpValid = matcher.matches();
            if (isIpValid) {
                try {
                    new PingAnyIp().ping(enterIpAddressTextField.getText(),
                            (t2CheckBox.isSelected() ? PING_T : PING_));
                    handleCancelButtonClick();
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            } else {
                enterIpAddressTextField.requestFocus();
            }
        } else {
            enterIpAddressTextField.requestFocus();
        }
    }

    @FXML
    private void clearTextField() {
        enterIpAddressTextField.clear();
        enterIpAddressTextField.requestFocus();
    }

    @FXML
    private void handleCancelButtonClick() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
