package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.domain.EtapaProjeto;
import br.com.fiap.inovagab.domain.Projeto;
import br.com.fiap.inovagab.domain.StatusIdeia;
import br.com.fiap.inovagab.dto.DashboardDtos.ProjetoResumoResponse;
import br.com.fiap.inovagab.dto.DashboardDtos.ResumoResponse;
import br.com.fiap.inovagab.repository.IdeiaRepository;
import br.com.fiap.inovagab.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private IdeiaRepository ideiaRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IaService iaService;

    private DashboardService dashboardService;

    @BeforeEach
    void preparar() {
        dashboardService = new DashboardService(projetoRepository, ideiaRepository, mongoTemplate,
                iaService, new ObjectMapper());
    }

    @Test
    @DisplayName("resumo consolida contagens, investimento, retorno e roi geral")
    void resumoConsolidaIndicadores() {
        when(projetoRepository.findAll()).thenReturn(List.of(
                projeto(EtapaProjeto.CONCLUIDO, 10000, 25000, 10),
                projeto(EtapaProjeto.EM_ANDAMENTO, 5000, 0, 0),
                projeto(EtapaProjeto.PLANEJAMENTO, 2000, 0, 0),
                projeto(EtapaProjeto.CANCELADO, 1000, 0, 0)));

        lenient().when(ideiaRepository.countByStatus(StatusIdeia.ENVIADA)).thenReturn(4L);
        lenient().when(ideiaRepository.countByStatus(StatusIdeia.APROVADA)).thenReturn(2L);
        lenient().when(ideiaRepository.countByStatus(StatusIdeia.REJEITADA)).thenReturn(1L);

        ResumoResponse resumo = dashboardService.resumo();

        assertThat(resumo.totalProjetos()).isEqualTo(4);
        assertThat(resumo.projetosAtivos()).isEqualTo(2);
        assertThat(resumo.projetosConcluidos()).isEqualTo(1);
        assertThat(resumo.projetosCancelados()).isEqualTo(1);
        assertThat(resumo.investimentoTotal()).isEqualTo(18000);
        assertThat(resumo.retornoTotal()).isEqualTo(25000);
        assertThat(resumo.lucroTotal()).isEqualTo(7000);
        assertThat(resumo.roiGeral()).isCloseTo(150, within(0.001));
        assertThat(resumo.ganhoMedioProdutividade()).isEqualTo(10);
        assertThat(resumo.distribuicaoPorEtapa()).containsEntry("CONCLUIDO", 1L).containsEntry("CANCELADO", 1L);
        assertThat(resumo.ideiasPorStatus()).containsEntry("ENVIADA", 4L).containsEntry("APROVADA", 2L);
    }

    @Test
    @DisplayName("sem projetos concluidos com retorno o roi geral fica em zero")
    void roiGeralZeroSemRetorno() {
        when(projetoRepository.findAll()).thenReturn(List.of(
                projeto(EtapaProjeto.EM_ANDAMENTO, 5000, 0, 0)));

        lenient().when(ideiaRepository.countByStatus(StatusIdeia.ENVIADA)).thenReturn(0L);
        lenient().when(ideiaRepository.countByStatus(StatusIdeia.APROVADA)).thenReturn(0L);
        lenient().when(ideiaRepository.countByStatus(StatusIdeia.REJEITADA)).thenReturn(0L);

        ResumoResponse resumo = dashboardService.resumo();

        assertThat(resumo.roiGeral()).isZero();
        assertThat(resumo.retornoTotal()).isZero();
        assertThat(resumo.ganhoMedioProdutividade()).isZero();
    }

    @Test
    @DisplayName("por-projeto devolve investimento, retorno, lucro e roi de cada projeto")
    void porProjetoCalculaIndicadores() {
        when(projetoRepository.findAllByOrderByDataCriacaoDesc()).thenReturn(List.of(
                projeto(EtapaProjeto.CONCLUIDO, 10000, 25000, 10),
                projeto(EtapaProjeto.PLANEJAMENTO, 0, 0, 0)));

        List<ProjetoResumoResponse> lista = dashboardService.porProjeto();

        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).lucro()).isEqualTo(15000);
        assertThat(lista.get(0).roi()).isCloseTo(150, within(0.001));
        assertThat(lista.get(1).roi()).isZero();
    }

    private Projeto projeto(EtapaProjeto etapa, double orcamento, double retorno, double ganho) {
        Projeto projeto = new Projeto();
        projeto.setId("pr-" + etapa.name());
        projeto.setTitulo("Projeto " + etapa.name());
        projeto.setEtapa(etapa);
        projeto.setOrcamento(orcamento);
        projeto.setRetornoFinanceiro(retorno);
        projeto.setGanhoProdutividade(ganho);
        projeto.setOrientacaoId("or-1");
        return projeto;
    }
}
