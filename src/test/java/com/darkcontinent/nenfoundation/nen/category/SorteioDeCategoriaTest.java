package com.darkcontinent.nenfoundation.nen.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do sorteio de categoria.
 *
 * <p>Ele morde dos dois lados: exige que o mesmo par (mundo, jogador) sempre
 * de o mesmo resultado, E que pares diferentes nao caiam todos na mesma
 * categoria. So o primeiro passaria com uma funcao que devolve ENHANCEMENT
 * sempre.
 */
class SorteioDeCategoriaTest {

    private static UUID uuid(int n) {
        return new UUID(0L, n);
    }

    @Test
    @DisplayName("a mesma semente da sempre a mesma categoria")
    void mesmaSementeMesmaCategoria() {
        for (long semente = -50L; semente <= 50L; semente++) {
            NenCategory primeira = SorteioDeCategoria.sortear(semente);
            NenCategory segunda = SorteioDeCategoria.sortear(semente);
            assertSame(primeira, segunda,
                    "Semente " + semente + " devolveu duas categorias diferentes."
                            + " Sem determinismo, um relato de bug sobre uma"
                            + " categoria nao e reproduzivel: a proxima tentativa"
                            + " sorteia outra coisa.");
        }
    }

    @Test
    @DisplayName("o sorteio nunca devolve UNDETERMINED")
    void nuncaDevolveONeutro() {
        for (long semente = -1000L; semente <= 1000L; semente++) {
            NenCategory sorteada = SorteioDeCategoria.sortear(semente);
            assertTrue(sorteada.eReal(),
                    "Semente " + semente + " devolveu " + sorteada + ". O neutro e"
                            + " a AUSENCIA de categoria; um sorteio que o devolve"
                            + " produz um jogador que fez a Water Divination e"
                            + " continua sem resposta.");
        }
    }

    @Test
    @DisplayName("sementes diferentes cobrem as seis categorias")
    void cobreAsSeis() {
        Set<NenCategory> vistas = EnumSet.noneOf(NenCategory.class);
        for (long semente = 0L; semente < 200L; semente++) {
            vistas.add(SorteioDeCategoria.sortear(semente));
        }
        assertEquals(EnumSet.copyOf(NenCategory.REAIS), vistas,
                "Em 200 sementes o sorteio nao alcancou as seis. Uma categoria"
                        + " inalcancavel nao da erro: ela so nunca aparece, e"
                        + " ninguem descobre ate alguem perguntar por que ninguem"
                        + " e Specialist.");
    }

    @Test
    @DisplayName("sementes consecutivas nao saem na ordem do enum")
    void sementesConsecutivasEspalham() {
        // ESTE E O PORTAO DO FINALIZADOR DE MISTURA, e ele mede o caminho
        // certo: `sortear(long)` cru.
        //
        // Uma primeira versao deste teste usava `sortear(mundo, uuid)` com
        // UUIDs 1..6, e passava mesmo com a mistura REMOVIDA -- porque
        // `sementeDe` ja multiplica por uma constante grande e impar, e aquilo
        // sozinho embaralha. O teste tinha o nome certo e media outra coisa.
        //
        // No caminho cru nao ha multiplicacao nenhuma: sem mistura,
        // `floorMod(n, 6)` devolve as seis EM ORDEM para n = 0..5. E este o
        // caminho que um comando de seed de QA usa, com numeros pequenos.
        //
        // A comparacao e sobre uma LISTA na ordem em que sairam, e nao sobre um
        // conjunto: EnumSet sempre itera na ordem do enum, entao um conjunto
        // compararia a ordem consigo mesma e o portao nunca morderia.
        List<NenCategory> naOrdemDoSorteio = new ArrayList<>();
        for (long semente = 0L; semente < 6L; semente++) {
            naOrdemDoSorteio.add(SorteioDeCategoria.sortear(semente));
        }
        assertNotEquals(NenCategory.REAIS, naOrdemDoSorteio,
                "As sementes 0..5 devolveram as seis categorias na ordem exata"
                        + " do enum: " + naOrdemDoSorteio + ". Isso e"
                        + " `floorMod` sem mistura nenhuma -- previsivel de"
                        + " fora e nao uniforme para sementes correlacionadas.");
    }

    @Test
    @DisplayName("mundo e jogador entram os dois na semente")
    void mundoEJogadorImportam() {
        UUID jogador = uuid(7);

        // Trocar o mundo tem de poder mudar a categoria. Se o UUID mandasse
        // sozinho, a mesma pessoa teria a mesma categoria em todo servidor.
        assertNotEquals(
                SorteioDeCategoria.sementeDe(1L, jogador),
                SorteioDeCategoria.sementeDe(2L, jogador),
                "A semente do mundo nao entrou no calculo.");

        // Trocar o jogador tem de mudar. Se o mundo mandasse sozinho, todo
        // mundo no servidor seria da MESMA categoria -- e ninguem notaria ate
        // o segundo jogador despertar.
        assertNotEquals(
                SorteioDeCategoria.sementeDe(1L, uuid(7)),
                SorteioDeCategoria.sementeDe(1L, uuid(8)),
                "O UUID do jogador nao entrou no calculo.");
    }

    @Test
    @DisplayName("num mesmo mundo, os jogadores nao recebem todos a mesma categoria")
    void mesmoMundoEspalhaEntreJogadores() {
        Map<NenCategory, Integer> contagem = new HashMap<>();
        for (int n = 0; n < 600; n++) {
            NenCategory c = SorteioDeCategoria.sortear(987654321L, UUID.nameUUIDFromBytes(
                    ("jogador-" + n).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            contagem.merge(c, 1, Integer::sum);
        }
        assertEquals(NenCategory.REAIS.size(), contagem.size(),
                "Num unico mundo, 600 jogadores nao cobriram as seis: " + contagem);
        for (NenCategory categoria : NenCategory.REAIS) {
            // 600/6 = 100 esperados. O piso e frouxo de proposito: isto e um
            // portao contra "uma categoria quase nunca sai", e nao um teste
            // estatistico de uniformidade, que seria fragil e piscaria.
            assertTrue(contagem.getOrDefault(categoria, 0) >= 40,
                    categoria + " saiu so " + contagem.getOrDefault(categoria, 0)
                            + " vez(es) em 600. Distribuicao: " + contagem);
        }
    }

    @Test
    @DisplayName("a ordem dos dois componentes importa")
    void ordemImporta() {
        // Uma combinacao por soma ou XOR daria a mesma semente ao trocar mundo
        // e jogador de lugar. Nao ha bug visivel nisso hoje; ha no dia em que
        // alguem usar a mesma funcao para outro par.
        long comoA = SorteioDeCategoria.sementeDe(11L, new UUID(22L, 33L));
        long comoB = SorteioDeCategoria.sementeDe(22L, new UUID(11L, 33L));
        assertNotEquals(comoA, comoB);
    }

    // ----------------------------------------------------------------------
    // OS DOIS PORTOES ABAIXO VIERAM DE OUTRA IMPLEMENTACAO DESTA MESMA ISSUE.
    //
    // A #59 foi implementada em paralelo, sem que as duas pessoas soubessem --
    // e o segundo trabalho ficou sem commit numa worktree. Comparando as duas,
    // aquela versao tinha estes dois portoes, que esta nao tinha.
    //
    // Eles foram adotados PORQUE medem coisas que os meus nao mediam, e porque
    // teste escrito por quem nao viu este codigo vale mais que auto-aprovacao:
    // se a implementacao daqui passa neles, a evidencia e independente.
    //
    // Os meus que aquela versao nao tinha ficam onde estao: o de sementes
    // consecutivas e o unico que prova que o finalizador de mistura carrega
    // peso de verdade.
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("nenhuma categoria fica abaixo de 5% em 60 000 sorteios")
    void distribuicaoNaoEDegenerada() {
        final int sorteios = 60_000;
        Map<NenCategory, Integer> contagem = new EnumMap<>(NenCategory.class);
        for (NenCategory c : NenCategory.REAIS) {
            contagem.put(c, 0);
        }

        for (int i = 0; i < sorteios; i++) {
            contagem.merge(SorteioDeCategoria.sortear(i, uuid(i % 997)), 1, Integer::sum);
        }

        // O piso e 5%, e nao os 16,7% esperados: isto e portao contra
        // distribuicao DEGENERADA -- "a categoria existe mas ninguem a recebe"
        // --, e nao teste estatistico de uniformidade, que piscaria.
        int piso = sorteios / 20;
        for (Map.Entry<NenCategory, Integer> e : contagem.entrySet()) {
            assertTrue(e.getValue() >= piso,
                    e.getKey() + " saiu " + e.getValue() + " vez(es) em " + sorteios
                            + ", abaixo do piso de " + piso + ".");
        }
        assertEquals(sorteios,
                contagem.values().stream().mapToInt(Integer::intValue).sum(),
                "a soma das contagens nao fecha com o numero de sorteios.");
    }

    @Test
    @DisplayName("trocar as metades do UUID muda o resultado na maioria dos pares")
    void asMetadesDoUuidNaoSeAnulam() {
        final int pares = 3_000;
        int diferentes = 0;

        for (int i = 0; i < pares; i++) {
            long a = 0x0123456789ABCDEFL * (i + 1);
            long b = 0x76543210FEDCBA98L * (i + 7);
            if (SorteioDeCategoria.sortear(5L, new UUID(a, b))
                    != SorteioDeCategoria.sortear(5L, new UUID(b, a))) {
                diferentes++;
            }
        }

        int piso = pares * 3 / 5;
        assertTrue(diferentes >= piso,
                "So " + diferentes + " de " + pares + " pares mudaram ao trocar as"
                        + " metades do UUID (piso " + piso + "). As metades estao se"
                        + " anulando na mistura, e UUIDs espelhados colidem em"
                        + " silencio.");
    }
}
