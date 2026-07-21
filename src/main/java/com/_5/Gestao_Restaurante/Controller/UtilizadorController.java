package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.Utilizador;
import com._5.Gestao_Restaurante.Repository.UtilizadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UtilizadorController {

    @Autowired
    private UtilizadorRepository utilizadorRepository;

    // Lista os utilizadores na tabela
    @GetMapping("/utilizadores")
    public String listarUtilizadores(Model model) {
        model.addAttribute("utilizadores", utilizadorRepository.findAll());
        return "utilizadores";
    }

    // Mostra o formulário isolado de registo de funcionários
    @GetMapping("/registar-utilizador")
    public String mostrarFormularioRegisto(Model model) {
        model.addAttribute("utilizador", new Utilizador());
        return "registar-utilizador";
    }

    // Guarda o novo funcionário e redireciona para a tabela
    @PostMapping("/utilizadores")
    public String registarUtilizador(@ModelAttribute Utilizador utilizador) {
        utilizadorRepository.save(utilizador);
        return "redirect:/utilizadores";
    }
}