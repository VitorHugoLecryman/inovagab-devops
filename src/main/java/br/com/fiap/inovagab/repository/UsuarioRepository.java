package br.com.fiap.inovagab.repository;

import br.com.fiap.inovagab.domain.Perfil;
import br.com.fiap.inovagab.domain.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByPerfilAndAtivoTrueOrderByPontosDesc(Perfil perfil, Pageable paginacao);
}
