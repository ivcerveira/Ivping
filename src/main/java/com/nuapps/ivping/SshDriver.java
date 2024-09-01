package com.nuapps.ivping;

import com.nuapps.ivping.model.RowData;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class SshDriver {
    public static void ssh(TableView<RowData> theTable) throws URISyntaxException, IOException {
        if (theTable.getSelectionModel().getSelectedIndex() >= 0) {
            ObservableList<RowData> data = theTable.getSelectionModel().getSelectedItems();
            if (data.size() == 1) {
                String hostName = data.get(0).hostName();

                if (hostName.matches("^(SW\\d|RT\\d|RD\\d|NB0|NB1).*$")) {
                    String uriSsh = "https://s6006as3039.petrobras.biz/cgi-bin/ssh.sh?" + hostName;
                    Desktop.getDesktop().browse(new URI(uriSsh));
                } else {
                    System.out.println("Apenas Switch, Roteador e No-Break");
                }
            } } }
}
