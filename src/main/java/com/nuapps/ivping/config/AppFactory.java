package com.nuapps.ivping.config;

import com.nuapps.ivping.service.BrowserService;
import com.nuapps.ivping.service.DesktopBrowserService;

import java.nio.file.Path;

public final class AppFactory {

    private static final BrowserService BROWSER_SERVICE = new DesktopBrowserService();
    private static AppConfig appConfig;

    private AppFactory() {
    }

    public static BrowserService browserService() {
        return BROWSER_SERVICE;
    }

    public static AppConfig appConfig() {
        if (appConfig == null) {
            Path configPath = ConfigFileManager.ensureConfigFileExists();
            appConfig = new AppConfig(configPath);
        }
        return appConfig;
    }
}