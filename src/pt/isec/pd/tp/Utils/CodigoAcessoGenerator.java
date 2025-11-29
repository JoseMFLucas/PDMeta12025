package pt.isec.pd.tp.Utils;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Classe utilitária para gerar códigos de acesso.
 */
public class CodigoAcessoGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final Random RANDOM = new SecureRandom();

    /**
     * Gera um código de acesso alfanumérico com 7 caracteres.
     * O código é gerado de forma segura e é ideal para registos ou acessos únicos.
     *
     * @return Uma String contendo o código de 7 caracteres gerado.
     */
    public static String gerarCodigo() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    // Construtor privado para impedir a instanciação da classe utilitária.
    private CodigoAcessoGenerator() {}
}