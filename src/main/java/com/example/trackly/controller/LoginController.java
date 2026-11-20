package com.example.trackly.controller;

import com.example.trackly.TracklyApplication;
import com.example.trackly.dao.UsuarioDAO;
import com.example.trackly.database.Sesion;
import com.example.trackly.model.Usuario;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;

public class LoginController {

    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtClave;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPresupuesto;
    @FXML private Label lblTitulo;
    @FXML private Label lblMensaje;
    @FXML private Button btnAccion;
    @FXML private Hyperlink lnkCambiar;
    @FXML private javafx.scene.layout.VBox cajaRegistro;

    private boolean modoRegistro = false;

    @FXML
    private void initialize() {
        actualizarModo();
    }

    @FXML
    private void cambiarModo() {
        modoRegistro = !modoRegistro;
        lblMensaje.setText("");
        actualizarModo();
    }

    private void actualizarModo() {
        cajaRegistro.setVisible(modoRegistro);
        cajaRegistro.setManaged(modoRegistro);
        lblTitulo.setText(modoRegistro ? "Crear cuenta" : "Iniciar sesión");
        btnAccion.setText(modoRegistro ? "Registrarme" : "Entrar");
        lnkCambiar.setText(modoRegistro ? "Ya tengo cuenta" : "No tengo cuenta, crear una");
    }

    @FXML
    private void accion() {
        String correo = txtCorreo.getText().trim();
        String clave = txtClave.getText();

        if (correo.isEmpty() || !correo.contains("@")) {
            error("Escribí un correo válido.");
            return;
        }
        if (clave.length() < 6) {
            error("La clave debe tener al menos 6 caracteres.");
            return;
        }

        Task<Usuario> tarea;
        if (modoRegistro) {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                error("Escribí tu nombre.");
                return;
            }
            BigDecimal presupuesto;
            try {
                String texto = txtPresupuesto.getText().trim();
                presupuesto = texto.isEmpty() ? BigDecimal.ZERO : new BigDecimal(texto);
                if (presupuesto.signum() < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                error("El presupuesto debe ser un número mayor o igual a 0.");
                return;
            }
            final BigDecimal p = presupuesto;
            tarea = new Task<>() {
                @Override
                protected Usuario call() {
                    return UsuarioDAO.registrar(nombre, correo, clave, p);
                }
            };
        } else {
            tarea = new Task<>() {
                @Override
                protected Usuario call() {
                    return UsuarioDAO.login(correo, clave);
                }
            };
        }

        // La consulta corre en otro hilo para que la ventana no se congele
        btnAccion.setDisable(true);
        lblMensaje.setText("Conectando...");
        tarea.setOnSucceeded(e -> {
            btnAccion.setDisable(false);
            Usuario u = tarea.getValue();
            if (u == null) {
                error("Correo o clave incorrectos.");
                return;
            }
            Sesion.iniciar(u);
            try {
                TracklyApplication.mostrar((Stage) btnAccion.getScene().getWindow(), "principal.fxml", "Trackly");
            } catch (IOException ex) {
                error("No se pudo abrir la pantalla principal.");
            }
        });
        tarea.setOnFailed(e -> {
            btnAccion.setDisable(false);
            Throwable causa = tarea.getException();
            String texto = String.valueOf(causa);
            if (modoRegistro && texto.contains("unique")) {
                error("Ese correo ya está registrado.");
            } else {
                error("No se pudo conectar con la base de datos. Revisá PostgreSQL y DB_PASSWORD.");
                causa.printStackTrace();
            }
        });
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void error(String texto) {
        lblMensaje.setText(texto);
    }
}
