package br.com.fiap.inovagab.exception;

import br.com.fiap.inovagab.exception.ApiExceptions.AcessoNegadoException;
import br.com.fiap.inovagab.exception.ApiExceptions.IntegracaoIaException;
import br.com.fiap.inovagab.exception.ApiExceptions.NaoEncontradoException;
import br.com.fiap.inovagab.exception.ApiExceptions.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarNaoEncontrado(NaoEncontradoException excecao) {
        return montar(HttpStatus.NOT_FOUND, "Nao encontrado", excecao.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> tratarRegraNegocio(RegraNegocioException excecao) {
        return montar(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negocio", excecao.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, Object>> tratarAcessoNegado(AcessoNegadoException excecao) {
        return montar(HttpStatus.FORBIDDEN, "Acesso negado", excecao.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> tratarAcessoNegadoSpring(AccessDeniedException excecao) {
        return montar(HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil nao tem permissao para esta operacao");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> tratarAutenticacao(AuthenticationException excecao) {
        return montar(HttpStatus.UNAUTHORIZED, "Nao autenticado", "E-mail ou senha invalidos");
    }

    @ExceptionHandler(IntegracaoIaException.class)
    public ResponseEntity<Map<String, Object>> tratarIntegracaoIa(IntegracaoIaException excecao) {
        log.warn("falha na integracao com ia mensagem={}", excecao.getMessage());
        return montar(HttpStatus.SERVICE_UNAVAILABLE, "Servico de IA indisponivel", excecao.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException excecao) {
        String mensagem = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return montar(HttpStatus.BAD_REQUEST, "Dados invalidos", mensagem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tratarTipoInvalido(MethodArgumentTypeMismatchException excecao) {
        return montar(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "Valor invalido para o parametro " + excecao.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarCorpoIlegivel(HttpMessageNotReadableException excecao) {
        log.warn("corpo da requisicao ilegivel mensagem={}", excecao.getMessage());
        return montar(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "O corpo da requisicao nao esta no formato esperado");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarInesperado(Exception excecao) {
        log.error("erro inesperado tipo={} mensagem={}", excecao.getClass().getSimpleName(), excecao.getMessage(), excecao);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado ao processar a requisicao");
    }

    private ResponseEntity<Map<String, Object>> montar(HttpStatus status, String erro, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", status.value());
        corpo.put("erro", erro);
        corpo.put("mensagem", mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
