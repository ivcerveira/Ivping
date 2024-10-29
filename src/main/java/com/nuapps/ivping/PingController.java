/**
Em 03-09-2024:
Classe RowData migrada para Record
*/

package com.nuapps.ivping;

import com.nuapps.ivping.model.RowData;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class PingController {
    private static final Logger LOGGER = Logger.getLogger(PingController.class.getName());
    private static final String PING_N = "@ping -n 10 ";
    private static final String PING_T = "@ping -t ";
    private static final String FIREFOX_PATH = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";

    private final ObservableList<RowData> rowDataList = FXCollections.observableArrayList();
    @FXML
    private TableView<RowData> tableView = new TableView<>();
    @FXML
    private TableColumn<RowData, String> hostNameTableColumn;
    @FXML
    private TableColumn<RowData, String> ipAddressTableColumn;
    @FXML
    private TableColumn<RowData, String> locationTableColumn;
    @FXML
    private CheckBox tCheckBox;
    @FXML
    private TextField searchTextField;
    private FilteredList<RowData> filteredData;

    @FXML
    private void initialize() {
        setupTableColumns();
        loadExcelData();
        setupSearchFilter();
    }

    private void setupTableColumns() {
        hostNameTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().hostName()));
        ipAddressTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().ipAddress()));
        locationTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().location()));
    }

    private void loadExcelData() {
        try {
            FileInputStream file = new FileInputStream("devices.xlsx");
            Workbook workbook = WorkbookFactory.create(file);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String col1Value = row.getCell(0).getStringCellValue();
                String col2Value = row.getCell(1).getStringCellValue();
                String col3Value = row.getCell(2).getStringCellValue();
                rowDataList.add(new RowData(col1Value, col2Value, col3Value));
            }
            tableView.setItems(rowDataList);
            tableView.getSelectionModel().selectFirst();
            tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            workbook.close();
            file.close();

            filteredData = new FilteredList<>(rowDataList, p -> true); // Inicializa o FilteredList
            tableView.setItems(filteredData); // Define o FilteredList como itens da TableView
        } catch (FileNotFoundException e) {
            showErrorDialog("Error loading data", Path.of("devices.xlsx") + " does not exist");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupSearchFilter() {
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> filteredData.setPredicate(rowData -> {
            // Se o texto do filtro estiver vazio, mostra todos os dados.
            if (newValue == null || newValue.isEmpty()) {
                return true;
            }

            // Compara o nome do host, endereço IP e localização de cada rowData com o texto do filtro.
            String lowerCaseFilter = newValue.toLowerCase();

            if (rowData.hostName().toLowerCase().contains(lowerCaseFilter)) {
                return true; // Filtro corresponde ao nome do host.
            } else if (rowData.ipAddress().toLowerCase().contains(lowerCaseFilter)) {
                return true; // Filtro corresponde ao endereço IP.
            } else
                return rowData.location().toLowerCase().contains(lowerCaseFilter); // Filtro corresponde à localização.

        }));
    }

    public void doExit() {
        Platform.exit();
    }

    @FXML
    private void pingSelectedHost() {
        ObservableList<RowData> selectedRowData = tableView.getSelectionModel().getSelectedItems();

        if (!selectedRowData.isEmpty()) {
            selectedRowData.forEach(this::processRowData);
        }
    }

    private void processRowData(RowData rowData) {
        String hostName = rowData.hostName();
        String ipAddress = rowData.ipAddress();
        int lineNumber = tableView.getSelectionModel().getSelectedItems().indexOf(rowData);
        String pingCommand = (tCheckBox.isSelected() ? PING_T : PING_N) + ipAddress;

        try {
            String batFileName = createBatchFile(hostName, ipAddress, lineNumber, pingCommand);
            executeBatchFile(batFileName);
        } catch (IOException e) {
            LOGGER.severe("Error processing row data: " + e.getMessage());
        }
    }

    private String createBatchFile(String hostName, String ipAddress, int lineNumber, String pingCommand) throws IOException {
        String tempDir = System.getenv("TEMP");
        if (tempDir == null) {
            throw new IllegalStateException("TEMP directory not found");
        }

        String batFileName = tempDir + "/ivping/ping" + lineNumber + ".bat";
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(batFileName))) {
            bufferedWriter.write("@echo off\n");
            bufferedWriter.write("@cls\n");
            bufferedWriter.write("@color 17\n");
            bufferedWriter.write("@title Ping  " + hostName + "  [" + ipAddress + "]\n");
            bufferedWriter.write(pingCommand + "\n");
            bufferedWriter.write("@pause\n");
        }
        return batFileName;
    }

    private void executeBatchFile(String batFileName) throws IOException {
        new ProcessBuilder("rundll32", "SHELL32.DLL,ShellExec_RunDLL", batFileName).start();
    }

    @FXML
    private void openSSHSession() throws URISyntaxException, IOException {
        SshDriver.ssh(tableView);
    }

    @FXML
    private void openTelnetSession() throws URISyntaxException, IOException {
        TelnetDriver.telnet(tableView);
    }

    @FXML
    private void openTmnWebsite() throws IOException {
        String url = "http://tms.petrobras.com.br";
        String[] command = {FIREFOX_PATH, url};

        // Inicia o processo
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.start();
    }

    @FXML
    private void openCiscoPrimeWebsite() throws IOException {
        String url = "https://ciscoprime.net.petrobras.com.br";

        // Constrói o comando para abrir o Firefox com a URL
        String[] command = {FIREFOX_PATH, url};

        // Inicia o processo
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.start();
    }

    @FXML
    private void openZabbixWebsite() throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("https://zbxcorp.petrobras.com.br/zabbix"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("searchMoreOptionsDialog.fxml"));
            AnchorPane page = loader.load();
            Scene scene = new Scene(page);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Mostrar linhas que:");
            dialogStage.initModality(Modality.WINDOW_MODAL);
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
        Predicate<RowData> p1 = host -> {
            if (aux1 == null || aux1.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = aux1.toLowerCase();
            return host.hostName().toLowerCase().contains(lowerCaseFilter)
                    || host.ipAddress().toLowerCase().contains(lowerCaseFilter)
                    || host.location().toLowerCase().contains(lowerCaseFilter);
        };

        Predicate<RowData> p2 = host -> {
            if (aux2 == null || aux2.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = aux2.toLowerCase();
            return host.hostName().toLowerCase().contains(lowerCaseFilter)
                    || host.ipAddress().toLowerCase().contains(lowerCaseFilter)
                    || host.location().toLowerCase().contains(lowerCaseFilter);
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
        Predicate<RowData> predicate = host -> true;
        filteredData.setPredicate(predicate);
    }

    @FXML
    private void showPingAnyIpDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("pingAnyIpDialog.fxml"));
            Scene scene = new Scene(loader.load());

            Stage pingAnyIpDialogStage = new Stage();
            pingAnyIpDialogStage.setTitle("Digite o IP");
            pingAnyIpDialogStage.initModality(Modality.WINDOW_MODAL);
            pingAnyIpDialogStage.setScene(scene);

            pingAnyIpDialogStage.setResizable(false);
            pingAnyIpDialogStage.showAndWait();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    private void showAboutDialog() {
        try {
            // Carrega o arquivo FXML do diálogo "About"
            FXMLLoader loader = new FXMLLoader(getClass().getResource("aboutDialog.fxml"));

            Stage aboutStage = new Stage();
            aboutStage.setTitle("Sobre o Ivping");
            aboutStage.initModality(Modality.WINDOW_MODAL);

            // Define a cena no palco do diálogo
            aboutStage.setScene(new Scene(loader.load()));

            aboutStage.setResizable(false);
            aboutStage.showAndWait();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
