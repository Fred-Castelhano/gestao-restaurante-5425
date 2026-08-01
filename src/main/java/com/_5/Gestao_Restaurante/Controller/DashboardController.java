package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.Repository.MesaRepository;
import com._5.Gestao_Restaurante.Repository.PedidoRepository;
import com._5.Gestao_Restaurante.Repository.ReservaRepository;
import com._5.Gestao_Restaurante.Repository.PagamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository; // 2. Injeta o repositório de pagamentos

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 1. Métricas
        long reservasHoje = reservaRepository.count();
        long mesasOcupadas = mesaRepository.countByEstado("OCUPADA");
        long pedidosAtivos = pedidoRepository.count();

        double vendasDia = pedidoRepository.findAll().stream()
                .mapToDouble(p -> 0.0)
                .sum();

        model.addAttribute("reservasHoje", reservasHoje);
        model.addAttribute("mesasOcupadas", mesasOcupadas);
        model.addAttribute("pedidosAtivos", pedidosAtivos);
        model.addAttribute("vendasDia", vendasDia);

        // 2. Dados para as tabelas e secções do Dashboard
        model.addAttribute("reservasRecentes", reservaRepository.findAll());
        model.addAttribute("pedidosEmPreparacao", pedidoRepository.findAll());

        // 3. Adicionar as listas reais para as novas secções
        model.addAttribute("listaMesas", mesaRepository.findAll());
        model.addAttribute("pagamentosRecentes", pagamentoRepository.findAll()); // Ou usa um método com limite se preferires

        model.addAttribute("conteudo", "dashboard");
        return "layout";
    }
}