package com._5.Gestao_Restaurante.Repository;

import com._5.Gestao_Restaurante.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
}