package com.example.trackly;

import com.example.trackly.database.JPAUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class TracklyApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        stage.setMinWidth(480);
        stage.setMinHeight(560);
        mostrar(stage, "login.fxml", "Trackly");
        stage.show();
    }

    /** Cambia la pantalla de la ventana (login y principal). */
    public static void mostrar(Stage stage, String fxml, String titulo) throws IOException {
        FXMLLoader loader = new FXMLLoader(TracklyApplication.class.getResource(fxml));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(TracklyApplication.class.getResource("trackly.css").toExternalForm());
        stage.setTitle(titulo);
        stage.setScene(scene);
    }

    @Override
    public void stop() {
        JPAUtil.cerrar();   // cierra el EntityManagerFactory al salir
    }

    public static void main(String[] args) {
        launch(args);
    }
}
