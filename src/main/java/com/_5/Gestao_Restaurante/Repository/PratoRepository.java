package com._5.Gestao_Restaurante.Repository;

import com._5.Gestao_Restaurante.model.Prato;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PratoRepository extends JpaRepository<Prato, Integer> {
    List<Prato> findByEstado(String estado);

    Prato findByNome(String nome);
}