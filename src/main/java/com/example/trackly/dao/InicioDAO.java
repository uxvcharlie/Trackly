package com.example.trackly.dao;

import com.example.trackly.database.JPAUtil;
import com.example.trackly.model.Clase;
import com.example.trackly.model.Recordatorio;
import com.example.trackly.model.TipoMovimiento;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Consultas de solo lectura para la pantalla de Inicio.
 * Todas filtran por id_usuario: cada quien ve únicamente lo suyo.
 */
public class InicioDAO {

    public static BigDecimal gastadoDelMes(int idUsuario, YearMonth mes) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            BigDecimal suma = em.createQuery(
                            "SELECT SUM(m.monto) FROM Movimiento m WHERE m.usuario.id = :u AND m.tipo = :t "
                                    + "AND m.fecha BETWEEN :desde AND :hasta", BigDecimal.class)
                    .setParameter("u", idUsuario)
                    .setParameter("t", TipoMovimiento.GASTO)
                    .setParameter("desde", mes.atDay(1))
                    .setParameter("hasta", mes.atEndOfMonth())
                    .getSingleResult();
            return suma == null ? BigDecimal.ZERO : suma;
        } finally {
            em.close();
        }
    }

    /** Pagos que todavía no se hacen y vencen hasta la fecha dada (incluye los atrasados). */
    public static BigDecimal pendientesHasta(int idUsuario, LocalDateTime hasta) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            BigDecimal suma = em.createQuery(
                            "SELECT SUM(r.monto) FROM Recordatorio r WHERE r.usuario.id = :u AND r.hecho = false "
                                    + "AND r.monto IS NOT NULL AND r.fechaHora <= :hasta", BigDecimal.class)
                    .setParameter("u", idUsuario)
                    .setParameter("hasta", hasta)
                    .getSingleResult();
            return suma == null ? BigDecimal.ZERO : suma;
        } finally {
            em.close();
        }
    }

    /** {total gastado en días de clase, cantidad de días distintos con ese gasto}. */
    public static Object[] gastosEnClases(int idUsuario, YearMonth mes) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Object[] fila = em.createQuery(
                            "SELECT SUM(m.monto), COUNT(DISTINCT m.fecha) FROM Movimiento m "
                                    + "WHERE m.usuario.id = :u AND m.tipo = :t AND m.clase IS NOT NULL "
                                    + "AND m.fecha BETWEEN :desde AND :hasta", Object[].class)
                    .setParameter("u", idUsuario)
                    .setParameter("t", TipoMovimiento.GASTO)
                    .setParameter("desde", mes.atDay(1))
                    .setParameter("hasta", mes.atEndOfMonth())
                    .getSingleResult();
            BigDecimal total = fila[0] == null ? BigDecimal.ZERO : (BigDecimal) fila[0];
            return new Object[]{total, (Long) fila[1]};
        } finally {
            em.close();
        }
    }

    /** Días de la semana (1 = lunes ... 7 = domingo) en los que el usuario tiene clase. */
    public static Set<Integer> diasConClase(int idUsuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return new HashSet<>(em.createQuery(
                            "SELECT DISTINCT c.dia FROM Clase c WHERE c.usuario.id = :u", Integer.class)
                    .setParameter("u", idUsuario)
                    .getResultList());
        } finally {
            em.close();
        }
    }

    /** Filas {nombre de categoría, total} de los gastos del mes. */
    public static List<Object[]> gastoPorCategoria(int idUsuario, YearMonth mes) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT m.categoria.nombre, SUM(m.monto) FROM Movimiento m "
                                    + "WHERE m.usuario.id = :u AND m.tipo = :t AND m.fecha BETWEEN :desde AND :hasta "
                                    + "GROUP BY m.categoria.nombre ORDER BY SUM(m.monto) DESC", Object[].class)
                    .setParameter("u", idUsuario)
                    .setParameter("t", TipoMovimiento.GASTO)
                    .setParameter("desde", mes.atDay(1))
                    .setParameter("hasta", mes.atEndOfMonth())
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Filas {materia, total} de los gastos ligados a una clase en el mes. */
    public static List<Object[]> gastoPorMateria(int idUsuario, YearMonth mes) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT m.clase.materia, SUM(m.monto) FROM Movimiento m "
                                    + "WHERE m.usuario.id = :u AND m.tipo = :t AND m.clase IS NOT NULL "
                                    + "AND m.fecha BETWEEN :desde AND :hasta "
                                    + "GROUP BY m.clase.materia ORDER BY SUM(m.monto) DESC", Object[].class)
                    .setParameter("u", idUsuario)
                    .setParameter("t", TipoMovimiento.GASTO)
                    .setParameter("desde", mes.atDay(1))
                    .setParameter("hasta", mes.atEndOfMonth())
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Los próximos pagos sin hacer, del más cercano al más lejano. */
    public static List<Recordatorio> proximosPagos(int idUsuario, int limite) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT r FROM Recordatorio r JOIN FETCH r.categoria WHERE r.usuario.id = :u "
                                    + "AND r.hecho = false AND r.monto IS NOT NULL ORDER BY r.fechaHora", Recordatorio.class)
                    .setParameter("u", idUsuario)
                    .setMaxResults(limite)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public static List<Clase> clasesDelUsuario(int idUsuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Clase c WHERE c.usuario.id = :u ORDER BY c.dia, c.horaInicio", Clase.class)
                    .setParameter("u", idUsuario)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
