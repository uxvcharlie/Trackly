package com.example.trackly.servicio;

import com.example.trackly.dao.InicioDAO;
import com.example.trackly.model.Clase;
import com.example.trackly.model.Recordatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Todo lo que muestra el Inicio, calculado de una vez para poder cargarlo en un Task. */
public record DatosInicio(ResumenMes resumen, Map<String, BigDecimal> porCategoria,
                          Map<String, BigDecimal> porMateria, List<Recordatorio> pagos,
                          ProximaClase proxima) {

    /** La clase que sigue y cuánto falta, con el día ya resuelto (hoy, mañana o el nombre del día). */
    public record ProximaClase(Clase clase, LocalDateTime inicio) {
    }

    public static DatosInicio cargar(int idUsuario, BigDecimal presupuesto, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        YearMonth mes = YearMonth.from(hoy);

        ResumenMes resumen = ResumenMes.calcular(idUsuario, presupuesto, hoy);
        Map<String, BigDecimal> porCategoria = aMapa(InicioDAO.gastoPorCategoria(idUsuario, mes));
        Map<String, BigDecimal> porMateria = aMapa(InicioDAO.gastoPorMateria(idUsuario, mes));
        List<Recordatorio> pagos = InicioDAO.proximosPagos(idUsuario, 5);
        ProximaClase proxima = buscarProximaClase(InicioDAO.clasesDelUsuario(idUsuario), ahora);
        return new DatosInicio(resumen, porCategoria, porMateria, pagos, proxima);
    }

    private static Map<String, BigDecimal> aMapa(List<Object[]> filas) {
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            mapa.put((String) fila[0], (BigDecimal) fila[1]);
        }
        return mapa;
    }

    /** La primera clase que empieza después de "ahora", mirando hasta 7 días hacia adelante. */
    public static ProximaClase buscarProximaClase(List<Clase> clases, LocalDateTime ahora) {
        ProximaClase mejor = null;
        List<Clase> lista = new ArrayList<>(clases);
        for (int sumar = 0; sumar <= 7; sumar++) {
            LocalDate dia = ahora.toLocalDate().plusDays(sumar);
            for (Clase c : lista) {
                if (c.getDia() != dia.getDayOfWeek().getValue()) {
                    continue;
                }
                LocalDateTime inicio = dia.atTime(c.getHoraInicio());
                if (inicio.isAfter(ahora) && (mejor == null || inicio.isBefore(mejor.inicio()))) {
                    mejor = new ProximaClase(c, inicio);
                }
            }
            if (mejor != null) {
                return mejor;
            }
        }
        return null;
    }
}
