package br.com.fiap.inovagab.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "ideias")
public class Ideia {

    @Id
    private String id;

    private String titulo;

    private String descricao;

    private String categoria;

    private StatusIdeia status = StatusIdeia.ENVIADA;

    private String orientacaoId;

    private String orientacaoTitulo;

    private String autorId;

    private String autorNome;

    private Instant dataEnvio = Instant.now();

    private Nivel impacto;

    private Nivel esforco;

    private Integer prioridade;

    private String motivoRejeicao;

    private String avaliadorId;

    private String avaliadorNome;

    private Instant dataAvaliacao;

    private Integer scoreIa;

    private String justificativaIa;

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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public StatusIdeia getStatus() {
        return status;
    }

    public void setStatus(StatusIdeia status) {
        this.status = status;
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

    public String getAutorId() {
        return autorId;
    }

    public void setAutorId(String autorId) {
        this.autorId = autorId;
    }

    public String getAutorNome() {
        return autorNome;
    }

    public void setAutorNome(String autorNome) {
        this.autorNome = autorNome;
    }

    public Instant getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(Instant dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public Nivel getImpacto() {
        return impacto;
    }

    public void setImpacto(Nivel impacto) {
        this.impacto = impacto;
    }

    public Nivel getEsforco() {
        return esforco;
    }

    public void setEsforco(Nivel esforco) {
        this.esforco = esforco;
    }

    public Integer getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(Integer prioridade) {
        this.prioridade = prioridade;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
    }

    public String getAvaliadorId() {
        return avaliadorId;
    }

    public void setAvaliadorId(String avaliadorId) {
        this.avaliadorId = avaliadorId;
    }

    public String getAvaliadorNome() {
        return avaliadorNome;
    }

    public void setAvaliadorNome(String avaliadorNome) {
        this.avaliadorNome = avaliadorNome;
    }

    public Instant getDataAvaliacao() {
        return dataAvaliacao;
    }

    public void setDataAvaliacao(Instant dataAvaliacao) {
        this.dataAvaliacao = dataAvaliacao;
    }

    public Integer getScoreIa() {
        return scoreIa;
    }

    public void setScoreIa(Integer scoreIa) {
        this.scoreIa = scoreIa;
    }

    public String getJustificativaIa() {
        return justificativaIa;
    }

    public void setJustificativaIa(String justificativaIa) {
        this.justificativaIa = justificativaIa;
    }
}
