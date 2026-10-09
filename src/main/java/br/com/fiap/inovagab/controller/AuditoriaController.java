package br.com.fiap.inovagab.controller;

import br.com.fiap.inovagab.dto.AuditoriaDtos.AuditoriaResponse;
import br.com.fiap.inovagab.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@Tag(name = "Governanca")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('LIDER')")
    @Operation(summary = "Lista o registro de auditoria das operacoes de escrita")
    public ResponseEntity<List<AuditoriaResponse>> listar(
            @RequestParam(required = false) String entidade,
            @RequestParam(defaultValue = "100") int limite) {
        return ResponseEntity.ok(auditoriaService.listar(entidade, limite).stream()
                .map(AuditoriaResponse::de)
                .toList());
    }
}
