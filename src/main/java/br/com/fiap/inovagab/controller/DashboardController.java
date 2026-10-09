package br.com.fiap.inovagab.controller;

import br.com.fiap.inovagab.dto.DashboardDtos.EstrategiaResponse;
import br.com.fiap.inovagab.dto.DashboardDtos.ProjetoResumoResponse;
import br.com.fiap.inovagab.dto.DashboardDtos.ResumoResponse;
import br.com.fiap.inovagab.dto.IaDtos.InsightsIaResponse;
import br.com.fiap.inovagab.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Painel estrategico")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    @PreAuthorize("hasRole('LIDER')")
    @Operation(summary = "Consolida os indicadores do portfolio de inovacao")
    public ResponseEntity<ResumoResponse> resumo() {
        return ResponseEntity.ok(dashboardService.resumo());
    }

    @GetMapping("/por-estrategia")
    @PreAuthorize("hasRole('LIDER')")
    @Operation(summary = "Agrupa os indicadores por orientacao estrategica")
    public ResponseEntity<List<EstrategiaResponse>> porEstrategia() {
        return ResponseEntity.ok(dashboardService.porEstrategia());
    }

    @GetMapping("/por-projeto")
    @PreAuthorize("hasRole('LIDER')")
    @Operation(summary = "Lista investimento, retorno, lucro e ROI de cada projeto")
    public ResponseEntity<List<ProjetoResumoResponse>> porProjeto() {
        return ResponseEntity.ok(dashboardService.porProjeto());
    }

    @PostMapping("/insights-ia")
    @PreAuthorize("hasRole('LIDER')")
    @Operation(summary = "Gera analise e sugestoes da IA a partir do painel")
    public ResponseEntity<InsightsIaResponse> insights() {
        return ResponseEntity.ok(dashboardService.insights());
    }
}
