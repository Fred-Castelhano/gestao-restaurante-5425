package com._5.Gestao_Restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPedido;

    @ManyToOne
    @JoinColumn(name = "id_mesa")
    private Mesa mesa;

    private LocalDateTime dataHora;
    private String estado;

    // EAGER garante que os itens vêm sempre preenchidos e o `= new ArrayList<>()` evita valores nulos
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ItemPedido> itens = new ArrayList<>();
}