package com.sharcky.klientt.common.web;

import com.sharcky.klientt.busca.service.BuscaNaoEncontradaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Tratamento centralizado de erros não previstos.
 * Devolve o fragmento de erro (compatível com as respostas HTMX da busca).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Job inexistente ou de outro utilizador — mensagem neutra (não revela existência). */
    @ExceptionHandler(BuscaNaoEncontradaException.class)
    public String tratarBuscaNaoEncontrada(BuscaNaoEncontradaException ex, Model model) {
        model.addAttribute("mensagens", List.of("Busca não encontrada."));
        return "fragments/resultados :: erro";
    }

    /** Recursos estáticos e rotas inexistentes são respostas 404 normais. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> tratarRecursoNaoEncontrado() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public String tratarErroInesperado(Exception ex, Model model) {
        log.error("Erro inesperado", ex);
        model.addAttribute("mensagens",
                List.of("Ocorreu um erro inesperado. Por favor, tente novamente."));
        return "fragments/resultados :: erro";
    }
}
