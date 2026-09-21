package com.darkcontinent.nenfoundation.client.vfx.debug;

import java.time.LocalDate;
import java.util.Locale;

/**
 * O nome de arquivo de uma captura de aprovacao.
 *
 * <p>O PROTOCOLO EXIGE TRES COISAS NO NOME -- data, commit e nivel de bloom --,
 * e esta escrito assim em {@code docs/testing/av-aura-visual.md} secao 3 e na
 * lista de entregas de todos os gates AV. O motivo e simples de enunciar e caro
 * de descobrir depois: uma captura sem commit nao se liga a nenhum codigo, e
 * seis meses depois ninguem sabe se aquela imagem mostra o estado atual ou um
 * que foi substituido.
 *
 * <p>CLASSE PURA, SEM MINECRAFT NA ASSINATURA, de proposito: a regra de
 * formacao do nome e a unica parte desta trilha que da para provar com JUnit, e
 * ela e exatamente a parte que um erro silencioso estragaria -- um nome
 * malformado nao da erro, so produz um arquivo que nao serve.
 *
 * <p>CLIENT-ONLY na pratica, mas nao no tipo.
 */
public final class NomeDeCaptura {

    /**
     * O que aparece onde o nivel de bloom vai ficar quando nao ha passe algum.
     *
     * <p>NAO E "0" NEM "off", e a diferenca importa. Zero significaria "o
     * passe de bloom rodou e estava em zero"; {@code off} significaria "o
     * jogador escolheu desligar". Ausente e a terceira coisa: <b>o passe nao
     * existe</b>. Escrever um numero aqui faria uma captura do AV1 parecer
     * comparavel com uma do AV5, e ela nao e.
     *
     * <p><b>ELE DEIXOU DE SER O VALOR PADRAO EM 2026-09-21.</b> O AV5 entregou
     * o {@code AuraGlowTarget}, e este javadoc ja dizia que a partir dai o
     * campo passaria a carregar {@code off}, {@code fast} ou {@code high} --
     * mas o ponto de chamada continuou passando a constante, fixa. A primeira
     * sessao de bancada tirou uma captura chamada {@code ...__bloom-ausente},
     * com o passe de brilho vivo. Ver #300.
     *
     * <p>O estrago que isso faria: o gate #198 compara {@code bloom_off},
     * {@code bloom_fast} e {@code bloom_high} DO MESMO QUADRO. Com as tres
     * saindo com o mesmo sufixo, o arquivo nao distingue mais a comparacao que
     * o gate inteiro existe para fazer -- e ninguem descobre olhando a pasta.
     */
    public static final String BLOOM_AUSENTE = "bloom-ausente";

    /**
     * O sufixo de um nivel de bloom, ou {@link #BLOOM_AUSENTE} se nao houver.
     *
     * <p>Recebe {@code null} sem reclamar: quem chama esta no caminho de uma
     * captura, e uma ferramenta de captura nao pode ser o motivo de um crash
     * durante uma sessao de arte -- e o mesmo motivo do {@code catch} de
     * {@code ReportedException} em {@code AuraCaptureMode}.
     */
    public static String nivelDeBloom(
            @javax.annotation.Nullable
            com.darkcontinent.nenfoundation.client.vfx.AuraBloomLevel nivel) {
        return nivel == null ? BLOOM_AUSENTE
                : "bloom-" + nivel.name().toLowerCase(Locale.ROOT);
    }

    /** Separador entre os campos. Duplo para nao colidir com o proprio nome. */
    private static final String SEPARADOR = "__";

    private static final int LIMITE_DO_NOME = 48;

    private NomeDeCaptura() {
    }

    /**
     * Monta o nome, com extensao.
     *
     * @param nome   o que a captura mostra, por exemplo {@code ten_dia}
     * @param data   a data da sessao; entra como argumento para dar para testar
     * @param commit o commit do build, ja curto
     * @param bloom  o nivel de bloom, ou {@link #BLOOM_AUSENTE}
     */
    public static String de(String nome, LocalDate data, String commit, String bloom) {
        return sanear(nome, LIMITE_DO_NOME, "captura")
                + SEPARADOR + (data == null ? "sem-data" : data.toString())
                + SEPARADOR + sanear(commit, 12, "sem-commit")
                + SEPARADOR + sanear(bloom, 16, BLOOM_AUSENTE)
                + ".png";
    }

    /**
     * Tira do texto tudo que um sistema de arquivos recusa ou trata de forma
     * diferente conforme a plataforma.
     *
     * <p>ELE RECUSA O VAZIO COM UM PADRAO, e nao com uma excecao. Um nome
     * invalido vindo do chat nao pode derrubar a captura inteira; o que ele
     * pode e produzir um arquivo com nome obviamente errado, que a pessoa ve na
     * pasta e conserta.
     */
    static String sanear(String bruto, int limite, String padrao) {
        if (bruto == null || bruto.isBlank()) {
            return padrao;
        }
        StringBuilder limpo = new StringBuilder(bruto.length());
        for (char c : bruto.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-') {
                limpo.append(c);
            } else if (c == ' ' || c == '.' || c == '/' || c == '\\') {
                limpo.append('_');
            }
        }
        while (limpo.length() > 0 && limpo.charAt(0) == '_') {
            limpo.deleteCharAt(0);
        }
        while (limpo.length() > 0 && limpo.charAt(limpo.length() - 1) == '_') {
            limpo.deleteCharAt(limpo.length() - 1);
        }
        if (limpo.length() == 0) {
            return padrao;
        }
        return limpo.length() > limite ? limpo.substring(0, limite) : limpo.toString();
    }
}
