package br.com.fiap.inovagab.repository;

import br.com.fiap.inovagab.domain.Projeto;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProjetoRepository extends MongoRepository<Projeto, String> {

    List<Projeto> findAllByOrderByDataCriacaoDesc();

    List<Projeto> findByOrientacaoIdOrderByDataCriacaoDesc(String orientacaoId);

    boolean existsByIdeiaId(String ideiaId);
}
