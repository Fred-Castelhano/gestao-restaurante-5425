package com._5.Gestao_Restaurante.repository;

import com._5.Gestao_Restaurante.model.Prato;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PratoRepository extends JpaRepository<Prato, Integer> {
    List<Prato> findByEstado(String estado);
}