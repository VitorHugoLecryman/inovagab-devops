package br.com.fiap.inovagab.repository;

import br.com.fiap.inovagab.domain.PontoTransacao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PontoTransacaoRepository extends MongoRepository<PontoTransacao, String> {

    List<PontoTransacao> findByUsuarioIdOrderByDataDesc(String usuarioId);
}
