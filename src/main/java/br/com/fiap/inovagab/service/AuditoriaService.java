package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.domain.Auditoria;
import br.com.fiap.inovagab.repository.AuditoriaRepository;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(UsuarioAutenticado usuario, String acao, String entidade, String entidadeId, String detalhes) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuarioId(usuario.getId());
        auditoria.setUsuarioNome(usuario.getNome());
        auditoria.setPerfil(usuario.getPerfil());
        auditoria.setAcao(acao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setDetalhes(detalhes);
        auditoriaRepository.save(auditoria);
        log.info("auditoria registrada acao={} entidade={} entidadeId={} usuarioId={}",
                acao, entidade, entidadeId, usuario.getId());
    }

    public List<Auditoria> listar(String entidade, int limite) {
        PageRequest paginacao = PageRequest.of(0, Math.max(1, Math.min(limite, 500)));
        return (entidade == null || entidade.isBlank())
                ? auditoriaRepository.findAllByOrderByDataDesc(paginacao)
                : auditoriaRepository.findByEntidadeIgnoreCaseOrderByDataDesc(entidade, paginacao);
    }
}
