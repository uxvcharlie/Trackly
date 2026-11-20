package com.example.trackly.servicio;

import com.example.trackly.dao.InicioDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

/**
 * Las cuentas del Inicio.
 * disponible = presupuesto - gastado - pendientes
 * proyección = gastado + pendientes + (días de clase que faltan × costo por día de clase)
 * costoPorDia, proyeccion y alFinalDelMes quedan en null mientras no haya gastos ligados a clases.
 */
public record ResumenMes(BigDecimal presupuesto, BigDecimal gastado, BigDecimal pendientes,
                         BigDecimal disponible, BigDecimal costoPorDia, long diasQueFaltan,
                         BigDecimal proyeccion, BigDecimal alFinalDelMes) {

    public boolean hayProyeccion() {
        return costoPorDia != null;
    }

    public static ResumenMes calcular(int idUsuario, BigDecimal presupuesto, LocalDate hoy) {
        YearMonth mes = YearMonth.from(hoy);

        BigDecimal gastado = InicioDAO.gastadoDelMes(idUsuario, mes);
        BigDecimal pendientes = InicioDAO.pendientesHasta(idUsuario, mes.atEndOfMonth().atTime(23, 59, 59));
        BigDecimal disponible = presupuesto.subtract(gastado).subtract(pendientes);

        Object[] enClases = InicioDAO.gastosEnClases(idUsuario, mes);
        BigDecimal totalEnClases = (BigDecimal) enClases[0];
        long diasConGasto = (Long) enClases[1];

        long diasQueFaltan = diasDeClaseQueFaltan(InicioDAO.diasConClase(idUsuario), hoy);

        if (diasConGasto == 0) {
            return new ResumenMes(presupuesto, gastado, pendientes, disponible, null, diasQueFaltan, null, null);
        }

        BigDecimal costoPorDia = totalEnClases.divide(BigDecimal.valueOf(diasConGasto), 2, RoundingMode.HALF_UP);
        BigDecimal proyeccion = gastado.add(pendientes).add(costoPorDia.multiply(BigDecimal.valueOf(diasQueFaltan)));
        BigDecimal alFinal = presupuesto.subtract(proyeccion);
        return new ResumenMes(presupuesto, gastado, pendientes, disponible, costoPorDia, diasQueFaltan, proyeccion, alFinal);
    }

    /** Cuenta desde mañana hasta el último día del mes los días que tienen clase. */
    public static long diasDeClaseQueFaltan(Set<Integer> diasConClase, LocalDate hoy) {
        LocalDate fin = YearMonth.from(hoy).atEndOfMonth();
        long dias = 0;
        for (LocalDate d = hoy.plusDays(1); !d.isAfter(fin); d = d.plusDays(1)) {
            if (diasConClase.contains(d.getDayOfWeek().getValue())) {
                dias++;
            }
        }
        return dias;
    }
}
