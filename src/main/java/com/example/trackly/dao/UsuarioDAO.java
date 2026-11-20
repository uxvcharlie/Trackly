package com.example.trackly.dao;

import com.example.trackly.database.JPAUtil;
import com.example.trackly.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.util.List;

public class UsuarioDAO {

    public static Usuario registrar(String nombre, String correo, String clave, BigDecimal presupuesto) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            String hash = BCrypt.hashpw(clave, BCrypt.gensalt());   // la clave nunca se guarda tal cual
            Usuario u = new Usuario(nombre, correo.trim().toLowerCase(), hash, presupuesto);
            em.persist(u);
            tx.commit();
            return u;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public static Usuario login(String correo, String clave) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Usuario> encontrados = em.createQuery(
                            "SELECT u FROM Usuario u WHERE u.correo = :correo", Usuario.class)
                    .setParameter("correo", correo.trim().toLowerCase())
                    .getResultList();
            if (encontrados.isEmpty()) {
                return null;
            }
            Usuario u = encontrados.get(0);
            return BCrypt.checkpw(clave, u.getHashClave()) ? u : null;
        } finally {
            em.close();
        }
    }
}
