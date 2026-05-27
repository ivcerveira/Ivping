package com.nuapps.ivping;

import com.nuapps.ivping.config.AppConfig;
import com.nuapps.ivping.config.AppConfigException;
import com.nuapps.ivping.config.AppFactory;
import com.nuapps.ivping.constants.ConfigKeys;
import com.nuapps.ivping.model.HostData;
import com.nuapps.ivping.util.AlertUtils;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.stage.Modality;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class RemoteAccessDriver {
    public enum AccessType {
        SSH("ssh.sh", "^(SW\\d|RT).*$"),
        TELNET("telnet.sh", "^(SW\\d).*$");

        private final String scriptName;
        private final String hostPattern;

        AccessType(String scriptName, String hostPattern) {
            this.scriptName = scriptName;
            this.hostPattern = hostPattern;
        }

        public String getScriptName() {
            return scriptName;
        }

        public String getHostPattern() {
            return hostPattern;
        }
    }

    public static void open(TableView<HostData> tableView, AccessType accessType)
            throws URISyntaxException, IOException {

        if (tableView.getSelectionModel().getSelectedIndex() < 0) {
            showNoHostSelectedWarning(tableView, accessType);
            return;
        }

        ObservableList<HostData> data = tableView.getSelectionModel().getSelectedItems();

        if (data.size() != 1) {
            return;
        }

        String hostName = data.getFirst().hostName();

        if (!hostName.matches(accessType.getHostPattern())) {
            showWarning(tableView);
            return;
        }

        try {
            AppConfig appConfig = AppFactory.appConfig();
            String baseUrl = appConfig.getRequired(ConfigKeys.REMOTE_ACCESS_BASE_URL);
            String uri = baseUrl + accessType.getScriptName() + "?" + hostName;
            Desktop.getDesktop().browse(new URI(uri));
        } catch (AppConfigException e) {
            AlertUtils.showError(
                    "Configuracao invalida",
                    e.getMessage()
            );
        }
    }

    private static void showWarning(TableView<HostData> tableView) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Conexão SSH/Telnet não disponível");
        alert.setHeaderText(null);
        alert.setContentText("Apenas equipamentos de rede (SW/RT) suportam acesso SSH/Telnet.");

        if (tableView.getScene() != null && tableView.getScene().getWindow() != null) {
            alert.initOwner(tableView.getScene().getWindow());
        }

        alert.showAndWait();
    }

    private static void showNoHostSelectedWarning(TableView<HostData> tableView, AccessType accessType) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(getNoHostSelectedMessage(accessType));

        if (tableView.getScene() != null && tableView.getScene().getWindow() != null) {
            alert.initOwner(tableView.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
        }

        alert.showAndWait();
    }

    private static String getNoHostSelectedMessage(AccessType accessType) {
        return switch (accessType) {
            case SSH -> "Por favor, selecione um Host na tabela antes de abrir a conexão SSH.";
            case TELNET -> "Por favor, selecione um Host na tabela antes de abrir a conexão Telnet.";
        };
    }
}
