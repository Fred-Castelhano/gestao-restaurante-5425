package com._5.Gestao_Restaurante.Repository;

import com._5.Gestao_Restaurante.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Integer> {
    long countByEstado(String estado);
}