package br.com.fiap.inovagab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Pontos pontos, Seed seed, Ia ia) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Pontos(int envioIdeia, int ideiaAprovada) {
    }

    public record Seed(boolean enabled, String senhaPadrao) {
    }

    public record Ia(String apiKey, String modelo, String baseUrl, int timeoutSegundos) {

        public boolean configurada() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
