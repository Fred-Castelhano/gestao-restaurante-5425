package com._5.Gestao_Restaurante.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mesa")
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMesa;

    @Column(name = "numero", unique = true, nullable = false)
    private Integer numero; // Número identificador da mesa (ex: 1, 2, 3...)

    @Column(name = "capacidade", nullable = false)
    private Integer capacidade = 4; // Número de lugares (ex: 2, 4, 6, 8)

    @Column(name = "estado", nullable = false)
    private String estado = "Livre";

    // Construtores
    public Mesa() {}

    public Mesa(Integer numero, Integer capacidade, String estado) {
        this.numero = numero;
        this.capacidade = capacidade;
        this.estado = estado;

    }

    // Getters e Setters
        public Integer getIdMesa() {
            return idMesa;
        }
        public void setIdMesa(Integer idMesa) {
            this.idMesa = idMesa;
        }

        public Integer getNumero() {
            return numero;
        }
        public void setNumero(Integer numero) {
            this.numero = numero;
        }

        public Integer getCapacidade() {
            return capacidade;
        }
        public void setCapacidade(Integer capacidade) {
            this.capacidade = capacidade;
        }

        public String getEstado() {
            return estado;
        }
        public void setEstado(String estado) {
            this.estado = estado;
        }
    }