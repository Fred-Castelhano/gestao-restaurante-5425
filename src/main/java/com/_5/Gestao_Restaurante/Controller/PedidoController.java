package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.ItemPedido;
import com._5.Gestao_Restaurante.model.Mesa;
import com._5.Gestao_Restaurante.model.Pedido;
import com._5.Gestao_Restaurante.model.Prato;
import com._5.Gestao_Restaurante.model.Utilizador;
import com._5.Gestao_Restaurante.Repository.MesaRepository;
import com._5.Gestao_Restaurante.Repository.PedidoRepository;
import com._5.Gestao_Restaurante.Repository.ItemPedidoRepository;
import com._5.Gestao_Restaurante.Repository.PratoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    // 1. Mostrar painel da cozinha (KDS) com mensagem de acesso negado se não tiver permissão
    @GetMapping("/pedidos/cozinha")
    public String verCozinha(Model model, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");

        boolean temPermissao = user != null && (
                user.getFuncao().equalsIgnoreCase("Gerente") ||
                        user.getFuncao().equalsIgnoreCase("Cozinheiro") ||
                        user.getFuncao().equalsIgnoreCase("Administrador")
        );

        if (!temPermissao) {
            model.addAttribute("erroPermissao", "Acesso Negado: Não tem permissões para aceder ao Painel da Cozinha.");
        }

        // Se tiver permissão busca os dados reais, senão passa listas vazias
        List<ItemPedido> listaEspera = temPermissao ? itemPedidoRepository.findByEstado("PENDENTE") : new ArrayList<>();
        List<ItemPedido> listaPreparacao = temPermissao ? itemPedidoRepository.findByEstado("PREPARADO") : new ArrayList<>();
        List<ItemPedido> listaPronto = temPermissao ? itemPedidoRepository.findByEstado("PRONTO") : new ArrayList<>();

        model.addAttribute("emEspera", listaEspera);
        model.addAttribute("emPreparacao", listaPreparacao);
        model.addAttribute("pronto", listaPronto);

        model.addAttribute("countEspera", listaEspera.size());
        model.addAttribute("countPreparacao", listaPreparacao.size());
        model.addAttribute("countPronto", listaPronto.size());

        // Configuração do Layout Geral
        model.addAttribute("conteudo", "cozinha-pedidos");
        model.addAttribute("menuAtivo", "pedidos");
        model.addAttribute("tituloPage", "Painel da Cozinha - Restaurante App");

        return "layout";
    }

    // 2. Formulário para criar pedido (Garçom / Gerente / Admin)
    @GetMapping("/pedidos/novo")
    public String novoPedidoForm(@RequestParam("idMesa") Integer idMesa, Model model, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");

        // Se for Cozinheiro, não deve criar pedidos de mesa
        if (user != null && user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/pedidos/cozinha";
        }

        Mesa mesa = mesaRepository.findById(idMesa).orElse(null);
        model.addAttribute("mesa", mesa);
        model.addAttribute("pratos", pratoRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "idPrato")));

        double totalAcumulado = 0.0;
        List<Pedido> todosPedidos = pedidoRepository.findAll();

        for (Pedido p : todosPedidos) {
            if (p.getMesa() != null && p.getMesa().getIdMesa().equals(idMesa)) {
                if (p.getItens() != null) {
                    for (ItemPedido item : p.getItens()) {
                        String estadoItem = item.getEstado();
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

    // 3. Guardar o pedido (Cozinheiros não criam pedidos de mesa)
    @PostMapping("/pedidos/guardar")
    public String guardarPedido(@RequestParam("idMesa") Integer idMesa,
                                @RequestParam Map<String, String> allParams,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                                HttpSession session) {

        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/pedidos/cozinha";
        }

        Mesa mesa = mesaRepository.findById(idMesa).orElse(null);
        if (mesa == null) {
            return "redirect:/pedidos/cozinha";
        }

        // Pré-validação de stock
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith("quantidades[") && key.endsWith("]")) {
                String idStr = key.substring(key.indexOf('[') + 1, key.indexOf(']'));

                if (idStr.matches("\\d+") && value != null && !value.trim().isEmpty()) {
                    int quantidadeSolicitada = Integer.parseInt(value);

                    if (quantidadeSolicitada > 0) {
                        Integer idPrato = Integer.parseInt(idStr);
                        Prato prato = pratoRepository.findById(idPrato).orElse(null);

                        if (prato != null) {
                            int stockAtual = (prato.getQuantidade() != null) ? prato.getQuantidade() : 0;

                            if (stockAtual <= 0) {
                                redirectAttributes.addFlashAttribute("erro", "Item indisponível: " + prato.getNome());
                                return "redirect:/pedidos/novo?idMesa=" + idMesa;
                            }

                            if (quantidadeSolicitada > stockAtual) {
                                redirectAttributes.addFlashAttribute("erro", "Insuficiente stock para " + prato.getNome() + " (" + stockAtual + " disponíveis).");
                                return "redirect:/pedidos/novo?idMesa=" + idMesa;
                            }
                        }
                    }
                }
            }
        }

        Pedido pedido = new Pedido();
        pedido.setMesa(mesa);
        pedido.setDataHora(LocalDateTime.now());
        pedido.setEstado("Em espera");
        pedido = pedidoRepository.save(pedido);

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
                            prato.setQuantidade(prato.getQuantidade() - quantidadeTotal);
                            pratoRepository.save(prato);

                            for (int i = 0; i < quantidadeTotal; i++) {
                                ItemPedido item = new ItemPedido();
                                item.setPedido(pedido);
                                item.setNomeProduto(prato.getNome());
                                item.setQuantidade(1);
                                item.setEstado("PENDENTE");

                                ItemPedido itemGuardado = itemPedidoRepository.save(item);
                                novosItens.add(itemGuardado);
                            }
                        }
                    }
                }
            }
        }

        pedido.setItens(novosItens);
        pedidoRepository.save(pedido);

        return "redirect:/pedidos/cozinha";
    }

    // 4. Mover o pedido geral (Apenas Administrador e Gerente)
    @PostMapping("/pedidos/avancar/{id}")
    public String avancarEstadoPedido(@PathVariable("id") Long idPedido, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || (!user.getFuncao().equalsIgnoreCase("Administrador") && !user.getFuncao().equalsIgnoreCase("Gerente"))) {
            return "redirect:/pedidos/cozinha";
        }

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

    // 5. Avançar o estado do item individual (Restrito a Cozinheiro, Gerente e Administrador)
    @PostMapping("/pedidos/item/toggle/{id}")
    public String toggleItemPronto(@PathVariable("id") Integer idItem, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");

        // Garçons NÃO podem mexer na cozinha
        if (user == null || user.getFuncao().equalsIgnoreCase("Garçom")) {
            return "redirect:/pedidos/cozinha";
        }

        ItemPedido item = itemPedidoRepository.findById(idItem).orElse(null);
        if (item != null) {
            String estadoAtual = item.getEstado() != null ? item.getEstado().trim() : "";

            if (estadoAtual.equalsIgnoreCase("PENDENTE")) {
                item.setEstado("PREPARADO");
            } else if (estadoAtual.equalsIgnoreCase("PREPARADO")) {
                item.setEstado("PRONTO");
            } else if (estadoAtual.equalsIgnoreCase("PRONTO")) {
                item.setEstado("CONCLUIDO");
            }
            itemPedidoRepository.save(item);
        }
        return "redirect:/pedidos/cozinha";
    }
}