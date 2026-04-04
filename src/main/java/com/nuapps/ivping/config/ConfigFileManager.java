package com.nuapps.ivping.config;

import com.nuapps.ivping.constants.ConfigKeys;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigFileManager {

    private static final Path CONFIG_DIRECTORY = Path.of("config");
    private static final Path CONFIG_FILE = CONFIG_DIRECTORY.resolve("app.properties");

    private ConfigFileManager() {
    }

    public static Path ensureConfigFileExists() {
        try {
            if (Files.notExists(CONFIG_DIRECTORY)) {
                Files.createDirectories(CONFIG_DIRECTORY);
            }

            if (Files.notExists(CONFIG_FILE)) {
                createDefaultConfigFile();
            }

            return CONFIG_FILE;

        } catch (IOException e) {
            throw new AppConfigException(
                    "Não foi possível preparar o arquivo de configuração.",
                    e
            );
        }
    }

    private static void createDefaultConfigFile() throws IOException {
        Properties properties = new Properties();
        properties.setProperty(ConfigKeys.TMN_URL, "");
        properties.setProperty(ConfigKeys.CISCOPRIME_URL, "");
        properties.setProperty(ConfigKeys.ZABBIX_URL, "");
        properties.setProperty(ConfigKeys.SIGMON_URL, "");
        properties.setProperty(ConfigKeys.REMOTE_ACCESS_BASE_URL, "");

        try (OutputStream outputStream = Files.newOutputStream(CONFIG_FILE)) {
            properties.store(outputStream,
                    "Configurações da aplicação. Preencha as URLs e reinicie o aplicativo.");
        }
    }
}
