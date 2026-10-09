package br.com.fiap.inovagab.dto;

import br.com.fiap.inovagab.domain.Orientacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class OrientacaoDtos {

    private OrientacaoDtos() {
    }

    public record OrientacaoRequest(
            @NotBlank(message = "informe o titulo")
            @Size(max = 150, message = "o titulo deve ter no maximo 150 caracteres")
            String titulo,

            @NotBlank(message = "informe a descricao")
            @Size(max = 1000, message = "a descricao deve ter no maximo 1000 caracteres")
            String descricao,

            @NotBlank(message = "informe a categoria")
            @Size(max = 80, message = "a categoria deve ter no maximo 80 caracteres")
            String categoria,

            @Size(max = 80, message = "a campanha deve ter no maximo 80 caracteres")
            String campanha) {
    }

    public record OrientacaoResponse(
            String id,
            String titulo,
            String descricao,
            String categoria,
            String campanha,
            String autorId,
            String autorNome,
            Instant dataCriacao,
            Instant dataAtualizacao,
            boolean ativo) {

        public static OrientacaoResponse de(Orientacao orientacao) {
            return new OrientacaoResponse(
                    orientacao.getId(),
                    orientacao.getTitulo(),
                    orientacao.getDescricao(),
                    orientacao.getCategoria(),
                    orientacao.getCampanha(),
                    orientacao.getAutorId(),
                    orientacao.getAutorNome(),
                    orientacao.getDataCriacao(),
                    orientacao.getDataAtualizacao(),
                    orientacao.isAtivo());
        }
    }
}
