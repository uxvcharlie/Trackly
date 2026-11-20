package com.example.trackly.dao;

import com.example.trackly.database.JPAUtil;
import com.example.trackly.model.Categoria;
import com.example.trackly.model.Movimiento;
import com.example.trackly.model.Recordatorio;
import com.example.trackly.model.TipoMovimiento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RecordatorioDAO {

    public static List<Recordatorio> listar(Integer idUsuario, boolean incluirHechos) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT r FROM Recordatorio r " +
                    "LEFT JOIN FETCH r.categoria " +
                    "WHERE r.usuario.id = :idUsuario" +
                    (incluirHechos ? "" : " AND r.hecho = false") +
                    " ORDER BY r.hecho, r.fechaHora";
            return em.createQuery(jpql, Recordatorio.class)
                    .setParameter("idUsuario", idUsuario)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public static void guardar(Recordatorio recordatorio) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(recordatorio);
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

    public static int actualizar(Integer id, Integer idUsuario, String titulo, LocalDateTime fechaHora,
                                 BigDecimal monto, Categoria categoria, boolean mensual) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int filas = em.createQuery(
                            "UPDATE Recordatorio r SET r.titulo = :titulo, r.fechaHora = :fechaHora, " +
                                    "r.monto = :monto, r.categoria = :categoria, r.mensual = :mensual " +
                                    "WHERE r.id = :id AND r.usuario.id = :idUsuario AND r.hecho = false")
                    .setParameter("titulo", titulo)
                    .setParameter("fechaHora", fechaHora)
                    .setParameter("monto", monto)
                    .setParameter("categoria", categoria)
                    .setParameter("mensual", mensual)
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

    public static int borrar(Integer id, Integer idUsuario) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int filas = em.createQuery(
                            "DELETE FROM Recordatorio r WHERE r.id = :id AND r.usuario.id = :idUsuario")
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

    public static int marcarHecho(Integer id, Integer idUsuario) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int filas = em.createQuery(
                            "UPDATE Recordatorio r SET r.hecho = true " +
                                    "WHERE r.id = :id AND r.usuario.id = :idUsuario AND r.monto IS NULL")
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

    public static void pagar(Integer id, Integer idUsuario) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();

            Recordatorio r = em.createQuery(
                            "SELECT r FROM Recordatorio r WHERE r.id = :id AND r.usuario.id = :idUsuario",
                            Recordatorio.class)
                    .setParameter("id", id)
                    .setParameter("idUsuario", idUsuario)
                    .getSingleResult();

            if (!r.esPago()) {
                throw new IllegalStateException("Ese recordatorio no es un pago.");
            }
            if (r.isHecho()) {
                throw new IllegalStateException("Ese pago ya se hizo.");
            }

            Movimiento gasto = new Movimiento(r.getUsuario(), r.getCategoria(), null,
                    TipoMovimiento.GASTO, r.getMonto(), LocalDate.now(), r.getTitulo());
            em.persist(gasto);

            r.setHecho(true);
            r.setMovimiento(gasto);

            if (r.isMensual()) {
                em.persist(r.copiaDelSiguienteMes());
            }

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
}