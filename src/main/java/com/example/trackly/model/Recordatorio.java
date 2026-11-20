package com.example.trackly.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recordatorios")
public class Recordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "hecho", nullable = false)
    private boolean hecho;

    // Si monto y categoria tienen valor, el recordatorio es un pago
    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @Column(name = "mensual", nullable = false)
    private boolean mensual;

    // El gasto que se creó al pagarlo (uno a uno)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_movimiento", unique = true)
    private Movimiento movimiento;

    public Recordatorio() {
    }

    public Recordatorio(Usuario usuario, String titulo, LocalDateTime fechaHora,
                        BigDecimal monto, Categoria categoria, boolean mensual) {
        this.usuario = usuario;
        this.titulo = titulo;
        this.fechaHora = fechaHora;
        this.monto = monto;
        this.categoria = categoria;
        this.mensual = mensual;
    }

    public boolean esPago() {
        return monto != null;
    }

    public Recordatorio copiaDelSiguienteMes() {
        return new Recordatorio(usuario, titulo, fechaHora.plusMonths(1), monto, categoria, true);
    }

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTitulo() { return titulo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public boolean isHecho() { return hecho; }
    public void setHecho(boolean hecho) { this.hecho = hecho; }
    public BigDecimal getMonto() { return monto; }
    public Categoria getCategoria() { return categoria; }
    public boolean isMensual() { return mensual; }
    public Movimiento getMovimiento() { return movimiento; }
    public void setMovimiento(Movimiento movimiento) { this.movimiento = movimiento; }
}
