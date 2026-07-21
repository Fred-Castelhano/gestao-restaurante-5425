package com._5.Gestao_Restaurante.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "utilizador")
@Data
@NoArgsConstructor
public class Utilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String funcao; // ex: "admin", "funcionario", "cliente"

    public Utilizador(int id, String nomeCompleto, String email, String password, String funcao) {
        this.id = id;
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.password = password;
        this.funcao = funcao;
    }
}
