package com.nuapps.ivping.controller;

import com.nuapps.ivping.ExcelHostLoader;
import com.nuapps.ivping.IpHistoryManager;
import com.nuapps.ivping.PingUtils;
import com.nuapps.ivping.RemoteAccessDriver;
import com.nuapps.ivping.config.AppConfig;
import com.nuapps.ivping.config.AppConfigException;
import com.nuapps.ivping.config.AppFactory;
import com.nuapps.ivping.constants.ConfigKeys;
import com.nuapps.ivping.model.HostData;
import com.nuapps.ivping.service.BrowserService;
import com.nuapps.ivping.util.AlertUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IvpingController {
    private static final Logger LOGGER = Logger.getLogger(IvpingController.class.getName());
    private static final String APP_FOLDER = System.getProperty("user.home") + "\\AppData\\Roaming\\Ivping_data";
    private static final int IP_HISTORY_LIMIT = 10;

    private final ObservableList<HostData> dataList = FXCollections.observableArrayList();
    private final ObservableList<String> ipHistoryList = FXCollections.observableArrayList();
    private final BrowserService browserService = AppFactory.browserService();
    private final IpHistoryManager ipHistoryManager = new IpHistoryManager(IP_HISTORY_LIMIT);
    public Button pingButton;
    private boolean exitConfirmed;
    @FXML
    private TableView<HostData> tableView;
    @FXML
    private TableColumn<HostData, String> colHost;
    @FXML
    private TableColumn<HostData, String> colIp;
    @FXML
    private TableColumn<HostData, String> colLocation;
    @FXML
    private CheckBox chkPingContinuous;
    @FXML
    private TextField txtSearch;
    private FilteredList<HostData> filteredData;
    @FXML
    private Button tmnButton;
    @FXML
    private Button ciscoprimeButton;
    @FXML
    private Button zabbixButton;
    @FXML
    private Button sigmonButton;
    private String advancedSearchText1 = "";
    private String advancedSearchText2 = "";
    private boolean advancedSearchAnd = true;

    @FXML
    private void initialize() {
        setupTableColumns();
        ExcelHostLoader excelLoader = new ExcelHostLoader();
        dataList.addAll(excelLoader.loadHosts());
        filteredData = new FilteredList<>(dataList, _ -> true);

        txtSearch.textProperty().addListener((_, _, _) -> applyFilters());

        SortedList<HostData> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        if (!dataList.isEmpty()) {
            tableView.getSelectionModel().selectFirst();
        }

        //setupSearchFilter();
        setupContextMenu();
        setupKeyboardActions();
        loadIpHistory();

        tmnButton.setOnAction(_ -> openConfiguredUrl(ConfigKeys.TMN_URL));
        ciscoprimeButton.setOnAction(_ -> openConfiguredUrl(ConfigKeys.CISCOPRIME_URL));
        zabbixButton.setOnAction(_ -> openConfiguredUrl(ConfigKeys.ZABBIX_URL));
        sigmonButton.setOnAction(_ -> openConfiguredUrl(ConfigKeys.SIGMON_URL));
    }
    @FXML
    private void onAdvancedSearchClicked() {
        Stage dialog = new Stage();
        dialog.setTitle("Filtro Personalizado");
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(tableView.getScene().getWindow());

        Label title = new Label("Mostrar linhas onde qualquer coluna contenha:");

        TextField txtFirstTerm = new TextField(advancedSearchText1);
        txtFirstTerm.setPromptText("Primeiro texto");
        TextField txtSecondTerm = new TextField(advancedSearchText2);
        txtSecondTerm.setPromptText("Segundo texto");

        RadioButton rbAnd = new RadioButton("E");
        RadioButton rbOr = new RadioButton("Ou");
        ToggleGroup logicalGroup = new ToggleGroup();
        rbAnd.setToggleGroup(logicalGroup);
        rbOr.setToggleGroup(logicalGroup);
        rbAnd.setSelected(advancedSearchAnd);
        rbOr.setSelected(!advancedSearchAnd);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Texto 1:"), 0, 0);
        form.add(txtFirstTerm, 1, 0);
        form.add(new Label("Texto 2:"), 0, 1);
        form.add(txtSecondTerm, 1, 1);

        Button btnOk = new Button("OK");
        Button btnClear = new Button("Limpar");
        Button btnCancel = new Button("Cancelar");
        double buttonWidth = 80;
        btnOk.setPrefWidth(buttonWidth);
        btnClear.setPrefWidth(buttonWidth);
        btnCancel.setPrefWidth(buttonWidth);

        Runnable applyAdvancedSearch = () -> {
            txtSearch.clear();
            advancedSearchText1 = normalizeSearchText(txtFirstTerm.getText());
            advancedSearchText2 = normalizeSearchText(txtSecondTerm.getText());
            advancedSearchAnd = rbAnd.isSelected();
            applyFilters();
            dialog.close();
        };

        btnOk.setOnAction(_ -> applyAdvancedSearch.run());
        txtFirstTerm.setOnAction(_ -> applyAdvancedSearch.run());
        txtSecondTerm.setOnAction(_ -> applyAdvancedSearch.run());

        btnClear.setOnAction(_ -> {
            txtFirstTerm.clear();
            txtSecondTerm.clear();
            clearAdvancedSearch();
            txtFirstTerm.requestFocus();
        });

        btnCancel.setOnAction(_ -> dialog.close());

        HBox logicalBox = new HBox(16, rbAnd, rbOr);
        HBox buttonBox = new HBox(10, btnOk, btnClear, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, title, form, logicalBox, buttonBox);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 460, 210);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.show();
        txtFirstTerm.requestFocus();
    }
    private void applyFilters() {
        filteredData.setPredicate(hostData -> matchesSimpleSearch(hostData) && matchesAdvancedSearch(hostData));
    }

    private boolean matchesSimpleSearch(HostData hostData) {
        String searchText = normalizeSearchText(txtSearch.getText());
        return searchText.isBlank() || containsInAnyColumn(hostData, searchText);
    }

    private boolean matchesAdvancedSearch(HostData hostData) {
        boolean hasFirstTerm = !advancedSearchText1.isBlank();
        boolean hasSecondTerm = !advancedSearchText2.isBlank();

        if (!hasFirstTerm && !hasSecondTerm) {
            return true;
        }

        boolean firstTermMatches = !hasFirstTerm || containsInAnyColumn(hostData, advancedSearchText1);
        boolean secondTermMatches = !hasSecondTerm || containsInAnyColumn(hostData, advancedSearchText2);

        if (hasFirstTerm && hasSecondTerm) {
            return advancedSearchAnd ? firstTermMatches && secondTermMatches : firstTermMatches || secondTermMatches;
        }

        return hasFirstTerm ? firstTermMatches : secondTermMatches;
    }

    private boolean containsInAnyColumn(HostData hostData, String searchText) {
        return normalizeSearchText(hostData.host()).contains(searchText) ||
                normalizeSearchText(hostData.ip()).contains(searchText) ||
                normalizeSearchText(hostData.location()).contains(searchText);
    }

    private String normalizeSearchText(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private void clearAdvancedSearch() {
        advancedSearchText1 = "";
        advancedSearchText2 = "";
        advancedSearchAnd = true;
        applyFilters();
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
            LOGGER.log(Level.SEVERE, "Não foi possível abrir a URL configurada.", e);
        }
    }

    private void setupTableColumns() {
        colHost.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().host()));
        colIp.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().ip()));
        colLocation.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().location()));

        colHost.setPrefWidth(170);
        colIp.setPrefWidth(150);

        tableView.widthProperty().addListener((_, _, newWidth) -> {
            double tableWidth = newWidth.doubleValue();
            double remainingWidth = tableWidth - (colHost.getWidth() + colIp.getWidth());
            if (remainingWidth > 0) {
                colLocation.setPrefWidth(remainingWidth);
            }
        });
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
    private void onPingClicked() {
        ObservableList<HostData> selectedHostData = tableView.getSelectionModel().getSelectedItems();

        if (selectedHostData.isEmpty()) {
            showNoHostSelectedWarning();
            return;
        }

        boolean continuous = chkPingContinuous.isSelected();
        selectedHostData.forEach(hostData -> PingUtils.runPing(hostData, continuous, tableView));
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
        txtSearch.clear();
        txtSearch.requestFocus();
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

    private void showNoHostSelectedWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText("Por favor, selecione um Host na tabela antes de enviar o Ping.");

        if (tableView.getScene() != null && tableView.getScene().getWindow() != null) {
            alert.initOwner(tableView.getScene().getWindow());
            alert.initModality(Modality.WINDOW_MODAL);
        }

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
            if (event.getCode() == KeyCode.ENTER) {
                onPingClicked();
                event.consume();
            }
        });
    }

    private MenuItem getPingItem() {
        MenuItem pingItem = new MenuItem("Ping");
        pingItem.setOnAction(_ -> onPingClicked());
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
                copyToClipboard(selected.ip());
            }
        });
        return copyIpItem;
    }

    private MenuItem getCopyHostnameItem() {
        MenuItem copyHostItem = new MenuItem("Copiar Hostname");
        copyHostItem.setOnAction(_ -> {
            HostData selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                copyToClipboard(selected.host());
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
        Window owner = pingButton.getScene().getWindow();
        dialog.initOwner(owner);

        ComboBox<String> cmbIpAddress = new ComboBox<>(ipHistoryList);
        cmbIpAddress.setEditable(true);
        cmbIpAddress.setPromptText("Digite o IP Address");
        cmbIpAddress.setMaxWidth(Double.MAX_VALUE);
        cmbIpAddress.setVisibleRowCount(IP_HISTORY_LIMIT);
        cmbIpAddress.valueProperty().addListener((_, _, _) -> positionIpAddressCaretAtEnd(cmbIpAddress));
        CheckBox chkPingContinuo = new CheckBox("-t");

        Button btnPingTest = new Button("_Enviar Ping Test");
        Button btnLimpar = new Button("_Limpar");
        Button btnCancelar = new Button("_Cancelar");
        btnPingTest.setMnemonicParsing(true);
        btnLimpar.setMnemonicParsing(true);
        btnCancelar.setMnemonicParsing(true);

        Runnable runPingTest = () -> {
            String ipAddress = getIpAddressFromCombo(cmbIpAddress);
            if (!isValidIpv4(ipAddress)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("IP inválido");
                alert.setHeaderText(null);
                alert.setContentText("Digite um endereço IPv4 válido. Exemplo: 10.0.0.1");
                alert.initOwner(dialog);
                alert.showAndWait();
                cmbIpAddress.requestFocus();
                positionIpAddressCaretAtEnd(cmbIpAddress);
                return;
            }
            PingUtils.runDefaultPingByIp(ipAddress, chkPingContinuo.isSelected());
            ipHistoryManager.addIp(ipAddress);
            loadIpHistory();
            cmbIpAddress.setItems(ipHistoryList);
            cmbIpAddress.setValue(ipAddress);
        };

        btnPingTest.setOnAction(_ -> runPingTest.run());

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
        installPingOnEnter(scene, cmbIpAddress, runPingTest);
        installCancelOnEscape(scene, btnCancelar);
        dialog.setScene(scene);
        dialog.setResizable(false);
        centerStageOnOwner(dialog, owner, scene);
        dialog.show();
        if (!ipHistoryList.isEmpty()) {
            cmbIpAddress.setValue(ipHistoryList.getFirst());
            positionIpAddressCaretAtEnd(cmbIpAddress);
        }
        cmbIpAddress.requestFocus();
    }

    private void centerStageOnOwner(Stage stage, Window owner, Scene scene) {
        if (owner == null) {
            return;
        }

        stage.sizeToScene();
        double width = stage.getWidth();
        double height = stage.getHeight();
        if (Double.isNaN(width) || width <= 0) {
            width = scene.getWidth();
        }
        if (Double.isNaN(height) || height <= 0) {
            height = scene.getHeight();
        }
        stage.setX(owner.getX() + (owner.getWidth() - width) / 2);
        stage.setY(owner.getY() + (owner.getHeight() - height) / 2);
    }

    private void installCancelOnEscape(Scene scene, Button btnCancelar) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                btnCancelar.fire();
                event.consume();
            }
        });
    }

    private void installPingOnEnter(Scene scene, ComboBox<String> cmbIpAddress, Runnable runPingTest) {
        AtomicBoolean enterPressedInApp = new AtomicBoolean();

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && isIpComboFocused(scene, cmbIpAddress)) {
                enterPressedInApp.set(true);
            }
        });
        scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> runPingTestOnEnter(event, runPingTest, enterPressedInApp));
    }

    private void runPingTestOnEnter(KeyEvent event, Runnable runPingTest, AtomicBoolean enterPressedInApp) {
        if (event.getCode() == KeyCode.ENTER) {
            event.consume();
            if (enterPressedInApp.getAndSet(false)) {
                runPingTest.run();
            }
        }
    }

    private boolean isIpComboFocused(Scene scene, ComboBox<String> cmbIpAddress) {
        return scene.getFocusOwner() == cmbIpAddress || scene.getFocusOwner() == cmbIpAddress.getEditor();
    }

    private void positionIpAddressCaretAtEnd(ComboBox<String> cmbIpAddress) {
        Platform.runLater(() -> {
            TextField editor = cmbIpAddress.getEditor();
            String text = editor.getText();
            editor.deselect();
            editor.positionCaret(text == null ? 0 : text.length());
        });
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
