package br.com.fiap.inovagab.repository;

import br.com.fiap.inovagab.domain.Ideia;
import br.com.fiap.inovagab.domain.StatusIdeia;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IdeiaRepository extends MongoRepository<Ideia, String> {

    List<Ideia> findByAutorIdOrderByDataEnvioDesc(String autorId);

    List<Ideia> findAllByOrderByDataEnvioDesc();

    List<Ideia> findByStatusOrderByDataEnvioDesc(StatusIdeia status);

    long countByStatus(StatusIdeia status);
}
