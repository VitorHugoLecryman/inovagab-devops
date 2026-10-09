package br.com.fiap.inovagab.repository;

import br.com.fiap.inovagab.domain.Auditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditoriaRepository extends MongoRepository<Auditoria, String> {

    List<Auditoria> findAllByOrderByDataDesc(Pageable paginacao);

    List<Auditoria> findByEntidadeIgnoreCaseOrderByDataDesc(String entidade, Pageable paginacao);
}
