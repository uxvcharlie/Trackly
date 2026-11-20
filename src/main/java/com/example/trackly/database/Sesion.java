package com.example.trackly.database;

import com.example.trackly.model.Usuario;

public final class Sesion {

    private static Usuario usuario;

    private Sesion() {
    }

    public static void iniciar(Usuario u) {
        usuario = u;
    }

    public static Usuario getUsuario() {
        return usuario;
    }

    public static void cerrar() {
        usuario = null;
    }
}
