package com.nuapps.ivping.model;

// Classe de modelo para representar uma linha de dados
public class RowData {
    private String hostName;
    private String ipAddress;
    private final String location;

    public RowData(String hostName, String ipAddress, String location) {
        this.hostName = hostName;
        this.ipAddress = ipAddress;
        this.location = location;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getLocation() {
        return location;
    }
}
