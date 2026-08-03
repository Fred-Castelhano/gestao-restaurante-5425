package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.Utilizador;
import com._5.Gestao_Restaurante.Repository.UtilizadorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UtilizadorController {

    @Autowired
    private UtilizadorRepository utilizadorRepository;

    // 1. Página de Login (GET)
    @GetMapping("/login")
    public String paginaLogin(Model model) {
        model.addAttribute("utilizador", new Utilizador());
        return "login"; // Vai procurar por login.html
    }

    // 1.1. Processar o Login (POST) - Para funcionários já registados entrarem
    @PostMapping("/login")
    public String processarLogin(@RequestParam("email") String email,
                                 @RequestParam("password") String password,
                                 HttpSession session,
                                 Model model) {

        Utilizador utilizador = utilizadorRepository.findByEmail(email);

        if (utilizador != null && utilizador.getPassword().equals(password)) {
            // Guarda o funcionário logado na sessão
            session.setAttribute("utilizadorLogado", utilizador);
            return "redirect:/dashboard";
        }

        model.addAttribute("erro", "Email ou palavra-passe incorretos");
        return "login";
    }

    // Rota de Log Off / Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Limpa a sessão
        return "redirect:/login";
    }

    // 2. Lista os utilizadores na tabela
    @GetMapping("/utilizadores")
    public String listarUtilizadores(Model model) {
        model.addAttribute("utilizadores", utilizadorRepository.findAll());
        model.addAttribute("conteudo", "utilizadores");
        model.addAttribute("menuAtivo", "utilizadores");
        return "layout";
    }

    // 3. Mostra o formulário de registo
    @GetMapping("/registar-utilizador")
    public String mostrarFormularioRegisto(Model model) {
        model.addAttribute("utilizador", new Utilizador());
        model.addAttribute("conteudo", "registar-utilizador");
        model.addAttribute("menuAtivo", "utilizadores");
        return "layout";
    }

    // 4. Guarda o novo funcionário MAS NÃO faz login automático (volta à lista de utilizadores)
    @PostMapping("/utilizadores")
    public String registarUtilizador(@ModelAttribute Utilizador utilizador) {
        utilizadorRepository.save(utilizador);
        return "redirect:/utilizadores"; // Redireciona para a tabela de gestão de funcionários
    }
}