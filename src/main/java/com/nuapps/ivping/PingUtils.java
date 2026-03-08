package com.nuapps.ivping;

import com.nuapps.ivping.model.HostData;
import javafx.scene.control.TableView;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class PingUtils {
    private static final Logger LOGGER = Logger.getLogger(PingUtils.class.getName());
    private static final String PING_N = "@ping -n 10 ";
    private static final String PING_T = "@ping -t ";
    private static final String PING_DEFAULT = "@ping ";

    public static void runPing(HostData selectedHostData, boolean continuous, TableView<HostData> tableView) {
        String hostName = selectedHostData.hostName();
        String ipAddress = selectedHostData.ipAddress();
        int lineNumber = tableView.getSelectionModel().getSelectedItems().indexOf(selectedHostData);
        String pingCommand = (continuous ? PING_T : PING_N) + ipAddress;

        try {
            String batFileName = createBatchFile(hostName, ipAddress, Integer.toString(lineNumber), pingCommand);
            executeBatchFile(batFileName);
        } catch (IOException e) {
            LOGGER.severe("Error processing row data: " + e.getMessage());
        }
    }

    private static String createBatchFile(String hostName, String ipAddress,
                                          String fileSuffix, String pingCommand) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null) {
            throw new IllegalStateException("TEMP directory not found");
        }

        String batFileName = tempDir + "/ping_test" + fileSuffix + ".bat";
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

    public static void runDefaultPingByIp(String ipAddress, boolean continuous) {
        String uniqueId = Long.toString(System.nanoTime());
        String pingCommand = (continuous ? PING_T : PING_DEFAULT) + ipAddress;
        String windowTitle = "Ping por IP  [" + ipAddress + "]";

        try {
            String batFileName = createDefaultPromptBatchFile(uniqueId, windowTitle, pingCommand);
            executeBatchFile(batFileName);
        } catch (IOException e) {
            LOGGER.severe("Error executing default ping: " + e.getMessage());
        }
    }

    private static String createDefaultPromptBatchFile(String fileSuffix, String windowTitle, String pingCommand) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir == null) {
            throw new IllegalStateException("TEMP directory not found");
        }

        String batFileName = tempDir + "/ping_default" + fileSuffix + ".bat";
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(batFileName))) {
            bufferedWriter.write("@echo off\n");
            bufferedWriter.write("@title " + windowTitle + "\n");
            bufferedWriter.write(pingCommand + "\n");
            bufferedWriter.write("@pause\n");
        }
        return batFileName;
    }
}
