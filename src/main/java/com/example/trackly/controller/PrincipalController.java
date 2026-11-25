package com.example.trackly.controller;

import com.example.trackly.TracklyApplication;
import com.example.trackly.database.Sesion;
import com.example.trackly.model.Usuario;
import com.example.trackly.servicio.ReporteMensual;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.YearMonth;

public class PrincipalController {

    @FXML private Label lblUsuario;
    @FXML private Button btnReporte;
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

    /** Guarda el reporte del mes en PDF. Compilar y llenar tarda, así que va en un Task. */
    @FXML
    private void generarReporte() {
        FileChooser elegir = new FileChooser();
        elegir.setTitle("Guardar reporte del mes");
        elegir.setInitialFileName("trackly-reporte-" + YearMonth.now() + ".pdf");
        elegir.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File archivo = elegir.showSaveDialog(tabs.getScene().getWindow());
        if (archivo == null) {
            return;
        }

        Usuario usuario = Sesion.getUsuario();
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ReporteMensual.exportarPdf(usuario, archivo.toPath());
                return null;
            }
        };
        btnReporte.setDisable(true);
        btnReporte.setText("Generando...");
        tarea.setOnSucceeded(e -> {
            terminarReporte();
            Alert listo = new Alert(Alert.AlertType.INFORMATION, "Reporte guardado en:\n" + archivo.getAbsolutePath()
                    + "\n\n¿Abrirlo ahora?", ButtonType.YES, ButtonType.NO);
            listo.setHeaderText("Reporte listo");
            listo.showAndWait()
                    .filter(b -> b == ButtonType.YES)
                    .ifPresent(b -> TracklyApplication.abrir(archivo));
        });
        tarea.setOnFailed(e -> {
            terminarReporte();
            tarea.getException().printStackTrace();
            Alert alerta = new Alert(Alert.AlertType.ERROR, "No se pudo generar el reporte: "
                    + tarea.getException().getMessage());
            alerta.setHeaderText(null);
            alerta.showAndWait();
        });
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void terminarReporte() {
        btnReporte.setDisable(false);
        btnReporte.setText("Reporte PDF");
    }

    @FXML
    private void cerrarSesion() throws IOException {
        Sesion.cerrar();
        TracklyApplication.mostrar((Stage) tabs.getScene().getWindow(), "login.fxml", "Trackly");
    }
}
