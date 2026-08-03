package com._5.Gestao_Restaurante.Controller;

import com._5.Gestao_Restaurante.model.ItemPedido;
import com._5.Gestao_Restaurante.model.Mesa;
import com._5.Gestao_Restaurante.model.Pedido;
import com._5.Gestao_Restaurante.model.Prato;
import com._5.Gestao_Restaurante.model.Utilizador;
import com._5.Gestao_Restaurante.Repository.ItemPedidoRepository;
import com._5.Gestao_Restaurante.Repository.MesaRepository;
import com._5.Gestao_Restaurante.Repository.PedidoRepository;
import com._5.Gestao_Restaurante.Repository.PratoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PagamentoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private PratoRepository pratoRepository;

    @Autowired
    private ItemPedidoRepository itemPedidoRepository;

    // 1. Redirecionar para a página de pagamentos
    @GetMapping("/pedidos/pagamento")
    public String realizarPagamento(@RequestParam("idMesa") Integer idMesa, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/pedidos/pagamentos";
        }
        return "redirect:/pedidos/pagamentos";
    }

    // 2. Página de Pagamentos (Listagem por Mesa e layout integrado)
    @GetMapping("/pedidos/pagamentos")
    public String paginaPagamentos(Model model, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");

        // Cozinheiro não tem permissão para aceder a pagamentos
        boolean temPermissao = user != null && (
                user.getFuncao().equalsIgnoreCase("Gerente") ||
                        user.getFuncao().equalsIgnoreCase("Administrador") ||
                        user.getFuncao().equalsIgnoreCase("Garçom")
        );

        if (!temPermissao) {
            model.addAttribute("erroPermissao", "Acesso Negado: Não tem permissões para aceder à Gestão de Pagamentos.");
        }

        List<Mesa> mesas = temPermissao ? mesaRepository.findAll() : new ArrayList<>();
        List<ItemPedido> todosItens = temPermissao ? itemPedidoRepository.findAll() : new ArrayList<>();
        List<Prato> pratosDisponiveis = temPermissao ? pratoRepository.findAll() : new ArrayList<>();

        Map<Integer, List<ItemPedido>> itensPorMesa = new LinkedHashMap<>();
        Map<Integer, Double> totaisPorMesa = new LinkedHashMap<>();

        if (temPermissao) {
            for (Mesa mesa : mesas) {
                double totalMesa = 0.0;
                List<ItemPedido> itensAtivosMesa = new ArrayList<>();

                for (ItemPedido item : todosItens) {
                    if (item.getPedido() != null && item.getPedido().getMesa() != null) {
                        Integer idMesaPedido = item.getPedido().getMesa().getIdMesa();

                        if (idMesaPedido != null && idMesaPedido.equals(mesa.getIdMesa())) {

                            String estadoPedido = item.getPedido().getEstado();
                            boolean pedidoPago = estadoPedido != null &&
                                    estadoPedido.toLowerCase().replace("ú", "u").contains("concluido");

                            if (!pedidoPago) {
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
        }

        model.addAttribute("mesas", mesas);
        model.addAttribute("itensPorMesa", itensPorMesa);
        model.addAttribute("totaisPorMesa", totaisPorMesa);
        model.addAttribute("pratos", pratosDisponiveis);

        // Atributos do Layout Geral
        model.addAttribute("conteudo", "pagamentos");
        model.addAttribute("menuAtivo", "pagamentos");
        model.addAttribute("tituloPage", "Gestão de Pagamentos - Restaurante App");

        return "layout";
    }

    // 3. Remover um item específico da conta
    @PostMapping("/pedidos/pagamentos/remover-item/{id}")
    public String removerItemPagamento(@PathVariable("id") Integer idItem, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/pedidos/pagamentos";
        }

        ItemPedido item = itemPedidoRepository.findById(idItem).orElse(null);
        if (item != null) {
            String nomeProduto = item.getNomeProduto();
            if (nomeProduto != null) {
                Prato prato = pratoRepository.findByNome(nomeProduto);
                if (prato == null) {
                    for (Prato pBD : pratoRepository.findAll()) {
                        if (pBD.getNome() != null && pBD.getNome().trim().equalsIgnoreCase(nomeProduto.trim())) {
                            prato = pBD;
                            break;
                        }
                    }
                }
                if (prato != null) {
                    int qtdAtual = prato.getQuantidade() != null ? prato.getQuantidade() : 0;
                    prato.setQuantidade(qtdAtual + item.getQuantidade());
                    pratoRepository.save(prato);
                }
            }
            itemPedidoRepository.delete(item);
        }
        return "redirect:/pedidos/pagamentos";
    }

    // 4. Adicionar um novo item diretamente à mesa
    @PostMapping("/pedidos/pagamentos/adicionar-item")
    public String adicionarItemPagamento(@RequestParam("idMesa") Integer idMesa,
                                         @RequestParam("idPrato") Integer idPrato,
                                         @RequestParam(value = "quantidade", defaultValue = "1") int quantidade,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/pedidos/pagamentos";
        }
        if (quantidade <= 0) return "redirect:/pedidos/pagamentos";

        Mesa mesa = mesaRepository.findById(idMesa).orElse(null);
        Prato prato = pratoRepository.findById(idPrato).orElse(null);

        if (mesa == null || prato == null) {
            return "redirect:/pedidos/pagamentos";
        }

        int stockAtual = prato.getQuantidade() != null ? prato.getQuantidade() : 0;
        if (stockAtual < quantidade) {
            redirectAttributes.addFlashAttribute("erro", "Stock insuficiente para adicionar " + prato.getNome());
            return "redirect:/pedidos/pagamentos";
        }

        prato.setQuantidade(stockAtual - quantidade);
        pratoRepository.save(prato);

        Pedido pedidoAtivo = null;
        List<Pedido> pedidos = pedidoRepository.findAll();
        for (Pedido p : pedidos) {
            if (p.getMesa() != null && p.getMesa().getIdMesa().equals(idMesa)) {
                String estado = p.getEstado();
                boolean concluido = estado != null && estado.toLowerCase().replace("ú", "u").contains("concluido");
                if (!concluido) {
                    pedidoAtivo = p;
                    break;
                }
            }
        }

        if (pedidoAtivo == null) {
            pedidoAtivo = new Pedido();
            pedidoAtivo.setMesa(mesa);
            pedidoAtivo.setDataHora(java.time.LocalDateTime.now());
            pedidoAtivo.setEstado("Em espera");
            pedidoAtivo = pedidoRepository.save(pedidoAtivo);
        }

        for (int i = 0; i < quantidade; i++) {
            ItemPedido novoItem = new ItemPedido();
            novoItem.setPedido(pedidoAtivo);
            novoItem.setNomeProduto(prato.getNome());
            novoItem.setQuantidade(1);
            novoItem.setEstado("PENDENTE");
            itemPedidoRepository.save(novoItem);
        }

        return "redirect:/pedidos/pagamentos";
    }

    // 5. Concluir o pagamento de uma mesa
    @PostMapping("/pedidos/concluir-pagamento")
    public String concluirPagamento(@RequestParam("idMesa") Integer idMesa, HttpSession session) {
        Utilizador user = (Utilizador) session.getAttribute("utilizadorLogado");
        if (user == null || user.getFuncao().equalsIgnoreCase("Cozinheiro")) {
            return "redirect:/pedidos/pagamentos";
        }

        List<Pedido> todosPedidos = pedidoRepository.findAll();

        for (Pedido p : todosPedidos) {
            if (p.getMesa() != null && p.getMesa().getIdMesa().equals(idMesa)) {
                String estadoPedido = p.getEstado();
                if (estadoPedido == null || !estadoPedido.trim().equalsIgnoreCase("CONCLUIDO")) {
                    p.setEstado("CONCLUIDO");

                    if (p.getItens() != null) {
                        for (ItemPedido item : p.getItens()) {
                            item.setEstado("CONCLUIDO");
                        }
                    }
                    pedidoRepository.save(p);
                }
            }
        }

        Mesa mesa = mesaRepository.findById(idMesa).orElse(null);
        if (mesa != null) {
            mesa.setEstado("Livre");
            mesaRepository.save(mesa);
        }

        return "redirect:/pedidos/pagamentos";
    }
}