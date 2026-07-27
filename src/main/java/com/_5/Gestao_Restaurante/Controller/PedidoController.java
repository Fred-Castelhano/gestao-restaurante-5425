package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.ItemPedido;
import com._5.Gestao_Restaurante.model.Mesa;
import com._5.Gestao_Restaurante.model.Pedido;
import com._5.Gestao_Restaurante.Repository.MesaRepository;
import com._5.Gestao_Restaurante.Repository.PedidoRepository;
import com._5.Gestao_Restaurante.Repository.ItemPedidoRepository;
import com._5.Gestao_Restaurante.model.Prato;
import com._5.Gestao_Restaurante.repository.PratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
public class PedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private ItemPedidoRepository itemPedidoRepository;

    // 1. Mostrar painel da cozinha (KDS) com pedidos agrupados por mesa e itens carregados
    @GetMapping("/pedidos/cozinha")
    public String verCozinha(Model model) {
        List<Pedido> listaEspera = agruparPedidosPorMesa(pedidoRepository.findByEstadoComItens("Em espera"));
        List<Pedido> listaPreparacao = agruparPedidosPorMesa(pedidoRepository.findByEstadoComItens("Em preparação"));
        List<Pedido> listaPronto = agruparPedidosPorMesa(pedidoRepository.findByEstadoComItens("Pronto"));

        model.addAttribute("emEspera", listaEspera);
        model.addAttribute("emPreparacao", listaPreparacao);
        model.addAttribute("pronto", listaPronto);

        model.addAttribute("countEspera", listaEspera.size());
        model.addAttribute("countPreparacao", listaPreparacao.size());
        model.addAttribute("countPronto", listaPronto.size());

        return "cozinha-pedidos";
    }

    // Método auxiliar para juntar múltiplos pedidos da mesma mesa num único cartão
    private List<Pedido> agruparPedidosPorMesa(List<Pedido> pedidosOriginais) {
        Map<Integer, Pedido> mapaMesas = new java.util.LinkedHashMap<>();

        for (Pedido p : pedidosOriginais) {
            Integer idMesa = (p.getMesa() != null) ? p.getMesa().getIdMesa() : -1;

            if (!mapaMesas.containsKey(idMesa)) {
                // Garante que a lista de itens não é nula
                if (p.getItens() == null) {
                    p.setItens(new java.util.ArrayList<>());
                }
                mapaMesas.put(idMesa, p);
            } else {
                Pedido pedidoPrincipal = mapaMesas.get(idMesa);
                if (p.getItens() != null) {
                    for (ItemPedido item : p.getItens()) {
                        item.setPedido(pedidoPrincipal); // Atualiza a referência para o pedido principal
                        if (!pedidoPrincipal.getItens().contains(item)) {
                            pedidoPrincipal.getItens().add(item);
                        }
                    }
                }
            }
        }
        return new java.util.ArrayList<>(mapaMesas.values());
    }

    // 2. Formulário para criar pedido para uma mesa específica
    @GetMapping("/pedidos/novo")
    public String novoPedidoForm(@RequestParam("idMesa") Integer idMesa, Model model) {
        Mesa mesa = mesaRepository.findById(idMesa).orElse(null);
        model.addAttribute("mesa", mesa);
        model.addAttribute("pratos", pratoRepository.findByEstado("Disponível"));
        return "novo-pedido";
    }

    // 3. Guardar o pedido acumulando na mesma mesa
    @PostMapping("/pedidos/guardar")
    public String guardarPedido(@RequestParam("idMesa") Integer idMesa,
                                @RequestParam Map<String, String> allParams) {
        Mesa mesa = mesaRepository.findById(idMesa).orElse(null);
        if (mesa != null) {
            // Cria sempre um novo pedido para esta ronda/submissão
            Pedido pedido = new Pedido();
            pedido.setMesa(mesa);
            pedido.setDataHora(LocalDateTime.now());
            pedido.setEstado("Em espera");
            pedidoRepository.save(pedido); // Guarda o pedido para gerar o ID

            // Percorre os parâmetros do formulário à procura das quantidades
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("quantidades[")) {
                    try {
                        String idStr = key.substring(key.indexOf('[') + 1, key.indexOf(']'));
                        Integer idPrato = Integer.parseInt(idStr);
                        int quantidade = Integer.parseInt(value);

                        if (quantidade > 0) {
                            Prato prato = pratoRepository.findById(idPrato).orElse(null);
                            if (prato != null) {
                                ItemPedido item = new ItemPedido();
                                item.setPedido(pedido);
                                item.setNomeProduto(prato.getNome());
                                item.setQuantidade(quantidade);
                                item.setEstado("PENDENTE");

                                // Guarda cada item associado ao pedido criado
                                itemPedidoRepository.save(item);
                            }
                        }
                    } catch (Exception e) {
                        // Ignora erros de parsing individuais
                    }
                }
            }
        }
        return "redirect:/pedidos/cozinha";
    }
}