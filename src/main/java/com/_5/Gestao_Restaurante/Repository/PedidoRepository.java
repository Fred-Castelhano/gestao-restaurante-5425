package com._5.Gestao_Restaurante.Repository;

import com._5.Gestao_Restaurante.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.itens WHERE p.estado = :estado")
    List<Pedido> findByEstadoComItens(@Param("estado") String estado);

    // Procura por ID da mesa em vez do objeto inteiro para evitar conflitos de mapeamento da coluna
    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.itens WHERE p.mesa.idMesa = :idMesa AND p.estado = :estado")
    List<Pedido> findByMesaIdAndEstadoComItens(@Param("idMesa") Integer idMesa, @Param("estado") String estado);
}