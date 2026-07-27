package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.Reserva;
import com._5.Gestao_Restaurante.model.Mesa;
import com._5.Gestao_Restaurante.Repository.MesaRepository;
import com._5.Gestao_Restaurante.Repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ReservaController {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @GetMapping("/reservas")
    public String listarReservas(Model model) {
        carregarAtributosPainel(model);
        model.addAttribute("reserva", new Reserva());
        model.addAttribute("mesas", mesaRepository.findAll());
        return "reservas";
    }

    @GetMapping("/reservas/nova")
    public String novaReservaForm(Model model, @RequestParam(required = false) Integer idMesa) {
        Reserva reserva = new Reserva();
        if (idMesa != null) {
            mesaRepository.findById(idMesa).ifPresent(reserva::setMesa);
        }
        model.addAttribute("reserva", reserva);
        model.addAttribute("mesas", mesaRepository.findAll());
        return "nova-reserva";
    }

    @GetMapping("/reservas/cancelar/{id}")
    public String cancelarReserva(@PathVariable Integer id) {
        // 1. Procurar a reserva antes de a apagar para sabermos qual era a mesa
        Reserva reserva = reservaRepository.findById(id).orElse(null);

        if (reserva != null && reserva.getMesa() != null) {
            Mesa mesa = reserva.getMesa();

            // 2. Libertar a mesa (ajusta o método ao nome que usas, ex: setEstado("DISPONIVEL"))
            mesa.setEstado("DISPONIVEL");
            mesaRepository.save(mesa);
        }
        reservaRepository.deleteById(id);
        return "redirect:/reservas";
    }
    @GetMapping("/reservas/chegada/{id}")
    public String confirmarChegada(@PathVariable Integer id) {
        Reserva reserva = reservaRepository.findById(id).orElse(null);

        if (reserva != null) {
            if (reserva.getMesa() != null) {
                Mesa mesa = reserva.getMesa();
                mesa.setEstado("OCUPADA");
                mesaRepository.save(mesa);
            }
            // Apaga a reserva da lista ativa pois o cliente já chegou
            reservaRepository.deleteById(id);
        }

        return "redirect:/reservas";
    }

    @PostMapping("/reservas/salvar")
    public String guardarReserva(@ModelAttribute Reserva reserva, Model model) {
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
                    carregarAtributosPainel(model);
                    model.addAttribute("reserva", reserva);
                    model.addAttribute("mesas", mesaRepository.findAll());
                    return "reservas";
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
                carregarAtributosPainel(model);
                model.addAttribute("reserva", reserva);
                model.addAttribute("mesas", mesaRepository.findAll());
                return "reservas";
            }
        }
        // 3. Validar se a data é no passado
        if (data != null && data.isBefore(hoje)) {
            model.addAttribute("erro", "Não é permitido fazer reservas para datas no passado.");
            carregarAtributosPainel(model);
            model.addAttribute("reserva", reserva);
            model.addAttribute("mesas", mesaRepository.findAll());
            return "reservas";
        }

        // 4. Validar se a mesa já está ocupada no mesmo período de refeição (Almoço ou Jantar) na mesma data
        if (reserva.getMesa() != null && reserva.getMesa().getIdMesa() != null && data != null && hora != null) {
            boolean mesmoAlmoco = ehAlmoco(hora);

            boolean mesaOcupadaNoTurno = reservaRepository.findAll().stream().anyMatch(r -> {
                if (r.getDataReserva() == null || !r.getDataReserva().equals(data)) return false;
                if (r.getMesa() == null || !r.getMesa().getIdMesa().equals(reserva.getMesa().getIdMesa())) return false;

                // Compara se a reserva existente pertence ao mesmo bloco (Almoço vs Jantar)
                return ehAlmoco(r.getHoraReserva()) == mesmoAlmoco;
            });

            if (mesaOcupadaNoTurno) {
                String turnoNome = mesmoAlmoco ? "Almoço" : "Jantar";
                model.addAttribute("erro", "A mesa selecionada já se encontra reservada para o turno do " + turnoNome + " nesta data!");
                carregarAtributosPainel(model);
                model.addAttribute("mesas", mesaRepository.findAll());
                model.addAttribute("reserva", reserva);
                return "reservas";
            }
        }

        reservaRepository.save(reserva);
        return "redirect:/reservas";
    }

    // Método auxiliar para determinar se o horário pertence ao Almoço (12:00 - 15:30)
    private boolean ehAlmoco(LocalTime hora) {
        if (hora == null) return false;
        LocalTime inicioAlmoço = LocalTime.of(12, 0);
        LocalTime fimAlmoço = LocalTime.of(15, 30);
        return !hora.isBefore(inicioAlmoço) && !hora.isAfter(fimAlmoço);
    }

    private void carregarAtributosPainel(Model model) {
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