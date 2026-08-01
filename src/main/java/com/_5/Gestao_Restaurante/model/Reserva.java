package com._5.Gestao_Restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idReserva;

    private String nomeCliente;
    private String contacto;
    private Integer numeroPessoas;
    private LocalDate dataReserva;
    private LocalTime horaReserva;
    private String estado;

    // Opcional: associação direta com a mesa (se a reserva for atribuída a uma mesa específica)
    @ManyToOne
    @JoinColumn(name = "id_mesa")
    private Mesa mesa;
}