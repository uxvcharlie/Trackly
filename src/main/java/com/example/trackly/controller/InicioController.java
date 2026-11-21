package com.example.trackly.controller;

import com.example.trackly.database.Sesion;
import com.example.trackly.model.Recordatorio;
import com.example.trackly.model.Usuario;
import com.example.trackly.servicio.DatosInicio;
import com.example.trackly.servicio.ResumenMes;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

public class InicioController {

    private static final Locale ES = Locale.of("es", "NI");
    private static final DateTimeFormatter FORMATO_PAGO = DateTimeFormatter.ofPattern("d MMM, HH:mm", ES);

    @FXML private Label lblMes;
    @FXML private Label lblDisponible;
    @FXML private Label lblGastado;
    @FXML private Label lblPendientes;
    @FXML private Label lblProyeccion;
    @FXML private Label lblProyeccionDetalle;
    @FXML private ProgressBar barraPresupuesto;
    @FXML private Label lblBarra;
    @FXML private PieChart graficaCategorias;
    @FXML private BarChart<String, Number> graficaMaterias;
    @FXML private ListView<String> listaPagos;
    @FXML private Label lblProximaClase;
    @FXML private Label lblEstado;

    @FXML
    private void initialize() {
        graficaMaterias.setLegendVisible(false);
        graficaMaterias.setAnimated(false);
        graficaCategorias.setAnimated(false);
        recargar();
    }

    /** Se llama al abrir la pestaña y después de guardar algo en otra pantalla. */
    public void recargar() {
        Usuario u = Sesion.getUsuario();
        if (u == null) {
            return;
        }
        int idUsuario = u.getId();
        BigDecimal presupuesto = u.getPresupuestoMes();

        // Las consultas corren en otro hilo para que la ventana no se congele
        Task<DatosInicio> tarea = new Task<>() {
            @Override
            protected DatosInicio call() {
                return DatosInicio.cargar(idUsuario, presupuesto, LocalDateTime.now());
            }
        };
        lblEstado.setText("Cargando...");
        tarea.setOnSucceeded(e -> {
            lblEstado.setText("");
            mostrar(tarea.getValue(), presupuesto);
        });
        tarea.setOnFailed(e -> {
            lblEstado.setText("No se pudo cargar el resumen.");
            tarea.getException().printStackTrace();
        });
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrar(DatosInicio datos, BigDecimal presupuesto) {
        ResumenMes r = datos.resumen();
        LocalDate hoy = LocalDate.now();
        lblMes.setText(capitalizar(hoy.getMonth().getDisplayName(TextStyle.FULL, ES)) + " " + hoy.getYear());

        lblDisponible.setText(dinero(r.disponible()));
        lblDisponible.getStyleClass().remove("negativo");
        if (r.disponible().signum() < 0) {
            lblDisponible.getStyleClass().add("negativo");
        }
        lblGastado.setText(dinero(r.gastado()));
        lblPendientes.setText(dinero(r.pendientes()));

        if (r.hayProyeccion()) {
            lblProyeccion.setText(dinero(r.alFinalDelMes()));
            lblProyeccion.getStyleClass().remove("negativo");
            if (r.alFinalDelMes().signum() < 0) {
                lblProyeccion.getStyleClass().add("negativo");
            }
            lblProyeccionDetalle.setText("Cada día de clase cuesta " + dinero(r.costoPorDia()) + ", y faltan "
                    + r.diasQueFaltan() + " en el mes. Se gastaría " + dinero(r.proyeccion()) + " en total.");
        } else {
            lblProyeccion.setText("—");
            lblProyeccion.getStyleClass().remove("negativo");
            lblProyeccionDetalle.setText("Todavía no hay gastos ligados a una clase. Cuando haya, aquí se ve si alcanza hasta fin de mes.");
        }

        // Barra: cuánto del presupuesto ya está comprometido (gastado + pendientes)
        if (presupuesto.signum() > 0) {
            BigDecimal comprometido = r.gastado().add(r.pendientes());
            double fraccion = comprometido.divide(presupuesto, 4, RoundingMode.HALF_UP).doubleValue();
            barraPresupuesto.setProgress(Math.min(fraccion, 1.0));
            lblBarra.setText(Math.round(fraccion * 100) + " % del presupuesto comprometido");
        } else {
            barraPresupuesto.setProgress(0);
            lblBarra.setText("Sin presupuesto definido");
        }

        graficaCategorias.setData(FXCollections.observableArrayList());
        for (Map.Entry<String, BigDecimal> e : datos.porCategoria().entrySet()) {
            graficaCategorias.getData().add(new PieChart.Data(e.getKey(), e.getValue().doubleValue()));
        }
        graficaCategorias.setTitle(datos.porCategoria().isEmpty() ? "Sin gastos este mes" : "Gastos por categoría");

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Map.Entry<String, BigDecimal> e : datos.porMateria().entrySet()) {
            serie.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        graficaMaterias.getData().setAll(java.util.List.of(serie));
        graficaMaterias.setTitle(datos.porMateria().isEmpty() ? "Sin gastos ligados a clases" : "Gastos por materia");

        listaPagos.getItems().clear();
        for (Recordatorio p : datos.pagos()) {
            listaPagos.getItems().add(p.getFechaHora().format(FORMATO_PAGO) + "  ·  " + p.getTitulo()
                    + "  ·  " + dinero(p.getMonto()));
        }
        if (datos.pagos().isEmpty()) {
            listaPagos.setPlaceholder(new Label("No hay pagos pendientes"));
        }

        DatosInicio.ProximaClase pc = datos.proxima();
        if (pc == null) {
            lblProximaClase.setText("No hay clases en el horario");
        } else {
            LocalDate dia = pc.inicio().toLocalDate();
            String cuando = dia.equals(hoy) ? "hoy" : dia.equals(hoy.plusDays(1)) ? "mañana"
                    : dia.getDayOfWeek().getDisplayName(TextStyle.FULL, ES);
            lblProximaClase.setText(pc.clase().getMateria() + " — " + cuando + " a las "
                    + pc.clase().getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    private static String dinero(BigDecimal monto) {
        return "C$ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
