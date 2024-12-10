package com.nuapps.ivping;

import com.nuapps.ivping.model.RowData;
import javafx.scene.control.TableView;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

@SuppressWarnings("rawtypes")
public class PingHelper {
    private static final Logger LOGGER = Logger.getLogger(PingHelper.class.getName());
    private static final String PING_N = "@ping -n 10 ";
    private static final String PING_T = "@ping -t ";

    public static void processRowData(RowData selectedRowData, boolean continuous, TableView tableView) {
        String hostName = selectedRowData.hostName();
        String ipAddress = selectedRowData.ipAddress();
        int lineNumber = tableView.getSelectionModel().getSelectedItems().indexOf(selectedRowData);
        String pingCommand = (continuous ? PING_T : PING_N) + ipAddress;

        try {
            String batFileName = createBatchFile(hostName, ipAddress, lineNumber, pingCommand);
            executeBatchFile(batFileName);
        } catch (IOException e) {
            LOGGER.severe("Error processing row data: " + e.getMessage());
        }
    }

    private static String createBatchFile(String hostName, String ipAddress, int lineNumber, String pingCommand) throws IOException {
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

    private static void executeBatchFile(String batFileName) throws IOException {
        new ProcessBuilder("rundll32", "SHELL32.DLL,ShellExec_RunDLL", batFileName).start();
    }
}