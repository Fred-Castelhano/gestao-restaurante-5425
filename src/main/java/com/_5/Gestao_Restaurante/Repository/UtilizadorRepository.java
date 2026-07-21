package com._5.Gestao_Restaurante.Repository;

import com._5.Gestao_Restaurante.model.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilizadorRepository extends JpaRepository<Utilizador, Integer> {

    // Método para procurar o utilizador pelo email
    Optional<Utilizador> findByEmail(String email);
}