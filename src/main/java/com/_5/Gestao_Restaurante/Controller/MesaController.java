package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.Repository.MesaRepository;
import com._5.Gestao_Restaurante.Repository.ReservaRepository;
import com._5.Gestao_Restaurante.model.Mesa;
import com._5.Gestao_Restaurante.model.Reserva;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Comparator;

import java.time.LocalDate;
import java.time.LocalTime;
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
        LocalTime horaAtual = LocalTime.now(); // Puxa a hora exata atual

        // Definir os intervalos dos períodos (ajusta se os teus horários forem diferentes)
        // Almoço: 12:00 até 15:30
        // Jantar: 19:30 até 23:00 (ou o resto do dia)
        boolean ePeriodoAlmoço = horaAtual.isAfter(LocalTime.of(11, 0)) && horaAtual.isBefore(LocalTime.of(16, 0));

        // Buscar todas as reservas de hoje
        List<Reserva> reservasHoje = reservaRepository.findAll().stream()
                .filter(r -> r.getDataReserva() != null &&
                        r.getDataReserva().getYear() == hoje.getYear() &&
                        r.getDataReserva().getMonth() == hoje.getMonth() &&
                        r.getDataReserva().getDayOfMonth() == hoje.getDayOfMonth())
                .toList();

        // Filtrar as reservas apenas para o período de refeição atual
        Map<Integer, Reserva> reservasPorMesa = new HashMap<>();
        for (Reserva r : reservasHoje) {
            if (r.getMesa() != null && r.getHoraReserva() != null) {
                LocalTime horaReserva = r.getHoraReserva(); // Assume que horaReserva é do tipo LocalTime (ou ajusta se for String)

                boolean ehAlmoçoReserva = horaReserva.isAfter(LocalTime.of(11, 0)) && horaReserva.isBefore(LocalTime.of(16, 0));

                // Se estamos no almoço e a reserva é de almoço, ou se estamos no jantar e a reserva é de jantar:
                if ((ePeriodoAlmoço && ehAlmoçoReserva) || (!ePeriodoAlmoço && !ehAlmoçoReserva)) {
                    reservasPorMesa.put(r.getMesa().getIdMesa(), r);
                }
            }
        }

        model.addAttribute("mesas", mesas);
        model.addAttribute("reservasPorMesa", reservasPorMesa);
        model.addAttribute("conteudo", "mesas");
        return "layout";
    }
}