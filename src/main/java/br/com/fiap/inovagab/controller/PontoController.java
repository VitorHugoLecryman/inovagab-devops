package br.com.fiap.inovagab.controller;

import br.com.fiap.inovagab.dto.PontoDtos.RankingItemResponse;
import br.com.fiap.inovagab.dto.PontoDtos.TransacaoResponse;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import br.com.fiap.inovagab.service.PontoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pontos")
@Tag(name = "Pontuacao")
public class PontoController {

    private final PontoService pontoService;

    public PontoController(PontoService pontoService) {
        this.pontoService = pontoService;
    }

    @GetMapping("/historico")
    @Operation(summary = "Lista o historico de pontos do usuario autenticado")
    public ResponseEntity<List<TransacaoResponse>> historico(
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(pontoService.historico(usuario.getId()));
    }

    @GetMapping("/ranking")
    @Operation(summary = "Lista o ranking dos operadores por pontos")
    public ResponseEntity<List<RankingItemResponse>> ranking(
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(pontoService.ranking(limite));
    }
}
