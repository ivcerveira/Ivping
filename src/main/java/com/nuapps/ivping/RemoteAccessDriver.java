package com.nuapps.ivping;

import com.nuapps.ivping.model.HostData;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class RemoteAccessDriver {

    private static final String BASE_URL = "https://s6006as3039.petrobras.biz/cgi-bin/";

    public enum AccessType {
        SSH("ssh.sh", "^(SW\\d|RT\\d|RD\\d|NB0|NB1).*$", "Apenas Switch, Roteador e No-Break"),
        TELNET("telnet.sh", "^(SW\\d|RT\\d|NB0|NB1).*$", "Apenas Roteador, Switch e No-Break");

        private final String scriptName;
        private final String hostPattern;
        private final String invalidMessage;

        AccessType(String scriptName, String hostPattern, String invalidMessage) {
            this.scriptName = scriptName;
            this.hostPattern = hostPattern;
            this.invalidMessage = invalidMessage;
        }

        public String getScriptName() {
            return scriptName;
        }

        public String getHostPattern() {
            return hostPattern;
        }

        public String getInvalidMessage() {
            return invalidMessage;
        }
    }

    public static void open(TableView<HostData> theTable, AccessType accessType)
            throws URISyntaxException, IOException {

        if (theTable.getSelectionModel().getSelectedIndex() < 0) {
            return;
        }

        ObservableList<HostData> data = theTable.getSelectionModel().getSelectedItems();

        if (data.size() != 1) {
            return;
        }

        String hostName = data.getFirst().hostName();

        if (!hostName.matches(accessType.getHostPattern())) {
            System.out.println(accessType.getInvalidMessage());
            return;
        }

        String uri = BASE_URL + accessType.getScriptName() + "?" + hostName;
        Desktop.getDesktop().browse(new URI(uri));
    }
}