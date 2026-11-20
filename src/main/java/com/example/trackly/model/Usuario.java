package com.example.trackly.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "hash_clave", nullable = false, length = 60)
    private String hashClave;

    @Column(name = "presupuesto_mes", nullable = false, precision = 10, scale = 2)
    private BigDecimal presupuestoMes = BigDecimal.ZERO;

    public Usuario() {
    }

    public Usuario(String nombre, String correo, String hashClave, BigDecimal presupuestoMes) {
        this.nombre = nombre;
        this.correo = correo;
        this.hashClave = hashClave;
        this.presupuestoMes = presupuestoMes;
    }

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getHashClave() { return hashClave; }
    public BigDecimal getPresupuestoMes() { return presupuestoMes; }
    public void setPresupuestoMes(BigDecimal presupuestoMes) { this.presupuestoMes = presupuestoMes; }
}
