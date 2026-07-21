package com._5.Gestao_Restaurante.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mesa")
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMesa;

    @Column(name = "estado", nullable = false)
    private String estado = "Livre";

    // Construtores
    public Mesa() {}

    // Getters e Setters
    public Integer getIdMesa() { return idMesa; }
    public void setIdMesa(Integer idMesa) { this.idMesa = idMesa; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}