package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.config.AppProperties;
import br.com.fiap.inovagab.domain.Ideia;
import br.com.fiap.inovagab.domain.Nivel;
import br.com.fiap.inovagab.domain.Orientacao;
import br.com.fiap.inovagab.domain.StatusIdeia;
import br.com.fiap.inovagab.dto.IaDtos.AvaliacaoIa;
import br.com.fiap.inovagab.dto.IdeiaDtos.AprovacaoRequest;
import br.com.fiap.inovagab.dto.IdeiaDtos.AvaliacaoIaResponse;
import br.com.fiap.inovagab.dto.IdeiaDtos.IdeiaRequest;
import br.com.fiap.inovagab.dto.IdeiaDtos.IdeiaResponse;
import br.com.fiap.inovagab.dto.IdeiaDtos.PriorizacaoRequest;
import br.com.fiap.inovagab.dto.IdeiaDtos.RejeicaoRequest;
import br.com.fiap.inovagab.exception.ApiExceptions.AcessoNegadoException;
import br.com.fiap.inovagab.exception.ApiExceptions.NaoEncontradoException;
import br.com.fiap.inovagab.exception.ApiExceptions.RegraNegocioException;
import br.com.fiap.inovagab.repository.IdeiaRepository;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class IdeiaService {

    private static final Logger log = LoggerFactory.getLogger(IdeiaService.class);

    private final IdeiaRepository ideiaRepository;
    private final OrientacaoService orientacaoService;
    private final PontoService pontoService;
    private final AuditoriaService auditoriaService;
    private final IaService iaService;
    private final AppProperties propriedades;

    public IdeiaService(IdeiaRepository ideiaRepository,
                        OrientacaoService orientacaoService,
                        PontoService pontoService,
                        AuditoriaService auditoriaService,
                        IaService iaService,
                        AppProperties propriedades) {
        this.ideiaRepository = ideiaRepository;
        this.orientacaoService = orientacaoService;
        this.pontoService = pontoService;
        this.auditoriaService = auditoriaService;
        this.iaService = iaService;
        this.propriedades = propriedades;
    }

    public IdeiaResponse criar(IdeiaRequest request, UsuarioAutenticado autor) {
        Orientacao orientacao = orientacaoService.buscarEntidade(request.orientacaoId());
        if (!orientacao.isAtivo()) {
            throw new RegraNegocioException("A orientacao estrategica selecionada nao esta vigente");
        }

        Ideia ideia = new Ideia();
        ideia.setTitulo(request.titulo());
        ideia.setDescricao(request.descricao());
        ideia.setCategoria(request.categoria());
        ideia.setOrientacaoId(orientacao.getId());
        ideia.setOrientacaoTitulo(orientacao.getTitulo());
        ideia.setAutorId(autor.getId());
        ideia.setAutorNome(autor.getNome());
        ideia.setStatus(StatusIdeia.ENVIADA);

        Ideia salva = ideiaRepository.save(ideia);
        pontoService.creditar(autor.getId(), propriedades.pontos().envioIdeia(), "Envio de Ideia", salva.getId());
        auditoriaService.registrar(autor, "CRIAR", "ideia", salva.getId(), salva.getTitulo());
        log.info("ideia criada id={} autorId={} orientacaoId={}", salva.getId(), autor.getId(), orientacao.getId());
        return IdeiaResponse.de(salva);
    }

    public List<IdeiaResponse> listarMinhas(String autorId) {
        return ideiaRepository.findByAutorIdOrderByDataEnvioDesc(autorId).stream()
                .map(IdeiaResponse::de)
                .toList();
    }

    public List<IdeiaResponse> listar(StatusIdeia status) {
        List<Ideia> ideias = (status == null)
                ? ideiaRepository.findAllByOrderByDataEnvioDesc()
                : ideiaRepository.findByStatusOrderByDataEnvioDesc(status);

        return ideias.stream()
                .sorted(Comparator
                        .comparing((Ideia ideia) -> ideia.getPrioridade() == null ? Integer.MAX_VALUE : ideia.getPrioridade())
                        .thenComparing(Ideia::getDataEnvio, Comparator.reverseOrder()))
                .map(IdeiaResponse::de)
                .toList();
    }

    public IdeiaResponse buscar(String id) {
        return IdeiaResponse.de(buscarEntidade(id));
    }

    public IdeiaResponse atualizar(String id, IdeiaRequest request, UsuarioAutenticado autor) {
        Ideia ideia = buscarEntidade(id);
        garantirAutoria(ideia, autor);
        garantirEditavel(ideia);

        Orientacao orientacao = orientacaoService.buscarEntidade(request.orientacaoId());
        if (!orientacao.isAtivo()) {
            throw new RegraNegocioException("A orientacao estrategica selecionada nao esta vigente");
        }

        ideia.setTitulo(request.titulo());
        ideia.setDescricao(request.descricao());
        ideia.setCategoria(request.categoria());
        ideia.setOrientacaoId(orientacao.getId());
        ideia.setOrientacaoTitulo(orientacao.getTitulo());

        Ideia salva = ideiaRepository.save(ideia);
        auditoriaService.registrar(autor, "ATUALIZAR", "ideia", id, salva.getTitulo());
        return IdeiaResponse.de(salva);
    }

    public void excluir(String id, UsuarioAutenticado autor) {
        Ideia ideia = buscarEntidade(id);
        garantirAutoria(ideia, autor);
        garantirEditavel(ideia);
        ideiaRepository.delete(ideia);
        auditoriaService.registrar(autor, "EXCLUIR", "ideia", id, ideia.getTitulo());
        log.info("ideia excluida id={} autorId={}", id, autor.getId());
    }

    public IdeiaResponse priorizar(String id, PriorizacaoRequest request, UsuarioAutenticado gestor) {
        Ideia ideia = buscarEntidade(id);
        if (ideia.getStatus() == StatusIdeia.REJEITADA) {
            throw new RegraNegocioException("Nao e possivel priorizar uma ideia rejeitada");
        }
        ideia.setImpacto(request.impacto());
        ideia.setEsforco(request.esforco());
        ideia.setPrioridade(request.prioridade() != null
                ? request.prioridade()
                : calcularPrioridade(request.impacto(), request.esforco()));
        ideia.setAvaliadorId(gestor.getId());
        ideia.setAvaliadorNome(gestor.getNome());

        Ideia salva = ideiaRepository.save(ideia);
        auditoriaService.registrar(gestor, "PRIORIZAR", "ideia", id, "prioridade=" + salva.getPrioridade());
        log.info("ideia priorizada id={} prioridade={} gestorId={}", id, salva.getPrioridade(), gestor.getId());
        return IdeiaResponse.de(salva);
    }

    public IdeiaResponse aprovar(String id, AprovacaoRequest request, UsuarioAutenticado gestor) {
        Ideia ideia = buscarEntidade(id);
        garantirPendente(ideia);

        ideia.setStatus(StatusIdeia.APROVADA);
        ideia.setImpacto(request.impacto());
        ideia.setEsforco(request.esforco());
        ideia.setPrioridade(calcularPrioridade(request.impacto(), request.esforco()));
        ideia.setAvaliadorId(gestor.getId());
        ideia.setAvaliadorNome(gestor.getNome());
        ideia.setDataAvaliacao(Instant.now());

        Ideia salva = ideiaRepository.save(ideia);
        pontoService.creditar(salva.getAutorId(), propriedades.pontos().ideiaAprovada(), "Ideia Aprovada", salva.getId());
        auditoriaService.registrar(gestor, "APROVAR", "ideia", id, salva.getTitulo());
        log.info("ideia aprovada id={} gestorId={}", id, gestor.getId());
        return IdeiaResponse.de(salva);
    }

    public IdeiaResponse rejeitar(String id, RejeicaoRequest request, UsuarioAutenticado gestor) {
        Ideia ideia = buscarEntidade(id);
        garantirPendente(ideia);

        ideia.setStatus(StatusIdeia.REJEITADA);
        ideia.setMotivoRejeicao(request.motivo());
        ideia.setAvaliadorId(gestor.getId());
        ideia.setAvaliadorNome(gestor.getNome());
        ideia.setDataAvaliacao(Instant.now());

        Ideia salva = ideiaRepository.save(ideia);
        auditoriaService.registrar(gestor, "REJEITAR", "ideia", id, request.motivo());
        log.info("ideia rejeitada id={} gestorId={}", id, gestor.getId());
        return IdeiaResponse.de(salva);
    }

    public AvaliacaoIaResponse avaliarComIa(String id, UsuarioAutenticado gestor) {
        Ideia ideia = buscarEntidade(id);
        Orientacao orientacao = orientacaoService.buscarEntidade(ideia.getOrientacaoId());

        AvaliacaoIa avaliacao = iaService.avaliarIdeia(ideia, orientacao);
        ideia.setScoreIa(avaliacao.score());
        ideia.setJustificativaIa(avaliacao.justificativa());
        ideiaRepository.save(ideia);

        auditoriaService.registrar(gestor, "AVALIAR_IA", "ideia", id, "score=" + avaliacao.score());
        log.info("ideia avaliada por ia id={} score={} gestorId={}", id, avaliacao.score(), gestor.getId());
        return new AvaliacaoIaResponse(id, avaliacao.score(), avaliacao.justificativa(), iaService.getModelo());
    }

    public Ideia buscarEntidade(String id) {
        return ideiaRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Ideia nao encontrada: " + id));
    }

    private int calcularPrioridade(Nivel impacto, Nivel esforco) {
        int pesoImpacto = switch (impacto) {
            case ALTO -> 3;
            case MEDIO -> 2;
            case BAIXO -> 1;
        };
        int pesoEsforco = switch (esforco) {
            case ALTO -> 3;
            case MEDIO -> 2;
            case BAIXO -> 1;
        };
        return (10 - (pesoImpacto * 3 - pesoEsforco));
    }

    private void garantirAutoria(Ideia ideia, UsuarioAutenticado autor) {
        if (!ideia.getAutorId().equals(autor.getId())) {
            throw new AcessoNegadoException("Voce so pode alterar as suas proprias ideias");
        }
    }

    private void garantirEditavel(Ideia ideia) {
        if (ideia.getStatus() != StatusIdeia.ENVIADA) {
            throw new RegraNegocioException("A ideia ja foi avaliada e nao pode mais ser alterada");
        }
    }

    private void garantirPendente(Ideia ideia) {
        if (ideia.getStatus() != StatusIdeia.ENVIADA) {
            throw new RegraNegocioException("A ideia ja foi avaliada anteriormente");
        }
    }
}
