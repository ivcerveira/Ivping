/*
package com.nuapps.ivping;

import com.nuapps.ivping.model.RowData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelReader {
    private static final List<RowData> rowDataList = new ArrayList<>();

    public static void excel() throws IOException {
        File file = new File("devices.xlsx");
        try (FileInputStream fileInputStream = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next(); // Skip header row

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                RowData rowData = new RowData(
                        getStringCellValue(cellIterator.next()),
                        getStringCellValue(cellIterator.next()),
                        getStringCellValue(cellIterator.next())
                );
                rowDataList.add(rowData);
            }
        }
    }

    private static String getStringCellValue(Cell cell) {
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        } else {
            return "";
        }
    }

    public static List<RowData> getHostsList() {
        return rowDataList;
    }
}
*/
