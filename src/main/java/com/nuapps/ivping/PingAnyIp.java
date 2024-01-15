package com.nuapps.ivping;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class PingAnyIp {
    public void ping(String ipAdrress, String strParameters) throws IOException {
        String strCommand = strParameters + ipAdrress;
        String env_temp = System.getenv("TEMP");
        FileWriter bat = new FileWriter(env_temp + "/ivping/ping_any_ip.bat");
        try (BufferedWriter bf = new BufferedWriter(bat)) {
            bf.write("@echo off");
            bf.newLine();
            bf.write("@cls");
            bf.newLine();
            bf.write(strCommand);
            bf.newLine();
            bf.write("@pause");
        }
        String strCommand2 = env_temp + "/ivping/ping_any_ip.bat";
        Runtime.getRuntime().exec("rundll32 SHELL32.DLL,ShellExec_RunDLL " + strCommand2);
    }
}
