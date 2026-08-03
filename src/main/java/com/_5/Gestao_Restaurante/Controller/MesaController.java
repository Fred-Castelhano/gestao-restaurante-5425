package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.Repository.MesaRepository;
import com._5.Gestao_Restaurante.Repository.ReservaRepository;
import com._5.Gestao_Restaurante.model.Mesa;
import com._5.Gestao_Restaurante.model.Reserva;
import com._5.Gestao_Restaurante.model.Utilizador;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/mesas")
public class MesaController {

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @GetMapping
    public String listarMesas(Model model) {
        List<Mesa> mesas = mesaRepository.findAll().stream()
                .sorted(Comparator.comparing(Mesa::getNumero))
                .toList();

        LocalDate hoje = LocalDate.now();

        // Filtra todas as reservas agendadas para o dia de hoje
        List<Reserva> reservasHoje = reservaRepository.findAll().stream()
                .filter(r -> r.getDataReserva() != null && r.getDataReserva().equals(hoje))
                .toList();

        // Mapeia diretamente todas as mesas que possuem reserva ativa para hoje
        Map<Integer, Reserva> reservasPorMesa = new HashMap<>();
        for (Reserva r : reservasHoje) {
            if (r.getMesa() != null && r.getMesa().getIdMesa() != null) {
                reservasPorMesa.put(r.getMesa().getIdMesa(), r);
            }
        }

        model.addAttribute("mesas", mesas);
        model.addAttribute("reservasPorMesa", reservasPorMesa);
        model.addAttribute("conteudo", "mesas");
        return "layout";
    }

    // --- INTERCECÇÃO DE SEGURANÇA PARA AÇÃO NAS MESAS (EX: CRIAR PEDIDO) ---

    @GetMapping("/pedido/novo")
    public String tentarCriarPedido(@RequestParam("idMesa") Integer idMesa, HttpSession session, RedirectAttributes redirectAttributes) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");

        if (user == null) {
            redirectAttributes.addFlashAttribute("erroPermissao", "Tem de se registar para poder alterar estes dados.");
            return "redirect:/login";
        }

        if (user.getFuncao().equals("Cozinheiro")) {
            redirectAttributes.addFlashAttribute("erroPermissao", "Não tem permissão para alterar estes dados.");
            return "redirect:/mesas";
        }

        return "redirect:/pedidos/novo?idMesa=" + idMesa;
    }

    // --- MÉTODOS DE ESCRITA / ALTERAÇÃO PROTEGIDOS ---

    @PostMapping("/salvar")
    public String salvarMesa(@ModelAttribute Mesa mesa, HttpSession session, RedirectAttributes redirectAttributes) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");

        if (user == null) {
            redirectAttributes.addFlashAttribute("erroPermissao", "Tem de se registar para poder alterar estes dados.");
            return "redirect:/login";
        }

        if (user.getFuncao().equals("Cozinheiro")) {
            redirectAttributes.addFlashAttribute("erroPermissao", "Não tem permissão para alterar estes dados.");
            return "redirect:/mesas";
        }

        mesaRepository.save(mesa);
        return "redirect:/mesas";
    }
}