package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.domain.EtapaProjeto;
import br.com.fiap.inovagab.domain.Projeto;
import br.com.fiap.inovagab.domain.StatusIdeia;
import br.com.fiap.inovagab.dto.DashboardDtos.EstrategiaResponse;
import br.com.fiap.inovagab.dto.DashboardDtos.ProjetoResumoResponse;
import br.com.fiap.inovagab.dto.DashboardDtos.ResumoResponse;
import br.com.fiap.inovagab.dto.IaDtos.InsightsIaResponse;
import br.com.fiap.inovagab.exception.ApiExceptions.IntegracaoIaException;
import br.com.fiap.inovagab.repository.IdeiaRepository;
import br.com.fiap.inovagab.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final ProjetoRepository projetoRepository;
    private final IdeiaRepository ideiaRepository;
    private final MongoTemplate mongoTemplate;
    private final IaService iaService;
    private final ObjectMapper objectMapper;

    public DashboardService(ProjetoRepository projetoRepository,
                            IdeiaRepository ideiaRepository,
                            MongoTemplate mongoTemplate,
                            IaService iaService,
                            ObjectMapper objectMapper) {
        this.projetoRepository = projetoRepository;
        this.ideiaRepository = ideiaRepository;
        this.mongoTemplate = mongoTemplate;
        this.iaService = iaService;
        this.objectMapper = objectMapper;
    }

    public ResumoResponse resumo() {
        List<Projeto> projetos = projetoRepository.findAll();

        long total = projetos.size();
        long ativos = projetos.stream()
                .filter(projeto -> projeto.getEtapa() == EtapaProjeto.PLANEJAMENTO
                        || projeto.getEtapa() == EtapaProjeto.EM_ANDAMENTO)
                .count();
        long concluidos = projetos.stream().filter(projeto -> projeto.getEtapa() == EtapaProjeto.CONCLUIDO).count();
        long cancelados = projetos.stream().filter(projeto -> projeto.getEtapa() == EtapaProjeto.CANCELADO).count();

        double investimentoTotal = projetos.stream().mapToDouble(Projeto::getOrcamento).sum();

        List<Projeto> comRetorno = projetos.stream()
                .filter(projeto -> projeto.getEtapa() == EtapaProjeto.CONCLUIDO && projeto.getRetornoFinanceiro() > 0)
                .toList();

        double retornoTotal = comRetorno.stream().mapToDouble(Projeto::getRetornoFinanceiro).sum();
        double investimentoConcluidos = comRetorno.stream().mapToDouble(Projeto::getOrcamento).sum();
        double roiGeral = investimentoConcluidos == 0
                ? 0
                : (retornoTotal - investimentoConcluidos) / investimentoConcluidos * 100;
        double lucroTotal = retornoTotal - investimentoTotal;

        double ganhoMedio = comRetorno.isEmpty()
                ? 0
                : comRetorno.stream().mapToDouble(Projeto::getGanhoProdutividade).average().orElse(0);

        Map<String, Long> distribuicaoPorEtapa = new LinkedHashMap<>();
        for (EtapaProjeto etapa : EtapaProjeto.values()) {
            distribuicaoPorEtapa.put(etapa.name(),
                    projetos.stream().filter(projeto -> projeto.getEtapa() == etapa).count());
        }

        Map<String, Long> ideiasPorStatus = new LinkedHashMap<>();
        for (StatusIdeia status : StatusIdeia.values()) {
            ideiasPorStatus.put(status.name(), ideiaRepository.countByStatus(status));
        }

        return new ResumoResponse(total, ativos, concluidos, cancelados, investimentoTotal, retornoTotal,
                lucroTotal, roiGeral, ganhoMedio, distribuicaoPorEtapa, ideiasPorStatus);
    }

    public List<EstrategiaResponse> porEstrategia() {
        Aggregation agregacao = Aggregation.newAggregation(
                Aggregation.group("orientacaoId")
                        .first("orientacaoTitulo").as("orientacaoTitulo")
                        .count().as("totalProjetos")
                        .sum("orcamento").as("investimento")
                        .sum("retornoFinanceiro").as("retorno")
                        .sum(ConditionalOperators.when(Criteria.where("etapa").is(EtapaProjeto.CONCLUIDO.name()))
                                .then(1).otherwise(0)).as("projetosConcluidos"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "investimento"));

        AggregationResults<Document> resultados =
                mongoTemplate.aggregate(agregacao, "projetos", Document.class);

        List<EstrategiaResponse> estrategias = new ArrayList<>();
        for (Document documento : resultados) {
            double investimento = numero(documento.get("investimento"));
            double retorno = numero(documento.get("retorno"));
            double lucro = retorno - investimento;
            double roi = investimento == 0 ? 0 : lucro / investimento * 100;

            estrategias.add(new EstrategiaResponse(
                    documento.getString("_id"),
                    documento.getString("orientacaoTitulo"),
                    (long) numero(documento.get("totalProjetos")),
                    (long) numero(documento.get("projetosConcluidos")),
                    investimento,
                    retorno,
                    lucro,
                    roi));
        }
        return estrategias;
    }

    public List<ProjetoResumoResponse> porProjeto() {
        return projetoRepository.findAllByOrderByDataCriacaoDesc().stream()
                .map(projeto -> {
                    double lucro = projeto.getRetornoFinanceiro() - projeto.getOrcamento();
                    double roi = projeto.getOrcamento() == 0 ? 0 : lucro / projeto.getOrcamento() * 100;
                    return new ProjetoResumoResponse(
                            projeto.getId(),
                            projeto.getTitulo(),
                            projeto.getEtapa() == null ? null : projeto.getEtapa().name(),
                            projeto.getPrazo(),
                            projeto.getOrcamento(),
                            projeto.getRetornoFinanceiro(),
                            lucro,
                            roi);
                })
                .toList();
    }

    public InsightsIaResponse insights() {
        String resumoJson;
        try {
            resumoJson = objectMapper.writeValueAsString(Map.of(
                    "resumo", resumo(),
                    "porEstrategia", porEstrategia()));
        } catch (Exception excecao) {
            throw new IntegracaoIaException("Nao foi possivel preparar o resumo para a IA", excecao);
        }

        log.info("insights de ia solicitados tamanhoResumo={}", resumoJson.length());
        return iaService.gerarInsights(resumoJson);
    }

    private double numero(Object valor) {
        return valor instanceof Number numero ? numero.doubleValue() : 0;
    }
}
