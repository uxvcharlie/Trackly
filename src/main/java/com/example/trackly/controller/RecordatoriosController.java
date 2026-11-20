package com.example.trackly.controller;

import com.example.trackly.dao.MovimientoDAO;
import com.example.trackly.dao.RecordatorioDAO;
import com.example.trackly.database.Sesion;
import com.example.trackly.model.Categoria;
import com.example.trackly.model.Recordatorio;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class RecordatoriosController {

    @FXML
    private CheckBox chkMostrarHechos;

    @FXML
    private TableView<Recordatorio> tblRecordatorios;
    @FXML
    private TableColumn<Recordatorio, String> colFecha;
    @FXML
    private TableColumn<Recordatorio, String> colHora;
    @FXML
    private TableColumn<Recordatorio, String> colTitulo;
    @FXML
    private TableColumn<Recordatorio, String> colMonto;
    @FXML
    private TableColumn<Recordatorio, String> colCategoria;
    @FXML
    private TableColumn<Recordatorio, String> colEstado;

    @FXML
    private TextField txtTitulo;
    @FXML
    private DatePicker dpFecha;
    @FXML
    private TextField txtHora;
    @FXML
    private CheckBox chkPago;
    @FXML
    private TextField txtMonto;
    @FXML
    private ComboBox<Categoria> cmbCategoria;
    @FXML
    private CheckBox chkMensual;
    @FXML
    private Button btnHecho;
    @FXML
    private Button btnPagar;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LEER_HORA = DateTimeFormatter.ofPattern("H:mm");

    private Recordatorio seleccionado;

    @FXML
    private void initialize() {
        configurarTabla();
        cargarCategorias();
        configurarFormulario();
        cargarRecordatorios();
        nuevo();
    }


    private void configurarTabla() {
        colFecha.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFechaHora().toLocalDate().format(FORMATO_FECHA)));
        colHora.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFechaHora().toLocalTime().format(FORMATO_HORA)));
        colTitulo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTitulo()));
        colMonto.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().esPago() ? c.getValue().getMonto().toPlainString() : "—"));
        colCategoria.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategoria() == null ? "—"
                        : c.getValue().getCategoria().getNombre()));
        colEstado.setCellValueFactory(c ->
                new SimpleStringProperty(estado(c.getValue())));

        tblRecordatorios.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null) {
                cargarEnFormulario(nuevo);
            }
        });
    }

    private void cargarCategorias() {
        cmbCategoria.setItems(FXCollections.observableArrayList(MovimientoDAO.listarCategorias()));
    }

    private void configurarFormulario() {
        chkPago.selectedProperty().addListener((obs, viejo, nuevo) -> activarCamposPago(nuevo));
    }

    private void activarCamposPago(boolean esPago) {
        txtMonto.setDisable(!esPago);
        cmbCategoria.setDisable(!esPago);
        chkMensual.setDisable(!esPago);
        if (!esPago) {
            txtMonto.clear();
            cmbCategoria.setValue(null);
            chkMensual.setSelected(false);
        }
    }


    private void cargarRecordatorios() {
        tblRecordatorios.setItems(FXCollections.observableArrayList(
                RecordatorioDAO.listar(idUsuario(), chkMostrarHechos.isSelected())));
    }

    private void cargarEnFormulario(Recordatorio r) {
        seleccionado = r;
        txtTitulo.setText(r.getTitulo());
        dpFecha.setValue(r.getFechaHora().toLocalDate());
        txtHora.setText(r.getFechaHora().toLocalTime().format(FORMATO_HORA));
        chkPago.setSelected(r.esPago());
        activarCamposPago(r.esPago());
        if (r.esPago()) {
            txtMonto.setText(r.getMonto().toPlainString());
            cmbCategoria.setValue(buscarCategoria(r.getCategoria().getId()));
            chkMensual.setSelected(r.isMensual());
        }
        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean pendiente = seleccionado != null && !seleccionado.isHecho();
        btnPagar.setDisable(!(pendiente && seleccionado.esPago()));
        btnHecho.setDisable(!(pendiente && !seleccionado.esPago()));
    }

    private String estado(Recordatorio r) {
        if (r.isHecho()) {
            return r.esPago() ? "Pagado" : "Hecho";
        }
        return r.getFechaHora().isBefore(LocalDateTime.now()) ? "Vencido" : "Pendiente";
    }


    @FXML
    private void filtrar() {
        cargarRecordatorios();
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        tblRecordatorios.getSelectionModel().clearSelection();
        txtTitulo.clear();
        dpFecha.setValue(LocalDate.now());
        txtHora.clear();
        chkPago.setSelected(false);
        activarCamposPago(false);
        actualizarBotones();
    }

    @FXML
    private void guardar() {
        String titulo = txtTitulo.getText().trim();
        LocalDate fecha = dpFecha.getValue();
        LocalTime hora = leerHora();

        if (titulo.isEmpty()) {
            mostrarAviso("Escribí un título.");
            return;
        }
        if (titulo.length() > 100) {
            mostrarAviso("El título no puede pasar de 100 caracteres.");
            return;
        }
        if (fecha == null || hora == null) {
            mostrarAviso("Elegí la fecha y escribí la hora como 7:30 o 19:00.");
            return;
        }
        if (seleccionado != null && seleccionado.isHecho()) {
            mostrarAviso("Ese recordatorio ya está hecho y no se puede editar.");
            return;
        }

        BigDecimal monto = null;
        Categoria categoria = null;
        boolean mensual = false;
        if (chkPago.isSelected()) {
            monto = leerMonto();
            categoria = cmbCategoria.getValue();
            mensual = chkMensual.isSelected();
            if (monto == null) {
                mostrarAviso("El monto tiene que ser un número mayor que 0.");
                return;
            }
            if (categoria == null) {
                mostrarAviso("Elegí la categoría del pago.");
                return;
            }
        }

        LocalDateTime fechaHora = LocalDateTime.of(fecha, hora);
        try {
            if (seleccionado == null) {
                RecordatorioDAO.guardar(new Recordatorio(Sesion.getUsuario(), titulo, fechaHora,
                        monto, categoria, mensual));
            } else {
                RecordatorioDAO.actualizar(seleccionado.getId(), idUsuario(), titulo, fechaHora,
                        monto, categoria, mensual);
            }
            cargarRecordatorios();
            nuevo();
        } catch (RuntimeException e) {
            mostrarAviso("No se pudo guardar el recordatorio.");
            e.printStackTrace();
        }
    }

    @FXML
    private void borrar() {
        if (seleccionado == null) {
            mostrarAviso("Seleccioná un recordatorio de la tabla.");
            return;
        }
        if (!confirmar("¿Borrar este recordatorio?")) {
            return;
        }
        try {
            RecordatorioDAO.borrar(seleccionado.getId(), idUsuario());
            cargarRecordatorios();
            nuevo();
        } catch (RuntimeException e) {
            mostrarAviso("No se pudo borrar el recordatorio.");
            e.printStackTrace();
        }
    }

    @FXML
    private void marcarHecho() {
        if (seleccionado == null || seleccionado.esPago()) {
            mostrarAviso("Seleccioná un recordatorio que no sea pago.");
            return;
        }
        try {
            RecordatorioDAO.marcarHecho(seleccionado.getId(), idUsuario());
            cargarRecordatorios();
            nuevo();
        } catch (RuntimeException e) {
            mostrarAviso("No se pudo marcar como hecho.");
            e.printStackTrace();
        }
    }

    @FXML
    private void pagar() {
        if (seleccionado == null || !seleccionado.esPago() || seleccionado.isHecho()) {
            mostrarAviso("Seleccioná un pago pendiente.");
            return;
        }
        String mensaje = "Se va a registrar un gasto de C$ " + seleccionado.getMonto().toPlainString()
                + " en Finanzas.";
        if (seleccionado.isMensual()) {
            mensaje += " También se crea el recordatorio del próximo mes.";
        }
        if (!confirmar(mensaje)) {
            return;
        }
        try {
            RecordatorioDAO.pagar(seleccionado.getId(), idUsuario());
            cargarRecordatorios();
            nuevo();
            Alert listo = new Alert(Alert.AlertType.INFORMATION, "Pago registrado. Ya aparece en Finanzas.");
            listo.setHeaderText(null);
            listo.showAndWait();
        } catch (RuntimeException e) {
            mostrarAviso("No se pudo registrar el pago. No se guardó nada.");
            e.printStackTrace();
        }
    }


    private Integer idUsuario() {
        return Sesion.getUsuario().getId();
    }

    private LocalTime leerHora() {
        try {
            return LocalTime.parse(txtHora.getText().trim(), LEER_HORA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private BigDecimal leerMonto() {
        try {
            BigDecimal monto = new BigDecimal(txtMonto.getText().trim().replace(",", ""));
            if (monto.signum() <= 0) {
                return null;
            }
            return monto.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Categoria buscarCategoria(Integer id) {
        for (Categoria c : cmbCategoria.getItems()) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    private boolean confirmar(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        alerta.setHeaderText(null);
        return alerta.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void mostrarAviso(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }
}