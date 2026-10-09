package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.config.AppProperties;
import br.com.fiap.inovagab.domain.Ideia;
import br.com.fiap.inovagab.domain.Orientacao;
import br.com.fiap.inovagab.dto.IaDtos.AvaliacaoIa;
import br.com.fiap.inovagab.dto.IaDtos.InsightsIaResponse;
import br.com.fiap.inovagab.exception.ApiExceptions.IntegracaoIaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IaService {

    private static final Logger log = LoggerFactory.getLogger(IaService.class);

    private final AppProperties propriedades;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public IaService(AppProperties propriedades, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.propriedades = propriedades;
        this.objectMapper = objectMapper;
        this.restClient = builder
                .baseUrl(propriedades.ia().baseUrl())
                .requestFactory(criarRequestFactory(propriedades.ia().timeoutSegundos()))
                .build();
    }

    public String getModelo() {
        return propriedades.ia().modelo();
    }

    public AvaliacaoIa avaliarIdeia(Ideia ideia, Orientacao orientacao) {
        String prompt = """
                Voce avalia ideias de inovacao corporativa do Grupo Aguia Branca.

                Orientacao estrategica vigente:
                - Titulo: %s
                - Descricao: %s
                - Categoria: %s

                Ideia enviada pelo colaborador:
                - Titulo: %s
                - Descricao: %s
                - Categoria: %s

                Avalie o alinhamento da ideia com a orientacao estrategica, a clareza da proposta
                e o potencial de impacto. Responda em portugues do Brasil.

                Responda apenas com um JSON no formato:
                {"score": <inteiro de 0 a 100>, "justificativa": "<no maximo 3 frases>"}
                """.formatted(
                orientacao.getTitulo(),
                orientacao.getDescricao(),
                orientacao.getCategoria(),
                ideia.getTitulo(),
                ideia.getDescricao(),
                ideia.getCategoria());

        JsonNode resposta = chamar(prompt);
        int score = resposta.path("score").asInt(-1);
        String justificativa = resposta.path("justificativa").asText(null);

        if (score < 0 || score > 100 || justificativa == null || justificativa.isBlank()) {
            throw new IntegracaoIaException("A resposta da IA veio em formato inesperado");
        }

        return new AvaliacaoIa(score, justificativa);
    }

    public InsightsIaResponse gerarInsights(String resumoDashboard) {
        String prompt = """
                Voce assessora a lideranca do Grupo Aguia Branca na gestao do portfolio de inovacao.

                Resumo consolidado do painel estrategico (JSON):
                %s

                Analise os numeros e aponte riscos, oportunidades e prioridades.
                Responda em portugues do Brasil, de forma objetiva e acionavel.

                Responda apenas com um JSON no formato:
                {"analise": "<2 a 4 frases>", "sugestoes": ["<sugestao 1>", "<sugestao 2>", "<sugestao 3>"]}
                """.formatted(resumoDashboard);

        JsonNode resposta = chamar(prompt);
        String analise = resposta.path("analise").asText(null);

        if (analise == null || analise.isBlank()) {
            throw new IntegracaoIaException("A resposta da IA veio em formato inesperado");
        }

        List<String> sugestoes = new ArrayList<>();
        resposta.path("sugestoes").forEach(item -> sugestoes.add(item.asText()));

        return new InsightsIaResponse(analise, sugestoes, propriedades.ia().modelo());
    }

    private JsonNode chamar(String prompt) {
        if (!propriedades.ia().configurada()) {
            throw new IntegracaoIaException(
                    "A avaliacao por IA nao esta configurada. Defina a variavel de ambiente GEMINI_API_KEY");
        }

        Map<String, Object> corpo = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2));

        String texto;
        try {
            String resposta = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{modelo}:generateContent")
                            .queryParam("key", propriedades.ia().apiKey())
                            .build(propriedades.ia().modelo()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(String.class);

            if (resposta == null || resposta.isBlank()) {
                throw new IntegracaoIaException("O provedor de IA nao retornou conteudo");
            }

            JsonNode envelope = objectMapper.readTree(resposta);
            texto = envelope.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
        } catch (IntegracaoIaException excecao) {
            throw excecao;
        } catch (HttpStatusCodeException excecao) {
            log.warn("provedor de ia recusou a chamada status={} corpo={}",
                    excecao.getStatusCode(), excecao.getResponseBodyAsString());
            throw new IntegracaoIaException(mensagemDoStatus(excecao), excecao);
        } catch (Exception excecao) {
            log.warn("falha ao chamar provedor de ia tipo={} mensagem={}",
                    excecao.getClass().getSimpleName(), excecao.getMessage());
            throw new IntegracaoIaException("Nao foi possivel consultar o servico de IA no momento", excecao);
        }

        if (texto == null || texto.isBlank()) {
            throw new IntegracaoIaException("O provedor de IA nao retornou conteudo");
        }

        try {
            return objectMapper.readTree(limparCerca(texto));
        } catch (Exception excecao) {
            log.warn("resposta da ia nao pode ser interpretada como json trecho={}",
                    texto.substring(0, Math.min(texto.length(), 200)));
            throw new IntegracaoIaException("A resposta da IA veio em formato inesperado", excecao);
        }
    }

    private String mensagemDoStatus(HttpStatusCodeException excecao) {
        int status = excecao.getStatusCode().value();
        return switch (status) {
            case 400, 401, 403 -> "A chave da IA foi recusada pelo provedor. Confira a variavel GEMINI_API_KEY";
            case 404 -> "O modelo de IA configurado nao existe mais. Ajuste a variavel GEMINI_MODEL";
            case 429 -> "A cota da IA foi excedida. Tente novamente em alguns minutos";
            case 503 -> "O modelo de IA esta sobrecarregado no momento. Tente novamente em alguns minutos";
            default -> "Nao foi possivel consultar o servico de IA no momento";
        };
    }

    private String limparCerca(String texto) {
        String limpo = texto.trim();
        if (limpo.startsWith("```")) {
            limpo = limpo.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return limpo.trim();
    }

    private org.springframework.http.client.ClientHttpRequestFactory criarRequestFactory(int timeoutSegundos) {
        org.springframework.http.client.SimpleClientHttpRequestFactory fabrica =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(Math.max(1, timeoutSegundos)));
        fabrica.setReadTimeout(Duration.ofSeconds(Math.max(1, timeoutSegundos)));
        return fabrica;
    }
}
