package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.Repository.MesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mesas")
public class MesaController {

    @Autowired
    private MesaRepository mesaRepository;

    @GetMapping
    public String listarMesas(Model model) {
        // Envia a lista de mesas da base de dados para o HTML
        model.addAttribute("mesas", mesaRepository.findAll());
        return "mesas";
    }
}