package com.sharcky.klientt.pagamento.web;

import com.sharcky.klientt.conta.repository.PlanoRepository;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import com.sharcky.klientt.conta.seguranca.KlienttUserDetails;
import com.sharcky.klientt.pagamento.dto.SubscricaoIntent;
import com.sharcky.klientt.pagamento.service.PagamentoIndisponivelException;
import com.sharcky.klientt.pagamento.service.PagamentoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class PagamentoController {

    private static final String PLANO_GRATIS = "Teste";

    private final PagamentoService pagamentoService;
    private final PlanoRepository planoRepository;
    private final UtilizadorRepository utilizadorRepository;

    public PagamentoController(PagamentoService pagamentoService, PlanoRepository planoRepository,
                               UtilizadorRepository utilizadorRepository) {
        this.pagamentoService = pagamentoService;
        this.planoRepository = planoRepository;
        this.utilizadorRepository = utilizadorRepository;
    }

    /** Lista os planos pagos para subscrição. */
    @GetMapping("/planos")
    public String planos(@AuthenticationPrincipal KlienttUserDetails utilizador, Model model) {
        model.addAttribute("planos", planoRepository.findAll().stream()
                .filter(p -> !PLANO_GRATIS.equals(p.getNome()))
                .toList());
        model.addAttribute("disponivel", pagamentoService.disponivel());
        utilizadorRepository.findById(utilizador.getId()).ifPresent(u ->
                model.addAttribute("planoAtual", u.getPlano() != null ? u.getPlano().getNome() : "—"));
        return "planos";
    }

    /** Página de pagamento (Stripe Payment Element). Cria a subscrição e embute o clientSecret. */
    @GetMapping("/assinar/{plano}")
    public String assinar(@PathVariable String plano,
                          @AuthenticationPrincipal KlienttUserDetails utilizador,
                          Model model) {
        model.addAttribute("plano", plano);
        model.addAttribute("disponivel", pagamentoService.disponivel());

        if (pagamentoService.disponivel()) {
            try {
                SubscricaoIntent intent = pagamentoService.iniciarSubscricao(utilizador.getId(), plano);
                model.addAttribute("clientSecret", intent.clientSecret());
                model.addAttribute("publishableKey", intent.publishableKey());
                model.addAttribute("sucessoUrl", ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/pagamento/sucesso").toUriString());
            } catch (PagamentoIndisponivelException ex) {
                model.addAttribute("erro", ex.getMessage());
            }
        }
        return "assinar";
    }

    @GetMapping("/pagamento/sucesso")
    public String sucesso() {
        return "pagamento-sucesso";
    }
}
