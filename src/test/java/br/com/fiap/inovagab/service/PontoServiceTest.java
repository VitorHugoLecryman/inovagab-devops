package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.domain.Perfil;
import br.com.fiap.inovagab.domain.PontoTransacao;
import br.com.fiap.inovagab.domain.Usuario;
import br.com.fiap.inovagab.dto.PontoDtos.RankingItemResponse;
import br.com.fiap.inovagab.dto.PontoDtos.TransacaoResponse;
import br.com.fiap.inovagab.repository.PontoTransacaoRepository;
import br.com.fiap.inovagab.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PontoServiceTest {

    @Mock
    private PontoTransacaoRepository pontoTransacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private PontoService pontoService;

    @Test
    @DisplayName("creditar registra a transacao e incrementa os pontos do usuario")
    void creditarRegistraTransacaoEIncrementa() {
        pontoService.creditar("op-1", 50, "Ideia Aprovada", "id-1");

        ArgumentCaptor<PontoTransacao> transacao = ArgumentCaptor.forClass(PontoTransacao.class);
        verify(pontoTransacaoRepository).save(transacao.capture());

        assertThat(transacao.getValue().getUsuarioId()).isEqualTo("op-1");
        assertThat(transacao.getValue().getPontos()).isEqualTo(50);
        assertThat(transacao.getValue().getMotivo()).isEqualTo("Ideia Aprovada");
        assertThat(transacao.getValue().getIdeiaId()).isEqualTo("id-1");

        ArgumentCaptor<Update> atualizacao = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), atualizacao.capture(), eq(Usuario.class));
        assertThat(atualizacao.getValue().toString()).contains("$inc").contains("pontos");
    }

    @Test
    @DisplayName("ranking lista apenas operadores com posicao a partir de 1")
    void rankingNumeraPosicoes() {
        when(usuarioRepository.findByPerfilAndAtivoTrueOrderByPontosDesc(eq(Perfil.OPERADOR), any(Pageable.class)))
                .thenReturn(List.of(
                        operador("op-1", "Ana", 120),
                        operador("op-2", "Bruno", 80),
                        operador("op-3", "Carla", 10)));

        List<RankingItemResponse> ranking = pontoService.ranking(10);

        assertThat(ranking).hasSize(3);
        assertThat(ranking.get(0).posicao()).isEqualTo(1);
        assertThat(ranking.get(0).nome()).isEqualTo("Ana");
        assertThat(ranking.get(2).posicao()).isEqualTo(3);
        assertThat(ranking.get(2).pontos()).isEqualTo(10);
    }

    @Test
    @DisplayName("historico devolve as transacoes do usuario")
    void historicoDoUsuario() {
        PontoTransacao transacao = new PontoTransacao();
        transacao.setId("tr-1");
        transacao.setUsuarioId("op-1");
        transacao.setPontos(10);
        transacao.setMotivo("Envio de Ideia");

        when(pontoTransacaoRepository.findByUsuarioIdOrderByDataDesc("op-1")).thenReturn(List.of(transacao));

        List<TransacaoResponse> historico = pontoService.historico("op-1");

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).motivo()).isEqualTo("Envio de Ideia");
    }

    private Usuario operador(String id, String nome, int pontos) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setPerfil(Perfil.OPERADOR);
        usuario.setPontos(pontos);
        return usuario;
    }
}
