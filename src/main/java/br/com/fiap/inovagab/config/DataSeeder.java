package br.com.fiap.inovagab.config;

import br.com.fiap.inovagab.domain.Orientacao;
import br.com.fiap.inovagab.domain.Perfil;
import br.com.fiap.inovagab.domain.Usuario;
import br.com.fiap.inovagab.repository.OrientacaoRepository;
import br.com.fiap.inovagab.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final OrientacaoRepository orientacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties propriedades;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      OrientacaoRepository orientacaoRepository,
                      PasswordEncoder passwordEncoder,
                      AppProperties propriedades) {
        this.usuarioRepository = usuarioRepository;
        this.orientacaoRepository = orientacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.propriedades = propriedades;
    }

    @Override
    public void run(String... args) {
        if (!propriedades.seed().enabled()) {
            return;
        }

        criarUsuario("Operador Demo", "operador@inovagab.com", Perfil.OPERADOR);
        criarUsuario("Gestor Demo", "gestor@inovagab.com", Perfil.GESTOR);
        criarUsuario("Lider Demo", "lider@inovagab.com", Perfil.LIDER);

        if (orientacaoRepository.count() == 0) {
            usuarioRepository.findByEmailIgnoreCase("lider@inovagab.com").ifPresent(lider -> {
                Orientacao orientacao = new Orientacao();
                orientacao.setTitulo("Reduzir custos operacionais em 15%");
                orientacao.setDescricao("Iniciativas que diminuam custo de manutencao, combustivel e retrabalho na operacao.");
                orientacao.setCategoria("Eficiencia Operacional");
                orientacao.setCampanha("Ciclo 2026");
                orientacao.setAutorId(lider.getId());
                orientacao.setAutorNome(lider.getNome());
                orientacaoRepository.save(orientacao);
                log.info("orientacao inicial criada");
            });
        }
    }

    private void criarUsuario(String nome, String email, Perfil perfil) {
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        usuario.setSenhaHash(passwordEncoder.encode(propriedades.seed().senhaPadrao()));
        usuarioRepository.save(usuario);
        log.info("usuario seed criado email={} perfil={}", email, perfil);
    }
}
