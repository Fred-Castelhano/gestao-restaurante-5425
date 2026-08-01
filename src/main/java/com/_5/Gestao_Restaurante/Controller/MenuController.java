package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.Prato;
import com._5.Gestao_Restaurante.Repository.PratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
public class MenuController {

    @Autowired
    private PratoRepository pratoRepository;

    @GetMapping("/menu")
    public String verMenu(Model model) {
        // Ordenar por ID para manter a tabela sempre na mesma ordem original
        List<Prato> pratos = pratoRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "idPrato"));

        // Corrigir a contagem exata com base na quantidade real apresentada na tabela
        long ativos = pratos.stream().filter(p -> p.getQuantidade() != null && p.getQuantidade() > 10).count();
        long stockBaixo = pratos.stream().filter(p -> p.getQuantidade() != null && p.getQuantidade() > 0 && p.getQuantidade() <= 10).count();
        long indisponiveis = pratos.stream().filter(p -> p.getQuantidade() == null || p.getQuantidade() == 0).count();

        long categorias = pratos.stream().map(Prato::getCategoria).distinct().count();

        BigDecimal precoMedio = BigDecimal.ZERO;
        if (!pratos.isEmpty()) {
            BigDecimal soma = pratos.stream().map(Prato::getPreco).reduce(BigDecimal.ZERO, BigDecimal::add);
            precoMedio = soma.divide(BigDecimal.valueOf(pratos.size()), 2, RoundingMode.HALF_UP);
        }

        model.addAttribute("pratos", pratos);
        model.addAttribute("totalAtivos", ativos + stockBaixo); // Considera ativos tudo o que tem stock > 0
        model.addAttribute("totalIndisponiveis", indisponiveis); // Só conta 0 ou nulo
        model.addAttribute("totalCategorias", categorias);
        model.addAttribute("precoMedio", precoMedio);
        model.addAttribute("conteudo", "menu"); // Nome do ficheiro HTML do menu (ex: menu.html)
        return "layout";
    }

    // --- MÉTODOS DE EDIÇÃO DE PRATOS ---

    @GetMapping("/pratos/editar/{id}")
    public String editarPratoForm(@PathVariable("id") Integer id, Model model) {
        Prato prato = pratoRepository.findById(id).orElse(null);
        if (prato == null) {
            return "redirect:/menu";
        }
        model.addAttribute("prato", prato);
        return "editar-prato";
    }

    @PostMapping("/pratos/atualizar/{id}")
    public String atualizarPrato(@PathVariable("id") Integer id,
                                 @RequestParam("nome") String nome,
                                 @RequestParam("categoria") String categoria,
                                 @RequestParam("preco") Double preco,
                                 @RequestParam("quantidade") Integer quantidade) {

        Prato prato = pratoRepository.findById(id).orElse(null);
        if (prato != null) {
            prato.setNome(nome);
            prato.setCategoria(categoria);
            prato.setPreco(BigDecimal.valueOf(preco));
            prato.setQuantidade(quantidade);

            // Atualizar o estado em texto consoante a quantidade introduzida
            if (quantidade > 10) {
                prato.setEstado("Disponível");
            } else if (quantidade > 0) {
                prato.setEstado("Stock Baixo");
            } else {
                prato.setEstado("Indisponível");
            }

            pratoRepository.save(prato);
        }
        return "redirect:/menu";
    }
}