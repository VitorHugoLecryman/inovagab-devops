package br.com.fiap.inovagab.dto;

import br.com.fiap.inovagab.domain.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "informe o e-mail")
            @Email(message = "e-mail invalido")
            String email,

            @NotBlank(message = "informe a senha")
            String senha) {
    }

    public record LoginResponse(String token, long expiraEmMinutos, UsuarioResponse usuario) {
    }

    public record UsuarioResponse(String id, String nome, String email, String perfil, int pontos, boolean ativo) {

        public static UsuarioResponse de(Usuario usuario) {
            return new UsuarioResponse(
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getEmail(),
                    usuario.getPerfil().name(),
                    usuario.getPontos(),
                    usuario.isAtivo());
        }
    }
}
