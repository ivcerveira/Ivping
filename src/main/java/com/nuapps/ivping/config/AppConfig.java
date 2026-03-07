package com.nuapps.ivping.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig {

    private final Properties properties = new Properties();

    public AppConfig(Path configPath) {
        load(configPath);
    }

    private void load(Path configPath) {
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new AppConfigException(
                    "Erro ao carregar o arquivo de configuração: " + configPath,
                    e
            );
        }
    }

    public String getRequired(String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new AppConfigException(
                    "A configuração \"" + key + "\" não foi preenchida no arquivo app.properties."
            );
        }

        return value.trim();
    }
}