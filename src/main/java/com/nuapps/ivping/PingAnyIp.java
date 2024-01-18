package com.nuapps.ivping;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PingAnyIp {
    public void ping(String ipAdrress, String strParameters) throws IOException {
        String pingCommand = strParameters + ipAdrress;
        String env_temp = System.getenv("TEMP");
        FileWriter bat = new FileWriter(env_temp + "/ivping/ping_any_ip.bat");
        try (BufferedWriter bf = new BufferedWriter(bat)) {
            bf.write("@echo off");
            bf.newLine();
            bf.write("@cls");
            bf.newLine();
            bf.write(pingCommand);
            bf.newLine();
            bf.write("@pause");
        }

        String command = env_temp + "/ivping/ping_any_ip.bat";
        ProcessBuilder processBuilder = new ProcessBuilder("rundll32", "SHELL32.DLL,ShellExec_RunDLL", command);
        processBuilder.start();
    }
}
