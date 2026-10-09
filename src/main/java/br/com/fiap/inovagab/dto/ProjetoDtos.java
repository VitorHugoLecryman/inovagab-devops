package br.com.fiap.inovagab.dto;

import br.com.fiap.inovagab.domain.EtapaProjeto;
import br.com.fiap.inovagab.domain.Projeto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ProjetoDtos {

    private ProjetoDtos() {
    }

    public record ProjetoRequest(
            @NotBlank(message = "informe o titulo")
            @Size(max = 150, message = "o titulo deve ter no maximo 150 caracteres")
            String titulo,

            @NotBlank(message = "informe a descricao")
            @Size(max = 1000, message = "a descricao deve ter no maximo 1000 caracteres")
            String descricao,

            String ideiaId,

            String orientacaoId,

            @PositiveOrZero(message = "o orcamento nao pode ser negativo")
            double orcamento,

            @Size(max = 80, message = "o prazo deve ter no maximo 80 caracteres")
            String prazo,

            @Size(max = 80, message = "o status deve ter no maximo 80 caracteres")
            String status,

            @Size(max = 1000, message = "as observacoes devem ter no maximo 1000 caracteres")
            String observacoes) {
    }

    public record EtapaRequest(
            @NotNull(message = "informe a etapa")
            EtapaProjeto etapa) {
    }

    public record ResultadosRequest(
            @PositiveOrZero(message = "o retorno financeiro nao pode ser negativo")
            double retornoFinanceiro,

            @PositiveOrZero(message = "o ganho de produtividade nao pode ser negativo")
            double ganhoProdutividade,

            @Size(max = 1000, message = "os resultados qualitativos devem ter no maximo 1000 caracteres")
            String resultadosQualitativos) {
    }

    public record ProjetoResponse(
            String id,
            String titulo,
            String descricao,
            String ideiaId,
            String orientacaoId,
            String orientacaoTitulo,
            String etapa,
            String status,
            double orcamento,
            String prazo,
            String gestorId,
            String gestorNome,
            Instant dataCriacao,
            double retornoFinanceiro,
            double ganhoProdutividade,
            String resultadosQualitativos,
            String observacoes,
            double lucro,
            double roi) {

        public static ProjetoResponse de(Projeto projeto) {
            double lucro = projeto.getRetornoFinanceiro() - projeto.getOrcamento();
            double roi = projeto.getOrcamento() == 0 ? 0 : lucro / projeto.getOrcamento() * 100;

            return new ProjetoResponse(
                    projeto.getId(),
                    projeto.getTitulo(),
                    projeto.getDescricao(),
                    projeto.getIdeiaId(),
                    projeto.getOrientacaoId(),
                    projeto.getOrientacaoTitulo(),
                    projeto.getEtapa() == null ? null : projeto.getEtapa().name(),
                    projeto.getStatus(),
                    projeto.getOrcamento(),
                    projeto.getPrazo(),
                    projeto.getGestorId(),
                    projeto.getGestorNome(),
                    projeto.getDataCriacao(),
                    projeto.getRetornoFinanceiro(),
                    projeto.getGanhoProdutividade(),
                    projeto.getResultadosQualitativos(),
                    projeto.getObservacoes(),
                    lucro,
                    roi);
        }
    }
}
