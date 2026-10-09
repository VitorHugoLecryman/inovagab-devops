package br.com.fiap.inovagab.exception;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    public static class NaoEncontradoException extends RuntimeException {

        public NaoEncontradoException(String mensagem) {
            super(mensagem);
        }
    }

    public static class RegraNegocioException extends RuntimeException {

        public RegraNegocioException(String mensagem) {
            super(mensagem);
        }
    }

    public static class AcessoNegadoException extends RuntimeException {

        public AcessoNegadoException(String mensagem) {
            super(mensagem);
        }
    }

    public static class IntegracaoIaException extends RuntimeException {

        public IntegracaoIaException(String mensagem) {
            super(mensagem);
        }

        public IntegracaoIaException(String mensagem, Throwable causa) {
            super(mensagem, causa);
        }
    }
}
