package com.darkcontinent.nenfoundation.enemy.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.api.ThreatTier;
import com.darkcontinent.nenfoundation.enemy.combat.StaggerRules;
import com.darkcontinent.nenfoundation.enemy.content.ChimeraProfiles;
import com.darkcontinent.nenfoundation.enemy.content.EnemyCatalog;
import com.darkcontinent.nenfoundation.enemy.content.GreedIslandProfiles;
import com.darkcontinent.nenfoundation.enemy.content.HunterExamProfiles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O botao morto que {@code StaggerRules} sozinho nao consegue enxergar.
 *
 * <p>O construtor de {@code StaggerRules} recusa {@code resistencia >= limiar},
 * porque nesse caso nenhum golpe acumula NADA. E a checagem certa, e ela nao e a
 * que morde: o que mata o sistema na pratica e o DECAIMENTO. Entre dois golpes o
 * acumulado perde {@code decaimentoPorTick * ticks}, e se essa perda for maior
 * que o ganho de um golpe o acumulador sobe e desce para sempre sem nunca chegar
 * ao limiar.</p>
 *
 * <p><b>Isso nao da erro, nao aparece em teste unitario e nao aparece em
 * playtest.</b> O teste unitario alimenta o acumulador com valores sinteticos e
 * ve a transicao acontecer -- ele prova que a MAQUINA funciona. Em jogo o mob
 * simplesmente nunca e interrompido, e ninguem reporta "o stagger nao funciona":
 * reportam "esse bicho e chato", que e um relato que nao leva a lugar nenhum.</p>
 *
 * <p>Foi exatamente o estado em que oito dos nove perfis de Chimera nasceram, e
 * todos os testes passavam.</p>
 *
 * <h2>A conta e deliberadamente OTIMISTA</h2>
 *
 * <p>A simulacao alimenta o acumulador com o dano CRU da arma de referencia,
 * ignorando a armadura do proprio mob -- que em jogo reduz esse numero antes de
 * ele virar stagger. Isso e de proposito: um perfil que nao alcanca o limiar nem
 * com o numero inflado esta morto <b>com certeza</b>. O contrario nao vale, e
 * esta dito em o-que-nao-provamos.md: passar aqui nao prova que a interrupcao
 * acontece no ritmo certo, so que ela e ALCANCAVEL.</p>
 */
class StaggerAlcancavelTest {

    /** Quantos acertos seguidos ainda contam como "da para interromper". */
    private static final int GOLPES_TOLERAVEIS = 10;

    /** Um perfil de stagger com o nome de quem o produz. */
    private record Perfil(String mob, String origem, StaggerRules regras) { }

    @Test
    @DisplayName("PORTAO: todo perfil de stagger e alcancavel com a arma do papel dele")
    void todoStaggerEAlcancavel() {
        List<String> mortos = new ArrayList<>();
        List<String> lentos = new ArrayList<>();

        List<String> absorveSemPontoFraco = new ArrayList<>();

        for (Perfil perfil : perfis()) {
            ThreatTier papel = papelDe(perfil.mob());
            LoadoutDeReferencia arma = FaixaDeTempo.loadoutDe(papel);

            // Um perfil cuja resistencia empata com o golpe comum nao esta
            // quebrado: ele e de PONTO FRACO, e absorver o acerto no lugar errado
            // e a mecanica dele. Medi-lo contra o golpe comum reprovaria o design
            // certo -- mas aceitar a absorcao sem conferir nada deixaria passar um
            // mob que absorve tudo e nao tem ponto fraco nenhum, que e um mob
            // simplesmente impossivel de interromper.
            if (perfil.regras().resistencia() >= (float) arma.danoPorGolpe()) {
                if (!temPontoFraco(perfil.mob())) {
                    absorveSemPontoFraco.add(perfil.mob() + " (" + perfil.origem() + ")");
                    continue;
                }
                arma = comPontoFraco(arma);
            }

            int golpes = golpesAteInterromper(perfil.regras(), arma);

            if (golpes < 0) {
                mortos.add(String.format(Locale.ROOT,
                        "%s (%s, %s): ganha %.1f por golpe e perde %.1f entre golpes",
                        perfil.mob(), perfil.origem(), papel,
                        ganhoPorGolpe(perfil.regras(), arma),
                        perdaEntreGolpes(perfil.regras(), arma)));
            } else if (golpes > GOLPES_TOLERAVEIS) {
                lentos.add(perfil.mob() + " (" + papel + "): " + golpes + " acertos seguidos");
            }
        }

        assertTrue(mortos.isEmpty(),
                "Perfis de stagger IMPOSSIVEIS de alcancar: " + mortos + ". O decaimento entre"
                        + " dois golpes e maior que o ganho de um golpe, entao o acumulado sobe e"
                        + " desce para sempre e o limiar nunca chega. O sistema inteiro --"
                        + " StaggerState, a ordem de tick do EnemyRuntime, o resetComRecarga --"
                        + " continua ligado e nunca dispara. Nenhum teste unitario pega isso:"
                        + " eles alimentam o acumulador com valores sinteticos e provam que a"
                        + " MAQUINA funciona. Em jogo nao se reporta 'o stagger nao funciona',"
                        + " reporta-se 'esse bicho e chato'.");
        assertTrue(absorveSemPontoFraco.isEmpty(),
                "Perfis que absorvem o golpe comum inteiro e nao tem ponto fraco: "
                        + absorveSemPontoFraco + ". Absorver o acerto no lugar errado so faz"
                        + " sentido quando existe um lugar CERTO -- sem ele o mob nao tem"
                        + " caminho de interrupcao nenhum, e o perfil e um sistema desligado"
                        + " que parece ligado. A checagem olha a fonte da entidade atras de"
                        + " WeakPointResolver, entao ela tambem reprova o dia em que alguem"
                        + " tirar o ponto fraco e esquecer o perfil.");
        assertTrue(lentos.isEmpty(),
                "Perfis de stagger alcancaveis so com sequencias longas demais: " + lentos
                        + " (o teto e " + GOLPES_TOLERAVEIS + " acertos SEGUIDOS, sem errar um"
                        + " unico). Na pratica isso e o mesmo que nao existir, e e pior do que"
                        + " nao existir: o numero esta la, alguem vai gira-lo numa sessao de"
                        + " balanceamento e nada vai mudar.");
    }

    /**
     * A regua reprova quando alimentada com o caso que ela existe para pegar.
     *
     * <p>Portao que nunca reprova e carimbo. Este caso usa exatamente o perfil do
     * {@code multiarm_centipede} como ele nasceu -- limiar 40, resistencia 8,
     * decaimento 0.6 -- que passava no construtor de {@code StaggerRules} e nunca
     * cambaleava com arma nenhuma.</p>
     */
    @Test
    @DisplayName("a regua morde: o perfil original do multiarm_centipede era morto")
    void aReguaMordeNoCasoQueElaExistePara() {
        StaggerRules morto = new StaggerRules(40.0F, 8.0F, 0.6F, 50);
        assertEquals(-1, golpesAteInterromper(morto, LoadoutDeReferencia.preparado()),
                "O perfil que motivou este portao passou a ser considerado alcancavel: ou a"
                        + " conta mudou, ou o loadout de referencia mudou. Nos dois casos o"
                        + " portao parou de medir o que ele nasceu para medir.");

        StaggerRules vivo = new StaggerRules(40.0F, 2.0F, 0.15F, 50);
        assertTrue(golpesAteInterromper(vivo, LoadoutDeReferencia.preparado()) > 0,
                "Um perfil folgado foi reprovado: a regua ficou apertada demais e passaria a"
                        + " reprovar design legitimo.");
    }

    @Test
    @DisplayName("a varredura acha todos os perfis, e nao um subconjunto")
    void aVarreduraAchaTodos() {
        Map<String, String> porMob = new TreeMap<>();
        for (Perfil perfil : perfis()) porMob.put(perfil.mob(), perfil.origem());

        assertFalse(porMob.isEmpty(), "Nenhum perfil encontrado: o portao aprovaria tudo.");
        // 17, e nao 24: os sete do exame Hunter (Kiriko, Master of the Swamp,
        // Foxbear, Great Stamp, Frog-In-Waiting, Man-faced Ape, Spider Eagle) sao
        // anteriores ao EnemyRuntime e nao passam por StaggerState -- eles nao
        // tem perfil porque nao tem o sistema, e nao porque alguem esqueceu. O
        // numero esta travado aqui para que essa diferenca seja uma DECISAO
        // visivel: no dia em que um deles ganhar interrupcao, este caso reprova e
        // obriga a conversa, em vez de deixar o mob entrar sem regua.
        assertEquals(17, porMob.size(),
                "O numero de perfis de stagger mudou (achados: " + porMob.keySet() + "). Se um"
                        + " mob ganhou ou perdeu stagger de proposito, ajuste aqui no mesmo PR"
                        + " -- senao a varredura encolhe em silencio e o verde passa a cobrir"
                        + " menos.");
        for (String mob : porMob.keySet()) {
            assertTrue(EnemyCatalog.publicados().containsKey(mob),
                    "O perfil de stagger '" + mob + "' (" + porMob.get(mob) + ") nao corresponde"
                            + " a inimigo publicado nenhum. Ou o mob sumiu e o perfil ficou, ou o"
                            + " nome do metodo deixou de bater com o id -- e nos dois casos este"
                            + " portao passaria a nao medir aquele mob.");
        }
    }

    // ------------------------------------------------------------------ conta

    private static float ganhoPorGolpe(StaggerRules regras, LoadoutDeReferencia arma) {
        return Math.max(0.0F, (float) arma.danoPorGolpe() - regras.resistencia());
    }

    private static float perdaEntreGolpes(StaggerRules regras, LoadoutDeReferencia arma) {
        return regras.decaimentoPorTick() * (float) (20.0D / arma.golpesPorSegundo());
    }

    /**
     * Quantos acertos seguidos ate a interrupcao, ou {@code -1} se ela e
     * inalcancavel.
     */
    private static int golpesAteInterromper(StaggerRules regras, LoadoutDeReferencia arma) {
        float ganho = ganhoPorGolpe(regras, arma);
        float perda = perdaEntreGolpes(regras, arma);
        if (ganho <= perda) return -1;

        float acumulado = 0.0F;
        for (int golpes = 1; golpes <= 1000; golpes++) {
            acumulado += ganho;
            if (acumulado >= regras.limiar()) return golpes;
            acumulado = Math.max(0.0F, acumulado - perda);
        }
        return -1;
    }

    // -------------------------------------------------------------- varredura

    /**
     * Os perfis saem por REFLEXAO, e nao de uma lista escrita a mao.
     *
     * <p>Uma lista escrita a mao teria o defeito que este arquivo inteiro existe
     * para evitar: o mob novo entraria sem que ninguem lembrasse de adiciona-lo, e
     * o portao ficaria verde varrendo menos.</p>
     */
    private static List<Perfil> perfis() {
        List<Perfil> achados = new ArrayList<>();
        for (Class<?> fonte : List.of(
                HunterExamProfiles.class, GreedIslandProfiles.class, ChimeraProfiles.class)) {
            for (Method metodo : fonte.getDeclaredMethods()) {
                if (metodo.getReturnType() != StaggerRules.class
                        || metodo.getParameterCount() != 0
                        || !Modifier.isStatic(metodo.getModifiers())) {
                    continue;
                }
                try {
                    achados.add(new Perfil(idDe(metodo.getName()), fonte.getSimpleName(),
                            (StaggerRules) metodo.invoke(null)));
                } catch (ReflectiveOperationException erro) {
                    throw new IllegalStateException("perfil ilegivel: " + metodo.getName(), erro);
                }
            }
        }
        return achados;
    }

    /** "crabHeavyStagger" -> "crab_heavy". */
    private static String idDe(String metodo) {
        String base = metodo.endsWith("Stagger")
                ? metodo.substring(0, metodo.length() - "Stagger".length())
                : metodo;
        StringBuilder id = new StringBuilder();
        for (char letra : base.toCharArray()) {
            if (Character.isUpperCase(letra)) {
                id.append('_').append(Character.toLowerCase(letra));
            } else {
                id.append(letra);
            }
        }
        return id.toString();
    }

    /**
     * A mesma arma, com o golpe multiplicado pelo MENOR ponto fraco em uso.
     *
     * <p>O menor, e nao o do mob: assim a regua mede um perfil de ponto fraco sem
     * precisar saber de qual mob ele e. Se ele fecha com 3x, fecha com 4x.</p>
     */
    private static LoadoutDeReferencia comPontoFraco(LoadoutDeReferencia arma) {
        return new LoadoutDeReferencia(
                arma.danoPorGolpe() * StaggerPorPapel.MULTIPLICADOR_MINIMO_DE_PONTO_FRACO,
                arma.golpesPorSegundo(), arma.vida(), arma.armadura(), arma.tenacidade());
    }

    /**
     * Aquele mob declara um multiplicador de ponto fraco digno do nome?
     *
     * <p>A checagem olha a constante {@code MULTIPLICADOR_DO_*} da classe de tuning
     * -- que e exatamente o numero que o perfil passa para
     * {@code StaggerPorPapel.porPontoFraco}. Procurar pela classe
     * {@code WeakPointResolver} seria mais obvio e estaria ERRADO: o besouro resolve
     * o ventre com regras proprias (fase de tombo + costas + altura) e nao usa
     * aquele record. A regua tem de medir o que o perfil usa, e nao a maneira mais
     * comum de chegar la.</p>
     */
    private static boolean temPontoFraco(String mob) {
        StringBuilder classe = new StringBuilder();
        for (String parte : mob.split("_")) {
            classe.append(Character.toUpperCase(parte.charAt(0))).append(parte.substring(1));
        }
        java.util.regex.Matcher achado = java.util.regex.Pattern
                .compile("MULTIPLICADOR_DO_[A-Z_]+ = ([0-9.]+)F")
                .matcher(Repo.texto("src/main/java/com/darkcontinent/nenfoundation/enemy/content/"
                        + classe + "Tuning.java"));
        while (achado.find()) {
            if (Float.parseFloat(achado.group(1))
                    >= StaggerPorPapel.MULTIPLICADOR_MINIMO_DE_PONTO_FRACO) {
                return true;
            }
        }
        return false;
    }

    private static ThreatTier papelDe(String mob) {
        var definicao = EnemyCatalog.publicados().get(mob);
        if (definicao == null) {
            throw new IllegalStateException("sem definicao publicada para '" + mob + "'");
        }
        return definicao.metadata().threatTier();
    }
}
