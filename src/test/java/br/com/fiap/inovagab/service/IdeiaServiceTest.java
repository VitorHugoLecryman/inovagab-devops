package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.config.AppProperties;
import br.com.fiap.inovagab.domain.Ideia;
import br.com.fiap.inovagab.domain.Nivel;
import br.com.fiap.inovagab.domain.Orientacao;
import br.com.fiap.inovagab.domain.Perfil;
import br.com.fiap.inovagab.domain.StatusIdeia;
import br.com.fiap.inovagab.domain.Usuario;
import br.com.fiap.inovagab.dto.IdeiaDtos.AprovacaoRequest;
import br.com.fiap.inovagab.dto.IdeiaDtos.IdeiaRequest;
import br.com.fiap.inovagab.dto.IdeiaDtos.IdeiaResponse;
import br.com.fiap.inovagab.dto.IdeiaDtos.PriorizacaoRequest;
import br.com.fiap.inovagab.dto.IdeiaDtos.RejeicaoRequest;
import br.com.fiap.inovagab.exception.ApiExceptions.AcessoNegadoException;
import br.com.fiap.inovagab.exception.ApiExceptions.NaoEncontradoException;
import br.com.fiap.inovagab.exception.ApiExceptions.RegraNegocioException;
import br.com.fiap.inovagab.repository.IdeiaRepository;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdeiaServiceTest {

    @Mock
    private IdeiaRepository ideiaRepository;

    @Mock
    private OrientacaoService orientacaoService;

    @Mock
    private PontoService pontoService;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private IaService iaService;

    private IdeiaService ideiaService;

    private UsuarioAutenticado operador;
    private UsuarioAutenticado gestor;

    @BeforeEach
    void preparar() {
        AppProperties propriedades = new AppProperties(
                new AppProperties.Jwt("chave-de-teste-com-tamanho-suficiente-256-bits", 480),
                new AppProperties.Pontos(10, 50),
                new AppProperties.Seed(false, "123456"),
                new AppProperties.Ia(null, "modelo-teste", "http://127.0.0.1", 5));

        ideiaService = new IdeiaService(ideiaRepository, orientacaoService, pontoService,
                auditoriaService, iaService, propriedades);

        operador = autenticado("op-1", "Operador Demo", Perfil.OPERADOR);
        gestor = autenticado("ge-1", "Gestor Demo", Perfil.GESTOR);
    }

    @Test
    @DisplayName("enviar ideia credita 10 pontos ao autor")
    void enviarIdeiaCreditaPontos() {
        when(orientacaoService.buscarEntidade("or-1")).thenReturn(orientacao(true));
        when(ideiaRepository.save(any(Ideia.class))).thenAnswer(chamada -> {
            Ideia ideia = chamada.getArgument(0);
            ideia.setId("id-1");
            return ideia;
        });

        IdeiaResponse resposta = ideiaService.criar(
                new IdeiaRequest("Titulo", "Descricao", "Eficiencia", "or-1"), operador);

        assertThat(resposta.status()).isEqualTo("ENVIADA");
        assertThat(resposta.orientacaoTitulo()).isEqualTo("Reduzir custos");
        verify(pontoService).creditar("op-1", 10, "Envio de Ideia", "id-1");
    }

    @Test
    @DisplayName("nao aceita ideia vinculada a orientacao desativada")
    void naoAceitaOrientacaoInativa() {
        when(orientacaoService.buscarEntidade("or-1")).thenReturn(orientacao(false));

        assertThatThrownBy(() -> ideiaService.criar(
                new IdeiaRequest("Titulo", "Descricao", "Eficiencia", "or-1"), operador))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nao esta vigente");

        verify(ideiaRepository, never()).save(any());
        verify(pontoService, never()).creditar(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    @DisplayName("aprovar credita 50 pontos ao autor da ideia")
    void aprovarCreditaPontos() {
        Ideia ideia = ideiaEnviada("id-1", "op-1");
        when(ideiaRepository.findById("id-1")).thenReturn(Optional.of(ideia));
        when(ideiaRepository.save(any(Ideia.class))).thenAnswer(chamada -> chamada.getArgument(0));

        IdeiaResponse resposta = ideiaService.aprovar("id-1",
                new AprovacaoRequest(Nivel.ALTO, Nivel.BAIXO), gestor);

        assertThat(resposta.status()).isEqualTo("APROVADA");
        assertThat(resposta.avaliadorNome()).isEqualTo("Gestor Demo");
        verify(pontoService).creditar("op-1", 50, "Ideia Aprovada", "id-1");
    }

    @Test
    @DisplayName("nao permite aprovar a mesma ideia duas vezes")
    void naoPermiteDuplaAprovacao() {
        Ideia ideia = ideiaEnviada("id-1", "op-1");
        ideia.setStatus(StatusIdeia.APROVADA);
        when(ideiaRepository.findById("id-1")).thenReturn(Optional.of(ideia));

        assertThatThrownBy(() -> ideiaService.aprovar("id-1",
                new AprovacaoRequest(Nivel.ALTO, Nivel.BAIXO), gestor))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja foi avaliada");

        verify(pontoService, never()).creditar(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    @DisplayName("rejeitar registra o motivo e nao credita pontos")
    void rejeitarRegistraMotivo() {
        Ideia ideia = ideiaEnviada("id-1", "op-1");
        when(ideiaRepository.findById("id-1")).thenReturn(Optional.of(ideia));
        when(ideiaRepository.save(any(Ideia.class))).thenAnswer(chamada -> chamada.getArgument(0));

        IdeiaResponse resposta = ideiaService.rejeitar("id-1",
                new RejeicaoRequest("Fora do escopo do ciclo"), gestor);

        assertThat(resposta.status()).isEqualTo("REJEITADA");
        assertThat(resposta.motivoRejeicao()).isEqualTo("Fora do escopo do ciclo");
        verify(pontoService, never()).creditar(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    @DisplayName("prioridade calculada segue 10 - (impacto*3 - esforco)")
    void calculaPrioridade() {
        Ideia ideia = ideiaEnviada("id-1", "op-1");
        when(ideiaRepository.findById("id-1")).thenReturn(Optional.of(ideia));
        when(ideiaRepository.save(any(Ideia.class))).thenAnswer(chamada -> chamada.getArgument(0));

        IdeiaResponse resposta = ideiaService.priorizar("id-1",
                new PriorizacaoRequest(Nivel.ALTO, Nivel.BAIXO, null), gestor);

        assertThat(resposta.prioridade()).isEqualTo(2);
    }

    @Test
    @DisplayName("prioridade informada pelo gestor prevalece sobre o calculo")
    void prioridadeManualPrevalece() {
        Ideia ideia = ideiaEnviada("id-1", "op-1");
        when(ideiaRepository.findById("id-1")).thenReturn(Optional.of(ideia));
        when(ideiaRepository.save(any(Ideia.class))).thenAnswer(chamada -> chamada.getArgument(0));

        IdeiaResponse resposta = ideiaService.priorizar("id-1",
                new PriorizacaoRequest(Nivel.BAIXO, Nivel.ALTO, 1), gestor);

        assertThat(resposta.prioridade()).isEqualTo(1);
    }

    @Test
    @DisplayName("operador nao altera ideia de outro autor")
    void naoAlteraIdeiaDeOutro() {
        Ideia ideia = ideiaEnviada("id-1", "op-2");
        when(ideiaRepository.findById("id-1")).thenReturn(Optional.of(ideia));

        assertThatThrownBy(() -> ideiaService.atualizar("id-1",
                new IdeiaRequest("Novo", "Nova descricao", "Eficiencia", "or-1"), operador))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("operador nao exclui ideia que ja foi avaliada")
    void naoExcluiIdeiaAvaliada() {
        Ideia ideia = ideiaEnviada("id-1", "op-1");
        ideia.setStatus(StatusIdeia.APROVADA);
        when(ideiaRepository.findById("id-1")).thenReturn(Optional.of(ideia));

        assertThatThrownBy(() -> ideiaService.excluir("id-1", operador))
                .isInstanceOf(RegraNegocioException.class);

        verify(ideiaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("ideia inexistente devolve nao encontrado")
    void ideiaInexistente() {
        when(ideiaRepository.findById("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ideiaService.buscar("nao-existe"))
                .isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    @DisplayName("listagem ordena por prioridade, com nao priorizadas no fim")
    void listagemOrdenaPorPrioridade() {
        Ideia semPrioridade = ideiaEnviada("id-1", "op-1");
        semPrioridade.setDataEnvio(Instant.now());

        Ideia prioridadeAlta = ideiaEnviada("id-2", "op-1");
        prioridadeAlta.setPrioridade(2);
        prioridadeAlta.setDataEnvio(Instant.now().minus(2, ChronoUnit.DAYS));

        Ideia prioridadeBaixa = ideiaEnviada("id-3", "op-1");
        prioridadeBaixa.setPrioridade(8);
        prioridadeBaixa.setDataEnvio(Instant.now().minus(1, ChronoUnit.DAYS));

        when(ideiaRepository.findAllByOrderByDataEnvioDesc())
                .thenReturn(List.of(semPrioridade, prioridadeBaixa, prioridadeAlta));

        List<IdeiaResponse> resposta = ideiaService.listar(null);

        assertThat(resposta).extracting(IdeiaResponse::id).containsExactly("id-2", "id-3", "id-1");
    }

    @Test
    @DisplayName("filtro por status consulta apenas aquele status")
    void listagemFiltraPorStatus() {
        when(ideiaRepository.findByStatusOrderByDataEnvioDesc(eq(StatusIdeia.ENVIADA)))
                .thenReturn(List.of(ideiaEnviada("id-1", "op-1")));

        List<IdeiaResponse> resposta = ideiaService.listar(StatusIdeia.ENVIADA);

        assertThat(resposta).hasSize(1);
        verify(ideiaRepository, never()).findAllByOrderByDataEnvioDesc();
    }

    private Orientacao orientacao(boolean ativa) {
        Orientacao orientacao = new Orientacao();
        orientacao.setId("or-1");
        orientacao.setTitulo("Reduzir custos");
        orientacao.setDescricao("Descricao da orientacao");
        orientacao.setCategoria("Eficiencia Operacional");
        orientacao.setAtivo(ativa);
        return orientacao;
    }

    private Ideia ideiaEnviada(String id, String autorId) {
        Ideia ideia = new Ideia();
        ideia.setId(id);
        ideia.setTitulo("Titulo");
        ideia.setDescricao("Descricao");
        ideia.setCategoria("Eficiencia");
        ideia.setStatus(StatusIdeia.ENVIADA);
        ideia.setAutorId(autorId);
        ideia.setAutorNome("Operador Demo");
        ideia.setOrientacaoId("or-1");
        ideia.setOrientacaoTitulo("Reduzir custos");
        return ideia;
    }

    private UsuarioAutenticado autenticado(String id, String nome, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail(nome.toLowerCase().replace(" ", ".") + "@inovagab.com");
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        return new UsuarioAutenticado(usuario);
    }
}
