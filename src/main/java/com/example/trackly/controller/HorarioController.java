package com.example.trackly.controller;

import com.example.trackly.dao.ClaseDAO;
import com.example.trackly.dao.ClaseDAO.ClaseConGasto;
import com.example.trackly.database.Sesion;
import com.example.trackly.model.Clase;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class HorarioController {

    @FXML private ComboBox<YearMonth> cmbMes;
    @FXML private Label lblTotal;

    @FXML private TableView<ClaseConGasto> tblClases;
    @FXML private TableColumn<ClaseConGasto, String> colDia;
    @FXML private TableColumn<ClaseConGasto, String> colHora;
    @FXML private TableColumn<ClaseConGasto, String> colMateria;
    @FXML private TableColumn<ClaseConGasto, String> colAula;
    @FXML private TableColumn<ClaseConGasto, String> colGastado;

    @FXML private ComboBox<DayOfWeek> cmbDia;
    @FXML private TextField txtHoraInicio;
    @FXML private TextField txtHoraFin;
    @FXML private TextField txtMateria;
    @FXML private TextField txtAula;

    private static final Locale ES = Locale.of("es", "NI");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MMMM yyyy", ES);

    private ClaseConGasto seleccionada;

    @FXML
    private void initialize() {
        configurarTabla();
        cargarCombos();
        cargarClases();
        nuevo();
    }

    private void configurarTabla() {
        colDia.setCellValueFactory(c ->
                new SimpleStringProperty(nombreDia(DayOfWeek.of(c.getValue().dia()))));
        colHora.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().horaInicio().format(FORMATO_HORA) + " - " + c.getValue().horaFin().format(FORMATO_HORA)));
        colMateria.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().materia()));
        colAula.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().aula() == null ? "—" : c.getValue().aula()));
        colGastado.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().gastado().toPlainString()));

        tblClases.getSelectionModel().selectedItemProperty().addListener((obs, vieja, nueva) -> {
            if (nueva != null) {
                cargarEnFormulario(nueva);
            }
        });
    }

    private void cargarCombos() {
        cmbDia.setItems(FXCollections.observableArrayList(DayOfWeek.values()));
        cmbDia.setConverter(new StringConverter<>() {
            @Override
            public String toString(DayOfWeek dia) {
                return dia == null ? "" : nombreDia(dia);
            }

            @Override
            public DayOfWeek fromString(String texto) {
                return null;
            }
        });

        ObservableList<YearMonth> meses = FXCollections.observableArrayList();
        YearMonth actual = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            meses.add(actual.minusMonths(i));
        }
        cmbMes.setItems(meses);
        cmbMes.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth mes) {
                return mes == null ? "" : mes.format(FORMATO_MES);
            }

            @Override
            public YearMonth fromString(String texto) {
                return null;
            }
        });
        cmbMes.setValue(actual);
    }

    private void cargarClases() {
        YearMonth mes = cmbMes.getValue() == null ? YearMonth.now() : cmbMes.getValue();
        List<ClaseConGasto> clases = ClaseDAO.listarConGasto(idUsuario(), mes);
        tblClases.setItems(FXCollections.observableArrayList(clases));

        BigDecimal total = BigDecimal.ZERO;
        for (ClaseConGasto c : clases) {
            total = total.add(c.gastado());
        }
        lblTotal.setText("Gastado en clases: C$ " + total.toPlainString());
    }

    private void cargarEnFormulario(ClaseConGasto c) {
        seleccionada = c;
        cmbDia.setValue(DayOfWeek.of(c.dia()));
        txtHoraInicio.setText(c.horaInicio().format(FORMATO_HORA));
        txtHoraFin.setText(c.horaFin().format(FORMATO_HORA));
        txtMateria.setText(c.materia());
        txtAula.setText(c.aula() == null ? "" : c.aula());
    }

    @FXML
    private void cambiarMes() {
        cargarClases();
    }

    @FXML
    private void nuevo() {
        seleccionada = null;
        tblClases.getSelectionModel().clearSelection();
        cmbDia.setValue(null);
        txtHoraInicio.clear();
        txtHoraFin.clear();
        txtMateria.clear();
        txtAula.clear();
    }

    @FXML
    private void guardar() {
        DayOfWeek dia = cmbDia.getValue();
        LocalTime inicio = leerHora(txtHoraInicio);
        LocalTime fin = leerHora(txtHoraFin);
        String materia = txtMateria.getText().trim();
        String aula = txtAula.getText().trim();

        if (dia == null || materia.isEmpty()) {
            mostrarAviso("Completá el día y la materia.");
            return;
        }
        if (inicio == null || fin == null) {
            mostrarAviso("Escribí las horas como 08:00 o 13:30.");
            return;
        }
        if (!fin.isAfter(inicio)) {
            mostrarAviso("La hora de fin tiene que ser después de la de inicio.");
            return;
        }
        if (materia.length() > 80) {
            mostrarAviso("La materia no puede pasar de 80 caracteres.");
            return;
        }
        if (aula.length() > 30) {
            mostrarAviso("El aula no puede pasar de 30 caracteres.");
            return;
        }

        Integer idExcluir = seleccionada == null ? null : seleccionada.id();
        String aulaFinal = aula.isEmpty() ? null : aula;

        try {
            if (ClaseDAO.hayChoque(idUsuario(), dia.getValue(), inicio, fin, idExcluir)) {
                mostrarAviso("Esa hora se cruza con otra clase del " + nombreDia(dia).toLowerCase(ES) + ".");
                return;
            }
            if (seleccionada == null) {
                ClaseDAO.guardar(new Clase(Sesion.getUsuario(), dia.getValue(), inicio, fin, materia, aulaFinal));
            } else {
                ClaseDAO.actualizar(seleccionada.id(), idUsuario(), dia.getValue(), inicio, fin, materia, aulaFinal);
            }
            cargarClases();
            nuevo();
        } catch (RuntimeException e) {
            mostrarAviso("No se pudo guardar la clase.");
            e.printStackTrace();
        }
    }

    @FXML
    private void borrar() {
        if (seleccionada == null) {
            mostrarAviso("Seleccioná una clase de la tabla.");
            return;
        }
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Borrar " + seleccionada.materia() + "? Los gastos ligados a esta clase se quedan, pero sin clase.",
                ButtonType.YES, ButtonType.NO);
        confirmar.setHeaderText(null);
        if (confirmar.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        try {
            ClaseDAO.borrar(seleccionada.id(), idUsuario());
            cargarClases();
            nuevo();
        } catch (RuntimeException e) {
            mostrarAviso("No se pudo borrar la clase.");
            e.printStackTrace();
        }
    }

    private Integer idUsuario() {
        return Sesion.getUsuario().getId();
    }

    private LocalTime leerHora(TextField campo) {
        String texto = campo.getText().trim();
        if (texto.length() == 4 && texto.charAt(1) == ':') {
            texto = "0" + texto;   // acepta 8:00 además de 08:00
        }
        try {
            return LocalTime.parse(texto, FORMATO_HORA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String nombreDia(DayOfWeek dia) {
        String nombre = dia.getDisplayName(TextStyle.FULL, ES);
        return nombre.substring(0, 1).toUpperCase(ES) + nombre.substring(1);
    }

    private void mostrarAviso(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.WARNING, mensaje);
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }
}
