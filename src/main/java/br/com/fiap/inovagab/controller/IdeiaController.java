package br.com.fiap.inovagab.controller;

import br.com.fiap.inovagab.domain.StatusIdeia;
import br.com.fiap.inovagab.dto.IdeiaDtos.AprovacaoRequest;
import br.com.fiap.inovagab.dto.IdeiaDtos.AvaliacaoIaResponse;
import br.com.fiap.inovagab.dto.IdeiaDtos.IdeiaRequest;
import br.com.fiap.inovagab.dto.IdeiaDtos.IdeiaResponse;
import br.com.fiap.inovagab.dto.IdeiaDtos.PriorizacaoRequest;
import br.com.fiap.inovagab.dto.IdeiaDtos.RejeicaoRequest;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import br.com.fiap.inovagab.service.IdeiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ideias")
@Tag(name = "Ideias")
public class IdeiaController {

    private final IdeiaService ideiaService;

    public IdeiaController(IdeiaService ideiaService) {
        this.ideiaService = ideiaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERADOR')")
    @Operation(summary = "Envia uma nova ideia vinculada a uma orientacao vigente")
    public ResponseEntity<IdeiaResponse> criar(@Valid @RequestBody IdeiaRequest request,
                                               @AuthenticationPrincipal UsuarioAutenticado autor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ideiaService.criar(request, autor));
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('OPERADOR')")
    @Operation(summary = "Lista as ideias do operador autenticado")
    public ResponseEntity<List<IdeiaResponse>> minhas(@AuthenticationPrincipal UsuarioAutenticado autor) {
        return ResponseEntity.ok(ideiaService.listarMinhas(autor.getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GESTOR', 'LIDER')")
    @Operation(summary = "Lista as ideias para curadoria, com filtro opcional por status")
    public ResponseEntity<List<IdeiaResponse>> listar(@RequestParam(required = false) StatusIdeia status) {
        return ResponseEntity.ok(ideiaService.listar(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma ideia pelo id")
    public ResponseEntity<IdeiaResponse> buscar(@PathVariable String id) {
        return ResponseEntity.ok(ideiaService.buscar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERADOR')")
    @Operation(summary = "Atualiza a propria ideia enquanto ela estiver enviada")
    public ResponseEntity<IdeiaResponse> atualizar(@PathVariable String id,
                                                   @Valid @RequestBody IdeiaRequest request,
                                                   @AuthenticationPrincipal UsuarioAutenticado autor) {
        return ResponseEntity.ok(ideiaService.atualizar(id, request, autor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERADOR')")
    @Operation(summary = "Exclui a propria ideia enquanto ela estiver enviada")
    public ResponseEntity<Void> excluir(@PathVariable String id,
                                        @AuthenticationPrincipal UsuarioAutenticado autor) {
        ideiaService.excluir(id, autor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/priorizacao")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Define impacto, esforco e prioridade da ideia")
    public ResponseEntity<IdeiaResponse> priorizar(@PathVariable String id,
                                                   @Valid @RequestBody PriorizacaoRequest request,
                                                   @AuthenticationPrincipal UsuarioAutenticado gestor) {
        return ResponseEntity.ok(ideiaService.priorizar(id, request, gestor));
    }

    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Aprova a ideia e credita os pontos ao autor")
    public ResponseEntity<IdeiaResponse> aprovar(@PathVariable String id,
                                                 @Valid @RequestBody AprovacaoRequest request,
                                                 @AuthenticationPrincipal UsuarioAutenticado gestor) {
        return ResponseEntity.ok(ideiaService.aprovar(id, request, gestor));
    }

    @PatchMapping("/{id}/rejeitar")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Rejeita a ideia informando o motivo")
    public ResponseEntity<IdeiaResponse> rejeitar(@PathVariable String id,
                                                  @Valid @RequestBody RejeicaoRequest request,
                                                  @AuthenticationPrincipal UsuarioAutenticado gestor) {
        return ResponseEntity.ok(ideiaService.rejeitar(id, request, gestor));
    }

    @PostMapping("/{id}/avaliacao-ia")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Avalia a ideia com IA, gerando score e justificativa")
    public ResponseEntity<AvaliacaoIaResponse> avaliarComIa(@PathVariable String id,
                                                            @AuthenticationPrincipal UsuarioAutenticado gestor) {
        return ResponseEntity.ok(ideiaService.avaliarComIa(id, gestor));
    }
}
