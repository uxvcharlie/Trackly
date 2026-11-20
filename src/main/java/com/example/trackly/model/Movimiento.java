package com.example.trackly.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movimientos")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_clase")            // sin nullable = false: la clase es opcional
    private Clase clase;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 7)
    private TipoMovimiento tipo;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "nota", length = 150)
    private String nota;

    public Movimiento() {
    }

    public Movimiento(Usuario usuario, Categoria categoria, Clase clase, TipoMovimiento tipo,
                      BigDecimal monto, LocalDate fecha, String nota) {
        this.usuario = usuario;
        this.categoria = categoria;
        this.clase = clase;
        this.tipo = tipo;
        this.monto = monto;
        this.fecha = fecha;
        this.nota = nota;
    }

    public Integer getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public Clase getClase() { return clase; }
    public TipoMovimiento getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public LocalDate getFecha() { return fecha; }
    public String getNota() { return nota; }
}
