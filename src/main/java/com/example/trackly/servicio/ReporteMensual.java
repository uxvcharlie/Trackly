package com.example.trackly.servicio;

import com.example.trackly.database.JPAUtil;
import com.example.trackly.model.Usuario;
import jakarta.persistence.EntityManager;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.hibernate.Session;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reporte mensual en PDF hecho con JasperReports.
 * Los .jrxml están en resources/com/example/trackly/reportes y se pueden abrir en Jaspersoft Studio.
 * El resumen (presupuesto, pendientes, disponible, proyección) sale de ResumenMes, igual que en Inicio;
 * los movimientos, recordatorios y el horario los consulta el propio reporte con la conexión de Hibernate.
 */
public class ReporteMensual {

    private static final String CARPETA = "/com/example/trackly/reportes/";

    // Compilar un .jrxml tarda un par de segundos: se hace una sola vez y se reutiliza
    private static JasperReport principal;
    private static JasperReport recordatorios;
    private static JasperReport horario;

    public static void exportarPdf(Usuario usuario, Path destino) throws JRException {
        LocalDate hoy = LocalDate.now();
        YearMonth mes = YearMonth.from(hoy);
        ResumenMes r = ResumenMes.calcular(usuario.getId(), usuario.getPresupuestoMes(), hoy);

        compilar();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ID_USUARIO", usuario.getId());
        parametros.put("USUARIO", usuario.getNombre());
        parametros.put("DESDE", Date.valueOf(mes.atDay(1)));
        parametros.put("HASTA", Date.valueOf(mes.atEndOfMonth()));
        parametros.put("PRESUPUESTO", r.presupuesto());
        parametros.put("PENDIENTES", r.pendientes());
        parametros.put("DISPONIBLE", r.disponible());
        parametros.put("PROYECCION", r.proyeccion());
        parametros.put("AL_FINAL", r.alFinalDelMes());
        parametros.put("COSTO_POR_DIA", r.costoPorDia());
        parametros.put("DIAS_QUE_FALTAN", r.diasQueFaltan());
        parametros.put("SUB_RECORDATORIOS", recordatorios);
        parametros.put("SUB_HORARIO", horario);
        parametros.put(JRParameter.REPORT_LOCALE, Locale.of("es", "NI"));

        JasperPrint impreso = llenar(parametros);
        JasperExportManager.exportReportToPdfFile(impreso, destino.toString());
    }

    /** Llena el reporte con la misma conexión JDBC que usa Hibernate (no se abre otra a mano). */
    private static JasperPrint llenar(Map<String, Object> parametros) throws JRException {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.unwrap(Session.class).doReturningWork(conexion -> {
                try {
                    return JasperFillManager.fillReport(principal, parametros, conexion);
                } catch (JRException e) {
                    throw new IllegalStateException(e);   // doReturningWork solo deja lanzar SQLException
                }
            });
        } catch (IllegalStateException e) {
            if (e.getCause() instanceof JRException jr) {
                throw jr;
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private static synchronized void compilar() throws JRException {
        if (principal == null) {
            recordatorios = compilar("trackly_recordatorios.jrxml");
            horario = compilar("trackly_horario.jrxml");
            principal = compilar("trackly_reporte_mensual.jrxml");
        }
    }

    private static JasperReport compilar(String archivo) throws JRException {
        try (InputStream jrxml = ReporteMensual.class.getResourceAsStream(CARPETA + archivo)) {
            if (jrxml == null) {
                throw new JRException("No se encontró el reporte " + archivo);
            }
            return JasperCompileManager.compileReport(jrxml);
        } catch (IOException e) {
            throw new JRException(e);
        }
    }
}
