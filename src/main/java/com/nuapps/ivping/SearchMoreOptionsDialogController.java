package com.nuapps.ivping;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SearchMoreOptionsDialogController {
    @FXML
    private TextField searchField1TextField;
    @FXML
    private TextField searchField2TextField;
    @FXML
    private ComboBox<String> andOrComboBox;

    private Stage searchMoreOptionsDialogStage;
    private boolean okClicked = false;
    private boolean cancelClicked = false;
    private String stringSearchField1 = "";
    private String stringSearchField2 = "";

    @FXML
    private void initialize() {
        andOrComboBox.getItems().add("E");
        andOrComboBox.getItems().add("OU");
        andOrComboBox.getSelectionModel().select(0);
    }

    public void setSearchMoreOptionsDialogStage(Stage searchMoreOptionsDialogStage) {
        this.searchMoreOptionsDialogStage = searchMoreOptionsDialogStage;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public boolean isCancelClicked() {
        return cancelClicked;
    }

    public ComboBox<String> getAndOrComboBox() {
        return andOrComboBox;
    }

    public String getStringSearchField1() {
        return stringSearchField1;
    }

    public String getStringSearchField2() {
        return stringSearchField2;
    }

    @FXML
    private void handleOk() {
        if (isInputValid()) {
            stringSearchField1 = searchField1TextField.getText();
            stringSearchField2 = searchField2TextField.getText();
            searchMoreOptionsDialogStage.close();
            okClicked = true;
        }
    }

    @FXML
    private void handleClear() {
        searchField1TextField.clear();
        searchField2TextField.clear();
        searchField1TextField.requestFocus();
    }

    @FXML
    private void handleCancel() {
        searchMoreOptionsDialogStage.close();
        cancelClicked = true;
    }

    private boolean isInputValid() {
        if (searchField1TextField.getText() == null || searchField1TextField.getText().isEmpty()
                || searchField2TextField.getText() == null || searchField2TextField.getText().isEmpty()) {
            searchField1TextField.requestFocus();
            return false;
        } else return true;
    }
}
