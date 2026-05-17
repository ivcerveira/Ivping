package com.nuapps.ivping;

import com.nuapps.ivping.model.HostData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelHostLoader {
    private static final String DATA_FOLDER = "Ivping_data";
    private static final String EXCEL_FILE_NAME = "data_hosts.xlsx";

    public List<HostData> loadHosts() {
        List<HostData> hostList = new ArrayList<>();
        String filePath = getExcelFilePath();

        File excelFile = new File(filePath);
        if (!excelFile.exists()) {
            System.err.println("Arquivo não encontrado: " + filePath);
            return hostList; // Retorna lista vazia, evita exceção
        }

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String host = getCellValue(row.getCell(0));
                    String ip = getCellValue(row.getCell(1));
                    String location = getCellValue(row.getCell(2));

                    hostList.add(new HostData(host, ip, location));
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler a planilha: " + e.getMessage());
        }

        return hostList;
    }

    // Determina o caminho completo do arquivo Excel
    private String getExcelFilePath() {
        String roamingAppData = System.getenv("APPDATA");

        if (roamingAppData != null && !roamingAppData.isBlank()) {
            return roamingAppData + File.separator + DATA_FOLDER + File.separator + EXCEL_FILE_NAME;
        } else {
            String userHome = System.getProperty("user.home");
            return userHome + File.separator + "AppData" + File.separator + "Roaming" +
                    File.separator + DATA_FOLDER + File.separator + EXCEL_FILE_NAME;
        }
    }

    // Converte o conteúdo de uma célula em String
    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
