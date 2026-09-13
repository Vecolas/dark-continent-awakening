package com.darkcontinent.nenfoundation.client.vfx.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O nome do arquivo de captura.
 *
 * <p>Parece cosmetico e nao e: o protocolo de aprovacao da trilha AV manda
 * "data, commit e nivel de bloom no nome do arquivo", e um nome malformado nao
 * da erro nenhum -- ele so produz uma imagem que, meses depois, nao se liga a
 * codigo nenhum. Nessa hora a captura vale zero, e ninguem sabe dizer por que.
 */
class NomeDeCapturaTest {

    private static final LocalDate DATA = LocalDate.of(2026, 9, 13);

    @Test
    @DisplayName("o nome carrega os tres campos do protocolo, nesta ordem")
    void carregaOsTresCampos() {
        String nome = NomeDeCaptura.de("ten_dia", DATA, "33ded12", NomeDeCaptura.BLOOM_AUSENTE);
        assertEquals("ten_dia__2026-09-13__33ded12__bloom-ausente.png", nome);
    }

    @Test
    @DisplayName("bloom ausente NAO vira zero")
    void bloomAusenteNaoEZero() {
        assertFalse(NomeDeCaptura.BLOOM_AUSENTE.contains("0"),
                "escrever 0 afirmaria que o passe de bloom rodou e estava em zero; o que"
                        + " e verdade hoje e que ele NAO EXISTE (nasce no AV5)");
    }

    @Test
    @DisplayName("caminho e caractere estranho nao atravessam para o sistema de arquivos")
    void saneiaOQueOChatManda() {
        String nome = NomeDeCaptura.de("../../etc/passwd", DATA, "abc1234", "off");
        assertFalse(nome.contains(".."), "um nome vindo do chat nao pode subir de diretorio");
        assertFalse(nome.contains("/"));
        assertFalse(nome.contains("\\"));
        assertTrue(nome.endsWith(".png"));
    }

    @Test
    @DisplayName("campo vazio vira um padrao visivel, e nao um buraco no nome")
    void vazioViraPadrao() {
        String nome = NomeDeCaptura.de("   ", DATA, "", "");
        assertTrue(nome.startsWith("captura__"), nome);
        assertTrue(nome.contains("sem-commit"),
                "um commit vazio produziria 'ten____bloom' e ninguem notaria o campo faltando");
        assertTrue(nome.contains(NomeDeCaptura.BLOOM_AUSENTE));
    }

    @Test
    @DisplayName("sem data tem nome proprio, em vez de sumir do arquivo")
    void semDataTemNome() {
        assertTrue(NomeDeCaptura.de("ten", null, "abc1234", "off").contains("sem-data"));
    }

    @Test
    @DisplayName("maiuscula e espaco viram forma unica -- dois nomes iguais nao se separam")
    void normalizaCaixaEEspaco() {
        assertEquals(NomeDeCaptura.de("Ten Dia", DATA, "abc1234", "off"),
                NomeDeCaptura.de("ten_dia", DATA, "abc1234", "off"),
                "'Ten Dia' e 'ten_dia' sao a mesma captura; arquivos separados quebram"
                        + " a comparacao A/B sem que nada acuse");
    }
}
