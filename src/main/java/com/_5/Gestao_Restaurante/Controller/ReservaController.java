package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.Reserva;
import com._5.Gestao_Restaurante.model.Mesa;
import com._5.Gestao_Restaurante.model.Utilizador;
import com._5.Gestao_Restaurante.Repository.MesaRepository;
import com._5.Gestao_Restaurante.Repository.ReservaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ReservaController {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @GetMapping("/reservas")
    public String listarReservas(Model model, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");

        // Cozinheiros não têm acesso a reservas
        boolean temPermissao = user != null && (
                user.getFuncao().equalsIgnoreCase("Gerente") ||
                        user.getFuncao().equalsIgnoreCase("Administrador") ||
                        user.getFuncao().equalsIgnoreCase("Garçom")
        );

        if (!temPermissao) {
            model.addAttribute("erroPermissao", "Acesso Negado: Não tem permissões para aceder à Gestão de Reservas.");
        }

        // Se tiver permissão carrega os dados normais, senão deixa listas vazias/painel limpo
        carregarAtributosPainel(model, temPermissao);
        model.addAttribute("reserva", new Reserva());
        model.addAttribute("mesas", temPermissao ? mesaRepository.findAll() : new ArrayList<>());
        model.addAttribute("conteudo", "reservas");
        model.addAttribute("menuAtivo", "reservas");
        model.addAttribute("tituloPage", "Gestão de Reservas - Restaurante App");

        return "layout";
    }

    @GetMapping("/reservas/nova")
    public String novaReservaForm(Model model, @RequestParam(required = false) Integer idMesa, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/reservas";
        }

        Reserva reserva = new Reserva();
        if (idMesa != null) {
            mesaRepository.findById(idMesa).ifPresent(reserva::setMesa);
        }
        model.addAttribute("reserva", reserva);
        model.addAttribute("mesas", mesaRepository.findAll());
        model.addAttribute("conteudo", "nova-reserva");
        model.addAttribute("menuAtivo", "reservas");
        model.addAttribute("tituloPage", "Nova Reserva - Restaurante App");

        return "layout";
    }

    @GetMapping("/reservas/cancelar/{id}")
    public String cancelarReserva(@PathVariable Integer id, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/reservas";
        }

        Reserva reserva = reservaRepository.findById(id).orElse(null);

        if (reserva != null && reserva.getMesa() != null) {
            Mesa mesa = reserva.getMesa();
            mesa.setEstado("DISPONIVEL");
            mesaRepository.save(mesa);
        }
        reservaRepository.deleteById(id);
        return "redirect:/reservas";
    }

    @GetMapping("/reservas/chegada/{id}")
    public String confirmarChegada(@PathVariable Integer id, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/reservas";
        }

        Reserva reserva = reservaRepository.findById(id).orElse(null);

        if (reserva != null) {
            if (reserva.getMesa() != null) {
                Mesa mesa = reserva.getMesa();
                mesa.setEstado("OCUPADA");
                mesaRepository.save(mesa);
            }
            reservaRepository.deleteById(id);
        }

        return "redirect:/reservas";
    }

    @PostMapping("/reservas/salvar")
    public String guardarReserva(@ModelAttribute Reserva reserva, Model model, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/reservas";
        }

        LocalDate data = reserva.getDataReserva();
        LocalTime hora = reserva.getHoraReserva();
        LocalDate hoje = LocalDate.now();

        // 1. Validar e atribuir a mesa escolhida
        if (reserva.getMesa() != null && reserva.getMesa().getIdMesa() != null) {
            Mesa mesaEscolhida = mesaRepository.findById(reserva.getMesa().getIdMesa()).orElse(null);

            if (mesaEscolhida != null) {
                reserva.setMesa(mesaEscolhida);

                // Validação de Capacidade da Mesa
                if (reserva.getNumeroPessoas() > mesaEscolhida.getCapacidade()) {
                    model.addAttribute("erro", "A capacidade da mesa selecionada é insuficiente para o número de pessoas (" + mesaEscolhida.getCapacidade() + " lugares máx).");
                    carregarAtributosPainel(model, true);
                    model.addAttribute("reserva", reserva);
                    model.addAttribute("mesas", mesaRepository.findAll());
                    model.addAttribute("conteudo", "reservas");
                    return "layout";
                }
            }
        }
        // 2. Verificar se a hora está fora dos horários de funcionamento
        if (hora != null) {
            LocalTime inicioAlmoco = LocalTime.of(12, 0);
            LocalTime fimAlmoco = LocalTime.of(15, 30);
            LocalTime inicioJantar = LocalTime.of(19, 30);
            LocalTime fimJantar = LocalTime.of(23, 0);

            boolean noAlmoco = !hora.isBefore(inicioAlmoco) && !hora.isAfter(fimAlmoco);
            boolean noJantar = !hora.isBefore(inicioJantar) && !hora.isAfter(fimJantar);

            if (!noAlmoco && !noJantar) {
                model.addAttribute("erro", "Horário fora do funcionamento (Horários permitidos: 12:00 - 15:30 e 19:30 - 23:00)");
                carregarAtributosPainel(model, true);
                model.addAttribute("reserva", reserva);
                model.addAttribute("mesas", mesaRepository.findAll());
                model.addAttribute("conteudo", "reservas");
                return "layout";
            }
        }
        // 3. Validar se a data é no passado
        if (data != null && data.isBefore(hoje)) {
            model.addAttribute("erro", "Não é permitido fazer reservas para datas no passado.");
            carregarAtributosPainel(model, true);
            model.addAttribute("reserva", reserva);
            model.addAttribute("mesas", mesaRepository.findAll());
            model.addAttribute("conteudo", "reservas");
            return "layout";
        }

        // 4. Validar se a mesa já está ocupada no mesmo período de refeição
        if (reserva.getMesa() != null && reserva.getMesa().getIdMesa() != null && data != null && hora != null) {
            boolean mesmoAlmoco = ehAlmoco(hora);

            boolean mesaOcupadaNoTurno = reservaRepository.findAll().stream().anyMatch(r -> {
                if (r.getDataReserva() == null || !r.getDataReserva().equals(data)) return false;
                if (r.getMesa() == null || !r.getMesa().getIdMesa().equals(reserva.getMesa().getIdMesa())) return false;

                return ehAlmoco(r.getHoraReserva()) == mesmoAlmoco;
            });

            if (mesaOcupadaNoTurno) {
                String turnoNome = mesmoAlmoco ? "Almoço" : "Jantar";
                model.addAttribute("erro", "A mesa selecionada já se encontra reservada para o turno do " + turnoNome + " nesta data!");
                carregarAtributosPainel(model, true);
                model.addAttribute("mesas", mesaRepository.findAll());
                model.addAttribute("reserva", reserva);
                model.addAttribute("conteudo", "reservas");
                return "layout";
            }
        }

        // 5. Guardar a reserva e atualizar o estado da mesa para "Reservado"
        reservaRepository.save(reserva);

        if (reserva.getMesa() != null) {
            Mesa mesa = reserva.getMesa();
            mesa.setEstado("Reservado");
            mesaRepository.save(mesa);
        }

        return "redirect:/reservas";
    }

    private boolean ehAlmoco(LocalTime hora) {
        if (hora == null) return false;
        LocalTime inicioAlmoço = LocalTime.of(12, 0);
        LocalTime fimAlmoço = LocalTime.of(15, 30);
        return !hora.isBefore(inicioAlmoço) && !hora.isAfter(fimAlmoço);
    }

    private void carregarAtributosPainel(Model model, boolean temPermissao) {
        if (!temPermissao) {
            model.addAttribute("almocoHoje", new ArrayList<>());
            model.addAttribute("jantarHoje", new ArrayList<>());
            model.addAttribute("cronogramaFuturo", new ArrayList<>());
            return;
        }

        LocalDate hoje = LocalDate.now();
        LocalTime inicioAlmoço = LocalTime.of(12, 0);
        LocalTime fimAlmoço = LocalTime.of(15, 30);
        LocalTime inicioJantar = LocalTime.of(19, 30);
        LocalTime fimJantar = LocalTime.of(23, 0);

        List<Reserva> reservasHoje = reservaRepository.findAll().stream()
                .filter(r -> r.getDataReserva() != null && r.getDataReserva().equals(hoje))
                .collect(Collectors.toList());

        model.addAttribute("almocoHoje", reservasHoje.stream()
                .filter(r -> r.getHoraReserva() != null && !r.getHoraReserva().isBefore(inicioAlmoço) && !r.getHoraReserva().isAfter(fimAlmoço))
                .collect(Collectors.toList()));

        model.addAttribute("jantarHoje", reservasHoje.stream()
                .filter(r -> r.getHoraReserva() != null && !r.getHoraReserva().isBefore(inicioJantar) && !r.getHoraReserva().isAfter(fimJantar))
                .collect(Collectors.toList()));

        LocalDate limiteFuturo = hoje.plusWeeks(4);
        model.addAttribute("cronogramaFuturo", reservaRepository.findAll().stream()
                .filter(r -> r.getDataReserva() != null && r.getDataReserva().isAfter(hoje) && !r.getDataReserva().isAfter(limiteFuturo))
                .collect(Collectors.toList()));
    }
}