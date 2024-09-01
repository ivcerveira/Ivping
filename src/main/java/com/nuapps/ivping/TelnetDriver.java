package com.nuapps.ivping;

import com.nuapps.ivping.model.RowData;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class TelnetDriver {
    public static void telnet(TableView<RowData> theTable) throws URISyntaxException, IOException {
        if (theTable.getSelectionModel().getSelectedIndex() >= 0) {
            ObservableList<RowData> data = theTable.getSelectionModel().getSelectedItems();
            if (data.size() == 1) {
                String hostName = data.get(0).hostName();

                if (hostName.matches("^(SW\\d|RT\\d|NB0|NB1).*$")) {
                    String uriTelnet = "https://s6006as3039.petrobras.biz/cgi-bin/telnet.sh?" + hostName;
                    Desktop.getDesktop().browse(new URI(uriTelnet));
                } else {
                    System.out.println("Apenas Roteador, Switch e No-Break");
                }
            } } }
}
