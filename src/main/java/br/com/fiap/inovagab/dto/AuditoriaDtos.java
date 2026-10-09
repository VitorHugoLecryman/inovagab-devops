package br.com.fiap.inovagab.dto;

import br.com.fiap.inovagab.domain.Auditoria;

import java.time.Instant;

public final class AuditoriaDtos {

    private AuditoriaDtos() {
    }

    public record AuditoriaResponse(
            String id,
            String usuarioId,
            String usuarioNome,
            String perfil,
            String acao,
            String entidade,
            String entidadeId,
            Instant data,
            String detalhes) {

        public static AuditoriaResponse de(Auditoria auditoria) {
            return new AuditoriaResponse(
                    auditoria.getId(),
                    auditoria.getUsuarioId(),
                    auditoria.getUsuarioNome(),
                    auditoria.getPerfil() == null ? null : auditoria.getPerfil().name(),
                    auditoria.getAcao(),
                    auditoria.getEntidade(),
                    auditoria.getEntidadeId(),
                    auditoria.getData(),
                    auditoria.getDetalhes());
        }
    }
}
