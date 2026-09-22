package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do criterio "REN APROVADO, E O MUNDO INTACTO" (AV4, #193).
 *
 * <p>POR QUE ELE EXISTE. Tres das oito verificacoes do AV4 nao sao de gosto, e
 * por isso nao podiam ser dispensadas junto com as outras cinco em 2026-09-22:
 *
 * <ul>
 *   <li><b>R2</b> — nenhum bloco quebrado depois de 5 min de Ren sobre terra,
 *       pedra, areia, grama alta e agua rasa;
 *   <li><b>R3</b> — zero {@code ItemEntity} novo, zero entidade orfa;
 *   <li><b>R7</b> — observadores NAO recebem o impulso de camera.
 * </ul>
 *
 * <p>As tres falham <b>em silencio</b>. Um bloco destruido por efeito visual nao
 * lanca nada: ele aparece como um buraco no mundo de alguem, dias depois, sem
 * causa. Uma entidade orfa aparece como TPS caindo devagar ao longo de uma
 * semana (erro no 8 do {@code CLAUDE.md}). E um impulso de camera disparado para
 * a pessoa errada e a acao de um jogador mexendo a camera de outro.
 *
 * <p><b>A DECISAO QUE ESTE ARQUIVO CARREGA.</b> O dono do projeto dispensou a
 * sessao humana do AV4 por julgar o restante tuning fino, <i>"sem registrar
 * dividas para depois"</i>. Estas tres nao viraram divida <b>nem foram
 * carimbadas</b>: viraram regua. E o unico desfecho que respeita as duas coisas
 * ao mesmo tempo.
 *
 * <p><b>PONTO CEGO DECLARADO.</b> Ele le TEXTO, e prova que o caminho de VFX
 * nao CHAMA nada que mude o mundo. Ele nao mede o mundo: {@code BlockState}
 * comparado antes e depois de cinco minutos reais, e a contagem de entidades,
 * continuam sendo do olho. O que ele garante e que a construcao que torna isso
 * improvavel <b>nao se desfaz em silencio</b>.
 */
class OMundoNaoMudaTest {

    private static final String VFX = "src/main/java/com/darkcontinent/nenfoundation/client/vfx";

    /**
     * Chamadas que mudam o mundo ou criam entidade.
     *
     * <p>Nenhuma delas tem o que fazer num caminho de efeito visual de CLIENTE.
     * A lista e de tokens e nao de tipos, porque o portao le fonte -- esta
     * limitacao esta no javadoc da classe.
     */
    private static final List<String> PROIBIDAS = List.of(
            "setBlock(", "setBlockAndUpdate(", "destroyBlock(", "removeBlock(",
            "setBlockState(", "new ItemEntity(", "spawnAtLocation(", "addFreshEntity(",
            "dropFromLootTable(", "popResource(");

    /** Quantos arquivos o portao precisa varrer para significar alguma coisa. */
    private static final int PISO_DE_ARQUIVOS = 30;

    // ------------------------------------------------------------ R2 e R3

    @Test
    @DisplayName("R2/R3: nada no caminho de VFX muda o mundo nem cria entidade")
    void nadaNoVfxMudaOMundo() {
        List<Path> arquivos = Repo.varrer(VFX, ".java");
        assertTrue(arquivos.size() >= PISO_DE_ARQUIVOS,
                "O portao varreu so " + arquivos.size() + " arquivos. Ou o pacote mudou de"
                        + " lugar, ou a varredura quebrou -- e nos dois casos ele passaria"
                        + " sem verificar nada.");

        List<String> achados = new ArrayList<>();
        for (Path arquivo : arquivos) {
            String fonte = semComentarios(texto(arquivo));
            for (String proibida : PROIBIDAS) {
                if (fonte.contains(proibida)) {
                    achados.add("  " + Repo.raiz().relativize(arquivo) + " -> " + proibida);
                }
            }
        }

        assertTrue(achados.isEmpty(),
                "Chamada que muda o mundo dentro do caminho de efeito visual:\n"
                        + String.join("\n", achados)
                        + "\n\nO AV4 exige `BlockState` identico antes e depois de cinco"
                        + " minutos de Ren, e zero ItemEntity novo. Um efeito de CLIENTE que"
                        + " chega a essas APIs ou nao faz nada (e e codigo morto) ou faz, e"
                        + " ai destroi o mundo de alguem sem lancar erro nenhum.");
    }

    @Test
    @DisplayName("a sondagem de chao LE o bloco, e continua so lendo")
    void aSondagemSoLe() {
        String fonte = semComentarios(texto(Repo.arquivo(VFX + "/SondagemDeChao.java")));

        assertTrue(fonte.contains("getBlockState("),
                "A sondagem parou de ler BlockState. Ou ela mudou de nome, ou o anel deixou"
                        + " de saber a cor do chao -- e este teste passaria a guardar um"
                        + " arquivo que nao faz mais nada.");

        for (String proibida : PROIBIDAS) {
            assertFalse(fonte.contains(proibida),
                    "A sondagem de chao passou a chamar `" + proibida + "`. Ela e o UNICO"
                            + " ponto do VFX que toca o mundo, e o contrato escrito nela e"
                            + " explicito: \"NADA AQUI ALTERA O MUNDO. Le BlockState para"
                            + " tirar uma cor, e so.\"");
        }
    }

    // ---------------------------------------------------------------- R7

    @Test
    @DisplayName("R7: o impulso de camera nao aceita jogador -- nao ha como dispara-lo para outro")
    void oImpulsoNaoAceitaJogador() {
        String fonte = semComentarios(texto(Repo.arquivo(VFX + "/ImpulsoDeCamera.java")));

        assertTrue(Pattern.compile("static\\s+\\w+\\s+disparar\\s*\\(\\s*\\)").matcher(fonte).find(),
                "`disparar` passou a receber argumento. Sem parametro ele so pode afetar a"
                        + " camera de quem chama, e essa e a garantia INTEIRA do R7: nao ha"
                        + " como mirar outro jogador porque nao ha onde escrever o alvo."
                        + " Com um parametro, a garantia vira uma checagem que alguem pode"
                        + " mover para o laco errado.");

        assertFalse(fonte.contains("Player ") || fonte.contains("LivingEntity "),
                "O impulso de camera passou a conhecer o tipo de jogador. Ele nao deve"
                        + " saber que jogadores existem.");
    }

    @Test
    @DisplayName("R7: existe exatamente UM ponto de disparo, e ele e o ramo do jogador local")
    void existeUmUnicoPontoDeDisparo() {
        List<String> locais = new ArrayList<>();
        for (Path arquivo : Repo.varrer("src/main/java", ".java")) {
            if (arquivo.getFileName().toString().equals("ImpulsoDeCamera.java")) {
                continue;
            }
            if (semComentarios(texto(arquivo)).contains("ImpulsoDeCamera.disparar(")) {
                locais.add(Repo.raiz().relativize(arquivo).toString().replace('\\', '/'));
            }
        }

        assertEquals(1, locais.size(),
                "O impulso de camera passou a ser disparado de " + locais.size() + " lugares "
                        + locais + ". Dois pontos de disparo e como o R7 quebra: um deles"
                        + " fica dentro do laco dos OUTROS jogadores, e a camera de quem"
                        + " observa passa a tremer porque um vizinho ligou Ren.");

        assertTrue(locais.get(0).endsWith("client/vfx/AudioDeAura.java"),
                "O disparo saiu de AudioDeAura para " + locais.get(0) + ". Ele nasce dentro do"
                        + " ramo `ativacaoDeRen`, que ja e exclusivo de `mc.player` -- e nao"
                        + " numa checagem de \"e o meu jogador?\" mais adiante, que alguem"
                        + " poderia mover por engano.");
    }

    // ------------------------------------------------- o caso que DEVE reprovar

    @Test
    @DisplayName("REGUA DA REGUA: uma chamada proibida seria vista")
    void umaChamadaProibidaSeriaVista() {
        String falso = "class X { void f(Level n) { n.setBlock(pos, estado, 3); } }";
        assertTrue(contemProibida(semComentarios(falso)),
                "Uma chamada explicita a setBlock NAO foi detectada. O portao inteiro seria"
                        + " decoracao.");
    }

    @Test
    @DisplayName("REGUA DA REGUA: menção em COMENTARIO nao reprova")
    void mencaoEmComentarioNaoReprova() {
        // Os javadocs do VFX prometem "sem ItemEntity" e "nao altera o mundo" de
        // proposito, para que a proxima pessoa saiba a regra. Sem esta limpeza o
        // portao reprovaria a propria documentacao dela -- e seria desligado.
        String comentado = "/** sem ItemEntity, sem setBlock( nenhum. */ class X {}";
        assertFalse(contemProibida(semComentarios(comentado)),
                "Uma mencao em COMENTARIO reprovou. O portao mandaria apagar o javadoc que"
                        + " explica a regra, que e o contrario do que ele existe para fazer.");
    }

    // ---------------------------------------------------------------- apoio

    private static boolean contemProibida(String fonte) {
        return PROIBIDAS.stream().anyMatch(fonte::contains);
    }

    private static String texto(Path arquivo) {
        return Repo.texto(Repo.raiz().relativize(arquivo).toString().replace('\\', '/'));
    }

    private static String semComentarios(String fonte) {
        return fonte.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }
}
