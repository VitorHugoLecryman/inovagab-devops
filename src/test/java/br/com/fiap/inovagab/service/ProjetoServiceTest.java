package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.domain.EtapaProjeto;
import br.com.fiap.inovagab.domain.Ideia;
import br.com.fiap.inovagab.domain.Orientacao;
import br.com.fiap.inovagab.domain.Perfil;
import br.com.fiap.inovagab.domain.Projeto;
import br.com.fiap.inovagab.domain.StatusIdeia;
import br.com.fiap.inovagab.domain.Usuario;
import br.com.fiap.inovagab.dto.ProjetoDtos.EtapaRequest;
import br.com.fiap.inovagab.dto.ProjetoDtos.ProjetoRequest;
import br.com.fiap.inovagab.dto.ProjetoDtos.ProjetoResponse;
import br.com.fiap.inovagab.dto.ProjetoDtos.ResultadosRequest;
import br.com.fiap.inovagab.exception.ApiExceptions.RegraNegocioException;
import br.com.fiap.inovagab.repository.ProjetoRepository;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private IdeiaService ideiaService;

    @Mock
    private OrientacaoService orientacaoService;

    @Mock
    private AuditoriaService auditoriaService;

    private ProjetoService projetoService;

    private UsuarioAutenticado gestor;

    @BeforeEach
    void preparar() {
        projetoService = new ProjetoService(projetoRepository, ideiaService, orientacaoService, auditoriaService);
        gestor = autenticado("ge-1", "Gestor Demo");
    }

    @Test
    @DisplayName("projeto criado a partir de ideia aprovada herda a orientacao dela")
    void criarAPartirDeIdeiaAprovada() {
        Ideia ideia = ideia(StatusIdeia.APROVADA);
        when(ideiaService.buscarEntidade("id-1")).thenReturn(ideia);
        when(projetoRepository.existsByIdeiaId("id-1")).thenReturn(false);
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(chamada -> chamada.getArgument(0));

        ProjetoResponse resposta = projetoService.criar(new ProjetoRequest(
                "Projeto piloto", "Descricao", "id-1", null, 10000, "3 meses", "No prazo", null), gestor);

        assertThat(resposta.orientacaoId()).isEqualTo("or-1");
        assertThat(resposta.orientacaoTitulo()).isEqualTo("Reduzir custos");
        assertThat(resposta.etapa()).isEqualTo("PLANEJAMENTO");
        assertThat(resposta.gestorNome()).isEqualTo("Gestor Demo");
    }

    @Test
    @DisplayName("nao cria projeto a partir de ideia que ainda nao foi aprovada")
    void naoCriaDeIdeiaNaoAprovada() {
        when(ideiaService.buscarEntidade("id-1")).thenReturn(ideia(StatusIdeia.ENVIADA));

        assertThatThrownBy(() -> projetoService.criar(new ProjetoRequest(
                "Projeto piloto", "Descricao", "id-1", null, 10000, "3 meses", null, null), gestor))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ideia aprovada");

        verify(projetoRepository, never()).save(any());
    }

    @Test
    @DisplayName("nao cria dois projetos para a mesma ideia")
    void naoDuplicaProjetoDaMesmaIdeia() {
        when(ideiaService.buscarEntidade("id-1")).thenReturn(ideia(StatusIdeia.APROVADA));
        when(projetoRepository.existsByIdeiaId("id-1")).thenReturn(true);

        assertThatThrownBy(() -> projetoService.criar(new ProjetoRequest(
                "Projeto piloto", "Descricao", "id-1", null, 10000, "3 meses", null, null), gestor))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja possui um projeto");
    }

    @Test
    @DisplayName("nao cria projeto sem ideia e sem orientacao de origem")
    void exigeOrigem() {
        assertThatThrownBy(() -> projetoService.criar(new ProjetoRequest(
                "Projeto piloto", "Descricao", null, null, 10000, "3 meses", null, null), gestor))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("orientacao estrategica de origem");
    }

    @Test
    @DisplayName("nao cria projeto vinculado a orientacao desativada")
    void naoCriaComOrientacaoInativa() {
        Orientacao orientacao = orientacao();
        orientacao.setAtivo(false);
        when(orientacaoService.buscarEntidade("or-1")).thenReturn(orientacao);

        assertThatThrownBy(() -> projetoService.criar(new ProjetoRequest(
                "Projeto piloto", "Descricao", null, "or-1", 10000, "3 meses", null, null), gestor))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nao esta vigente");
    }

    @Test
    @DisplayName("registrar resultados conclui o projeto e calcula lucro e roi")
    void registrarResultadosConcluiECalcula() {
        Projeto projeto = projeto(EtapaProjeto.EM_ANDAMENTO, 10000, 0);
        when(projetoRepository.findById("pr-1")).thenReturn(Optional.of(projeto));
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(chamada -> chamada.getArgument(0));

        ProjetoResponse resposta = projetoService.registrarResultados("pr-1",
                new ResultadosRequest(25000, 12, "Menos retrabalho na operacao"), gestor);

        assertThat(resposta.etapa()).isEqualTo("CONCLUIDO");
        assertThat(resposta.lucro()).isEqualTo(15000);
        assertThat(resposta.roi()).isEqualTo(150);
    }

    @Test
    @DisplayName("roi e zero quando o projeto nao tem orcamento")
    void roiZeroSemOrcamento() {
        Projeto projeto = projeto(EtapaProjeto.EM_ANDAMENTO, 0, 0);
        when(projetoRepository.findById("pr-1")).thenReturn(Optional.of(projeto));
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(chamada -> chamada.getArgument(0));

        ProjetoResponse resposta = projetoService.registrarResultados("pr-1",
                new ResultadosRequest(5000, 0, null), gestor);

        assertThat(resposta.roi()).isZero();
        assertThat(resposta.lucro()).isEqualTo(5000);
    }

    @Test
    @DisplayName("projeto concluido nao muda mais de etapa")
    void concluidoNaoMudaEtapa() {
        when(projetoRepository.findById("pr-1")).thenReturn(Optional.of(projeto(EtapaProjeto.CONCLUIDO, 100, 200)));

        assertThatThrownBy(() -> projetoService.alterarEtapa("pr-1",
                new EtapaRequest(EtapaProjeto.EM_ANDAMENTO), gestor))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("projeto cancelado nao aceita registro de resultados")
    void canceladoNaoAceitaResultados() {
        when(projetoRepository.findById("pr-1")).thenReturn(Optional.of(projeto(EtapaProjeto.CANCELADO, 100, 0)));

        assertThatThrownBy(() -> projetoService.registrarResultados("pr-1",
                new ResultadosRequest(5000, 0, null), gestor))
                .isInstanceOf(RegraNegocioException.class);
    }

    private Ideia ideia(StatusIdeia status) {
        Ideia ideia = new Ideia();
        ideia.setId("id-1");
        ideia.setTitulo("Ideia");
        ideia.setStatus(status);
        ideia.setOrientacaoId("or-1");
        ideia.setOrientacaoTitulo("Reduzir custos");
        return ideia;
    }

    private Orientacao orientacao() {
        Orientacao orientacao = new Orientacao();
        orientacao.setId("or-1");
        orientacao.setTitulo("Reduzir custos");
        orientacao.setAtivo(true);
        return orientacao;
    }

    private Projeto projeto(EtapaProjeto etapa, double orcamento, double retorno) {
        Projeto projeto = new Projeto();
        projeto.setId("pr-1");
        projeto.setTitulo("Projeto piloto");
        projeto.setEtapa(etapa);
        projeto.setOrcamento(orcamento);
        projeto.setRetornoFinanceiro(retorno);
        projeto.setOrientacaoId("or-1");
        return projeto;
    }

    private UsuarioAutenticado autenticado(String id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail("gestor@inovagab.com");
        usuario.setSenhaHash("hash");
        usuario.setPerfil(Perfil.GESTOR);
        return new UsuarioAutenticado(usuario);
    }
}
