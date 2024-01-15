package com.nuapps.ivping;

import com.nuapps.ivping.model.RowData;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PingDriver {
    public static void ping(TableView<RowData> theTable, String strParameters) throws IOException {
        if (theTable.getSelectionModel().getSelectedIndex() >= 0) {
            ObservableList<RowData> data = theTable.getSelectionModel().getSelectedItems();
            for (RowData rowData1 : data) {
                String strCommand;
                String hostName = rowData1.getHostName();
                strCommand = strParameters + rowData1.getIpAddress();
                String ip_address = rowData1.getIpAddress();
                int lineNumber = data.indexOf(rowData1);

                String env_temp = System.getenv("TEMP");
                FileWriter bat = new FileWriter(env_temp + "/ivping/ping" + lineNumber + ".bat");
                try (BufferedWriter bf = new BufferedWriter(bat)) {
                    bf.write("@echo off");
                    bf.newLine();
                    bf.write("@cls");
                    bf.newLine();
                    bf.write("@color 17");
                    bf.newLine();
                    bf.write("@title Ping  " + hostName + "  [" + ip_address + "]");
                    bf.newLine();
                    bf.write(strCommand);
                    bf.newLine();
                    bf.write("@pause");
                }
                String strCommand2 = env_temp + "/ivping/ping" + lineNumber + ".bat";
                Runtime.getRuntime().exec("rundll32 SHELL32.DLL,ShellExec_RunDLL " + strCommand2);
            }
        }
    }
}
