package com.example.trackly.controller;

import com.example.trackly.TracklyApplication;
import com.example.trackly.database.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;

public class PrincipalController {

    @FXML private Label lblUsuario;
    @FXML private TabPane tabs;
    @FXML private Tab tabInicio;
    @FXML private InicioController inicioController;   // fx:id="inicio" + "Controller"

    @FXML
    private void initialize() {
        lblUsuario.setText(Sesion.getUsuario().getNombre());
        // Al volver a la pestaña Inicio se recalcula con lo que se guardó en las otras
        tabInicio.setOnSelectionChanged(e -> {
            if (tabInicio.isSelected()) {
                inicioController.recargar();
            }
        });
    }

    @FXML
    private void cerrarSesion() throws IOException {
        Sesion.cerrar();
        TracklyApplication.mostrar((Stage) tabs.getScene().getWindow(), "login.fxml", "Trackly");
    }
}
