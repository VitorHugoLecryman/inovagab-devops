package br.com.fiap.inovagab.service;

import br.com.fiap.inovagab.domain.EtapaProjeto;
import br.com.fiap.inovagab.domain.Ideia;
import br.com.fiap.inovagab.domain.Orientacao;
import br.com.fiap.inovagab.domain.Projeto;
import br.com.fiap.inovagab.domain.StatusIdeia;
import br.com.fiap.inovagab.dto.ProjetoDtos.EtapaRequest;
import br.com.fiap.inovagab.dto.ProjetoDtos.ProjetoRequest;
import br.com.fiap.inovagab.dto.ProjetoDtos.ProjetoResponse;
import br.com.fiap.inovagab.dto.ProjetoDtos.ResultadosRequest;
import br.com.fiap.inovagab.exception.ApiExceptions.NaoEncontradoException;
import br.com.fiap.inovagab.exception.ApiExceptions.RegraNegocioException;
import br.com.fiap.inovagab.repository.ProjetoRepository;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjetoService {

    private static final Logger log = LoggerFactory.getLogger(ProjetoService.class);

    private final ProjetoRepository projetoRepository;
    private final IdeiaService ideiaService;
    private final OrientacaoService orientacaoService;
    private final AuditoriaService auditoriaService;

    public ProjetoService(ProjetoRepository projetoRepository,
                          IdeiaService ideiaService,
                          OrientacaoService orientacaoService,
                          AuditoriaService auditoriaService) {
        this.projetoRepository = projetoRepository;
        this.ideiaService = ideiaService;
        this.orientacaoService = orientacaoService;
        this.auditoriaService = auditoriaService;
    }

    public ProjetoResponse criar(ProjetoRequest request, UsuarioAutenticado gestor) {
        Projeto projeto = new Projeto();
        projeto.setTitulo(request.titulo());
        projeto.setDescricao(request.descricao());
        projeto.setOrcamento(request.orcamento());
        projeto.setPrazo(request.prazo());
        projeto.setStatus(request.status());
        projeto.setObservacoes(request.observacoes());
        projeto.setGestorId(gestor.getId());
        projeto.setGestorNome(gestor.getNome());
        projeto.setEtapa(EtapaProjeto.PLANEJAMENTO);

        vincularOrigem(projeto, request);

        Projeto salvo = projetoRepository.save(projeto);
        auditoriaService.registrar(gestor, "CRIAR", "projeto", salvo.getId(), salvo.getTitulo());
        log.info("projeto criado id={} gestorId={} orientacaoId={}", salvo.getId(), gestor.getId(), salvo.getOrientacaoId());
        return ProjetoResponse.de(salvo);
    }

    public List<ProjetoResponse> listar() {
        return projetoRepository.findAllByOrderByDataCriacaoDesc().stream()
                .map(ProjetoResponse::de)
                .toList();
    }

    public ProjetoResponse buscar(String id) {
        return ProjetoResponse.de(buscarEntidade(id));
    }

    public ProjetoResponse atualizar(String id, ProjetoRequest request, UsuarioAutenticado gestor) {
        Projeto projeto = buscarEntidade(id);
        garantirEditavel(projeto);

        projeto.setTitulo(request.titulo());
        projeto.setDescricao(request.descricao());
        projeto.setOrcamento(request.orcamento());
        projeto.setPrazo(request.prazo());
        projeto.setStatus(request.status());
        projeto.setObservacoes(request.observacoes());

        if (request.orientacaoId() != null && !request.orientacaoId().isBlank()
                && !request.orientacaoId().equals(projeto.getOrientacaoId())) {
            Orientacao orientacao = orientacaoService.buscarEntidade(request.orientacaoId());
            projeto.setOrientacaoId(orientacao.getId());
            projeto.setOrientacaoTitulo(orientacao.getTitulo());
        }

        Projeto salvo = projetoRepository.save(projeto);
        auditoriaService.registrar(gestor, "ATUALIZAR", "projeto", id, salvo.getTitulo());
        log.info("projeto atualizado id={} gestorId={}", id, gestor.getId());
        return ProjetoResponse.de(salvo);
    }

    public void excluir(String id, UsuarioAutenticado gestor) {
        Projeto projeto = buscarEntidade(id);
        if (projeto.getEtapa() == EtapaProjeto.CONCLUIDO) {
            throw new RegraNegocioException("Um projeto concluido nao pode ser excluido");
        }
        projetoRepository.delete(projeto);
        auditoriaService.registrar(gestor, "EXCLUIR", "projeto", id, projeto.getTitulo());
        log.info("projeto excluido id={} gestorId={}", id, gestor.getId());
    }

    public ProjetoResponse alterarEtapa(String id, EtapaRequest request, UsuarioAutenticado gestor) {
        Projeto projeto = buscarEntidade(id);

        if (projeto.getEtapa() == request.etapa()) {
            throw new RegraNegocioException("O projeto ja esta na etapa " + request.etapa().name());
        }
        if (projeto.getEtapa() == EtapaProjeto.CONCLUIDO) {
            throw new RegraNegocioException("Um projeto concluido nao pode mudar de etapa");
        }

        projeto.setEtapa(request.etapa());
        Projeto salvo = projetoRepository.save(projeto);
        auditoriaService.registrar(gestor, "ALTERAR_ETAPA", "projeto", id, request.etapa().name());
        log.info("projeto mudou de etapa id={} etapa={} gestorId={}", id, request.etapa(), gestor.getId());
        return ProjetoResponse.de(salvo);
    }

    public ProjetoResponse registrarResultados(String id, ResultadosRequest request, UsuarioAutenticado gestor) {
        Projeto projeto = buscarEntidade(id);

        if (projeto.getEtapa() == EtapaProjeto.CANCELADO) {
            throw new RegraNegocioException("Um projeto cancelado nao aceita registro de resultados");
        }

        projeto.setRetornoFinanceiro(request.retornoFinanceiro());
        projeto.setGanhoProdutividade(request.ganhoProdutividade());
        projeto.setResultadosQualitativos(request.resultadosQualitativos());
        projeto.setEtapa(EtapaProjeto.CONCLUIDO);

        Projeto salvo = projetoRepository.save(projeto);
        auditoriaService.registrar(gestor, "REGISTRAR_RESULTADOS", "projeto", id,
                "retorno=" + request.retornoFinanceiro());
        log.info("resultados registrados projetoId={} retorno={} gestorId={}",
                id, request.retornoFinanceiro(), gestor.getId());
        return ProjetoResponse.de(salvo);
    }

    public Projeto buscarEntidade(String id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Projeto nao encontrado: " + id));
    }

    private void vincularOrigem(Projeto projeto, ProjetoRequest request) {
        boolean temIdeia = request.ideiaId() != null && !request.ideiaId().isBlank();
        boolean temOrientacao = request.orientacaoId() != null && !request.orientacaoId().isBlank();

        if (!temIdeia && !temOrientacao) {
            throw new RegraNegocioException("Informe a ideia aprovada ou a orientacao estrategica de origem");
        }

        if (temIdeia) {
            Ideia ideia = ideiaService.buscarEntidade(request.ideiaId());
            if (ideia.getStatus() != StatusIdeia.APROVADA) {
                throw new RegraNegocioException("So e possivel criar projeto a partir de uma ideia aprovada");
            }
            if (projetoRepository.existsByIdeiaId(ideia.getId())) {
                throw new RegraNegocioException("Esta ideia ja possui um projeto vinculado");
            }
            projeto.setIdeiaId(ideia.getId());
            projeto.setOrientacaoId(ideia.getOrientacaoId());
            projeto.setOrientacaoTitulo(ideia.getOrientacaoTitulo());
            return;
        }

        Orientacao orientacao = orientacaoService.buscarEntidade(request.orientacaoId());
        if (!orientacao.isAtivo()) {
            throw new RegraNegocioException("A orientacao estrategica selecionada nao esta vigente");
        }
        projeto.setOrientacaoId(orientacao.getId());
        projeto.setOrientacaoTitulo(orientacao.getTitulo());
    }

    private void garantirEditavel(Projeto projeto) {
        if (projeto.getEtapa() == EtapaProjeto.CONCLUIDO) {
            throw new RegraNegocioException("Um projeto concluido nao pode mais ser alterado");
        }
    }
}
