package com._5.Gestao_Restaurante.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "itens_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idItem;

    @ManyToOne
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;

    private String nomeProduto;
    private int quantidade;
    private String observacoes;
    private String estado = "PENDENTE";
}