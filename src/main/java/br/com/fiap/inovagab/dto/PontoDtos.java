package br.com.fiap.inovagab.dto;

import br.com.fiap.inovagab.domain.PontoTransacao;

import java.time.Instant;

public final class PontoDtos {

    private PontoDtos() {
    }

    public record TransacaoResponse(
            String id,
            String usuarioId,
            int pontos,
            String motivo,
            String ideiaId,
            Instant data) {

        public static TransacaoResponse de(PontoTransacao transacao) {
            return new TransacaoResponse(
                    transacao.getId(),
                    transacao.getUsuarioId(),
                    transacao.getPontos(),
                    transacao.getMotivo(),
                    transacao.getIdeiaId(),
                    transacao.getData());
        }
    }

    public record RankingItemResponse(int posicao, String usuarioId, String nome, int pontos) {
    }
}
