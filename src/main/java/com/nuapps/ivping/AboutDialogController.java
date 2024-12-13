package com.nuapps.ivping;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class AboutDialogController {
    @FXML
    private Button okButton;

    @FXML
    private void handleOkButtonClick() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }
}
