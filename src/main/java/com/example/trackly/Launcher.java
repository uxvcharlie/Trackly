package com.example.trackly;

import javafx.application.Application;

/** Punto de entrada para correr desde un jar (JavaFX no arranca directo desde una clase que extiende Application). */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(TracklyApplication.class, args);
    }
}
