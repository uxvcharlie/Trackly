package com.example.trackly.model;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "clases")
public class Clase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "dia", nullable = false)
    private Integer dia; // 1 = lunes ... 7 = domingo, igual que DayOfWeek.getValue()

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "materia", nullable = false, length = 80)
    private String materia;

    @Column(name = "aula", length = 30)
    private String aula;

    public Clase() {
    }

    public Clase(Usuario usuario, int dia, LocalTime horaInicio, LocalTime horaFin, String materia, String aula) {
        this.usuario = usuario;
        this.dia = dia;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.materia = materia;
        this.aula = aula;
    }

    public Integer getId() { return id; }
    public Integer getDia() { return dia; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public String getMateria() { return materia; }

    @Override
    public String toString() { return materia; }
}
