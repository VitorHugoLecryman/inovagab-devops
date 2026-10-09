package br.com.fiap.inovagab.repository;

import br.com.fiap.inovagab.domain.Orientacao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrientacaoRepository extends MongoRepository<Orientacao, String> {

    List<Orientacao> findByAtivoTrueOrderByDataCriacaoDesc();

    List<Orientacao> findAllByOrderByDataCriacaoDesc();

    List<Orientacao> findByCategoriaIgnoreCaseOrderByDataCriacaoDesc(String categoria);
}
