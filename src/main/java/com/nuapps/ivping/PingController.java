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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.function.Predicate;

public class PingController {
    private static final String FIREFOX_PATH = System.getenv("ProgramFiles") + "\\Mozilla Firefox\\firefox.exe";
    private static final String APP_FOLDER = System.getProperty("user.home") + "\\AppData\\Local\\Ivping";
    private static final String EXCEL_FILE_NAME = "devices.xlsx";
    private static final Path EXCEL_FILE_PATH = Paths.get(APP_FOLDER, EXCEL_FILE_NAME);

    private final ObservableList<RowData> rowDataList = FXCollections.observableArrayList();
    @FXML
    private TableView<RowData> tableView;
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
        ensureExcelFileExists();   // 🔹 garante que o arquivo exista antes de carregar
        loadExcelData();
        setupSearchFilter();
    }

    /**
     * Garante que a pasta AppData\Local\Ivping e o arquivo devices.xlsx existam.
     * Caso contrário, cria/copiar de um modelo.
     */
    private void ensureExcelFileExists() {
        try {
            // cria pasta se não existir
            Files.createDirectories(Paths.get(APP_FOLDER));

            if (!Files.exists(EXCEL_FILE_PATH)) {
                // 🔹 Copia o modelo da pasta resources
                try (InputStream defaultFile = getClass().getResourceAsStream("/devices.xlsx")) {
                    if (defaultFile != null) {
                        Files.copy(defaultFile, EXCEL_FILE_PATH, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        // Se o arquivo não for encontrado nos resources, cria um novo vazio
                        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
                            workbook.createSheet("Devices");
                            try (FileOutputStream out = new FileOutputStream(EXCEL_FILE_PATH.toFile())) {
                                workbook.write(out);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            showErrorDialog("Erro ao preparar arquivo", e.getMessage());
        }
    }

    private void setupTableColumns() {
        hostNameTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().hostName()));
        ipAddressTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().ipAddress()));
        locationTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().location()));

        hostNameTableColumn.setPrefWidth(170);
        ipAddressTableColumn.setPrefWidth(150);

        // Configura a terceira coluna para ocupar o restante da largura
        tableView.widthProperty().addListener((observable, oldValue, newWidth) -> {
            double tableWidth = newWidth.doubleValue();
            double remainingWidth = tableWidth - (hostNameTableColumn.getWidth() + ipAddressTableColumn.getWidth());
            if (remainingWidth > 0) {
                locationTableColumn.setPrefWidth(remainingWidth);
            }
        });
    }

    /**
     * Carrega os dados do Excel na tabela.
     */
    private void loadExcelData() {
        try (FileInputStream file = new FileInputStream(EXCEL_FILE_PATH.toFile());
             Workbook workbook = WorkbookFactory.create(file)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) {
                rowIterator.next(); // pula cabeçalho
            }

            rowDataList.clear();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String col1Value = getCellValue(row, 0);
                String col2Value = getCellValue(row, 1);
                String col3Value = getCellValue(row, 2);

                rowDataList.add(new RowData(col1Value, col2Value, col3Value));
            }

            filteredData = new FilteredList<>(rowDataList, p -> true);
            tableView.setItems(filteredData);
            tableView.getSelectionModel().selectFirst();
            tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        } catch (FileNotFoundException e) {
            showErrorDialog("Erro", EXCEL_FILE_PATH + " não existe.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCellValue(Row row, int index) {
        try {
            return row.getCell(index).getStringCellValue();
        } catch (Exception e) {
            return "";
        }
    }

    private void setupSearchFilter() {
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> filteredData.setPredicate(rowData -> {
            if (newValue == null || newValue.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = newValue.toLowerCase();

            if (rowData.hostName().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else if (rowData.ipAddress().toLowerCase().contains(lowerCaseFilter)) {
                return true;
            } else
                return rowData.location().toLowerCase().contains(lowerCaseFilter);
        }));
    }

    public void doExit() {
        Platform.exit();
    }

    @FXML
    private void pingSelectedHost() {
        ObservableList<RowData> selectedRowData = tableView.getSelectionModel().getSelectedItems();

        if (!selectedRowData.isEmpty()) {
            boolean continuous = tCheckBox.isSelected();
            selectedRowData.forEach(rowData -> PingHelper.processRowData(rowData, continuous, tableView));
        }
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

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.start();
    }

    @FXML
    private void openCiscoPrimeWebsite() throws IOException {
        String url = "https://ciscoprime.net.petrobras.com.br";
        String[] command = {FIREFOX_PATH, url};

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.start();
    }

    @FXML
    private void openZabbixWebsite() throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("https://zbxcorp.petrobras.com.br/zabbix"));
    }

    @FXML
    private void openSigmonWebsite() throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("https://sigmon/std/cgi-bin/admin-net-hosts.cgi?opcao=Consulta+Detalhada"));
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
                applyFilter(ctrl.getStringSearchField1(), ctrl.getStringSearchField2(), ctrl.getAndOrComboBox().getSelectionModel().getSelectedItem());
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
            return host.hostName().toLowerCase().contains(lowerCaseFilter) || host.ipAddress().toLowerCase().contains(lowerCaseFilter) || host.location().toLowerCase().contains(lowerCaseFilter);
        };

        Predicate<RowData> p2 = host -> {
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("aboutDialog.fxml"));
            Stage aboutStage = new Stage();
            aboutStage.setTitle("Sobre o Ivping");
            aboutStage.initModality(Modality.WINDOW_MODAL);

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
