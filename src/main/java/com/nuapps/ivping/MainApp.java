package com.nuapps.ivping;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/ivp_icon.png"))));

        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("ping-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 522);

        stage.setTitle("Ivping");
        stage.setScene(scene);
        stage.show();
    }
}
