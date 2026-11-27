package com.example.trackly.dao;

import com.example.trackly.database.JPAUtil;
import com.example.trackly.model.Clase;
import com.example.trackly.model.TipoMovimiento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ClaseDAO {

    /** Una fila del horario: la clase con lo gastado en ella durante el mes. */
    public record ClaseConGasto(Integer id, int dia, LocalTime horaInicio, LocalTime horaFin,
                                String materia, String aula, BigDecimal gastado) {
    }

    public static List<ClaseConGasto> listarConGasto(Integer idUsuario, YearMonth mes) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Object[]> filas = em.createQuery(
                            "SELECT c.id, c.dia, c.horaInicio, c.horaFin, c.materia, c.aula, " +
                                    "(SELECT SUM(m.monto) FROM Movimiento m " +
                                    " WHERE m.clase.id = c.id AND m.tipo = :gasto " +
                                    " AND m.fecha BETWEEN :inicio AND :fin) " +
                                    "FROM Clase c WHERE c.usuario.id = :idUsuario " +
                                    "ORDER BY c.dia, c.horaInicio", Object[].class)
                    .setParameter("gasto", TipoMovimiento.GASTO)
                    .setParameter("inicio", mes.atDay(1))
                    .setParameter("fin", mes.atEndOfMonth())
                    .setParameter("idUsuario", idUsuario)
                    .getResultList();

            List<ClaseConGasto> clases = new ArrayList<>();
            for (Object[] f : filas) {
                BigDecimal gastado = f[6] == null ? BigDecimal.ZERO : (BigDecimal) f[6];
                clases.add(new ClaseConGasto((Integer) f[0], (Integer) f[1], (LocalTime) f[2],
                        (LocalTime) f[3], (String) f[4], (String) f[5], gastado));
            }
            return clases;
        } finally {
            em.close();
        }
    }

    /** true si ya hay otra clase ese día que se cruza con el rango de horas. */
    public static boolean hayChoque(Integer idUsuario, int dia, LocalTime inicio, LocalTime fin,
                                    Integer idExcluir) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT COUNT(c) FROM Clase c WHERE c.usuario.id = :idUsuario " +
                    "AND c.dia = :dia AND c.horaInicio < :fin AND c.horaFin > :inicio" +
                    (idExcluir == null ? "" : " AND c.id <> :idExcluir");
            TypedQuery<Long> q = em.createQuery(jpql, Long.class)
                    .setParameter("idUsuario", idUsuario)
                    .setParameter("dia", dia)
                    .setParameter("inicio", inicio)
                    .setParameter("fin", fin);
            if (idExcluir != null) {
                q.setParameter("idExcluir", idExcluir);
            }
            return q.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }

    public static void guardar(Clase clase) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(clase);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static int actualizar(Integer id, Integer idUsuario, int dia, LocalTime horaInicio,
                                 LocalTime horaFin, String materia, String aula) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int filas = em.createQuery(
                            "UPDATE Clase c SET c.dia = :dia, c.horaInicio = :horaInicio, " +
                                    "c.horaFin = :horaFin, c.materia = :materia, c.aula = :aula " +
                                    "WHERE c.id = :id AND c.usuario.id = :idUsuario")
                    .setParameter("dia", dia)
                    .setParameter("horaInicio", horaInicio)
                    .setParameter("horaFin", horaFin)
                    .setParameter("materia", materia)
                    .setParameter("aula", aula)
                    .setParameter("id", id)
                    .setParameter("idUsuario", idUsuario)
                    .executeUpdate();
            tx.commit();
            return filas;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /** Los gastos ligados a la clase no se borran: quedan sin clase (ON DELETE SET NULL). */
    public static int borrar(Integer id, Integer idUsuario) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int filas = em.createQuery(
                            "DELETE FROM Clase c WHERE c.id = :id AND c.usuario.id = :idUsuario")
                    .setParameter("id", id)
                    .setParameter("idUsuario", idUsuario)
                    .executeUpdate();
            tx.commit();
            return filas;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
