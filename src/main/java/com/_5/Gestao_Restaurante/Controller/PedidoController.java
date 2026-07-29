package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.ItemPedido;
import com._5.Gestao_Restaurante.model.Mesa;
import com._5.Gestao_Restaurante.model.Pedido;
import com._5.Gestao_Restaurante.model.Prato;
import com._5.Gestao_Restaurante.Repository.MesaRepository;
import com._5.Gestao_Restaurante.Repository.PedidoRepository;
import com._5.Gestao_Restaurante.Repository.ItemPedidoRepository;
import com._5.Gestao_Restaurante.Repository.PratoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    // 1. Mostrar painel da cozinha (KDS) listando cada item individualmente em cartões próprios
    @GetMapping("/pedidos/cozinha")
    public String verCozinha(Model model) {
        // Vai buscar diretamente os itens individuais com base no estado deles
        List<ItemPedido> listaEspera = itemPedidoRepository.findByEstado("PENDENTE");
        List<ItemPedido> listaPreparacao = itemPedidoRepository.findByEstado("PREPARADO");
        List<ItemPedido> listaPronto = itemPedidoRepository.findByEstado("PRONTO");

        model.addAttribute("emEspera", listaEspera);
        model.addAttribute("emPreparacao", listaPreparacao);
        model.addAttribute("pronto", listaPronto);

        model.addAttribute("countEspera", listaEspera.size());
        model.addAttribute("countPreparacao", listaPreparacao.size());
        model.addAttribute("countPronto", listaPronto.size());

        return "cozinha-pedidos";
    }

    // 2. Formulário para criar pedido para uma mesa específica
    @GetMapping("/pedidos/novo")
    public String novoPedidoForm(@RequestParam("idMesa") Integer idMesa, Model model) {
        Mesa mesa = mesaRepository.findById(idMesa).orElse(null);
        model.addAttribute("mesa", mesa);
        model.addAttribute("pratos", pratoRepository.findByEstado("Disponível"));

        double totalAcumulado = 0.0;

        // Procura todos os pedidos ativos da mesa e soma apenas os itens que NÃO estão concluídos
        List<Pedido> todosPedidos = pedidoRepository.findAll();

        for (Pedido p : todosPedidos) {
            if (p.getMesa() != null && p.getMesa().getIdMesa().equals(idMesa)) {
                if (p.getItens() != null) {
                    for (ItemPedido item : p.getItens()) {
                        String estadoItem = item.getEstado();
                        // Só soma se o item individual ainda não estiver concluído
                        if (estadoItem == null || !estadoItem.trim().equalsIgnoreCase("CONCLUIDO")) {
                            String nomeProd = item.getNomeProduto();
                            if (nomeProd != null) {
                                Prato prato = pratoRepository.findByNome(nomeProd);
                                if (prato == null) {
                                    for (Prato pBD : pratoRepository.findAll()) {
                                        if (pBD.getNome() != null && pBD.getNome().trim().equalsIgnoreCase(nomeProd.trim())) {
                                            prato = pBD;
                                            break;
                                        }
                                    }
                                }

                                if (prato != null && prato.getPreco() != null) {
                                    double precoPrato = prato.getPreco().doubleValue();
                                    int qtd = item.getQuantidade();
                                    totalAcumulado += (precoPrato * qtd);
                                }
                            }
                        }
                    }
                }
            }
        }

        model.addAttribute("totalAcumulado", totalAcumulado);
        return "novo-pedido";
    }

    // 3. Guardar o pedido e os itens individualmente com segurança total na BD
    @PostMapping("/pedidos/guardar")
    public String guardarPedido(@RequestParam("idMesa") Integer idMesa,
                                @RequestParam Map<String, String> allParams) {

        System.out.println("--> A tentar guardar pedido para a mesa ID: " + idMesa);

        Mesa mesa = mesaRepository.findById(idMesa).orElse(null);
        if (mesa != null) {
            Pedido pedido = new Pedido();
            pedido.setMesa(mesa);
            pedido.setDataHora(LocalDateTime.now());
            pedido.setEstado("Em espera");
            pedido = pedidoRepository.save(pedido);
            System.out.println("--> Pedido principal guardado com ID: " + pedido.getIdPedido());

            List<ItemPedido> novosItens = new ArrayList<>();

            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("quantidades[") && key.endsWith("]")) {
                    String idStr = key.substring(key.indexOf('[') + 1, key.indexOf(']'));

                    if (idStr.matches("\\d+") && value != null && !value.trim().isEmpty()) {
                        int quantidadeTotal = Integer.parseInt(value);

                        if (quantidadeTotal > 0) {
                            Integer idPrato = Integer.parseInt(idStr);
                            Prato prato = pratoRepository.findById(idPrato).orElse(null);

                            if (prato != null) {
                                // Cria um registo individual (quantidade = 1) para cada unidade pedida
                                for (int i = 0; i < quantidadeTotal; i++) {
                                    ItemPedido item = new ItemPedido();
                                    item.setPedido(pedido);
                                    item.setNomeProduto(prato.getNome());
                                    item.setQuantidade(1);
                                    item.setEstado("PENDENTE");

                                    ItemPedido itemGuardado = itemPedidoRepository.save(item);
                                    novosItens.add(itemGuardado);
                                }
                                System.out.println("--> SUCESSO: " + quantidadeTotal + " item(ns) guardados -> " + prato.getNome());
                            }
                        }
                    }
                }
            }

            // ATRIBUIÇÃO CRUCIAL: Garante que o pedido fica com a lista de itens associada em memória
            pedido.setItens(novosItens);
            pedidoRepository.save(pedido);

        } else {
            System.out.println("--> ERRO: Mesa com ID " + idMesa + " não foi encontrada!");
        }

        return "redirect:/pedidos/cozinha";
    }
    // 4. Mover o pedido para o próximo estado
    @PostMapping("/pedidos/avancar/{id}")
    public String avancarEstadoPedido(@PathVariable("id") Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido).orElse(null);
        if (pedido != null) {
            String estadoAtual = pedido.getEstado() != null ? pedido.getEstado().trim() : "";

            if (estadoAtual.equalsIgnoreCase("Em espera")) {
                pedido.setEstado("Em preparação");
            } else if (estadoAtual.equalsIgnoreCase("Em preparação")) {
                pedido.setEstado("Pronto");
            } else if (estadoAtual.equalsIgnoreCase("Pronto")) {
                pedido.setEstado("Concluído");
            }
            pedidoRepository.save(pedido);
        }
        return "redirect:/pedidos/cozinha";
    }

    // 5. Avançar o estado do item individual sequencialmente
    @PostMapping("/pedidos/item/toggle/{id}")
    public String toggleItemPronto(@PathVariable("id") Integer idItem) {
        ItemPedido item = itemPedidoRepository.findById(idItem).orElse(null);
        if (item != null) {
            String estadoAtual = item.getEstado() != null ? item.getEstado().trim() : "";

            if (estadoAtual.equalsIgnoreCase("PENDENTE")) {
                item.setEstado("PREPARADO");
            } else if (estadoAtual.equalsIgnoreCase("PREPARADO")) {
                item.setEstado("PRONTO");
            } else if (estadoAtual.equalsIgnoreCase("PRONTO")) {
                item.setEstado("CONCLUIDO"); // Ou podes apagar o item se preferires: itemPedidoRepository.delete(item);
            }
            itemPedidoRepository.save(item);
        }
        return "redirect:/pedidos/cozinha";
    }

    // 6. Redirecionar do formulário de pedido para a página de pagamentos
    @GetMapping("/pedidos/pagamento")
    public String realizarPagamento(@RequestParam("idMesa") Integer idMesa) {
        return "redirect:/pedidos/pagamentos";
    }

    // 7. Página de Pagamentos (Abordagem Direta e Infalível por ID de Mesa)
    @GetMapping("/pedidos/pagamentos")
    public String paginaPagamentos(Model model) {
        List<Mesa> mesas = mesaRepository.findAll();
        List<ItemPedido> todosItens = itemPedidoRepository.findAll();

        Map<Integer, List<ItemPedido>> itensPorMesa = new LinkedHashMap<>();
        Map<Integer, Double> totaisPorMesa = new LinkedHashMap<>();

        for (Mesa mesa : mesas) {
            double totalMesa = 0.0;
            List<ItemPedido> itensAtivosMesa = new ArrayList<>();

            for (ItemPedido item : todosItens) {
                if (item.getPedido() != null && item.getPedido().getMesa() != null) {
                    Integer idMesaPedido = item.getPedido().getMesa().getIdMesa();

                    if (idMesaPedido != null && idMesaPedido.equals(mesa.getIdMesa())) {

                        // Verifica o estado do pedido pai (ignora se estiver concluído de qualquer forma)
                        String estadoPedido = item.getPedido().getEstado();
                        boolean pedidoConcluido = estadoPedido != null &&
                                estadoPedido.toLowerCase().replace("ú", "u").contains("concluido");

                        // Verifica o estado do item individual
                        String estadoItem = item.getEstado();
                        boolean itemConcluido = estadoItem != null &&
                                estadoItem.toLowerCase().replace("ú", "u").contains("concluido");

                        // Só adiciona se nem o pedido nem o item estiverem concluídos
                        if (!pedidoConcluido && !itemConcluido) {
                            itensAtivosMesa.add(item);

                            Prato prato = pratoRepository.findByNome(item.getNomeProduto());
                            if (prato == null && item.getNomeProduto() != null) {
                                for (Prato pBD : pratoRepository.findAll()) {
                                    if (pBD.getNome() != null && pBD.getNome().trim().equalsIgnoreCase(item.getNomeProduto().trim())) {
                                        prato = pBD;
                                        break;
                                    }
                                }
                            }

                            if (prato != null && prato.getPreco() != null) {
                                totalMesa += (prato.getPreco().doubleValue() * item.getQuantidade());
                            }
                        }
                    }
                }
            }

            itensPorMesa.put(mesa.getIdMesa(), itensAtivosMesa);
            totaisPorMesa.put(mesa.getIdMesa(), totalMesa);
        }

        model.addAttribute("mesas", mesas);
        model.addAttribute("itensPorMesa", itensPorMesa);
        model.addAttribute("totaisPorMesa", totaisPorMesa);

        return "pagamentos";
    }

    // 8. Concluir o pagamento de uma mesa (atualiza os pedidos/itens para CONCLUIDO e liberta a mesa)
    @PostMapping("/pedidos/concluir-pagamento")
    public String concluirPagamento(@RequestParam("idMesa") Integer idMesa) {
        // Procura todos os pedidos da mesa e marca-os como concluídos
        List<Pedido> todosPedidos = pedidoRepository.findAll();

        for (Pedido p : todosPedidos) {
            if (p.getMesa() != null && p.getMesa().getIdMesa().equals(idMesa)) {
                String estadoPedido = p.getEstado();
                if (estadoPedido == null || !estadoPedido.trim().equalsIgnoreCase("CONCLUIDO")) {
                    p.setEstado("CONCLUIDO");

                    // Opcional: Se também quiseres marcar os itens individuais como concluídos
                    if (p.getItens() != null) {
                        for (ItemPedido item : p.getItens()) {
                            item.setEstado("CONCLUIDO");
                        }
                    }
                    pedidoRepository.save(p);
                }
            }
        }

        // Opcional: Atualizar o estado da mesa para "Livre", caso tenhas esse campo na entidade Mesa
        Mesa mesa = mesaRepository.findById(idMesa).orElse(null);
        if (mesa != null) {
            mesa.setEstado("Livre");
            mesaRepository.save(mesa);
        }

        return "redirect:/pedidos/pagamentos";
    }
}