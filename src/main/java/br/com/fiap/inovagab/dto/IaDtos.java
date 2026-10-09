package br.com.fiap.inovagab.dto;

import java.util.List;

public final class IaDtos {

    private IaDtos() {
    }

    public record AvaliacaoIa(int score, String justificativa) {
    }

    public record InsightsIaResponse(String analise, List<String> sugestoes, String modelo) {
    }
}
