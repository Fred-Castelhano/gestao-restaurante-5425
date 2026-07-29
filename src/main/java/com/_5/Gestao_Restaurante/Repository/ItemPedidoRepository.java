package com._5.Gestao_Restaurante.Repository;

import com._5.Gestao_Restaurante.model.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Integer> {

    List<ItemPedido> findByEstado(String estado);

}