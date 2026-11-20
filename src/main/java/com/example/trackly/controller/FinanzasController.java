package com.example.trackly.controller;

import com.example.trackly.dao.MovimientoDAO;
import com.example.trackly.database.Sesion;
import com.example.trackly.model.Categoria;
import com.example.trackly.model.Clase;
import com.example.trackly.model.Movimiento;
import com.example.trackly.model.TipoMovimiento;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class FinanzasController {

    @FXML private ComboBox<YearMonth> cmbFiltroMes;
    @FXML private ComboBox<Categoria> cmbFiltroCategoria;

    @FXML
    private TableView<Movimiento> tblMovimientos;
    @FXML
    private TableColumn<Movimiento, String> colFecha;
    @FXML
    private TableColumn<Movimiento, String> colTipo;
    @FXML
    private TableColumn<Movimiento, String> colCategoria;
    @FXML
    private TableColumn<Movimiento, String> colClase;
    @FXML
    private TableColumn<Movimiento, String> colNota;
    @FXML
    private TableColumn<Movimiento, String> colMonto;

    @FXML
    private ComboBox<TipoMovimiento> cmbTipo;
    @FXML
    private TextField txtMonto;
    @FXML
    private ComboBox<Categoria> cmbCategoria;
    @FXML
    private DatePicker dpFecha;
    @FXML
    private ComboBox<Clase> cmbClase;
    @FXML
    private Label lblClaseSugerida;
    @FXML
    private TextField txtNota;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_MES =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es", "NI"));

    private Movimiento seleccionado;

    @FXML
    private void initialize() {
        configurarTabla();
        cargarCombos();
        configurarFormulario();
        cargarMovimientos();
        nuevo();
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFecha().format(FORMATO_FECHA)));
        colTipo.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTipo() == TipoMovimiento.GASTO ? "Gasto" : "Ingreso"));
        colCategoria.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategoria().getNombre()));
        colClase.setCellValueFactory(c -> {
            Clase clase = c.getValue().getClase();
            return new SimpleStringProperty(clase == null ? "—" : clase.getMateria());
        });
        colNota.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNota() == null ? "" : c.getValue().getNota()));
        colMonto.setCellValueFactory(c -> {
            Movimiento m = c.getValue();
            String signo = m.getTipo() == TipoMovimiento.GASTO ? "-" : "+";
            return new SimpleStringProperty(signo + m.getMonto().toPlainString());
        });

        tblMovimientos.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null) {
                cargarEnFormulario(nuevo);
            }
        });
    }

    private void cargarCombos() {
        List<Categoria> categorias = MovimientoDAO.listarCategorias();
        cmbCategoria.setItems(FXCollections.observableArrayList(categorias));
        cmbFiltroCategoria.setItems(FXCollections.observableArrayList(categorias));
        cmbClase.setItems(FXCollections.observableArrayList(MovimientoDAO.listarClases(idUsuario())));
        cmbTipo.setItems(FXCollections.observableArrayList(TipoMovimiento.values()));

        ObservableList<YearMonth> meses = FXCollections.observableArrayList();
        YearMonth actual = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            meses.add(actual.minusMonths(i));
        }
        cmbFiltroMes.setItems(meses);
        cmbFiltroMes.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth mes) {
                return mes == null ? "" : mes.format(FORMATO_MES);
            }

            @Override
            public YearMonth fromString(String texto) {
                return null;
            }
        });
    }

    private void configurarFormulario() {
        cmbTipo.valueProperty().addListener((obs, viejo, nuevo) -> {
            boolean esGasto = nuevo == TipoMovimiento.GASTO;
            cmbClase.setDisable(!esGasto);
            if (!esGasto) {
                cmbClase.setValue(null);
            }
            sugerirClase();
        });
        dpFecha.valueProperty().addListener((obs, viejo, nuevo) -> sugerirClase());
    }

    private void cargarMovimientos() {
        YearMonth mes = cmbFiltroMes.getValue();
        Categoria categoria = cmbFiltroCategoria.getValue();
        Integer idCategoria = categoria == null ? null : categoria.getId();
        tblMovimientos.setItems(FXCollections.observableArrayList(
                MovimientoDAO.listar(idUsuario(), mes, idCategoria)));
    }

    private void cargarEnFormulario(Movimiento m) {
        seleccionado = m;
        cmbTipo.setValue(m.getTipo());
        txtMonto.setText(m.getMonto().toPlainString());
        cmbCategoria.setValue(buscarPorId(cmbCategoria.getItems(), Categoria::getId, m.getCategoria().getId()));
        dpFecha.setValue(m.getFecha());
        cmbClase.setValue(m.getClase() == null ? null
                : buscarPorId(cmbClase.getItems(), Clase::getId, m.getClase().getId()));
        txtNota.setText(m.getNota() == null ? "" : m.getNota());
        lblClaseSugerida.setText("");
    }

    private void sugerirClase() {
        lblClaseSugerida.setText("");
        if (seleccionado != null || cmbTipo.getValue() != TipoMovimiento.GASTO) {
            return;
        }
        if (!LocalDate.now().equals(dpFecha.getValue())) {
            return;
        }
        int hoy = LocalDate.now().getDayOfWeek().getValue();
        LocalTime ahora = LocalTime.now();
        for (Clase clase : cmbClase.getItems()) {
            if (clase.getDia() == hoy
                    && !ahora.isBefore(clase.getHoraInicio())
                    && ahora.isBefore(clase.getHoraFin())) {
                cmbClase.setValue(clase);
                lblClaseSugerida.setText("Sugerida: " + clase.getMateria()
                        + " (" + clase.getHoraInicio() + " - " + clase.getHoraFin() + ")");
                return;
            }
        }
    }

    @FXML
    private void filtrar() {
        cargarMovimientos();
    }

    @FXML
    private void quitarFiltros() {
        cmbFiltroMes.setValue(null);
        cmbFiltroCategoria.setValue(null);
        cargarMovimientos();
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        tblMovimientos.getSelectionModel().clearSelection();
        cmbTipo.setValue(TipoMovimiento.GASTO);
        txtMonto.clear();
        cmbCategoria.setValue(null);
        cmbClase.setValue(null);
        dpFecha.setValue(LocalDate.now());
        txtNota.clear();
        sugerirClase();
    }

    @FXML
    private void guardar() {
        TipoMovimiento tipo = cmbTipo.getValue();
        Categoria categoria = cmbCategoria.getValue();
        LocalDate fecha = dpFecha.getValue();
        BigDecimal monto = leerMonto();
        String nota = txtNota.getText().trim();

        if (tipo == null || categoria == null || fecha == null) {
            mostrarAviso("Completá el tipo, la categoría y la fecha.");
            return;
        }
        if (monto == null) {
            mostrarAviso("El monto tiene que ser un número mayor que 0.");
            return;
        }
        if (nota.length() > 150) {
            mostrarAviso("La nota no puede pasar de 150 caracteres.");
            return;
        }

        Clase clase = tipo == TipoMovimiento.GASTO ? cmbClase.getValue() : null;
        String notaFinal = nota.isEmpty() ? null : nota;

        try {
            if (seleccionado == null) {
                MovimientoDAO.guardar(new Movimiento(Sesion.getUsuario(), categoria, clase,
                        tipo, monto, fecha, notaFinal));
            } else {
                MovimientoDAO.actualizar(seleccionado.getId(), idUsuario(), categoria, clase,
                        tipo, monto, fecha, notaFinal);
            }
            cargarMovimientos();
            nuevo();
        } catch (RuntimeException e) {
            mostrarAviso("No se pudo guardar el movimiento.");
            e.printStackTrace();
        }
    }

    @FXML
    private void borrar() {
        if (seleccionado == null) {
            mostrarAviso("Seleccioná un movimiento de la tabla.");
            return;
        }
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Borrar este movimiento?", ButtonType.YES, ButtonType.NO);
        confirmar.setHeaderText(null);
        if (confirmar.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        try {
            MovimientoDAO.borrar(seleccionado.getId(), idUsuario());
            cargarMovimientos();
            nuevo();
        } catch (RuntimeException e) {
            mostrarAviso("No se pudo borrar el movimiento.");
            e.printStackTrace();
        }
    }

    private Integer idUsuario() {
        return Sesion.getUsuario().getId();
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

    private <T> T buscarPorId(List<T> lista, Function<T, Integer> getId, Integer id) {
        for (T item : lista) {
            if (id.equals(getId.apply(item))) {
                return item;
            }
        }
        return null;
    }

    private void mostrarAviso(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }
}