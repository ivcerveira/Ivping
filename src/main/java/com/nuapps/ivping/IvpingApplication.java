package com.nuapps.ivping;

import com.nuapps.ivping.controller.IvpingController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class IvpingApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/ivp_icon.png"))));

        FXMLLoader fxmlLoader = new FXMLLoader(IvpingApplication.class.getResource("ivping_view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1050, 602);
        IvpingController controller = fxmlLoader.getController();

        stage.setTitle("Ivping");
        stage.setScene(scene);
        controller.configureCloseConfirmation(stage);
        stage.show();
    }
}
