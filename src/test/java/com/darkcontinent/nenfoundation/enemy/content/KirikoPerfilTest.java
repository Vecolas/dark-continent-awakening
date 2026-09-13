package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.data.EnemyDefinition;
import com.darkcontinent.nenfoundation.enemy.encounter.RegrasDeJulgamento;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que o perfil do kiriko e a unica fonte dos numeros do julgamento, e -- mais
 * importante que tudo o que ja foi medido neste repositorio -- que depois de qualquer
 * sessao de balanceamento o kiriko continua sendo um TESTE e nao um inimigo.
 *
 * <p>A ficha e explicita: "nao deve ser tratado como inimigo comum". O jogador vence este
 * encontro NAO LUTANDO. Quem espera e aprovado e ganha recompensa; quem ataca o kiriko ou
 * fere um bicho pacifico na frente dele e reprovado e ai sim leva briga.</p>
 *
 * <p>ESTE E O MOB MAIS FACIL DE ARRUINAR EM SILENCIO DO REPOSITORIO. Nenhum dos dois modos
 * de quebra-lo produz erro: um kiriko cuja aprovacao demora demais nunca e aprovado por
 * ninguem (o jogador vai embora antes, e o encontro social vira um bicho estranho parado
 * na floresta); um kiriko que aguenta um golpe sem reprovar ensina que a violencia era uma
 * resposta aceita. Nos dois casos o build segue verde, o mob spawna, anima, ataca e morre
 * -- e a ficha inteira foi ignorada. As duas contas do fim deste arquivo existem so para
 * isso.</p>
 */
class KirikoPerfilTest {

    /**
     * Trinta segundos. E o TETO do que se pode pedir a um jogador que esta parado de
     * proposito na frente de um bicho que nao faz nada.
     *
     * <p>Nao e botao de balanceamento e por isso nao mora em config: e a medida de
     * paciencia humana contra a qual a janela de observacao e conferida. Passado isso, o
     * jogador ja concluiu que o mob esta bugado e foi embora -- e a aprovacao, que e o
     * desfecho BOM do encontro, passa a ser um caminho que existe so no codigo.</p>
     */
    private static final int TETO_RAZOAVEL_DE_ESPERA = 600;

    /** Ticks por segundo do jogo. Nao e ajustavel: e a regra do jogo base. */
    private static final int TICKS_POR_SEGUNDO = 20;

    // ------------------------------------------------------------------- o perfil

    @Test
    void atributosDoKirikoMoramNoPerfil() {
        EnemyDefinition definicao = HunterExamProfiles.kiriko();
        assertEquals("nenfoundation:kiriko", definicao.metadata().id().toString());
        assertEquals(ThreatTier.ELITE, definicao.metadata().threatTier(),
                "ELITE e um aviso para quem for integrar o mob depois: ele nao entra em "
                        + "lista de fauna comum nem em onda de spawn hostil");
        assertEquals(40.0F, definicao.attributes().maxHealth());
        assertEquals(0.32F, definicao.attributes().movementSpeed());
        assertEquals(7.0F, definicao.attributes().attackDamage());
        assertEquals(3.0F, definicao.attributes().armor());
        assertEquals(24.0F, definicao.attributes().followRange());
        assertEquals(0.2F, definicao.attributes().knockbackResistance());
    }

    /**
     * Nem territorial nem gregario, e as duas negativas sao a ficha.
     *
     * <p>Territorial faria dele um guardiao de lugar, que ataca quem passa perto -- e o
     * jogador seria agredido ANTES de existir qualquer comportamento para julgar, que e
     * exatamente o mob que a ficha proibe. Social faria bando, e um bando de juizes nao
     * julga: o primeiro que reprovar arrasta os outros, e a decisao individual -- que e a
     * mecanica inteira -- desaparece.</p>
     */
    @Test
    void oKirikoNaoEterritorialNemGregario() {
        EnemyDefinition definicao = HunterExamProfiles.kiriko();
        assertFalse(definicao.metadata().territorial(),
                "territorial e um kiriko que ataca quem entrou no raio dele: o julgamento "
                        + "nunca comeca, porque a briga comeca antes");
        assertFalse(definicao.metadata().social(),
                "social e um bando, e um bando transforma o encontro social num combate "
                        + "contra varios -- alem de quebrar o 'raro' que a ficha pede");
    }

    @Test
    void aRegraDeSpawnPoeUmKirikoRaroEmTerraFirme() {
        EnemyDefinition definicao = HunterExamProfiles.kiriko();
        assertTrue(definicao.spawnRule().biomeTags()
                        .contains("#nenfoundation:magical_beast_biomes"),
                "a tag e o que liga o perfil ao biome modifier; trocada, o mob nunca nasce");
        assertTrue(definicao.spawnRule().requireGround(),
                "ele aparece EM PE, em forma humana, num caminho onde alguem passa: sem "
                        + "chao o disfarce nasce dentro de uma arvore e nao engana ninguem");
        assertFalse(definicao.spawnRule().allowWater(),
                "um viajante humano boiando no rio nao e disfarce, e denuncia");
        assertEquals(0, definicao.spawnRule().minLight());
        assertEquals(15, definicao.spawnRule().maxLight(),
                "o encontro e de DIA tanto quanto de noite: exigir escuridao faria o kiriko "
                        + "so aparecer quando ninguem esta viajando, e a floresta ficaria "
                        + "vazia sem que nada reclamasse");
        assertEquals(1, definicao.spawnRule().maxNearbySameFaction(),
                "UM por regiao e a ficha: dois kirikos julgando o mesmo jogador ao mesmo "
                        + "tempo transformam o teste numa emboscada");
    }

    /**
     * O golpe existe, mas ele e o que acontece DEPOIS da reprovacao -- nunca a abertura do
     * encontro.
     *
     * <p>Os 10 ticks de windup sao o aviso: meio segundo entre a revelacao e a pancada. Ele
     * importa mais neste mob do que nos outros porque o jogador que acabou de ser reprovado
     * esta, por definicao, olhando para um bicho que ate agora nao tinha reagido a nada.
     * Encurtar isso transforma a reprovacao em morte sem telegrafo.</p>
     */
    @Test
    void numerosDoGolpeMoramNoPerfil() {
        AttackDefinition strike = HunterExamProfiles.kirikoStrike();
        assertEquals("strike", strike.id());
        assertEquals(10, strike.windupTicks(),
                "sem windup a reprovacao vira dano vindo de um mob que nunca tinha atacado");
        assertEquals(5, strike.activeTicks());
        assertEquals(14, strike.recoveryTicks());
        assertEquals(0.6F, strike.knockback());
    }

    // ---------------------------------------------------------------- o julgamento

    @Test
    void numerosDoJulgamentoMoramNoPerfil() {
        RegrasDeJulgamento julgamento = HunterExamProfiles.kirikoJulgamento();
        assertEquals(200, julgamento.ticksDeObservacao());
        assertEquals(80, julgamento.custoDeAgressao());
        assertEquals(25, julgamento.custoDeCrueldade());
        assertEquals(1, julgamento.ganhoPorPaciencia());
        assertEquals(60, julgamento.limiteDeAprovacao());
    }

    /**
     * O golpe e o ataque comum do kiriko: o dano dele E o ATTACK_DAMAGE do perfil, nao uma
     * copia dele.
     *
     * <p>A comparacao le os DOIS lados em vez de conferir cada um contra um literal. Um
     * literal aprovaria em silencio o dia em que alguem girasse o atributo numa sessao de
     * balanceamento e o golpe continuasse batendo o numero antigo -- a representacao
     * prometeria um dano e o jogador reprovado levaria outro.</p>
     */
    @Test
    void oDanoDoGolpeEOMesmoAttackDamageDoPerfil() {
        float atributo = HunterExamProfiles.kiriko().attributes().attackDamage();
        float golpe = HunterExamProfiles.kirikoStrike().damage();

        assertEquals(atributo, golpe,
                "o atributo diz " + atributo + " e o golpe bate " + golpe + ": viraram dois "
                        + "numeros para a mesma verdade, e girar um so nao produz erro "
                        + "nenhum -- produz um ataque que mente sobre o proprio dano.");
    }

    // ------------------------------------------------------------- A REGUA DO MOB

    /**
     * PRIMEIRA CONTA: QUEM SO ESPERA E APROVADO, E DENTRO DE UM TEMPO QUE ALGUEM AGUENTA.
     *
     * <p>Este e o desfecho BOM do encontro e o unico que justifica o mob existir. Se ele
     * nao couber no tempo de espera de uma pessoa, ninguem nunca o alcanca: o jogador fica
     * parado, se cansa, vai embora, e o kiriko que ele conheceu e um bicho que nao fazia
     * nada. A aprovacao continua no codigo, verde, sem nunca acontecer em jogo.</p>
     *
     * <p>A conta NAO esta escrita como literal: ela e rodada contra o proprio
     * {@code pontuar}/{@code aprova}. Um numero fechado a mao seria uma SEGUNDA fonte da
     * mesma verdade -- mudar a formula da nota deixaria este teste verde medindo uma regra
     * que o jogo nao usa mais.</p>
     */
    @Test
    void quemApenasEsperaEAprovadoDentroDeUmTempoRazoavel() {
        RegrasDeJulgamento julgamento = HunterExamProfiles.kirikoJulgamento();

        int nota = 0;
        int tickDaAprovacao = -1;
        for (int tick = 1; tick <= TETO_RAZOAVEL_DE_ESPERA && tickDaAprovacao < 0; tick++) {
            nota = julgamento.pontuar(nota, false, false, true);
            assertFalse(julgamento.reprova(nota),
                    "o jogador que so esperou foi REPROVADO no tick " + tick + " (nota "
                            + nota + "). Nao existe mais resposta certa neste encontro: "
                            + "esperar reprova e atacar reprova, entao o kiriko ataca todo "
                            + "mundo e a ficha inteira virou enfeite.");
            if (julgamento.aprova(nota, tick)) tickDaAprovacao = tick;
        }

        assertTrue(tickDaAprovacao > 0,
                "um jogador parado, sem sacar arma e sem ferir nada, atravessou "
                        + TETO_RAZOAVEL_DE_ESPERA + " ticks ("
                        + (TETO_RAZOAVEL_DE_ESPERA / TICKS_POR_SEGUNDO) + "s) SEM ser "
                        + "aprovado (nota final " + nota + ", limite "
                        + julgamento.limiteDeAprovacao() + ", janela "
                        + julgamento.ticksDeObservacao() + " ticks). A aprovacao existe so "
                        + "no codigo: em jogo ninguem espera tanto parado na frente de um "
                        + "bicho que nao reage. Desca ticksDeObservacao, desca "
                        + "limiteDeAprovacao ou suba ganhoPorPaciencia.");

        assertEquals(julgamento.ticksDeObservacao(), tickDaAprovacao,
                "a aprovacao saiu no tick " + tickDaAprovacao + " e a janela de observacao e "
                        + julgamento.ticksDeObservacao() + ". Enquanto a nota chegar ao "
                        + "limite ANTES da janela, quem manda no tempo do encontro e a "
                        + "janela -- que e a intencao. Quando a nota passa a ser o gargalo, "
                        + "ticksDeObservacao vira um numero morto na config e girar ele numa "
                        + "sessao de balanceamento nao muda nada em jogo.");
    }

    /**
     * SEGUNDA CONTA: UM UNICO GOLPE REPROVA NA HORA.
     *
     * <p>O par da primeira, e sem ele a primeira aprovaria um mob quebrado. A aprovacao
     * funcionar nao serve de nada se agredir tambem terminar em aprovacao: o jogador
     * atacaria por curiosidade no primeiro encontro, seria recompensado assim mesmo, e a
     * licao que ele levaria e a oposta da que o mob veio dar.</p>
     *
     * <p>A conta e feita do ZERO de proposito -- e o estado em que o jogador encontra o
     * kiriko. Ver o ponto cego declarado no fim deste arquivo.</p>
     */
    @Test
    void umUnicoGolpeNoKirikoReprovaNaHora() {
        RegrasDeJulgamento julgamento = HunterExamProfiles.kirikoJulgamento();

        // DO PIOR CASO, e nao do zero. A primeira versao desta regua media a partir de
        // 0 e declarava como ponto cego que nao cobria o aluno exemplar -- aquele que
        // esperou ate o teto e so entao bateu. Era justamente esse o buraco: com o custo
        // antigo (40) contra o limite (60), quem esperou saia de 60 para 20 e era
        // PERDOADO. Medir do teto e o que transforma "quem ataca e reprovado" de
        // promessa em conta.
        int melhorNotaPossivel = julgamento.limiteDeAprovacao();
        assertTrue(julgamento.pontuar(melhorNotaPossivel, true, false, false) < 0,
                "o jogador que esperou ate a melhor nota possivel (" + melhorNotaPossivel
                        + ") bateu no kiriko e continuou aprovavel. custoDeAgressao precisa"
                        + " superar o limite de aprovacao, senao paciencia vira credito para"
                        + " bater -- e o mob ensina o oposto do que veio ensinar.");

        int depoisDoGolpe = julgamento.pontuar(0, true, false, false);
        assertTrue(depoisDoGolpe < 0,
                "um golpe no kiriko levou a nota de 0 para " + depoisDoGolpe
                        + ", que nao e negativo. custoDeAgressao ("
                        + julgamento.custoDeAgressao() + ") precisa ser suficiente para "
                        + "afundar a nota inicial sozinho -- senao bater sai de graca no "
                        + "primeiro encontro, que e exatamente quando o jogador testa.");
        assertTrue(julgamento.reprova(depoisDoGolpe),
                "a nota ficou em " + depoisDoGolpe + " e o kiriko nao chamou isso de "
                        + "reprovacao. O mob aguenta pancada calado: ele deixou de ser um "
                        + "teste e virou um inimigo comum com passos extras.");

        assertTrue(julgamento.reprova(julgamento.pontuar(0, false, true, false)),
                "ferir um bicho pacifico na frente dele tambem reprova do zero: metade do "
                        + "julgamento nao e sobre o kiriko, e sobre o jogador");
    }

    /**
     * A TERCEIRA METADE DA MESMA REGUA: a janela de observacao nao pode ser decorativa.
     *
     * <p>A nota bate no limite em {@code limiteDeAprovacao / ganhoPorPaciencia} ticks. Se
     * a aprovacao dependesse so dela, o kiriko premiaria antes de ter observado, e o
     * "encounter social" viraria uma entrega de item por proximidade -- sem erro nenhum.
     * Este teste prova que naquele tick o jogador AINDA NAO foi aprovado.</p>
     */
    @Test
    void aNotaSozinhaNaoAprovaAntesDaJanelaDeObservacao() {
        RegrasDeJulgamento julgamento = HunterExamProfiles.kirikoJulgamento();

        int ticksAteALimite = 0;
        int nota = 0;
        while (nota < julgamento.limiteDeAprovacao()) {
            nota = julgamento.pontuar(nota, false, false, true);
            ticksAteALimite++;
        }

        assertTrue(ticksAteALimite < julgamento.ticksDeObservacao(),
                "a nota chega ao limite em " + ticksAteALimite + " ticks e a janela de "
                        + "observacao e de " + julgamento.ticksDeObservacao()
                        + ": a janela deixou de ser o gargalo e virou numero morto");
        assertFalse(julgamento.aprova(nota, ticksAteALimite),
                "no tick " + ticksAteALimite + " a nota ja e " + nota + " e a aprovacao "
                        + "saiu, com apenas " + (ticksAteALimite / (double) TICKS_POR_SEGUNDO)
                        + "s de encontro. O kiriko premiou sem observar, e o teste que da "
                        + "nome ao mob deixou de existir.");
    }

    // --------------------------------------------------------------- o portao

    /**
     * O mapa de publicados e o que o portao de spawn varre. Faltar aqui nao acende alarme
     * nenhum -- o portao segue verde varrendo um mob a menos, e o kiriko nunca aparece no
     * mundo por falta de tag ou de biome modifier.
     */
    @Test
    void oKirikoEstaNoMapaDePublicadosSobOSeuProprioId() {
        EnemyDefinition publicado = HunterExamProfiles.publicados().get("kiriko");
        assertNotNull(publicado,
                "kiriko fora de publicados(): o portao de spawn nao varre a tag de bioma "
                        + "dele, e um mob sem tag ou sem biome modifier simplesmente nunca "
                        + "nasce, sem erro nenhum");
        assertEquals("nenfoundation:kiriko", publicado.metadata().id().toString(),
                "a chave do mapa e o id da entidade divergiram, e nada em jogo notaria");
    }

    // ------------------------------------------------------ PONTO CEGO DECLARADO
    //
    // ESTE ARQUIVO NAO PROVA QUE UM GOLPE REPROVA EM QUALQUER MOMENTO DO ENCONTRO --
    // so no inicio dele, com a nota em zero, que e onde o jogador de fato testa.
    //
    // Com os numeros do contrato a diferenca e grande: a paciencia rende +1 por tick e
    // a janela de observacao tem 200 ticks, entao quem espera a janela inteira chega ao
    // fim dela com nota 200, enquanto um golpe custa 40. Nesse ponto seriam precisos SEIS
    // golpes para a nota ficar NEGATIVA (cinco levam a zero, e zero nao reprova). Ou
    // seja: o kiriko e implacavel com quem
    // ataca cedo e complacente com quem atacou depois de esperar muito.
    //
    // Isso NAO e medido aqui de proposito, por duas razoes. A primeira e que a regra que
    // decide isso nao esta neste lado da fronteira: RegrasDeJulgamento e um record puro e
    // nao lembra de nada, e "uma vez reprovado nao se volta atras" e uma trava que mora em
    // KirikoEntity (ver RegrasDeJulgamentoTest#oRecordNaoLatchaAReprovacaoEPorIssoQuemChamaPrecisaLatchar).
    // Se a entidade trava a reprovacao no primeiro golpe, como o contrato manda, a nota
    // acumulada deixa de importar e o ponto cego nao existe em jogo.
    //
    // A segunda e que transformar isso em assercao aqui seria escolher um balanceamento
    // que ninguem aprovou: exigir custoDeAgressao > ganhoPorPaciencia * ticksDeObservacao
    // reprovaria os numeros do contrato hoje. A pergunta e legitima e pertence a uma
    // sessao de balanceamento, nao a um portao escrito por esta lane.
    //
    // O QUE FECHA ESTE PONTO CEGO e um teste na entidade, no lado que tem a trava:
    // "reprovado uma vez, reprovado ate o fim do encontro". Enquanto ele nao existir, a
    // consequencia da reprovacao esta provada apenas no primeiro golpe.
}
