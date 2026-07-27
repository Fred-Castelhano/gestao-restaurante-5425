package com._5.Gestao_Restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "pratos")
public class Prato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPrato;

    private String nome;
    private String categoria; // Ex: "Prato principal", "Entrada", "Sobremesa", "Bebida"
    private BigDecimal preco;
    private String estado = "Disponível"; // "Disponível" ou "Indisponível"
}