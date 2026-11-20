package com.example.trackly.dao;

import com.example.trackly.database.JPAUtil;
import com.example.trackly.model.Categoria;
import com.example.trackly.model.Clase;
import com.example.trackly.model.Movimiento;
import com.example.trackly.model.TipoMovimiento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class MovimientoDAO {

    public static List<Movimiento> listar(Integer idUsuario, YearMonth mes, Integer idCategoria) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT m FROM Movimiento m " +
                            "JOIN FETCH m.categoria " +
                            "LEFT JOIN FETCH m.clase " +
                            "WHERE m.usuario.id = :idUsuario");
            if (mes != null) {
                jpql.append(" AND m.fecha BETWEEN :inicio AND :fin");
            }
            if (idCategoria != null) {
                jpql.append(" AND m.categoria.id = :idCategoria");
            }
            jpql.append(" ORDER BY m.fecha DESC, m.id DESC");

            TypedQuery<Movimiento> q = em.createQuery(jpql.toString(), Movimiento.class);
            q.setParameter("idUsuario", idUsuario);
            if (mes != null) {
                q.setParameter("inicio", mes.atDay(1));
                q.setParameter("fin", mes.atEndOfMonth());
            }
            if (idCategoria != null) {
                q.setParameter("idCategoria", idCategoria);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public static void guardar(Movimiento movimiento) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(movimiento);
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

    public static int actualizar(Integer id, Integer idUsuario, Categoria categoria, Clase clase,
                                 TipoMovimiento tipo, BigDecimal monto, LocalDate fecha, String nota) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int filas = em.createQuery(
                            "UPDATE Movimiento m SET m.categoria = :categoria, m.clase = :clase, " +
                                    "m.tipo = :tipo, m.monto = :monto, m.fecha = :fecha, m.nota = :nota " +
                                    "WHERE m.id = :id AND m.usuario.id = :idUsuario")
                    .setParameter("categoria", categoria)
                    .setParameter("clase", clase)
                    .setParameter("tipo", tipo)
                    .setParameter("monto", monto)
                    .setParameter("fecha", fecha)
                    .setParameter("nota", nota)
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
                            "DELETE FROM Movimiento m WHERE m.id = :id AND m.usuario.id = :idUsuario")
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

    public static List<Categoria> listarCategorias() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM Categoria c ORDER BY c.nombre", Categoria.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public static List<Clase> listarClases(Integer idUsuario) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT c FROM Clase c WHERE c.usuario.id = :idUsuario " +
                                    "ORDER BY c.dia, c.horaInicio", Clase.class)
                    .setParameter("idUsuario", idUsuario)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}