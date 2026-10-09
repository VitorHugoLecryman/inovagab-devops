package br.com.fiap.inovagab.controller;

import br.com.fiap.inovagab.domain.Usuario;
import br.com.fiap.inovagab.dto.AuthDtos.LoginRequest;
import br.com.fiap.inovagab.dto.AuthDtos.LoginResponse;
import br.com.fiap.inovagab.dto.AuthDtos.UsuarioResponse;
import br.com.fiap.inovagab.exception.ApiExceptions.NaoEncontradoException;
import br.com.fiap.inovagab.repository.UsuarioRepository;
import br.com.fiap.inovagab.security.JwtService;
import br.com.fiap.inovagab.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacao")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                          UsuarioRepository usuarioRepository,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o usuario e devolve o token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new NaoEncontradoException("Usuario nao encontrado: " + request.email()));

        String token = jwtService.gerarToken(usuario);
        log.info("login realizado email={} perfil={}", usuario.getEmail(), usuario.getPerfil());
        return ResponseEntity.ok(new LoginResponse(token, jwtService.getExpiracaoMinutos(),
                UsuarioResponse.de(usuario)));
    }

    @GetMapping("/me")
    @Operation(summary = "Devolve os dados do usuario autenticado")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        Usuario usuario = usuarioRepository.findById(autenticado.getId())
                .orElseThrow(() -> new NaoEncontradoException("Usuario nao encontrado: " + autenticado.getId()));
        return ResponseEntity.ok(UsuarioResponse.de(usuario));
    }
}
