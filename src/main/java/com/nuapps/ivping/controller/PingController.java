package com.nuapps.ivping.controller;

import com.nuapps.ivping.*;
import com.nuapps.ivping.config.AppConfig;
import com.nuapps.ivping.config.AppConfigException;
import com.nuapps.ivping.config.AppFactory;
import com.nuapps.ivping.constants.ConfigKeys;
import com.nuapps.ivping.model.HostData;
import com.nuapps.ivping.service.BrowserService;
import com.nuapps.ivping.util.AlertUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingController {
    private static final String APP_FOLDER = System.getProperty("user.home") + "\\AppData\\Roaming\\Ivping_data";
    private static final int IP_HISTORY_LIMIT = 10;

    private final ObservableList<HostData> hostDataList = FXCollections.observableArrayList();
    private final ObservableList<String> ipHistoryList = FXCollections.observableArrayList();
    private final BrowserService browserService = AppFactory.browserService();
    private final IpHistoryManager ipHistoryManager = new IpHistoryManager(IP_HISTORY_LIMIT);
    private boolean exitConfirmed;

    public Button pingButton;
    
    @FXML
    private TableView<HostData> tableView;
    @FXML
    private TableColumn<HostData, String> hostNameTableColumn;
    @FXML
    private TableColumn<HostData, String> ipAddressTableColumn;
    @FXML
    private TableColumn<HostData, String> locationTableColumn;
    @FXML
    private CheckBox chkPingContinuous;
    @FXML
    private TextField searchTextField;
    private FilteredList<HostData> filteredData;
    @FXML
    private Button tmnButton;
    @FXML
    private Button ciscoprimeButton;
    @FXML
    private Button zabbixButton;
    @FXML
    private Button sigmonButton;

    @FXML
    private void initialize() {
        setupTableColumns();

        ExcelHostLoader excelLoader = new ExcelHostLoader();
        hostDataList.addAll(excelLoader.loadHosts());

        filteredData = new FilteredList<>(hostDataList, _ -> true);

        SortedList<HostData> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (!hostDataList.isEmpty()) {
            tableView.getSelectionModel().selectFirst();
        }

        setupSearchFilter();
        setupContextMenu();
        setupKeyboardActions();
        loadIpHistory();

        tmnButton.setOnAction(_ -> openConfiguredUrl(ConfigKeys.TMN_URL));
        ciscoprimeButton.setOnAction(_ -> openConfiguredUrl(ConfigKeys.CISCOPRIME_URL));
        zabbixButton.setOnAction(_ -> openConfiguredUrl(ConfigKeys.ZABBIX_URL));
        sigmonButton.setOnAction(_ -> openConfiguredUrl(ConfigKeys.SIGMON_URL));
    }

    private void openConfiguredUrl(String configKey) {
        try {
            AppConfig appConfig = AppFactory.appConfig();
            String url = appConfig.getRequired(configKey);
            browserService.open(url);

        } catch (AppConfigException e) {
            AlertUtils.showError(
                    "Configuração inválida",
                    e.getMessage()
            );

        } catch (Exception e) {
            AlertUtils.showError(
                    "Erro",
                    "Não foi possível abrir a URL configurada."
            );
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        hostNameTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().hostName()));
        ipAddressTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().ipAddress()));
        locationTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().location()));

        hostNameTableColumn.setPrefWidth(170);
        ipAddressTableColumn.setPrefWidth(150);

        tableView.widthProperty().addListener((_, _, newWidth) -> {
            double tableWidth = newWidth.doubleValue();
            double remainingWidth = tableWidth - (hostNameTableColumn.getWidth() + ipAddressTableColumn.getWidth());
            if (remainingWidth > 0) {
                locationTableColumn.setPrefWidth(remainingWidth);
            }
        });
    }

    private void setupSearchFilter() {
        searchTextField.textProperty().addListener((_,
                                                    _, newValue) -> filteredData.setPredicate
                (hostData -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();

                    if (hostData.hostName().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else if (hostData.ipAddress().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    } else
                        return hostData.location().toLowerCase().contains(lowerCaseFilter);
                }));
    }

    public void configureCloseConfirmation(Stage stage) {
        stage.setOnCloseRequest(this::handleWindowCloseRequest);
    }

    private void handleWindowCloseRequest(WindowEvent event) {
        if (exitConfirmed) {
            return;
        }

        event.consume();
        handleCloseApp();
    }

    @FXML
    private void handleCloseApp() {
        if (exitConfirmed) {
            javafx.application.Platform.exit();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar saída");
        alert.setHeaderText(null);
        alert.setContentText("Deseja realmente fechar o aplicativo?");

        Stage owner = (Stage) tableView.getScene().getWindow();
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL); // opcional (initOwner já ajuda bastante)

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                exitConfirmed = true;
                javafx.application.Platform.exit();
            }
        });
    }

    @FXML
    private void pingSelectedHost() {
        ObservableList<HostData> selectedHostData = tableView.getSelectionModel().getSelectedItems();

        if (!selectedHostData.isEmpty()) {
            boolean continuous = chkPingContinuous.isSelected();
            selectedHostData.forEach(hostData -> PingUtils.runPing(hostData, continuous, tableView));
        }
    }

    @FXML
    private void openSSHSession() throws URISyntaxException, IOException {
        RemoteAccessDriver.open(tableView, RemoteAccessDriver.AccessType.SSH);
    }

    @FXML
    private void openTelnetSession() throws URISyntaxException, IOException {
        RemoteAccessDriver.open(tableView, RemoteAccessDriver.AccessType.TELNET);
    }

    @FXML
    private void clearTextField() {
        searchTextField.clear();
        searchTextField.requestFocus();
    }

    @FXML
    private void showSearchMoreOptionsDialog() {
        try {
            searchTextField.clear();
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    PingController.class.getResource("/com/nuapps/ivping/search_more_options_dialog.fxml")
            ));
            AnchorPane page = loader.load();
            Scene scene = new Scene(page);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Mostrar linhas que:");
            dialogStage.initModality(Modality.WINDOW_MODAL);

            Stage owner = (Stage) tableView.getScene().getWindow();
            dialogStage.initOwner(owner);

            dialogStage.setScene(scene);

            SearchMoreOptionsDialogController ctrl = loader.getController();
            ctrl.setSearchMoreOptionsDialogStage(dialogStage);

            dialogStage.setResizable(false);
            dialogStage.showAndWait();

            if (ctrl.isOkClicked()) {
                applyFilter(ctrl.getStringSearchField1(), ctrl.getStringSearchField2(),
                        ctrl.getAndOrComboBox().getSelectionModel().getSelectedItem());
            }
            if (ctrl.isCancelClicked()) cancelClicked();

        } catch (IOException exception) {
            showErrorDialog("Error", exception.getMessage());
        }
    }

    @FXML
    private void applyFilter(String aux1, String aux2, String selectedItem) {
        Predicate<HostData> p1 = host -> {
            if (aux1 == null || aux1.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = aux1.toLowerCase();
            return host.hostName().toLowerCase().contains(lowerCaseFilter) || host.ipAddress().toLowerCase().contains(lowerCaseFilter) || host.location().toLowerCase().contains(lowerCaseFilter);
        };

        Predicate<HostData> p2 = host -> {
            if (aux2 == null || aux2.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = aux2.toLowerCase();
            return host.hostName().toLowerCase().contains(lowerCaseFilter) || host.ipAddress().toLowerCase().contains(lowerCaseFilter) || host.location().toLowerCase().contains(lowerCaseFilter);
        };

        if (selectedItem.equals("E")) {
            filteredData.setPredicate(p1.and(p2));
        } else if (selectedItem.equals("OU")) {
            filteredData.setPredicate(p1.or(p2));
        } else {
            showErrorDialog("Error", "Invalid selection");
        }
    }

    @FXML
    private void cancelClicked() {
        Predicate<HostData> predicate = _ -> true;
        filteredData.setPredicate(predicate);
    }

    @FXML
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sobre");
        alert.setHeaderText("Ivping");
        alert.setContentText("Autor: Ivaldo Cerveira");

        // pega o Stage da janela principal (a que contém o TableView)
        Stage owner = (Stage) tableView.getScene().getWindow();

        // define o owner para o Alert abrir no mesmo monitor e ficar ligado à janela
        alert.initOwner(owner);
        alert.initModality(Modality.WINDOW_MODAL); // opcional (initOwner já ajuda bastante)
        alert.showAndWait();
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadIpHistory() {
        ipHistoryList.setAll(ipHistoryManager.loadHistory());
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        contextMenu.getItems().addAll(
                getPingItem(),
                getSshItem(),
                new SeparatorMenuItem(),
                getCopyHostnameItem(),
                getCopyIpItem(),
                getCopyLocationItem()
        );

        tableView.setRowFactory(_ -> {
            TableRow<HostData> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    tableView.getSelectionModel().select(row.getIndex()); // garante que a linha clicada fique selecionada
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
    }

    private void setupKeyboardActions() {
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER
                    && !tableView.getSelectionModel().getSelectedItems().isEmpty()) {
                pingSelectedHost();
                event.consume();
            }
        });
    }

    private MenuItem getPingItem() {
        MenuItem pingItem = new MenuItem("Ping");
        pingItem.setOnAction(_ -> pingSelectedHost());
        return pingItem;
    }

    private MenuItem getSshItem() {
        MenuItem sshItem = new MenuItem("SSH");
        sshItem.setOnAction(_ -> {
            try {
                openSSHSession();
            } catch (URISyntaxException | IOException exception) {
                showErrorDialog("Erro", exception.getMessage());
            }
        });
        return sshItem;
    }

    private MenuItem getCopyIpItem() {
        MenuItem copyIpItem = new MenuItem("Copiar IP");
        copyIpItem.setOnAction(_ -> {
            HostData selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.ipAddress());
            }
        });
        return copyIpItem;
    }

    private MenuItem getCopyHostnameItem() {
        MenuItem copyHostItem = new MenuItem("Copiar Hostname");
        copyHostItem.setOnAction(_ -> {
            HostData selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.hostName());
            }
        });
        return copyHostItem;
    }

    private MenuItem getCopyLocationItem() {
        MenuItem copyLocationItem = new MenuItem("Copiar Location");
        copyLocationItem.setOnAction(_ -> {
            HostData selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.location());
            }
        });
        return copyLocationItem;
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    @FXML
    private void openIvpingDataFolder() {
        try {
            Path folderPath = Paths.get(APP_FOLDER);

            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(folderPath.toFile());
            } else {
                showErrorDialog("Erro", "Abertura de pastas não é suportada neste sistema.");
            }

        } catch (IOException e) {
            showErrorDialog("Erro ao abrir pasta", e.getMessage());
        }
    }

    @FXML
    private void onPingByIpClicked() {
        Stage dialog = new Stage();
        dialog.setTitle("Ping por IP");
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(pingButton.getScene().getWindow());

        ComboBox<String> cmbIpAddress = new ComboBox<>(ipHistoryList);
        cmbIpAddress.setEditable(true);
        cmbIpAddress.setPromptText("Digite o IP Address");
        cmbIpAddress.setMaxWidth(Double.MAX_VALUE);
        cmbIpAddress.setVisibleRowCount(IP_HISTORY_LIMIT);
        CheckBox chkPingContinuo = new CheckBox("-t");

        Button btnPingTest = new Button("Enviar Ping Test");
        Button btnLimpar = new Button("Limpar");
        Button btnCancelar = new Button("Cancelar");

        Runnable runPingTest = () -> {
            String ipAddress = getIpAddressFromCombo(cmbIpAddress);
            if (!isValidIpv4(ipAddress)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("IP invalido");
                alert.setHeaderText(null);
                alert.setContentText("Digite um endereco IPv4 valido. Exemplo: 10.0.0.1");
                alert.initOwner(dialog);
                alert.showAndWait();
                cmbIpAddress.requestFocus();
                return;
            }
            PingUtils.runDefaultPingByIp(ipAddress, chkPingContinuo.isSelected());
            ipHistoryManager.addIp(ipAddress);
            loadIpHistory();
            cmbIpAddress.setItems(ipHistoryList);
            cmbIpAddress.setValue(ipAddress);
        };

        btnPingTest.setOnAction(_ -> runPingTest.run());
        cmbIpAddress.getEditor().setOnAction(_ -> runPingTest.run());

        btnLimpar.setOnAction(_ -> {
            cmbIpAddress.getEditor().clear();
            cmbIpAddress.requestFocus();
        });

        btnCancelar.setOnAction(_ -> dialog.close());

        HBox buttonBox = new HBox(10, btnPingTest, btnLimpar, btnCancelar);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, new Label("IP Address:"), cmbIpAddress, chkPingContinuo, buttonBox);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 430, 180);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.show();
        if (!ipHistoryList.isEmpty()) {
            cmbIpAddress.setValue(ipHistoryList.getFirst());
            cmbIpAddress.getEditor().selectAll();
        }
        cmbIpAddress.requestFocus();
    }

    private String getIpAddressFromCombo(ComboBox<String> cmbIpAddress) {
        String editorText = cmbIpAddress.getEditor().getText();
        if (editorText != null && !editorText.isBlank()) {
            return editorText.trim();
        }

        String selectedValue = cmbIpAddress.getValue();
        return selectedValue == null ? "" : selectedValue.trim();
    }

    private boolean isValidIpv4(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        String zeroTo255 = "(\\d{1,2}|([01])\\" + "d{2}|2[0-4]\\d|25[0-5])";
        String regex = zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }
}

/*
Últimas modificações realizadas - em 19/04:
- Ping e SSH no menu de contexto
- Teclar Enter para pingar
 */
