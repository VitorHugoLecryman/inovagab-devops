package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.domain.Perfil;
import br.com.fiap.inovagab.domain.PontoTransacao;
import br.com.fiap.inovagab.domain.Usuario;
import br.com.fiap.inovagab.dto.PontoDtos.RankingItemResponse;
import br.com.fiap.inovagab.dto.PontoDtos.TransacaoResponse;
import br.com.fiap.inovagab.repository.PontoTransacaoRepository;
import br.com.fiap.inovagab.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PontoService {

    private static final Logger log = LoggerFactory.getLogger(PontoService.class);

    private final PontoTransacaoRepository pontoTransacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MongoTemplate mongoTemplate;

    public PontoService(PontoTransacaoRepository pontoTransacaoRepository,
                        UsuarioRepository usuarioRepository,
                        MongoTemplate mongoTemplate) {
        this.pontoTransacaoRepository = pontoTransacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public void creditar(String usuarioId, int pontos, String motivo, String ideiaId) {
        PontoTransacao transacao = new PontoTransacao();
        transacao.setUsuarioId(usuarioId);
        transacao.setPontos(pontos);
        transacao.setMotivo(motivo);
        transacao.setIdeiaId(ideiaId);
        pontoTransacaoRepository.save(transacao);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(usuarioId)),
                new Update().inc("pontos", pontos),
                Usuario.class);

        log.info("pontos creditados usuarioId={} pontos={} motivo={} ideiaId={}", usuarioId, pontos, motivo, ideiaId);
    }

    public List<TransacaoResponse> historico(String usuarioId) {
        return pontoTransacaoRepository.findByUsuarioIdOrderByDataDesc(usuarioId).stream()
                .map(TransacaoResponse::de)
                .toList();
    }

    public List<RankingItemResponse> ranking(int limite) {
        int tamanho = Math.max(1, Math.min(limite, 100));
        List<Usuario> operadores = usuarioRepository.findByPerfilAndAtivoTrueOrderByPontosDesc(
                Perfil.OPERADOR, PageRequest.of(0, tamanho));

        List<RankingItemResponse> ranking = new ArrayList<>();
        int posicao = 1;
        for (Usuario operador : operadores) {
            ranking.add(new RankingItemResponse(posicao++, operador.getId(), operador.getNome(), operador.getPontos()));
        }
        return ranking;
    }
}
