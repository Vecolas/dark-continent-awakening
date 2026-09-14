package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.combat.AttackDefinition;
import com.darkcontinent.nenfoundation.enemy.combat.GrabController;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRefusal;
import com.darkcontinent.nenfoundation.enemy.combat.GrabRules;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerRules;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import com.darkcontinent.nenfoundation.enemy.greedisland.CaptureCondition;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sustenta as relacoes do Melanin Lizard que NENHUM portao generico ve.
 *
 * <p>Cada caso aqui mede uma ligacao entre dois numeros que vivem em arquivos
 * diferentes. Nenhuma das divergencias abaixo levanta excecao, aparece no log ou
 * reprova um portao de formato: elas aparecem como um bicho que morde quem esta
 * pelas costas, uma janela de escape que ninguem usa, um preso flutuando fora da
 * boca, ou um card que nunca acontece.</p>
 */
class MelaninLizardPerfilTest {

    private static final String GEO =
            "src/main/resources/assets/nenfoundation/geo/entity/melanin_lizard.geo.json";
    private static final String REGISTRO =
            "src/main/java/com/darkcontinent/nenfoundation/enemy/registry/EnemyEntityTypes.java";

    /** Vida de um jogador cheio, em pontos. Nao e ajustavel: e a regra do jogo base. */
    private static final float VIDA_DE_UM_JOGADOR = 20.0F;

    // --------------------------------------------------------------- agarrao

    @Test
    @DisplayName("os numeros do agarrao moram no tuning, e sao estes")
    void numerosDoAgarraoMoramNoTuning() {
        GrabRules regras = MelaninLizardTuning.agarrao();
        assertEquals(80, regras.ticksMaximos());
        assertEquals(20, regras.intervaloDeDano());
        assertEquals(2.0F, regras.danoPorPulso());
        assertEquals(9.0F, regras.danoParaEscapar());
    }

    /**
     * A REGUA DA JANELA DE ESCAPE, e o coracao deste mob.
     *
     * <p>Se aguentar calado custar menos do que reagir, a janela continua
     * existindo no codigo e ninguem nunca a usa: o mob vira um atraso de quatro
     * segundos em vez de uma briga. Isso nao reprova nada e nao aparece em log.</p>
     */
    @Test
    @DisplayName("bater tem de ser melhor do que esperar, senao a janela e decoracao")
    void baterEMelhorDoQueEsperar() {
        GrabRules regras = MelaninLizardTuning.agarrao();
        int pulsos = regras.ticksMaximos() / regras.intervaloDeDano();
        float custoDeEsperar = pulsos * regras.danoPorPulso();
        assertTrue(custoDeEsperar > 0.0F, "Agarrao que nao machuca nao cria urgencia nenhuma.");
        assertTrue(custoDeEsperar < VIDA_DE_UM_JOGADOR,
                "Esperar o relogio nao pode MATAR um jogador de vida cheia: a captura deste mob"
                        + " e nao-letal dos dois lados, e um agarrao que mata sem aviso e"
                        + " exatamente o que o plano proibe.");
        float vidaDoLagarto = GreedIslandProfiles.melaninLizard().attributes().maxHealth();
        assertTrue(regras.danoParaEscapar() < vidaDoLagarto * 0.5F,
                "Se soltar custasse mais da metade da vida do lagarto, reagir seria o mesmo que"
                        + " mata-lo -- e matar CANCELA o card deste bicho.");
    }

    /**
     * A vitima sai ANTES de o lagarto cambalear -- e por isso a tela nunca mente.
     *
     * <p>O clipe de stagger abre a boca. Se o limiar de stagger fosse menor que o
     * dano de escape, existiria um estado em que o bicho cambaleia com alguem
     * preso: boca aberta na tela, vitima ainda montada no servidor. Nao daria erro
     * nenhum -- daria um jogador que se ve solto e continua preso.</p>
     */
    @Test
    @DisplayName("o escape custa menos do que a interrupcao: ninguem cambaleia com alguem na boca")
    void escapeVemAntesDoStagger() {
        StaggerRules stagger = GreedIslandProfiles.melaninLizardStagger();
        assertTrue(MelaninLizardTuning.agarrao().danoParaEscapar() < stagger.limiar(),
                "escape " + MelaninLizardTuning.agarrao().danoParaEscapar()
                        + " tem de ser menor que o limiar de stagger " + stagger.limiar() + ".");
        // A resistencia so aumenta a folga: o stagger desconta dela cada golpe, e o
        // agarrao conta o dano cheio. Conferida para que ninguem a zere achando que
        // "nao muda nada" e encoste os dois limiares.
        assertTrue(stagger.resistencia() > 0.0F,
                "Resistencia zero encosta o stagger no escape e cria o estado incoerente.");
    }

    @Test
    @DisplayName("a boca recusa o que nao cabe, e a recusa tem motivo")
    void bocaPequenaRecusaComMotivo() {
        GrabController controlador = new GrabController(MelaninLizardTuning.agarrao(),
                MelaninLizardTuning.ALTURA_MAXIMA_DA_PRESA,
                MelaninLizardTuning.LARGURA_MAXIMA_DA_PRESA);
        // Jogador em pe: 0.6 x 1.8.
        assertEquals(GrabRefusal.NENHUMA, controlador.podeAgarrar(1.8D, 0.6D, false, true),
                "Se o jogador nao couber, o mob inteiro deixa de existir sem uma linha de log.");
        // Golem de ferro: 1.4 x 2.7. E o caso que a boca pequena existe para recusar.
        assertEquals(GrabRefusal.ALVO_GRANDE_DEMAIS, controlador.podeAgarrar(2.7D, 1.4D, false, true));
        assertEquals(GrabRefusal.ALVO_OCUPADO, controlador.podeAgarrar(1.8D, 0.6D, true, true),
                "Quem ja esta montado em outra coisa nao pode ser puxado para ca: o relato de"
                        + " bug seria 'as vezes ele me morde e nao me prende'.");
    }

    // ------------------------------------------------------------------ bote

    @Test
    @DisplayName("a caixa do bote aponta para a FRENTE do mundo, e nao para a frente do modelo")
    void caixaDoBoteApontaParaFrente() {
        assertTrue(MelaninLizardTuning.CAIXA_DO_BOTE.minZ() > 0.0D,
                "A geometria Bedrock tem a frente em -Z e AttackHitbox.noMundo e matematica de"
                        + " MUNDO, onde yaw 0 olha para +Z. Com o sinal do modelo, a caixa fica"
                        + " ATRAS do bicho: ele ataca, anima, publica a fase certa e machuca quem"
                        + " estiver pelas costas -- sem um erro no log. Foi assim que o boneco de"
                        + " treino (#138) descobriu, e so com gametest.");
        assertEquals(MelaninLizardTuning.ALCANCE_DO_BOTE, MelaninLizardTuning.CAIXA_DO_BOTE.maxZ(),
                "Decidir atacar de mais longe do que a caixa alcanca produz um bicho que morde o"
                        + " vazio; de mais perto, desperdica metade da caixa. As duas versoes"
                        + " rodam sem erro nenhum.");
    }

    @Test
    @DisplayName("o dano do bote vem do ATRIBUTO, e nao de uma constante paralela")
    void danoDoBoteNaoEstaCongelado() {
        AttackDefinition comSete = MelaninLizardTuning.bote(7.0F);
        AttackDefinition comDez = MelaninLizardTuning.bote(10.0F);
        assertEquals(7.0F, comSete.damage());
        assertNotEquals(comSete.damage(), comDez.damage(),
                "Se o dano fosse constante aqui, o ATTACK_DAMAGE do perfil viraria o botao morto"
                        + " que este projeto ja sabe que cria: a sessao de balanceamento giraria"
                        + " o numero do perfil e nada mudaria.");
        assertEquals(12, comSete.windupTicks(), "o aviso e o unico tempo de reacao que existe");
        assertEquals(5, comSete.activeTicks());
        assertEquals(18, comSete.recoveryTicks());
    }

    @Test
    @DisplayName("o telegrafo e mais longo que o do golpe comum, porque ele tira CONTROLE")
    void oAvisoEMaisLongoDoQueODeUmGolpeComum() {
        assertTrue(MelaninLizardTuning.BOTE_WINDUP_TICKS
                        > HunterExamProfiles.dummyEnemyStrike().windupTicks(),
                "Perder o controle do personagem nao tem como ser desfeito depois, entao precisa"
                        + " de mais tempo de leitura do que perder HP.");
    }

    // ------------------------------------------------------------ ponto fraco

    @Test
    @DisplayName("o ponto fraco so paga de frente e no alto -- e sobra corpo comum")
    void pontoFracoExigeAlturaEAngulo() {
        WeakPointResolver resolver = MelaninLizardTuning.pontoFraco();
        assertEquals("olho", resolver.resolver(0.70D, 0.9D));
        assertEquals("corpo", resolver.resolver(0.70D, -0.9D),
                "Quem ataca um lagarto pelas costas acerta a crista, que e placa de rocha.");
        assertEquals("corpo", resolver.resolver(0.20D, 0.9D),
                "Sem regiao comum sobrando, todo golpe viraria critico e o multiplicador deixaria"
                        + " de ser recompensa por mira para virar desconto geral de HP.");
        assertTrue(MelaninLizardTuning.pontosFracos().multiplier("olho") > 1.0F);
        assertEquals(1.0F, MelaninLizardTuning.pontosFracos().multiplier("corpo"),
                "Regiao desconhecida ou comum nao pode multiplicar nada.");
    }

    /**
     * A altura do olho DESENHADO tem de bater com a altura que o servidor cobra.
     *
     * <p>O gerador de textura ja confere isso do lado do Python, mas o Python nao
     * roda no build. Aqui a mesma relacao vira portao: se alguem mover o olho no
     * modelo -- ou girar o limiar no tuning --, o desenho passa a prometer um
     * acerto que a regra nao paga, e o jogador culpa a propria mira.</p>
     */
    @Test
    @DisplayName("o limiar do ponto fraco cai dentro da CABECA desenhada")
    void limiarDoPontoFracoCaiNaCabecaDesenhada() {
        double alturaEmPx = alturaDaHitboxEmPx();
        double limite = MelaninLizardTuning.pontoFraco().alturaMinima() * alturaEmPx;
        double[] cabeca = faixaVerticalDoOsso("head");
        assertTrue(limite >= cabeca[0] && limite <= cabeca[1],
                "O servidor comeca a chamar de 'olho' em y=" + limite + " px e a cabeca desenhada"
                        + " vai de " + cabeca[0] + " a " + cabeca[1] + " px. Fora dessa faixa o"
                        + " ambar da textura marca um lugar que a regra nao reconhece -- e o"
                        + " desenho passa a ENSINAR errado, que e pior do que nao marcar nada.");
    }

    /**
     * A vitima presa tem de ficar dentro da MANDIBULA desenhada.
     *
     * <p>Mesma historia do olho, do outro lado do bicho: o agarrao continua
     * funcionando perfeitamente com a fracao errada, so que o preso aparece
     * flutuando ao lado de uma boca aberta que nao segura nada.</p>
     */
    @Test
    @DisplayName("o encaixe da vitima cai dentro da mandibula desenhada")
    void encaixeDaVitimaCaiNaMandibulaDesenhada() {
        double encaixe = MelaninLizardTuning.FRACAO_DE_ENCAIXE_DA_VITIMA * alturaDaHitboxEmPx();
        double[] mandibula = faixaVerticalDoOsso("jaw");
        assertTrue(encaixe >= mandibula[0] && encaixe <= mandibula[1],
                "A vitima e presa a " + encaixe + " px e a mandibula desenhada vai de "
                        + mandibula[0] + " a " + mandibula[1] + " px.");
    }

    // ---------------------------------------------------------------- captura

    @Test
    @DisplayName("a captura e NAO-LETAL: matar cancela o card")
    void capturaNaoLetal() {
        CaptureCondition condicao = GreedIslandProfiles.capturas().get("melanin_lizard");
        assertTrue(condicao.exigeNaoLetal());
        assertFalse(condicao.satisfeita(0.0D, true, false, 0),
                "Matar tem de CANCELAR: um lagarto morto nao guarda a cor que o card representa.");
        assertTrue(condicao.satisfeita(0.2D, false, false, 0),
                "Enfraquecer e parar na hora certa e a unica forma de captura deste bicho, e e"
                        + " ela que justifica o multiplicador do ponto fraco existir.");
        assertFalse(condicao.satisfeita(0.5D, false, false, 0));
    }

    // ------------------------------------------------------------- utilitario

    /** (y minimo, y maximo) dos cubos de um osso, em px do modelo, lidos do GEO. */
    private static double[] faixaVerticalDoOsso(String osso) {
        JsonObject geometria = JsonParser.parseString(Repo.texto(GEO)).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        for (JsonElement elemento : geometria.getAsJsonArray("bones")) {
            JsonObject b = elemento.getAsJsonObject();
            if (!osso.equals(b.get("name").getAsString()) || !b.has("cubes")) continue;
            double minimo = Double.POSITIVE_INFINITY;
            double maximo = Double.NEGATIVE_INFINITY;
            for (JsonElement cubo : b.getAsJsonArray("cubes")) {
                JsonObject c = cubo.getAsJsonObject();
                double origem = c.getAsJsonArray("origin").get(1).getAsDouble();
                double tamanho = c.getAsJsonArray("size").get(1).getAsDouble();
                minimo = Math.min(minimo, origem);
                maximo = Math.max(maximo, origem + tamanho);
            }
            return new double[] {minimo, maximo};
        }
        throw new AssertionError("o geo do melanin_lizard nao tem cubo no osso '" + osso
                + "'. O modelo mudou e este portao perdeu o que media -- varredura vazia nao e"
                + " aprovacao.");
    }

    /**
     * A altura da hitbox, LIDA do literal de registro.
     *
     * <p>Copiar 0.8 para ca criaria a segunda fonte que o resto deste arquivo
     * existe para evitar: o dia em que a caixa de colisao mudasse, o portao
     * continuaria medindo contra o numero velho e aprovaria um desenho que nao
     * cabe mais.</p>
     */
    private static double alturaDaHitboxEmPx() {
        String fonte = Repo.texto(REGISTRO);
        int inicio = fonte.indexOf("MELANIN_LIZARD");
        if (inicio < 0) throw new AssertionError("EnemyEntityTypes nao registra MELANIN_LIZARD.");
        Matcher m = Pattern.compile("\\.sized\\(([0-9.]+)F,\\s*([0-9.]+)F\\)")
                .matcher(fonte.substring(inicio));
        if (!m.find()) {
            throw new AssertionError("nao achei o .sized(...) do melanin_lizard em"
                    + " EnemyEntityTypes: este portao passaria a nao medir nada.");
        }
        return Double.parseDouble(m.group(2)) * 16.0D;
    }
}
