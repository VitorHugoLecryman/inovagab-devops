package br.com.fiap.inovagab.controller;

import br.com.fiap.inovagab.dto.OrientacaoDtos.OrientacaoRequest;
import br.com.fiap.inovagab.dto.OrientacaoDtos.OrientacaoResponse;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import br.com.fiap.inovagab.service.OrientacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orientacoes")
@Tag(name = "Orientacoes estrategicas")
public class OrientacaoController {

    private final OrientacaoService orientacaoService;

    public OrientacaoController(OrientacaoService orientacaoService) {
        this.orientacaoService = orientacaoService;
    }

    @GetMapping
    @Operation(summary = "Lista as orientacoes estrategicas vigentes")
    public ResponseEntity<List<OrientacaoResponse>> listarVigentes() {
        return ResponseEntity.ok(orientacaoService.listarVigentes());
    }

    @GetMapping("/historico")
    @Operation(summary = "Lista o historico completo, com filtro opcional por categoria")
    public ResponseEntity<List<OrientacaoResponse>> historico(
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(orientacaoService.historico(categoria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma orientacao estrategica pelo id")
    public ResponseEntity<OrientacaoResponse> buscar(@PathVariable String id) {
        return ResponseEntity.ok(orientacaoService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('LIDER')")
    @Operation(summary = "Cria uma orientacao estrategica")
    public ResponseEntity<OrientacaoResponse> criar(@Valid @RequestBody OrientacaoRequest request,
                                                    @AuthenticationPrincipal UsuarioAutenticado lider) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orientacaoService.criar(request, lider));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIDER')")
    @Operation(summary = "Atualiza uma orientacao estrategica")
    public ResponseEntity<OrientacaoResponse> atualizar(@PathVariable String id,
                                                        @Valid @RequestBody OrientacaoRequest request,
                                                        @AuthenticationPrincipal UsuarioAutenticado lider) {
        return ResponseEntity.ok(orientacaoService.atualizar(id, request, lider));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LIDER')")
    @Operation(summary = "Desativa uma orientacao estrategica, mantendo o historico")
    public ResponseEntity<Void> desativar(@PathVariable String id,
                                          @AuthenticationPrincipal UsuarioAutenticado lider) {
        orientacaoService.desativar(id, lider);
        return ResponseEntity.noContent().build();
    }
}
