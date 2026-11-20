package com.example.trackly.controller;

import com.example.trackly.TracklyApplication;
import com.example.trackly.database.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;

public class PrincipalController {

    @FXML private Label lblUsuario;
    @FXML private TabPane tabs;

    @FXML
    private void initialize() {
        lblUsuario.setText(Sesion.getUsuario().getNombre());
    }

    @FXML
    private void cerrarSesion() throws IOException {
        Sesion.cerrar();
        TracklyApplication.mostrar((Stage) tabs.getScene().getWindow(), "login.fxml", "Trackly");
    }
}
