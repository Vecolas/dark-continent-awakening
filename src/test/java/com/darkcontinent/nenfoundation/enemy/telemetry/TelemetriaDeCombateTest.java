package com.darkcontinent.nenfoundation.enemy.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Portao da telemetria LOCAL e OPT-IN (issue #150).
 *
 * <p>As tres falhas cobertas nao produzem erro nenhum, e duas delas nao produzem
 * nem sintoma: telemetria ligada por padrao (que transforma "medir o proprio
 * jogo" em "coletar sem perguntar"), identificador de jogador guardado "porque
 * pode ser util depois", e um interruptor lido uma vez no boot que ignora quem
 * desligou depois.</p>
 */
class TelemetriaDeCombateTest {

    private static AmostraDeCombate amostra(String mob, int jogadores, int ticks) {
        return new AmostraDeCombate(mob, jogadores, ticks, 120.0F, 40.0F, true);
    }

    // ---------------------------------------------------------------- opt-in

    @Test
    @DisplayName("DESLIGADA nao guarda nada")
    void desligadaNaoGuarda() {
        TelemetriaDeCombate telemetria = new TelemetriaDeCombate(() -> false);
        assertFalse(telemetria.registrar(amostra("cyclops", 1, 200)));
        assertEquals(0, telemetria.emMemoria(),
                "Telemetria desligada que guarda 'so em memoria' e telemetria ligada com um"
                        + " passo a mais: o dado existe, e alguem acaba gravando.");
    }

    @Test
    @DisplayName("o interruptor e lido A CADA amostra, e nao guardado no boot")
    void interruptorERelido() {
        AtomicBoolean ligado = new AtomicBoolean(true);
        TelemetriaDeCombate telemetria = new TelemetriaDeCombate(ligado::get);
        assertTrue(telemetria.registrar(amostra("cyclops", 1, 200)));

        ligado.set(false);
        assertFalse(telemetria.registrar(amostra("cyclops", 1, 210)),
                "A config e recarregavel: uma copia lida no boot ignoraria o /reload e"
                        + " continuaria gravando depois de alguem desligar -- e ninguem"
                        + " reclamaria, o arquivo so continuaria crescendo.");
        assertEquals(1, telemetria.emMemoria());
    }

    @Test
    @DisplayName("PORTAO: o padrao declarado na config e DESLIGADO")
    void oPadraoEDesligado() {
        String config = Repo.texto(
                "src/main/java/com/darkcontinent/nenfoundation/config/NenConfig.java");
        assertTrue(config.contains(".define(\"telemetry.enemyCombat\", false)"),
                "A chave de telemetria nao esta declarada com padrao false. Um padrao ligado"
                        + " transforma 'medir o proprio jogo' em 'coletar sem perguntar', e a"
                        + " diferenca nao aparece em lugar nenhum do jogo.");
    }

    // ----------------------------------------------------------- sem rede

    @Test
    @DisplayName("PORTAO: o pacote de telemetria nao tem NADA de rede")
    void telemetriaNaoTemRede() {
        List<Path> fontes = Repo.varrer(
                "src/main/java/com/darkcontinent/nenfoundation/enemy/telemetry", ".java");
        assertFalse(fontes.isEmpty(), "Varredura vazia nao e aprovacao.");

        List<String> proibidos = List.of("java.net.", "HttpClient", "Socket", "URL(",
                "URI.create", "openConnection");
        for (Path fonte : fontes) {
            String texto = ler(fonte);
            for (String proibido : proibidos) {
                assertFalse(texto.contains(proibido),
                        Repo.raiz().relativize(fonte) + " usa '" + proibido + "'. Telemetria"
                                + " deste projeto e LOCAL: nada sai da maquina, e nao existe"
                                + " 'so um ping para saber quantos usam'. Se um dia houver"
                                + " envio, ele e OUTRA decisao com OUTRO consentimento -- e o"
                                + " lugar de discuti-la nao e dentro da classe que ja tem os"
                                + " dados na mao.");
            }
        }
    }

    @Test
    @DisplayName("PORTAO: a amostra nao guarda identidade de jogador")
    void amostraNaoGuardaIdentidade() {
        String fonte = Repo.texto("src/main/java/com/darkcontinent/nenfoundation/enemy/"
                + "telemetry/AmostraDeCombate.java");
        for (String campo : List.of("UUID", "getName", "ServerPlayer", "BlockPos")) {
            assertFalse(fonte.contains("record AmostraDeCombate") && fonte.contains(campo + " "),
                    "AmostraDeCombate parece guardar '" + campo + "'. Nenhuma pergunta de"
                            + " balanceamento precisa de quem estava jogando, e guardar o"
                            + " identificador 'porque pode ser util depois' e como um arquivo"
                            + " local vira um arquivo que alguem manda por engano.");
        }
    }

    // ---------------------------------------------------------------- teto

    @Test
    @DisplayName("o teto para de acumular e AVISA, em vez de descartar as antigas")
    void tetoAvisaEmVezDeDescartar() {
        TelemetriaDeCombate telemetria = new TelemetriaDeCombate(() -> true);
        for (int i = 0; i < TelemetriaDeCombate.TETO_EM_MEMORIA; i++) {
            telemetria.registrar(amostra("cyclops", 1, 100 + i));
        }
        assertTrue(telemetria.cheia());
        assertFalse(telemetria.registrar(amostra("cyclops", 1, 999)));
        assertTrue(telemetria.bateuNoTeto(),
                "Descartar as antigas em silencio enviesaria a amostra para o FIM da sessao,"
                        + " e a media mudaria sem que ninguem soubesse por que.");
        assertEquals(TelemetriaDeCombate.TETO_EM_MEMORIA, telemetria.emMemoria());
    }

    // -------------------------------------------------------------- arquivo

    @Test
    @DisplayName("gravar ACRESCENTA, e a segunda sessao nao apaga a primeira")
    void gravarAcrescenta(@TempDir Path pasta) {
        Path destino = pasta.resolve("sub").resolve("telemetria.csv");

        TelemetriaDeCombate primeira = new TelemetriaDeCombate(() -> true);
        primeira.registrar(amostra("cyclops", 1, 200));
        assertEquals(1, primeira.gravar(destino));

        TelemetriaDeCombate segunda = new TelemetriaDeCombate(() -> true);
        segunda.registrar(amostra("cyclops", 2, 140));
        assertEquals(1, segunda.gravar(destino));

        List<String> linhas = lerLinhas(destino);
        assertEquals(3, linhas.size(),
                "Substituir faria a ultima sessao apagar todas as outras, e ninguem repararia"
                        + " ate a media mudar sem motivo.");
        assertEquals(AmostraDeCombate.cabecalho(), linhas.get(0),
                "O cabecalho sai UMA vez, no arquivo novo.");
    }

    @Test
    @DisplayName("gravar ESVAZIA a memoria -- senao a proxima gravacao duplica tudo")
    void gravarEsvazia(@TempDir Path pasta) {
        TelemetriaDeCombate telemetria = new TelemetriaDeCombate(() -> true);
        telemetria.registrar(amostra("cyclops", 1, 200));
        Path destino = pasta.resolve("t.csv");
        telemetria.gravar(destino);
        assertEquals(0, telemetria.emMemoria());
        assertEquals(0, telemetria.gravar(destino),
                "Sem esvaziar, cada gravacao reescreveria tudo que ja foi gravado, e a media"
                        + " passaria a contar as primeiras amostras varias vezes.");
    }

    @Test
    @DisplayName("a media separa solo de grupo")
    void mediaSeparaSoloDeGrupo() {
        TelemetriaDeCombate telemetria = new TelemetriaDeCombate(() -> true);
        telemetria.registrar(amostra("cyclops", 1, 400));
        telemetria.registrar(amostra("cyclops", 4, 100));

        var medias = telemetria.segundosMedios().get("cyclops");
        assertEquals(20.0D, medias.get(1), 1.0e-9D);
        assertEquals(5.0D, medias.get(4), 1.0e-9D,
                "Juntar solo e grupo faz todo mob parecer facil demais em solo e dificil"
                        + " demais em grupo ao mesmo tempo, e a media nao descreve nenhum.");
    }

    // ------------------------------------------------------------- validacao

    @Test
    @DisplayName("amostra invalida reprova, e linha torta nao vira amostra zerada")
    void amostraInvalidaReprova() {
        assertThrows(IllegalArgumentException.class, () -> amostra("", 1, 100));
        assertThrows(IllegalArgumentException.class, () -> amostra("cyclops", 0, 100));
        assertThrows(IllegalArgumentException.class, () -> amostra("cyclops", 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> AmostraDeCombate.de("faltando;campos"));
    }

    @Test
    @DisplayName("a linha escrita volta igual")
    void idaEVolta() {
        AmostraDeCombate original = new AmostraDeCombate("cyclops", 3, 250, 180.5F, 62.25F, false);
        assertEquals(original, AmostraDeCombate.de(original.linha()));
    }

    private static String ler(Path arquivo) {
        try {
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (java.io.IOException erro) {
            throw new java.io.UncheckedIOException(erro);
        }
    }

    private static List<String> lerLinhas(Path arquivo) {
        try {
            return Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        } catch (java.io.IOException erro) {
            throw new java.io.UncheckedIOException(erro);
        }
    }
}
