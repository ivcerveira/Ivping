package com.nuapps.ivping;

import com.nuapps.ivping.model.RowData;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.function.Predicate;

public class PingController {
    private static final String PING_N = "@ping -n 10 ";
    private static final String PING_T = "@ping -t ";

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

    //private ObservableList<RowData> rowDataList;
    private final ObservableList<RowData> rowDataList = FXCollections.observableArrayList();
    private FilteredList<RowData> filteredData;

    @FXML
    private void initialize() {
        setupTableColumns();
        //loadDataObservableList();
        loadExcelData();
        setupFiltering();
        setupCellFactories();
        setupListeners();
    }

    private void setupTableColumns() {
        hostNameTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getHostName()));
        ipAddressTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getIpAddress()));
        locationTableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLocation()));
    }

    private void loadExcelData() { // verificar/inserir if (Files.exists(path)) {
        try {
            FileInputStream file = new FileInputStream("devices.xlsx");
            Workbook workbook = WorkbookFactory.create(file);
            Sheet sheet = workbook.getSheetAt(0); // Assumindo que os dados estão na primeira planilha

            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Lendo os dados da planilha e adicionando à lista
                String col1Value = row.getCell(0).getStringCellValue();
                String col2Value = row.getCell(1).getStringCellValue();
                String col3Value = row.getCell(2).getStringCellValue();

                rowDataList.add(new RowData(col1Value, col2Value, col3Value));

                tableView.setItems(rowDataList);
                tableView.getSelectionModel().selectFirst();
                tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            }

            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void loadDataObservableList() {
//        Path path = Path.of("devices.xlsx");
//
//        if (Files.exists(path)) {
//            try {
//                //dataObservableList = FXCollections.observableArrayList(new ExcelReader().getHostsList());
//                ExcelReader.excel();
//                rowDataList = FXCollections.observableArrayList(ExcelReader.getHostsList());
//                tableView.setItems(rowDataList);
//                tableView.getSelectionModel().selectFirst();
//                tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
//            } catch (IOException exception) {
//                showErrorDialog("Error loading data", exception.getMessage());
//            }
//        } else {
//            showErrorDialog("File not found", path + " does not exist");
//        }
//    }

    private void setupFiltering() {
        filteredData = new FilteredList<>(rowDataList, b -> true);
        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> filteredData.setPredicate(host -> {
            if (newValue == null || newValue.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = newValue.toLowerCase();
            return host.getHostName().toLowerCase().contains(lowerCaseFilter)
                    || host.getIpAddress().toLowerCase().contains(lowerCaseFilter)
                    || host.getLocation().toLowerCase().contains(lowerCaseFilter);
        }));
    }

    private void setupCellFactories() {
        hostNameTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        hostNameTableColumn.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setHostName(t.getNewValue()));
        ipAddressTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        ipAddressTableColumn.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setIpAddress(t.getNewValue()));
    }

    private void setupListeners() {
        filteredData.addListener((ListChangeListener<RowData>) change -> {
            SortedList<RowData> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(tableView.comparatorProperty());
            tableView.setItems(sortedData);
        });
    }

    public void doExit() {
        Platform.exit();
    }

    @FXML
    private void runPing() throws IOException {
        String strParameters = (tCheckBox.isSelected() ? PING_T : PING_N);
        PingDriver.ping(tableView, strParameters);
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
        String firefoxPath = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";

        // Constrói o comando para abrir o Chrome com a URL
        String[] command = {firefoxPath, url};

        // Inicia o processo
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.start();
    }

    @FXML
    private void openCiscoPrimeWebsite() throws IOException {
        String url = "https://ciscoprime.net.petrobras.com.br";
        String firefoxPath = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";

        // Constrói o comando para abrir o Firefox com a URL
        String[] command = {firefoxPath, url};

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
    private void showAdvancedFilterDialog() {
        try {
            searchTextField.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("advancedFilterDialog.fxml"));
            AnchorPane page = loader.load();
            Scene scene2 = new Scene(page);

            Stage advancedFilterDialogStage = new Stage();
            advancedFilterDialogStage.setTitle("Mostrar linhas que:");
            advancedFilterDialogStage.initModality(Modality.WINDOW_MODAL);
            advancedFilterDialogStage.setScene(scene2);
            AdvancedFilterDialogController ctrl = loader.getController();
            ctrl.setDialogStage(advancedFilterDialogStage);
            advancedFilterDialogStage.setResizable(false);
            advancedFilterDialogStage.showAndWait();

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
            return host.getHostName().toLowerCase().contains(lowerCaseFilter)
                    || host.getIpAddress().toLowerCase().contains(lowerCaseFilter)
                    || host.getLocation().toLowerCase().contains(lowerCaseFilter);
        };

        Predicate<RowData> p2 = host -> {
            if (aux2 == null || aux2.isEmpty()) {
                return true;
            }
            String lowerCaseFilter = aux2.toLowerCase();
            return host.getHostName().toLowerCase().contains(lowerCaseFilter)
                    || host.getIpAddress().toLowerCase().contains(lowerCaseFilter)
                    || host.getLocation().toLowerCase().contains(lowerCaseFilter);
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
            //Scene scene = new Scene(loader.load());

            // Cria um novo palco (Stage) para o diálogo
            Stage aboutStage = new Stage();
            aboutStage.setTitle("Sobre o Ivping");
            aboutStage.initModality(Modality.WINDOW_MODAL);

            // Define a cena no palco do diálogo
            //aboutStage.setScene(scene);
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
