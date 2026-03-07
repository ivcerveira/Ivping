package com.nuapps.ivping.service;

import java.awt.Desktop;
import java.net.URI;

public class DesktopBrowserService implements BrowserService {

    @Override
    public void open(String url) {
        try {
            if (!Desktop.isDesktopSupported()) {
                throw new UnsupportedOperationException(
                        "Desktop não é suportado neste ambiente."
                );
            }

            Desktop desktop = Desktop.getDesktop();

            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                throw new UnsupportedOperationException(
                        "A abertura de navegador não é suportada neste ambiente."
                );
            }

            desktop.browse(new URI(url));

        } catch (Exception e) {
            throw new RuntimeException("Falha ao abrir a URL: " + url, e);
        }
    }
}