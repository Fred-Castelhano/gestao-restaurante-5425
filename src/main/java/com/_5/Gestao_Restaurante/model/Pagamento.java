package com._5.Gestao_Restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPagamento;

    private Double valor;
    private String metodo; // Ex: Multibanco, Dinheiro, MB Way
    private LocalDateTime dataPagamento = LocalDateTime.now();

    // Opcional: associação ao pedido correspondente
    @ManyToOne
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;
}