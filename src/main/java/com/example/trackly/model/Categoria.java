package com.example.trackly.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 40)
    private String nombre;

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }

    @Override
    public String toString() { return nombre; }
}
