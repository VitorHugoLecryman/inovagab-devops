package br.com.fiap.inovagab.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "projetos")
public class Projeto {

    @Id
    private String id;

    private String titulo;

    private String descricao;

    private String ideiaId;

    private String orientacaoId;

    private String orientacaoTitulo;

    private EtapaProjeto etapa = EtapaProjeto.PLANEJAMENTO;

    private String status;

    private double orcamento;

    private String prazo;

    private String gestorId;

    private String gestorNome;

    private Instant dataCriacao = Instant.now();

    private double retornoFinanceiro;

    private double ganhoProdutividade;

    private String resultadosQualitativos;

    private String observacoes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getIdeiaId() {
        return ideiaId;
    }

    public void setIdeiaId(String ideiaId) {
        this.ideiaId = ideiaId;
    }

    public String getOrientacaoId() {
        return orientacaoId;
    }

    public void setOrientacaoId(String orientacaoId) {
        this.orientacaoId = orientacaoId;
    }

    public String getOrientacaoTitulo() {
        return orientacaoTitulo;
    }

    public void setOrientacaoTitulo(String orientacaoTitulo) {
        this.orientacaoTitulo = orientacaoTitulo;
    }

    public EtapaProjeto getEtapa() {
        return etapa;
    }

    public void setEtapa(EtapaProjeto etapa) {
        this.etapa = etapa;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getOrcamento() {
        return orcamento;
    }

    public void setOrcamento(double orcamento) {
        this.orcamento = orcamento;
    }

    public String getPrazo() {
        return prazo;
    }

    public void setPrazo(String prazo) {
        this.prazo = prazo;
    }

    public String getGestorId() {
        return gestorId;
    }

    public void setGestorId(String gestorId) {
        this.gestorId = gestorId;
    }

    public String getGestorNome() {
        return gestorNome;
    }

    public void setGestorNome(String gestorNome) {
        this.gestorNome = gestorNome;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(Instant dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public double getRetornoFinanceiro() {
        return retornoFinanceiro;
    }

    public void setRetornoFinanceiro(double retornoFinanceiro) {
        this.retornoFinanceiro = retornoFinanceiro;
    }

    public double getGanhoProdutividade() {
        return ganhoProdutividade;
    }

    public void setGanhoProdutividade(double ganhoProdutividade) {
        this.ganhoProdutividade = ganhoProdutividade;
    }

    public String getResultadosQualitativos() {
        return resultadosQualitativos;
    }

    public void setResultadosQualitativos(String resultadosQualitativos) {
        this.resultadosQualitativos = resultadosQualitativos;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
