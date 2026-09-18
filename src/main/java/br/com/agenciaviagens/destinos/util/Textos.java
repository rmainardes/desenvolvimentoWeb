package br.com.agenciaviagens.destinos.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilitarios de comparacao textual usados na pesquisa de destinos.
 * A normalizacao remove acentos e diferencas de caixa para que
 * "sao paulo" encontre "Sao Paulo" e "Sao Paulo" encontre "SAO PAULO".
 */
public final class Textos {

    private Textos() {
    }

    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean contem(String texto, String termo) {
        return normalizar(texto).contains(normalizar(termo));
    }

    public static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
