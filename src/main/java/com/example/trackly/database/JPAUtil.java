package com.example.trackly.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public class JPAUtil {

    private static EntityManagerFactory emf;

    public static synchronized void iniciar() {
        if (emf == null) {
            Map<String, Object> propiedades = new HashMap<>();
            String password = System.getenv("DB_PASSWORD");
            propiedades.put("jakarta.persistence.jdbc.password", password == null ? "" : password);
            emf = Persistence.createEntityManagerFactory("tracklyPU", propiedades);
        }
    }

    public static EntityManager getEntityManager() {
        iniciar();
        return emf.createEntityManager();
    }

    public static synchronized void cerrar() {
        if (emf != null && emf.isOpen()) {
            emf.close();
            emf = null;
        }
    }
}
