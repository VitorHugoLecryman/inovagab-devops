package br.com.fiap.inovagab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest requisicao,
                                    @NonNull HttpServletResponse resposta,
                                    @NonNull FilterChain cadeia) throws ServletException, IOException {
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        String token = cabecalho.substring(PREFIXO.length()).trim();

        if (!jwtService.tokenValido(token) || SecurityContextHolder.getContext().getAuthentication() != null) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        try {
            String email = jwtService.extrairEmail(token);
            UserDetails usuario = usuarioDetailsService.loadUserByUsername(email);

            if (usuario.isEnabled()) {
                UsernamePasswordAuthenticationToken autenticacao = new UsernamePasswordAuthenticationToken(
                        usuario, null, usuario.getAuthorities());
                autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(requisicao));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            }
        } catch (Exception excecao) {
            SecurityContextHolder.clearContext();
            logger.debug("token recusado motivo=" + excecao.getClass().getSimpleName());
        }

        cadeia.doFilter(requisicao, resposta);
    }
}
