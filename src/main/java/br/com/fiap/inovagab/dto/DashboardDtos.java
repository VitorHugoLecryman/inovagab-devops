package br.com.fiap.inovagab.dto;

import java.util.Map;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record ResumoResponse(
            long totalProjetos,
            long projetosAtivos,
            long projetosConcluidos,
            long projetosCancelados,
            double investimentoTotal,
            double retornoTotal,
            double lucroTotal,
            double roiGeral,
            double ganhoMedioProdutividade,
            Map<String, Long> distribuicaoPorEtapa,
            Map<String, Long> ideiasPorStatus) {
    }

    public record EstrategiaResponse(
            String orientacaoId,
            String orientacaoTitulo,
            long totalProjetos,
            long projetosConcluidos,
            double investimento,
            double retorno,
            double lucro,
            double roi) {
    }

    public record ProjetoResumoResponse(
            String id,
            String titulo,
            String etapa,
            String prazo,
            double investimento,
            double retorno,
            double lucro,
            double roi) {
    }
}
