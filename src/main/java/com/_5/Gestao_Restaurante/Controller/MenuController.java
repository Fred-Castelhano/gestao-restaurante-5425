package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.Prato;
import com._5.Gestao_Restaurante.repository.PratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
public class MenuController {

    @Autowired
    private PratoRepository pratoRepository;

    @GetMapping("/menu")
    public String verMenu(Model model) {
        List<Prato> pratos = pratoRepository.findAll();

        // Calcular estatísticas para os cartões de topo
        long ativos = pratos.stream().filter(p -> "Disponível".equalsIgnoreCase(p.getEstado())).count();
        long indisponiveis = pratos.stream().filter(p -> "Indisponível".equalsIgnoreCase(p.getEstado())).count();
        long categorias = pratos.stream().map(Prato::getCategoria).distinct().count();

        BigDecimal precoMedio = BigDecimal.ZERO;
        if (!pratos.isEmpty()) {
            BigDecimal soma = pratos.stream().map(Prato::getPreco).reduce(BigDecimal.ZERO, BigDecimal::add);
            precoMedio = soma.divide(BigDecimal.valueOf(pratos.size()), 2, RoundingMode.HALF_UP);
        }

        model.addAttribute("pratos", pratos);
        model.addAttribute("totalAtivos", ativos);
        model.addAttribute("totalIndisponiveis", indisponiveis);
        model.addAttribute("totalCategorias", categorias);
        model.addAttribute("precoMedio", precoMedio);

        return "menu"; // Nome do ficheiro HTML
    }
}