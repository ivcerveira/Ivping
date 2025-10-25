package com.nuapps.ivping;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PingAnyIp {
    public void ping(String ipAdrress, String strParameters) throws IOException {
        String pingCommand = strParameters + ipAdrress;
        String tempDir = System.getProperty("java.io.tmpdir");

        if (tempDir == null) {
            throw new IllegalStateException("TEMP directory not found");
        }

        ProcessBuilder processBuilder = getProcessBuilder(tempDir, pingCommand);
        processBuilder.start();
    }

    private static ProcessBuilder getProcessBuilder(String _tempDir, String _pingCommand) throws IOException {
        FileWriter bat = new FileWriter(_tempDir + "ping_any_ip.bat");
        try (BufferedWriter bf = new BufferedWriter(bat)) {
            bf.write("@echo off");
            bf.newLine();
            bf.write("@cls");
            bf.newLine();
            bf.write(_pingCommand);
            bf.newLine();
            bf.write("@pause");
        }

        String command = _tempDir + "ping_any_ip.bat";
        return new ProcessBuilder("rundll32", "SHELL32.DLL,ShellExec_RunDLL", command);
    }
}
