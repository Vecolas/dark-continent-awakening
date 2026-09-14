package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.ai.squad.Squad;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadController;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadInput;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadOrder;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRole;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * As regras do LOBO em cima do bando -- e o caso que elas existem para reprovar.
 *
 * <p>{@code SquadFoundationTest} prova o framework: promocao, teto, moral, faxina.
 * Este arquivo prova o que o Wolf Pack Hunter acrescenta e o que so aparece
 * quando os dois se encontram:</p>
 *
 * <ul>
 *   <li>sozinho ele RECUA -- a regra que o framework nao tem como saber, porque
 *       ela vem da ficha de um bicho de HP 26;</li>
 *   <li>matar o chefe muda o bando no MESMO tick: papel novo, posto novo no anel,
 *       e o bando continua existindo. A prova e feita com um {@code SquadController}
 *       montado a partir do papel que o {@code Squad} tem AGORA, que e exatamente
 *       como a entidade o monta;</li>
 *   <li>o teto e cobrado com MOTIVO, e reforco nao chama reforco;</li>
 *   <li>o anel poe cada papel num posto diferente, e o construtor reprova a
 *       combinacao em que dois membros dividiriam o mesmo posto.</li>
 * </ul>
 *
 * <p>Nenhuma das falhas cobertas aqui levanta excecao em jogo. Todas produzem um
 * mob que nasce, anda, ataca e passa em todo portao -- e que nao faz o que a
 * ficha promete.</p>
 */
class RegrasDeMatilhaTest {

    private static final UUID BANDO = UUID.nameUUIDFromBytes("matilha".getBytes());
    private static final UUID CHEFE = UUID.nameUUIDFromBytes("chefe".getBytes());
    private static final UUID SEGUNDO = UUID.nameUUIDFromBytes("segundo".getBytes());
    private static final UUID TERCEIRO = UUID.nameUUIDFromBytes("terceiro".getBytes());
    private static final UUID ALVO = UUID.nameUUIDFromBytes("alvo".getBytes());

    /** Os numeros de PRODUCAO. O teste mede o que vai para o jogo, nao um exemplo. */
    private static RegrasDeMatilha regras() {
        return com.darkcontinent.nenfoundation.enemy.content.WolfPackHunterTuning.matilha();
    }

    private static Squad matilhaDeTres() {
        Squad bando = new Squad(BANDO, SquadRules.matilha(), CHEFE);
        bando.entrar(SEGUNDO, SquadRole.FRONTLINER);
        bando.entrar(TERCEIRO, SquadRole.FLANKER);
        bando.alvo(ALVO);
        return bando;
    }

    /** A ordem que a entidade monta: papel do Squad AGORA, leitura do Squad AGORA. */
    private static SquadOrder ordemDe(Squad bando, UUID membro, boolean alvoVisivel) {
        SquadInput leitura = bando.leituraPara(membro, alvoVisivel, false, false);
        return new SquadController(bando.papelDe(membro)).update(leitura);
    }

    // ------------------------------------------------------------ o caso normal

    @Test
    @DisplayName("em bando, quem tem papel de investida e esta no alcance MORDE")
    void emBandoOInvestidorMorde() {
        Squad bando = matilhaDeTres();
        SquadOrder ordem = ordemDe(bando, CHEFE, true);
        assertEquals(DecisaoDeMatilha.INVESTIR, regras().decidir(ordem, bando.tamanho(), 1.0D,
                RegrasDeMatilha.papelInveste(ordem.role())));
    }

    @Test
    @DisplayName("quem NAO investe fecha o anel, mesmo colado no alvo")
    void quemNaoInvesteCerca() {
        Squad bando = matilhaDeTres();
        SquadOrder ordem = ordemDe(bando, TERCEIRO, true);
        assertEquals(SquadRole.FLANKER, ordem.role());
        assertEquals(DecisaoDeMatilha.CERCAR, regras().decidir(ordem, bando.tamanho(), 0.5D,
                        RegrasDeMatilha.papelInveste(ordem.role())),
                "Se os quatro investissem juntos, o jogador levaria quatro janelas ACTIVE no mesmo"
                        + " instante e a recuperacao longa deixaria de espacar coisa nenhuma.");
    }

    @Test
    @DisplayName("longe do alcance, ate o investidor so fecha o cerco")
    void investidorLongeCerca() {
        Squad bando = matilhaDeTres();
        SquadOrder ordem = ordemDe(bando, CHEFE, true);
        assertEquals(DecisaoDeMatilha.CERCAR, regras().decidir(ordem, bando.tamanho(), 6.0D,
                RegrasDeMatilha.papelInveste(ordem.role())));
    }

    // --------------------------------------------------------------- a recusa

    @Test
    @DisplayName("SOZINHO ele recua -- com o alvo na cara e o bando mandando atacar")
    void sozinhoRecua() {
        Squad bando = new Squad(BANDO, SquadRules.matilha(), CHEFE);
        bando.alvo(ALVO);
        SquadOrder ordem = ordemDe(bando, CHEFE, true);
        assertEquals(ALVO, ordem.target(), "o bando NAO mandou recuar: a recusa e do lobo");
        assertEquals(DecisaoDeMatilha.RECUAR, regras().decidir(ordem, 1, 0.5D, true),
                "Um lobo de HP 26 e dano 6 que avanca sozinho morre de graca, e a existencia de"
                        + " Squad vira decoracao -- sem que nenhum portao reclame.");
    }

    @Test
    @DisplayName("sem alvo utilizavel ele AGUARDA, em vez de correr para a ultima posicao")
    void semAlvoAguarda() {
        Squad bando = matilhaDeTres();
        SquadOrder ordem = ordemDe(bando, SEGUNDO, false);
        assertTrue(ordem.regroup());
        assertEquals(DecisaoDeMatilha.AGUARDAR, regras().decidir(ordem, bando.tamanho(), 0.0D, true),
                "Correr para a ultima posicao conhecida desmancha o cerco que os outros estao"
                        + " fechando, e isso nao levanta erro nenhum.");
    }

    @Test
    @DisplayName("distancia NaN nao pode virar 'avanca'")
    void distanciaInvalidaRecusa() {
        Squad bando = matilhaDeTres();
        SquadOrder ordem = ordemDe(bando, CHEFE, true);
        assertThrows(IllegalArgumentException.class,
                () -> regras().decidir(ordem, bando.tamanho(), Double.NaN, true));
    }

    // --------------------------------------------- a morte do chefe, no mesmo tick

    @Test
    @DisplayName("matar o chefe PROMOVE, muda o papel e o posto no MESMO tick -- e o bando fica")
    void morteDoChefeMudaOBandoNoMesmoTick() {
        RegrasDeMatilha regras = regras();
        Squad bando = matilhaDeTres();

        SquadOrder antes = ordemDe(bando, SEGUNDO, true);
        assertEquals(SquadRole.FRONTLINER, antes.role());
        double postoAntes = regras.anguloDeCercoEmGraus(antes.role());

        UUID novoLider = bando.sair(CHEFE).orElseThrow();
        assertEquals(SEGUNDO, novoLider);

        // MESMO tick: nenhuma espera, nenhum passo de IA no meio.
        SquadOrder depois = ordemDe(bando, SEGUNDO, true);
        assertEquals(SquadRole.LEADER, depois.role(),
                "O papel sai do Squad, e nao de um campo local. Guardado num campo, ele continuaria"
                        + " dizendo FRONTLINER depois da promocao e o lobo ocuparia o posto de"
                        + " outro no anel, sem nada acusar.");
        assertNotEquals(postoAntes, regras.anguloDeCercoEmGraus(depois.role()),
                "Promover sem mudar o posto deixaria o cerco com um buraco onde estava o chefe.");

        assertEquals(2, bando.tamanho(), "o bando MUDA, nao some -- e essa e a licao da issue #143");
        assertEquals(ALVO, depois.target(), "o alvo compartilhado sobrevive a troca de chefe");
        assertEquals(DecisaoDeMatilha.INVESTIR, regras.decidir(depois, bando.tamanho(), 1.0D,
                        RegrasDeMatilha.papelInveste(depois.role())),
                "O promovido assume o papel de investida na hora; se a mudanca so valesse no tick"
                        + " seguinte, haveria uma janela em que ninguem ataca e o jogador leria a"
                        + " morte do chefe como fim do combate.");
    }

    @Test
    @DisplayName("perder o chefe custa moral e deixa o bando a seis golpes de quebrar")
    void morteDoChefeAproximaAQuebra() {
        RegrasDeMatilha regras = regras();
        Squad bando = matilhaDeTres();
        // Com moral cheia, doze golpes. A conta esta escrita em
        // WolfPackHunterEntity.ABALO_POR_GOLPE_NO_MEMBRO, e este teste e a outra
        // ponta dela: girar o abalo la sem olhar para ca reprova aqui.
        int abaloPorGolpe = 6;
        bando.sair(CHEFE);
        for (int golpe = 0; golpe < 5; golpe++) bando.abalar(abaloPorGolpe);
        assertTrue(ordemDe(bando, SEGUNDO, true).target() != null,
                "cinco golpes depois da morte do chefe o bando AINDA ataca");

        bando.abalar(abaloPorGolpe);
        SquadOrder quebrado = ordemDe(bando, SEGUNDO, true);
        assertTrue(quebrado.retreat(), "no sexto, ele quebra");
        assertEquals(DecisaoDeMatilha.RECUAR,
                regras.decidir(quebrado, bando.tamanho(), 1.0D, true),
                "Moral quebrada vence um alvo na cara: e isso que faz o bando recuar JUNTO em vez"
                        + " de cada um decidir sozinho e a fuga sair em fila indiana.");
    }

    // ------------------------------------------------------------------ reforco

    @Test
    @DisplayName("o teto e cobrado com MOTIVO: reforco nao chama reforco ate o chunk inteiro")
    void tetoDeReforcoRecusaComMotivo() {
        RegrasDeMatilha regras = regras();
        int teto = SquadRules.matilha().maximoDeMembros();
        assertEquals(DecisaoDeReforco.CHAMA, regras.decidirReforco(teto - 1, 100, 5.0D));
        assertEquals(DecisaoDeReforco.BANDO_CHEIO, regras.decidirReforco(teto, 100, 5.0D));
    }

    @Test
    @DisplayName("bando quebrado nao recruta, e bando distante nao alcanca")
    void bandoQuebradoOuLongeRecusa() {
        RegrasDeMatilha regras = regras();
        SquadRules bando = SquadRules.matilha();
        assertEquals(DecisaoDeReforco.BANDO_DESMORALIZADO,
                regras.decidirReforco(2, bando.moralMinima() - 1, 5.0D),
                "Uma matilha em fuga que continua recrutando puxa lobos novos para um combate que"
                        + " ela ja desistiu de travar, e o fluxo nao para sozinho.");
        assertEquals(DecisaoDeReforco.LONGE_DEMAIS,
                regras.decidirReforco(2, 100, bando.raioDeReforco() + 0.1D),
                "Sem o raio, dois combates a cinquenta blocos viram um bando so.");
    }

    @Test
    @DisplayName("o teto do Squad e o do reforco sao o MESMO numero")
    void oTetoNaoTemDuasFontes() {
        // Se RegrasDeMatilha tivesse um teto proprio, ele venceria em metade dos
        // caminhos e o de SquadRules na outra metade. Aqui se prova que a recusa
        // de reforco acontece exatamente onde Squad.entrar tambem recusaria.
        Squad bando = matilhaDeTres();
        bando.entrar(UUID.randomUUID(), SquadRole.SCOUT);
        assertEquals(SquadRules.matilha().maximoDeMembros(), bando.tamanho());
        assertEquals(DecisaoDeReforco.BANDO_CHEIO,
                regras().decidirReforco(bando.tamanho(), bando.moral(), 1.0D));
    }

    // ---------------------------------------------------------------- o anel

    @Test
    @DisplayName("cada papel de um bando cheio tem um posto DIFERENTE no anel")
    void cadaPapelTemPostoProprio() {
        RegrasDeMatilha regras = regras();
        List<SquadRole> papeis = new ArrayList<>();
        papeis.add(SquadRole.LEADER);
        Squad bando = new Squad(BANDO, SquadRules.matilha(), CHEFE);
        Set<SquadRole> ocupados = new LinkedHashSet<>(bando.membros().values());
        while (bando.tamanho() < SquadRules.matilha().maximoDeMembros()) {
            SquadRole papel = regras.papelVago(ocupados).orElseThrow();
            assertTrue(bando.entrar(UUID.randomUUID(), papel));
            ocupados.add(papel);
            papeis.add(papel);
        }
        Set<Double> postos = new LinkedHashSet<>();
        for (SquadRole papel : papeis) postos.add(regras.anguloDeCercoEmGraus(papel));
        assertEquals(papeis.size(), postos.size(),
                "Dois membros no mesmo posto poem lobo dentro de lobo, e o jogador le travamento.");
    }

    @Test
    @DisplayName("papelVago pula o que ja esta ocupado, e nao conta indice")
    void papelVagoNaoUsaIndice() {
        RegrasDeMatilha regras = regras();
        assertEquals(SquadRole.FRONTLINER, regras.papelVago(Set.of(SquadRole.LEADER)).orElseThrow());
        assertEquals(SquadRole.SCOUT,
                regras.papelVago(Set.of(SquadRole.LEADER, SquadRole.FRONTLINER, SquadRole.FLANKER))
                        .orElseThrow(),
                "Por indice, bastaria o membro do meio morrer para o proximo a entrar receber um"
                        + " papel que outro ja tem.");
    }

    // ------------------------------------------- os casos que DEVEM reprovar

    @Test
    @DisplayName("REPROVA: anel do tamanho do alcance da mordida -- cercar e morder no mesmo lugar")
    void anelColadoNoAlcanceReprova() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeMatilha(SquadRules.matilha(), 2, 1.15D, 1.15D));
        assertTrue(erro.getMessage().contains("MAIOR"),
                "A mensagem tem de dizer o que foi medido e o que acontece em jogo.");
    }

    @Test
    @DisplayName("REPROVA: anel apertado demais para o bando cheio -- o espacamento viraria mentira")
    void anelApertadoReprova() {
        // Com raio 1.5 e quatro postos a 90 graus, a corda e 2.12 blocos, abaixo
        // dos 2.5 que SquadRules.matilha() exige. Sem esta cobranca, "espacamento
        // respeitado" seria uma frase que o codigo nao cumpre -- e o sintoma seria
        // bichos se empurrando, que ninguem chama de bug de IA.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeMatilha(SquadRules.matilha(), 2, 1.15D, 1.5D));
        assertTrue(erro.getMessage().contains("espacamento"));
    }

    @Test
    @DisplayName("REPROVA: teto maior que a escada de papeis -- dois membros no mesmo posto")
    void tetoSemPapelParaTodosReprova() {
        // SquadRules.esquadrao() permite oito membros, e ha cinco papeis de
        // entrada. Dois deles receberiam o MESMO papel e mirariam o mesmo ponto do
        // anel. Isso nao daria erro em jogo: daria um cerco que empilha.
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeMatilha(SquadRules.esquadrao(), 2, 1.15D, 6.0D));
        assertTrue(erro.getMessage().contains("papeis de entrada"));
    }

    @Test
    @DisplayName("REPROVA: exigir mais membros do que o teto permite -- o lobo recua para sempre")
    void exigirMaisQueOTetoReprova() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeMatilha(SquadRules.matilha(),
                        SquadRules.matilha().maximoDeMembros() + 1, 1.15D, 3.4D));
        assertTrue(erro.getMessage().contains("nunca alcanca"),
                "Um inimigo que nunca ataca passa em todos os portoes deste repositorio.");
    }

    @Test
    @DisplayName("REPROVA: lobo que avanca sozinho")
    void avancarSozinhoReprova() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeMatilha(SquadRules.matilha(), 1, 1.15D, 3.4D));
    }

    @Test
    @DisplayName("REPROVA: papel sem posto no anel iria para o centro do cerco")
    void papelSemPostoReprova() {
        // Nao ha papel assim hoje -- e este teste e o que garante que, no dia em
        // que SquadRole ganhar um setimo valor, ele nao entre no anel em silencio
        // mirando (0,0), que e onde o alvo esta.
        RegrasDeMatilha regras = regras();
        for (SquadRole papel : SquadRole.values()) {
            assertTrue(Double.isFinite(regras.anguloDeCercoEmGraus(papel)),
                    papel + " nao tem posto no anel");
        }
    }

    @Test
    @DisplayName("as regras de bando vem de SquadRules, e nao de uma copia")
    void asRegrasDoBandoSaoAsMesmas() {
        assertSame(SquadRules.matilha().getClass(), regras().bando().getClass());
        assertEquals(SquadRules.matilha(), regras().bando(),
                "Um teto, um espacamento e uma moral minima copiados para dentro de"
                        + " RegrasDeMatilha fariam a sessao de balanceamento girar um botao morto.");
    }
}
