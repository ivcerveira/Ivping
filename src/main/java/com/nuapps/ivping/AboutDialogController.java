package com.nuapps.ivping;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class AboutDialogController {
    @FXML
    private Button okButton;

    @FXML
    private void handleOkButtonClick() {
        // Obtém a referência ao palco (Stage) atual do diálogo
        Stage stage = (Stage) okButton.getScene().getWindow();

        // Fecha o palco (diálogo)
        stage.close();
    }
}
