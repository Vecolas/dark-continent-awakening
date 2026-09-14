package com.darkcontinent.nenfoundation.enemy.ai.squad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da fundacao de squad/pack (issue #143).
 *
 * <p>O gate da issue e o Wolf Pack com 2 a 4 jogadores: cap de reforco e mudanca
 * imediata depois da morte do chefe. O que este arquivo prova sao as regras que
 * aquele gate vai exercitar -- e, principalmente, os vazamentos: bando fantasma
 * que ressuscita com alvo velho, membro em dois bandos recebendo duas ordens por
 * tick, e coordenacao todo tick derrubando o TPS de uma colonia.</p>
 */
class SquadFoundationTest {

    private static final UUID BANDO = UUID.nameUUIDFromBytes("bando".getBytes());
    private static final UUID CHEFE = UUID.nameUUIDFromBytes("chefe".getBytes());
    private static final UUID SEGUNDO = UUID.nameUUIDFromBytes("segundo".getBytes());
    private static final UUID TERCEIRO = UUID.nameUUIDFromBytes("terceiro".getBytes());
    private static final UUID ALVO = UUID.nameUUIDFromBytes("alvo".getBytes());

    private static Squad matilha() {
        Squad bando = new Squad(BANDO, SquadRules.matilha(), CHEFE);
        bando.entrar(SEGUNDO, SquadRole.FLANKER);
        bando.entrar(TERCEIRO, SquadRole.FRONTLINER);
        return bando;
    }

    // -------------------------------------------------------------- lideranca

    @Test
    @DisplayName("matar o chefe PROMOVE o proximo -- a matilha muda, nao some")
    void morteDoLiderPromove() {
        Squad bando = matilha();
        assertEquals(SquadRole.LEADER, bando.papelDe(CHEFE));

        UUID novo = bando.sair(CHEFE).orElseThrow();
        assertEquals(SEGUNDO, novo, "A promocao segue a ordem de entrada: um sorteio faria dois"
                + " servidores com o mesmo save promoverem gente diferente.");
        assertEquals(SquadRole.LEADER, bando.papelDe(SEGUNDO));
        assertEquals(2, bando.tamanho());
    }

    @Test
    @DisplayName("perder o chefe custa moral DE UMA VEZ")
    void morteDoLiderAbalaOBando() {
        Squad bando = matilha();
        assertEquals(Squad.MORAL_MAXIMA, bando.moral());
        bando.sair(CHEFE);
        assertTrue(bando.moral() < Squad.MORAL_MAXIMA,
                "Sem custo, a matilha continuaria avancando como se nada tivesse acontecido e"
                        + " matar o lider deixaria de ser decisao tatica.");
    }

    @Test
    @DisplayName("o ultimo a sair dissolve o bando, e ninguem sobra como lider")
    void ultimoASairDissolve() {
        Squad bando = new Squad(BANDO, SquadRules.matilha(), CHEFE);
        assertTrue(bando.sair(CHEFE).isEmpty());
        assertTrue(bando.dissolvido());
        assertTrue(bando.lider().isEmpty());
    }

    @Test
    @DisplayName("nao se ENTRA como lider: dois lideres dariam duas ordens ao mesmo bando")
    void naoSeEntraComoLider() {
        Squad bando = matilha();
        assertThrows(IllegalArgumentException.class, () -> bando.entrar(ALVO, SquadRole.LEADER));
    }

    @Test
    @DisplayName("squad sem lider reprova no construtor")
    void squadSemLiderReprova() {
        assertThrows(NullPointerException.class,
                () -> new Squad(BANDO, SquadRules.matilha(), null));
    }

    // ------------------------------------------------------------------ teto

    @Test
    @DisplayName("o teto de membros e cobrado -- sem ele o reforco chama reforco")
    void tetoDeMembros() {
        Squad bando = matilha();
        assertTrue(bando.entrar(UUID.randomUUID(), SquadRole.RANGED));
        assertEquals(4, bando.tamanho());
        assertFalse(bando.entrar(UUID.randomUUID(), SquadRole.SCOUT),
                "Sem teto a matilha cresce ate o chunk inteiro, e nada reclama.");
    }

    @Test
    @DisplayName("entrar duas vezes nao conta duas vezes")
    void entrarDuasVezesNaoDuplica() {
        Squad bando = matilha();
        assertFalse(bando.entrar(SEGUNDO, SquadRole.RANGED));
        assertEquals(SquadRole.FLANKER, bando.papelDe(SEGUNDO),
                "A segunda entrada nao pode trocar o papel pelas costas de quem o definiu.");
    }

    // ----------------------------------------------------------------- alvo

    @Test
    @DisplayName("o alvo e do BANDO: trocar troca para todos")
    void alvoECompartilhado() {
        Squad bando = matilha();
        bando.alvo(ALVO);
        assertEquals(ALVO, bando.leituraPara(SEGUNDO, true, false, false).target());
        assertEquals(ALVO, bando.leituraPara(TERCEIRO, true, false, false).target(),
                "Alvo por membro faria oito bichos perseguirem oito alvos, e o cerco nunca"
                        + " fecharia -- sem erro nenhum.");
    }

    @Test
    @DisplayName("bando desmoralizado recua JUNTO, e a leitura ja diz isso")
    void desmoralizadoRecuaJunto() {
        Squad bando = matilha();
        bando.alvo(ALVO);
        assertFalse(bando.leituraPara(SEGUNDO, true, false, false).targetRetreating());

        bando.abalar(80);
        assertTrue(bando.desmoralizado());
        assertTrue(bando.leituraPara(SEGUNDO, true, false, false).targetRetreating(),
                "Sem moral compartilhada cada membro decide sozinho, e a fuga sai em fila"
                        + " indiana em vez de o bando quebrar de uma vez.");
    }

    @Test
    @DisplayName("moral fica presa em 0..100 e nao estoura para negativo")
    void moralTemLimites() {
        Squad bando = matilha();
        bando.abalar(500);
        assertEquals(0, bando.moral());
        bando.moral(9999);
        assertEquals(Squad.MORAL_MAXIMA, bando.moral());
    }

    // -------------------------------------------------------------- registro

    @Test
    @DisplayName("um membro pertence a UM bando")
    void umMembroUmBando() {
        SquadRegistry registro = new SquadRegistry();
        registro.criar(BANDO, SquadRules.matilha(), CHEFE);
        UUID outro = UUID.nameUUIDFromBytes("outro_bando".getBytes());
        registro.criar(outro, SquadRules.matilha(), SEGUNDO);

        assertThrows(IllegalStateException.class,
                () -> registro.entrar(outro, CHEFE, SquadRole.FLANKER),
                "Em dois bandos, o bicho recebe duas ordens por tick e a ultima vence -- o que"
                        + " aparece como um mob indeciso, sem causa visivel.");
    }

    @Test
    @DisplayName("bando sem ninguem sai do mapa, e o indice sai junto")
    void faxinaRemoveBandoFantasma() {
        SquadRegistry registro = new SquadRegistry();
        registro.criar(BANDO, SquadRules.matilha(), CHEFE);
        registro.entrar(BANDO, SEGUNDO, SquadRole.FLANKER);
        registro.bando(BANDO).orElseThrow().alvo(ALVO);

        registro.sair(CHEFE);
        registro.sair(SEGUNDO);
        assertEquals(1, registro.removerDissolvidos());
        assertEquals(0, registro.quantidade());

        // Se o indice nao fosse limpo junto, o membro seria recusado em todo
        // bando futuro -- para sempre, e sem erro.
        registro.criar(BANDO, SquadRules.matilha(), CHEFE);
        assertTrue(registro.bandoDe(CHEFE).isPresent());
        assertTrue(registro.bando(BANDO).orElseThrow().alvo().isEmpty(),
                "O bando novo nao pode nascer com o alvo de um combate que acabou.");
    }

    @Test
    @DisplayName("sair de quem nao esta em bando nenhum e inerte")
    void sairSemBandoEInerte() {
        SquadRegistry registro = new SquadRegistry();
        registro.sair(CHEFE);
        assertEquals(0, registro.quantidade());
    }

    // ------------------------------------------------------------- orcamento

    @Test
    @DisplayName("coordenacao todo tick reprova: o orcamento e da issue, nao sugestao")
    void orcamentoDeCoordenacaoECobrado() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new SquadRules(1, 4, 2.0D, 16.0D, 30));
        assertTrue(erro.getMessage().contains("quadrado"),
                "Consultas por membro ao quadrado derrubam o TPS de uma colonia sem erro"
                        + " nenhum, e por isso o limite e validado em vez de comentado.");
        assertThrows(IllegalArgumentException.class, () -> new SquadRules(100, 4, 2.0D, 16.0D, 30));
    }

    @Test
    @DisplayName("raio de reforco menor que o espacamento reprova")
    void reforcoMenorQueEspacamentoReprova() {
        assertThrows(IllegalArgumentException.class, () -> new SquadRules(10, 4, 20.0D, 5.0D, 30));
    }

    @Test
    @DisplayName("dois bandos com desfasagens diferentes nao coordenam no mesmo tick")
    void desfasagemEspalhaOCusto() {
        SquadRules regras = SquadRules.matilha();
        int coincidencias = 0;
        for (int tick = 0; tick < 100; tick++) {
            if (regras.atualizaNesteTick(tick, 0) && regras.atualizaNesteTick(tick, 5)) {
                coincidencias++;
            }
        }
        assertEquals(0, coincidencias,
                "Sem desfasagem o custo se concentra num pico, e o sintoma vira travada"
                        + " periodica -- que se parece com rede e e pior de diagnosticar.");
    }

    // ---------------------------------------------------------------- carga

    @Test
    @DisplayName("save com dois lideres reprova em vez de virar cerco contraditorio")
    void saveComDoisLideresReprova() {
        Squad bando = new Squad(BANDO, SquadRules.matilha(), CHEFE);
        assertThrows(IllegalArgumentException.class, () -> bando.restaurar(CHEFE,
                Map.of(CHEFE, SquadRole.LEADER, SEGUNDO, SquadRole.LEADER), ALVO, 80));
    }

    @Test
    @DisplayName("restaurar substitui o bando inteiro")
    void restaurarSubstitui() {
        Squad bando = matilha();
        bando.restaurar(SEGUNDO, Map.of(SEGUNDO, SquadRole.LEADER, TERCEIRO, SquadRole.SCOUT),
                ALVO, 55);
        assertEquals(SEGUNDO, bando.lider().orElseThrow());
        assertFalse(bando.contem(CHEFE));
        assertEquals(55, bando.moral());
        assertEquals(ALVO, bando.alvo().orElseThrow());
    }
}
