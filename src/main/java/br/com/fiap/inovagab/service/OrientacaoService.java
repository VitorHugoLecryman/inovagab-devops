package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.domain.Orientacao;
import br.com.fiap.inovagab.dto.OrientacaoDtos.OrientacaoRequest;
import br.com.fiap.inovagab.dto.OrientacaoDtos.OrientacaoResponse;
import br.com.fiap.inovagab.exception.ApiExceptions.NaoEncontradoException;
import br.com.fiap.inovagab.exception.ApiExceptions.RegraNegocioException;
import br.com.fiap.inovagab.repository.OrientacaoRepository;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class OrientacaoService {

    private static final Logger log = LoggerFactory.getLogger(OrientacaoService.class);

    private final OrientacaoRepository orientacaoRepository;
    private final AuditoriaService auditoriaService;

    public OrientacaoService(OrientacaoRepository orientacaoRepository, AuditoriaService auditoriaService) {
        this.orientacaoRepository = orientacaoRepository;
        this.auditoriaService = auditoriaService;
    }

    public OrientacaoResponse criar(OrientacaoRequest request, UsuarioAutenticado lider) {
        Orientacao orientacao = new Orientacao();
        orientacao.setTitulo(request.titulo());
        orientacao.setDescricao(request.descricao());
        orientacao.setCategoria(request.categoria());
        orientacao.setCampanha(request.campanha());
        orientacao.setAutorId(lider.getId());
        orientacao.setAutorNome(lider.getNome());

        Orientacao salva = orientacaoRepository.save(orientacao);
        auditoriaService.registrar(lider, "CRIAR", "orientacao", salva.getId(), salva.getTitulo());
        log.info("orientacao criada id={} autorId={}", salva.getId(), lider.getId());
        return OrientacaoResponse.de(salva);
    }

    public List<OrientacaoResponse> listarVigentes() {
        return orientacaoRepository.findByAtivoTrueOrderByDataCriacaoDesc().stream()
                .map(OrientacaoResponse::de)
                .toList();
    }

    public List<OrientacaoResponse> historico(String categoria) {
        List<Orientacao> orientacoes = (categoria == null || categoria.isBlank())
                ? orientacaoRepository.findAllByOrderByDataCriacaoDesc()
                : orientacaoRepository.findByCategoriaIgnoreCaseOrderByDataCriacaoDesc(categoria);

        return orientacoes.stream()
                .map(OrientacaoResponse::de)
                .toList();
    }

    public OrientacaoResponse buscar(String id) {
        return OrientacaoResponse.de(buscarEntidade(id));
    }

    public OrientacaoResponse atualizar(String id, OrientacaoRequest request, UsuarioAutenticado lider) {
        Orientacao orientacao = buscarEntidade(id);
        orientacao.setTitulo(request.titulo());
        orientacao.setDescricao(request.descricao());
        orientacao.setCategoria(request.categoria());
        orientacao.setCampanha(request.campanha());
        orientacao.setDataAtualizacao(Instant.now());

        Orientacao salva = orientacaoRepository.save(orientacao);
        auditoriaService.registrar(lider, "ATUALIZAR", "orientacao", id, salva.getTitulo());
        log.info("orientacao atualizada id={} liderId={}", id, lider.getId());
        return OrientacaoResponse.de(salva);
    }

    public void desativar(String id, UsuarioAutenticado lider) {
        Orientacao orientacao = buscarEntidade(id);
        if (!orientacao.isAtivo()) {
            throw new RegraNegocioException("A orientacao ja esta desativada");
        }
        orientacao.setAtivo(false);
        orientacao.setDataAtualizacao(Instant.now());
        orientacaoRepository.save(orientacao);
        auditoriaService.registrar(lider, "DESATIVAR", "orientacao", id, orientacao.getTitulo());
        log.info("orientacao desativada id={} liderId={}", id, lider.getId());
    }

    public Orientacao buscarEntidade(String id) {
        return orientacaoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Orientacao estrategica nao encontrada: " + id));
    }
}
