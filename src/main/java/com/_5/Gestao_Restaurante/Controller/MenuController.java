package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.Prato;
import com._5.Gestao_Restaurante.model.Utilizador;
import com._5.Gestao_Restaurante.Repository.PratoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
public class MenuController {

    @Autowired
    private PratoRepository pratoRepository;

    @GetMapping("/menu")
    public String verMenu(Model model) {
        List<Prato> pratos = pratoRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "idPrato"));

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
        model.addAttribute("totalAtivos", ativos + stockBaixo);
        model.addAttribute("totalIndisponiveis", indisponiveis);
        model.addAttribute("totalCategorias", categorias);
        model.addAttribute("precoMedio", precoMedio);
        model.addAttribute("conteudo", "menu");
        return "layout";
    }

    // --- MÉTODOS DE EDIÇÃO DE PRATOS ---

    @GetMapping("/pratos/editar/{id}")
    public String editarPratoForm(@PathVariable("id") Integer id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");

        // Validação de segurança no GET do formulário de edição
        if (user == null) {
            redirectAttributes.addFlashAttribute("erroPermissao", "Tem de se registar para poder alterar estes dados.");
            return "redirect:/login";
        }
        if (!user.getFuncao().equals("Gerente") && !user.getFuncao().equals("Administrador")) {
            redirectAttributes.addFlashAttribute("erroPermissao", "Não tem permissão para alterar estes dados.");
            return "redirect:/menu";
        }

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
                                 @RequestParam("quantidade") Integer quantidade,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");

        // 1. Verificar se alguém está logado
        if (user == null) {
            redirectAttributes.addFlashAttribute("erroPermissao", "Tem de se registar para poder alterar estes dados.");
            return "redirect:/login";
        }

        // 2. Verificar se tem permissão (apenas Gerente ou Administrador podem alterar)
        if (!user.getFuncao().equals("Gerente") && !user.getFuncao().equals("Administrador")) {
            redirectAttributes.addFlashAttribute("erroPermissao", "Não tem permissão para alterar estes dados.");
            return "redirect:/menu";
        }

        // 3. Executar atualização se passar nas validações
        Prato prato = pratoRepository.findById(id).orElse(null);
        if (prato != null) {
            prato.setNome(nome);
            prato.setCategoria(categoria);
            prato.setPreco(BigDecimal.valueOf(preco));
            prato.setQuantidade(quantidade);

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