package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.domain.Orientacao;
import br.com.fiap.inovagab.domain.Perfil;
import br.com.fiap.inovagab.domain.Usuario;
import br.com.fiap.inovagab.dto.OrientacaoDtos.OrientacaoRequest;
import br.com.fiap.inovagab.dto.OrientacaoDtos.OrientacaoResponse;
import br.com.fiap.inovagab.exception.ApiExceptions.NaoEncontradoException;
import br.com.fiap.inovagab.exception.ApiExceptions.RegraNegocioException;
import br.com.fiap.inovagab.repository.OrientacaoRepository;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrientacaoServiceTest {

    @Mock
    private OrientacaoRepository orientacaoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private OrientacaoService orientacaoService;

    private UsuarioAutenticado lider;

    @BeforeEach
    void preparar() {
        orientacaoService = new OrientacaoService(orientacaoRepository, auditoriaService);

        Usuario usuario = new Usuario();
        usuario.setId("li-1");
        usuario.setNome("Lider Demo");
        usuario.setEmail("lider@inovagab.com");
        usuario.setSenhaHash("hash");
        usuario.setPerfil(Perfil.LIDER);
        lider = new UsuarioAutenticado(usuario);
    }

    @Test
    @DisplayName("orientacao criada nasce ativa e com o autor preenchido")
    void criarNasceAtiva() {
        when(orientacaoRepository.save(any(Orientacao.class))).thenAnswer(chamada -> {
            Orientacao orientacao = chamada.getArgument(0);
            orientacao.setId("or-1");
            return orientacao;
        });

        OrientacaoResponse resposta = orientacaoService.criar(new OrientacaoRequest(
                "Reduzir custos", "Descricao", "Eficiencia Operacional", "Ciclo 2026"), lider);

        assertThat(resposta.ativo()).isTrue();
        assertThat(resposta.autorNome()).isEqualTo("Lider Demo");
        assertThat(resposta.campanha()).isEqualTo("Ciclo 2026");
        verify(auditoriaService).registrar(lider, "CRIAR", "orientacao", "or-1", "Reduzir custos");
    }

    @Test
    @DisplayName("desativar tira das vigentes mas mantem no historico")
    void desativarMantemHistorico() {
        Orientacao orientacao = orientacao(true);
        when(orientacaoRepository.findById("or-1")).thenReturn(Optional.of(orientacao));
        when(orientacaoRepository.findAllByOrderByDataCriacaoDesc()).thenReturn(List.of(orientacao));

        orientacaoService.desativar("or-1", lider);

        assertThat(orientacao.isAtivo()).isFalse();
        assertThat(orientacao.getDataAtualizacao()).isNotNull();
        assertThat(orientacaoService.historico(null)).hasSize(1);
        verify(orientacaoRepository).save(orientacao);
    }

    @Test
    @DisplayName("nao desativa duas vezes a mesma orientacao")
    void naoDesativaDuasVezes() {
        when(orientacaoRepository.findById("or-1")).thenReturn(Optional.of(orientacao(false)));

        assertThatThrownBy(() -> orientacaoService.desativar("or-1", lider))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("historico filtra por categoria quando informada")
    void historicoFiltraPorCategoria() {
        when(orientacaoRepository.findByCategoriaIgnoreCaseOrderByDataCriacaoDesc("Eficiencia Operacional"))
                .thenReturn(List.of(orientacao(true)));

        List<OrientacaoResponse> historico = orientacaoService.historico("Eficiencia Operacional");

        assertThat(historico).hasSize(1);
        verify(orientacaoRepository, org.mockito.Mockito.never()).findAllByOrderByDataCriacaoDesc();
    }

    @Test
    @DisplayName("orientacao inexistente devolve nao encontrado")
    void orientacaoInexistente() {
        when(orientacaoRepository.findById("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orientacaoService.buscar("nao-existe"))
                .isInstanceOf(NaoEncontradoException.class);
    }

    private Orientacao orientacao(boolean ativa) {
        Orientacao orientacao = new Orientacao();
        orientacao.setId("or-1");
        orientacao.setTitulo("Reduzir custos");
        orientacao.setDescricao("Descricao");
        orientacao.setCategoria("Eficiencia Operacional");
        orientacao.setCampanha("Ciclo 2026");
        orientacao.setAtivo(ativa);
        return orientacao;
    }
}
