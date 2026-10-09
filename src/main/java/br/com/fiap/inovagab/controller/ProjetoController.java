package br.com.fiap.inovagab.controller;

import br.com.fiap.inovagab.dto.ProjetoDtos.EtapaRequest;
import br.com.fiap.inovagab.dto.ProjetoDtos.ProjetoRequest;
import br.com.fiap.inovagab.dto.ProjetoDtos.ProjetoResponse;
import br.com.fiap.inovagab.dto.ProjetoDtos.ResultadosRequest;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import br.com.fiap.inovagab.service.ProjetoService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projetos")
@Tag(name = "Projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GESTOR', 'LIDER')")
    @Operation(summary = "Lista os projetos em execucao")
    public ResponseEntity<List<ProjetoResponse>> listar() {
        return ResponseEntity.ok(projetoService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GESTOR', 'LIDER')")
    @Operation(summary = "Busca um projeto pelo id")
    public ResponseEntity<ProjetoResponse> buscar(@PathVariable String id) {
        return ResponseEntity.ok(projetoService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Cria um projeto a partir de uma ideia aprovada ou de uma orientacao vigente")
    public ResponseEntity<ProjetoResponse> criar(@Valid @RequestBody ProjetoRequest request,
                                                 @AuthenticationPrincipal UsuarioAutenticado gestor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.criar(request, gestor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Atualiza os dados do projeto")
    public ResponseEntity<ProjetoResponse> atualizar(@PathVariable String id,
                                                     @Valid @RequestBody ProjetoRequest request,
                                                     @AuthenticationPrincipal UsuarioAutenticado gestor) {
        return ResponseEntity.ok(projetoService.atualizar(id, request, gestor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Exclui um projeto que ainda nao foi concluido")
    public ResponseEntity<Void> excluir(@PathVariable String id,
                                        @AuthenticationPrincipal UsuarioAutenticado gestor) {
        projetoService.excluir(id, gestor);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/etapa")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Move o projeto para outra etapa")
    public ResponseEntity<ProjetoResponse> alterarEtapa(@PathVariable String id,
                                                        @Valid @RequestBody EtapaRequest request,
                                                        @AuthenticationPrincipal UsuarioAutenticado gestor) {
        return ResponseEntity.ok(projetoService.alterarEtapa(id, request, gestor));
    }

    @PatchMapping("/{id}/resultados")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Registra os resultados e conclui o projeto")
    public ResponseEntity<ProjetoResponse> registrarResultados(@PathVariable String id,
                                                               @Valid @RequestBody ResultadosRequest request,
                                                               @AuthenticationPrincipal UsuarioAutenticado gestor) {
        return ResponseEntity.ok(projetoService.registrarResultados(id, request, gestor));
    }
}
