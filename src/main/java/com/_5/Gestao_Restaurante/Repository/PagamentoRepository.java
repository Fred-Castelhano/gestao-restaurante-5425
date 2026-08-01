package com._5.Gestao_Restaurante.Repository;

import com._5.Gestao_Restaurante.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    // Método útil para ires buscar os pagamentos mais recentes para o dashboard
    List<Pagamento> findTop10ByOrderByDataPagamentoDesc();
}