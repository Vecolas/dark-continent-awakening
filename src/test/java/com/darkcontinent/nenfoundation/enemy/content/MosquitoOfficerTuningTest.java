package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDaPicada;
import com.darkcontinent.nenfoundation.enemy.combat.DecisaoDeDreno;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDaPicada;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeDreno;
import com.darkcontinent.nenfoundation.enemy.combat.ResultadoDeDreno;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros que o Mosquito Officer vai levar para o jogo, medidos contra a ficha
 * dele -- e nao contra si mesmos.
 *
 * <p>{@code RegrasDeDrenoTest} e {@code RegrasDaPicadaTest} provam as REGRAS com
 * numeros proprios. Este arquivo prova os VALORES DE PRODUCAO: que a combinacao
 * declarada em {@link MosquitoOfficerTuning} e construivel, que a agulha alcanca
 * quem ela decidiu picar, que o teto do dreno e alcancavel e cabe na vida dela, e
 * que o gerador de arte esta cobrando exatamente a caixa de dano que o servidor
 * usa.</p>
 *
 * <p>Nenhuma dessas coisas levanta erro quando quebra: o mod carrega, o oficial
 * nasce, voa, pica, cura e morre. So o que ele ENSINA deixa de funcionar.</p>
 */
class MosquitoOfficerTuningTest {

    /** O gerador de geometria dela; ele COPIA numeros deste arquivo. */
    private static final String GERADOR_DE_GEOMETRIA =
            "art-source/enemies/mosquito_officer/mosquito_officer_geo.py";

    private static final float VIDA_MAXIMA =
            ChimeraProfiles.mosquitoOfficer().attributes().maxHealth();

    // ------------------------------------------------------------- construivel

    @Test
    @DisplayName("os numeros declarados formam regras de dreno e de flanco validas")
    void osNumerosDeProducaoSaoConstruiveis() {
        RegrasDeDreno dreno = MosquitoOfficerTuning.regrasDeDreno();
        assertEquals(MosquitoOfficerTuning.FRACAO_DRENADA, dreno.fracaoDoDano());
        assertEquals(MosquitoOfficerTuning.TETO_DE_DRENO_POR_ENCONTRO, dreno.tetoDoEncontro());
        assertEquals(MosquitoOfficerTuning.CURA_MINIMA_DO_DRENO, dreno.curaMinima());

        RegrasDaPicada flanco = MosquitoOfficerTuning.regrasDaPicada();
        assertEquals(MosquitoOfficerTuning.RAIO_DO_CONTORNO, flanco.raioDoContorno());
        assertEquals(MosquitoOfficerTuning.ALCANCE_DA_PICADA, flanco.distanciaDeInvestida());
        assertEquals(Math.cos(Math.toRadians(MosquitoOfficerTuning.ARCO_FRONTAL_DO_ALVO_EM_GRAUS)),
                flanco.cossenoDoArcoFrontal(),
                "o cosseno tem de sair do angulo, e nao de um literal ao lado: com duas fontes,"
                        + " estreitar o angulo deixaria o oficial atacando num arco diferente"
                        + " daquele que a documentacao promete");
        // Os dois construtores ja cobram a continencia (piso menor que teto, anel
        // fora do alcance, arco nao degenerado). Estas chamadas sao o que garante
        // que a cobranca de fato roda sobre os numeros de PRODUCAO, e nao so
        // sobre os dos testes de regra.
    }

    // ----------------------------------------------------------------- alcance

    @Test
    @DisplayName("a agulha alcanca quem ela decidiu picar")
    void oAlcanceDeDecisaoCabeNaAgulha() {
        AttackHitbox caixa = MosquitoOfficerTuning.caixaDaPicada();
        double alcanceUtil = caixa.maxZ() + MosquitoOfficerTuning.MEIA_LARGURA_DE_UM_ALVO;
        assertTrue(MosquitoOfficerTuning.ALCANCE_DA_PICADA <= alcanceUtil,
                "ela decide picar a " + MosquitoOfficerTuning.ALCANCE_DA_PICADA + " e a agulha so"
                        + " cobre " + alcanceUtil + ": a picada erraria TODA vez, o oficial ficaria"
                        + " num ciclo de aviso e recuperacao sem nunca acertar, e isso nao levanta"
                        + " erro nenhum -- parece um mob quebrado");
    }

    @Test
    @DisplayName("a caixa da picada e uma agulha, e nao uma varredura")
    void aCaixaEEstreita() {
        AttackHitbox caixa = MosquitoOfficerTuning.caixaDaPicada();
        double arco = caixa.maxX() - caixa.minX();
        assertTrue(arco < caixa.maxZ() * 2.0D,
                "o arco de " + arco + " blocos e largo demais para uma agulha: alargar a caixa"
                        + " daria um oficial que acerta sem precisar mirar, e o unico preco que"
                        + " ele paga pela cura que recebe -- ter de encostar exatamente --"
                        + " desapareceria sem que nada acusasse");
        // Um jogador tem 1.8 blocos de altura. A faixa tem de ser uma FAIXA: comecar
        // em zero faria a picada valer no chao, e terminar acima da cabeca a faria
        // valer no ar -- os dois transformam a agulha numa coluna, e uma coluna
        // acerta sem que ninguem precise mirar.
        assertTrue(caixa.minY() > 0.0D && caixa.maxY() < 1.8D,
                "a faixa de altura vai de " + caixa.minY() + " a " + caixa.maxY()
                        + ": fora de (0, 1.8) ela deixa de ser uma faixa e vira a coluna inteira");
    }

    @Test
    @DisplayName("o gerador de arte cobra EXATAMENTE a caixa que o servidor usa")
    void oDesenhoEARegraFalamDoMesmoNumero() {
        String gerador = Repo.texto(GERADOR_DE_GEOMETRIA);
        AttackHitbox caixa = MosquitoOfficerTuning.caixaDaPicada();
        // O gerador de geometria COPIA os tres numeros e reprova o desenho contra
        // eles. E duplicacao declarada: as duas pontas de um portao que morde dos
        // dois lados. Sem esta conferencia, mudar a caixa aqui deixaria o gerador
        // medindo a caixa ANTIGA e aprovando uma agulha curta demais -- e o
        // sintoma seria um bicho que encosta e nao acerta nada.
        exigirCopia(gerador, "ALCANCE_DA_PICADA_EM_BLOCOS", caixa.maxZ());
        exigirCopia(gerador, "PISO_DA_PICADA_EM_BLOCOS", caixa.minY());
        exigirCopia(gerador, "TETO_DA_PICADA_EM_BLOCOS", caixa.maxY());
    }

    private static void exigirCopia(String gerador, String constante, double valor) {
        String esperado = constante + " = " + valor;
        assertTrue(gerador.contains(esperado),
                "o gerador de geometria precisa conter a linha '" + esperado + "'. Ele copia esse"
                        + " numero do servidor para cobrar o desenho contra ele; copia velha"
                        + " significa um portao medindo uma caixa que nao existe mais, e portao"
                        + " que mede a coisa errada e pior do que portao nenhum.");
    }

    // --------------------------------------------------------------- telegrafo

    @Test
    @DisplayName("a picada e telegrafada: o aviso e mais longo que a janela que machuca")
    void aPicadaETelegrafada() {
        AttackDefinition picada = MosquitoOfficerTuning.picada();
        assertTrue(picada.windupTicks() > picada.activeTicks(),
                "windup mais curto que a janela deixa de ser aviso e vira um mob que bate sem"
                        + " telegrafo, com o mesmo dano e o mesmo log limpo");
        assertTrue(picada.recoveryTicks() > picada.activeTicks(),
                "sem recuperacao longa nao ha janela de punicao, e um bicho de armadura 2 que"
                        + " nao se expoe depois do golpe e um bicho sem preco");
        assertTrue(MosquitoOfficerTuning.TICKS_DE_MIRA_NO_WINDUP < picada.windupTicks(),
                "mirar o aviso inteiro daria uma agulha mira-laser no mob mais rapido da"
                        + " familia: o desvio deixaria de existir e a unica defesa restante"
                        + " seria mata-la antes");
        assertEquals(ChimeraProfiles.mosquitoOfficer().attributes().attackDamage(),
                picada.damage(),
                "o dano e LIDO do perfil: repetido aqui, girar o atributo numa sessao de"
                        + " balanceamento mudaria a barra de vida do jogador, nao este arquivo --"
                        + " e nao mudaria o dreno, que e uma FRACAO desse dano");
    }

    @Test
    @DisplayName("a picada nao empurra, e o zero e a mecanica")
    void aPicadaNaoEmpurra() {
        assertEquals(0.0F, MosquitoOfficerTuning.EMPURRAO_DA_PICADA,
                "qualquer empurrao tiraria a vitima dos "
                        + MosquitoOfficerTuning.ALCANCE_DA_PICADA + " blocos de alcance da propria"
                        + " agulha, e a cadeia picar-curar-picar -- que e a ficha do bicho --"
                        + " deixaria de existir. Isso nao daria erro: daria um oficial rapido que"
                        + " bate uma vez e some.");
        assertEquals(0.0F, MosquitoOfficerTuning.picada().knockback(),
                "o valor tem de chegar INTEIRO na definicao do ataque");
    }

    @Test
    @DisplayName("a recarga de interrupcao e mais longa que a recarga normal")
    void interromperTemDeValer() {
        assertTrue(MosquitoOfficerTuning.RECARGA_APOS_INTERRUPCAO
                        > ChimeraProfiles.mosquitoOfficerRecarga(),
                "sem isso a oficial interrompida volta a picar mais depressa do que se ninguem"
                        + " tivesse batido, e o jogador aprende a NAO interromper -- que e o"
                        + " oposto do que uma armadura 2 existe para ensinar");
    }

    // ------------------------------------------------------------------- dreno

    @Test
    @DisplayName("o teto do dreno e alcancavel, e para dentro da vida dela")
    void oTetoSeparaTensaDeImpossivel() {
        assertTrue(MosquitoOfficerTuning.TETO_DE_DRENO_POR_ENCONTRO < VIDA_MAXIMA,
                "um teto maior que a vida maxima e o mesmo que nao ter teto: ela poderia refazer"
                        + " a barra inteira dentro de um encontro so, e o combate longo nunca"
                        + " acabaria");
        assertTrue(MosquitoOfficerTuning.TETO_DE_DRENO_POR_ENCONTRO > VIDA_MAXIMA * 0.15F,
                "um teto pequeno demais faz o dreno virar decoracao: o jogador nunca ve a barra"
                        + " voltar, e a ficha inteira do bicho deixa de ser observavel");
    }

    @Test
    @DisplayName("o teto cabe num numero de picadas que o jogador consegue contar")
    void oTetoEUmNumeroDePicadas() {
        RegrasDeDreno dreno = MosquitoOfficerTuning.regrasDeDreno();
        float dano = ChimeraProfiles.mosquitoOfficer().attributes().attackDamage();
        float acumulado = 0.0F;
        int picadas = 0;
        while (true) {
            ResultadoDeDreno r = dreno.decidir(dano, true, true, 1.0F, VIDA_MAXIMA, acumulado);
            if (r.motivo() != DecisaoDeDreno.DRENA) {
                assertEquals(DecisaoDeDreno.TETO_ATINGIDO, r.motivo(),
                        "com a vida no fundo e a vitima valida, a unica recusa possivel e o teto");
                break;
            }
            acumulado += r.cura();
            picadas++;
            assertTrue(picadas < 100, "o teto nao esta segurando: cem picadas e loop, nao mecanica");
        }
        assertEquals(MosquitoOfficerTuning.TETO_DE_DRENO_POR_ENCONTRO, acumulado,
                "o total devolvido por um encontro e exatamente o teto");
        assertTrue(picadas >= 3 && picadas <= 10,
                "sao " + picadas + " picadas ate o teto. Menos de tres e um limite que ninguem"
                        + " sente; mais de dez e um combate em que o jogador nunca percebe que o"
                        + " dreno acabou -- e nos dois casos o numero some da experiencia sem"
                        + " que nada reprove");
    }

    @Test
    @DisplayName("uma picada devolve menos do que a vitima perdeu")
    void aPicadaNaoEUmaTroca() {
        RegrasDeDreno dreno = MosquitoOfficerTuning.regrasDeDreno();
        float dano = ChimeraProfiles.mosquitoOfficer().attributes().attackDamage();
        ResultadoDeDreno r = dreno.decidir(dano, true, true, 1.0F, VIDA_MAXIMA, 0.0F);
        assertEquals(DecisaoDeDreno.DRENA, r.motivo());
        assertTrue(r.cura() < dano,
                "devolver o dano inteiro faria cada picada valer duas vezes -- tira 8 do jogador e"
                        + " poe 8 nela -- e a corrida de vida ficaria empatada num bicho que tem"
                        + " mais alcance de decisao do que o jogador tem de reacao");
    }

    // ------------------------------------------------------------------ flanco

    @Test
    @DisplayName("com os numeros de producao, atacar de frente e recusado")
    void deFrenteElaRecusa() {
        RegrasDaPicada flanco = MosquitoOfficerTuning.regrasDaPicada();
        assertEquals(DecisaoDaPicada.ENCARADA,
                flanco.decidir(true, false, 0.1D, 1.0D, true),
                "colada e de frente ela ainda recusa: e a defesa inteira do encontro");
        assertEquals(DecisaoDaPicada.PICA,
                flanco.decidir(true, false, MosquitoOfficerTuning.ALCANCE_DA_PICADA, -1.0D, true),
                "pelas costas e exatamente no alcance declarado ela pica: se nao picasse, o"
                        + " alcance publicado seria maior que o alcance real e o oficial pararia"
                        + " a um passo do jogador sem atacar");
    }

    @Test
    @DisplayName("o anel de espera fica FORA do alcance da agulha")
    void oAnelFicaForaDoAlcance() {
        assertTrue(MosquitoOfficerTuning.RAIO_DO_CONTORNO > MosquitoOfficerTuning.ALCANCE_DA_PICADA,
                "anel dentro do alcance deixaria o oficial parado onde ele ja podia picar,"
                        + " oscilando entre recuar e atacar no mesmo ponto -- o que o jogador le"
                        + " como bicho travado, nunca como tatica");
        assertTrue(MosquitoOfficerTuning.ARCO_FRONTAL_DO_ALVO_EM_GRAUS > 0.0D
                        && MosquitoOfficerTuning.ARCO_FRONTAL_DO_ALVO_EM_GRAUS < 90.0D,
                "em 0 graus o arco proibido some e o flanco deixa de existir; em 90 ou mais ela"
                        + " so ataca de tras do ombro do jogador e praticamente nunca ataca");
    }

    // ------------------------------------------------- o que DEVE reprovar

    @Test
    @DisplayName("a regua morde: trocar anel por alcance e recusado na construcao")
    void aReguaReprovaOsNumerosTrocados() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDaPicada(
                        Math.cos(Math.toRadians(MosquitoOfficerTuning.ARCO_FRONTAL_DO_ALVO_EM_GRAUS)),
                        MosquitoOfficerTuning.ALCANCE_DA_PICADA,
                        MosquitoOfficerTuning.RAIO_DO_CONTORNO),
                "com os dois numeros trocados o anel cai dentro do alcance, e o construtor tem de"
                        + " reprovar: sem isso, a troca passaria como um oficial que ataca de"
                        + " qualquer lugar do anel e nunca reposiciona");
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeDreno(MosquitoOfficerTuning.FRACAO_DRENADA,
                        MosquitoOfficerTuning.CURA_MINIMA_DO_DRENO,
                        MosquitoOfficerTuning.TETO_DE_DRENO_POR_ENCONTRO),
                "com teto e piso trocados nenhuma picada pagaria, e o oficial viraria um mob"
                        + " fraco sem mecanica nenhuma -- sem uma linha de log");
    }
}
