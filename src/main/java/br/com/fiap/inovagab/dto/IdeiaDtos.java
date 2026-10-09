package br.com.fiap.inovagab.dto;

import br.com.fiap.inovagab.domain.Ideia;
import br.com.fiap.inovagab.domain.Nivel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class IdeiaDtos {

    private IdeiaDtos() {
    }

    public record IdeiaRequest(
            @NotBlank(message = "informe o titulo")
            @Size(max = 100, message = "o titulo deve ter no maximo 100 caracteres")
            String titulo,

            @NotBlank(message = "informe a descricao")
            @Size(max = 500, message = "a descricao deve ter no maximo 500 caracteres")
            String descricao,

            @NotBlank(message = "informe a categoria")
            @Size(max = 80, message = "a categoria deve ter no maximo 80 caracteres")
            String categoria,

            @NotBlank(message = "selecione a orientacao estrategica")
            String orientacaoId) {
    }

    public record PriorizacaoRequest(
            @NotNull(message = "informe o impacto")
            Nivel impacto,

            @NotNull(message = "informe o esforco")
            Nivel esforco,

            @Min(value = 1, message = "a prioridade minima e 1")
            @Max(value = 20, message = "a prioridade maxima e 20")
            Integer prioridade) {
    }

    public record AprovacaoRequest(
            @NotNull(message = "informe o impacto")
            Nivel impacto,

            @NotNull(message = "informe o esforco")
            Nivel esforco) {
    }

    public record RejeicaoRequest(
            @NotBlank(message = "informe o motivo da rejeicao")
            @Size(max = 500, message = "o motivo deve ter no maximo 500 caracteres")
            String motivo) {
    }

    public record IdeiaResponse(
            String id,
            String titulo,
            String descricao,
            String categoria,
            String status,
            String orientacaoId,
            String orientacaoTitulo,
            String autorId,
            String autorNome,
            Instant dataEnvio,
            String impacto,
            String esforco,
            Integer prioridade,
            String motivoRejeicao,
            String avaliadorId,
            String avaliadorNome,
            Instant dataAvaliacao,
            Integer scoreIa,
            String justificativaIa) {

        public static IdeiaResponse de(Ideia ideia) {
            return new IdeiaResponse(
                    ideia.getId(),
                    ideia.getTitulo(),
                    ideia.getDescricao(),
                    ideia.getCategoria(),
                    ideia.getStatus() == null ? null : ideia.getStatus().name(),
                    ideia.getOrientacaoId(),
                    ideia.getOrientacaoTitulo(),
                    ideia.getAutorId(),
                    ideia.getAutorNome(),
                    ideia.getDataEnvio(),
                    ideia.getImpacto() == null ? null : ideia.getImpacto().name(),
                    ideia.getEsforco() == null ? null : ideia.getEsforco().name(),
                    ideia.getPrioridade(),
                    ideia.getMotivoRejeicao(),
                    ideia.getAvaliadorId(),
                    ideia.getAvaliadorNome(),
                    ideia.getDataAvaliacao(),
                    ideia.getScoreIa(),
                    ideia.getJustificativaIa());
        }
    }

    public record AvaliacaoIaResponse(String ideiaId, int score, String justificativa, String modelo) {
    }
}
