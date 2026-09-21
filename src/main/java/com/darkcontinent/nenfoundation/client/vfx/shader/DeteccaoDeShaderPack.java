package com.darkcontinent.nenfoundation.client.vfx.shader;

import java.lang.reflect.Method;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Se algum pack de shader substituiu o pipeline de render.
 *
 * <p><b>O COMPROMISSO DO ADR-016 E DETECTAR E DOCUMENTAR, e nao prometer que
 * funciona com todos.</b> Um pack que substitui o pipeline pode ignorar o passe,
 * duplica-lo, ou desenhar em cima dele -- e nenhum desses tres da erro. A
 * resposta e cair para {@code FAST}, escrever uma linha no log e registrar a
 * combinacao em {@code docs/testing/compatibility.md}.
 *
 * <p><b>POR REFLEXAO, E SEM DEPENDENCIA.</b> O Iris nao esta no
 * {@code build.gradle} e nao vai estar: o JAR exige apenas NeoForge e GeckoLib
 * (ADR-003 e ADR-012). Uma dependencia de compilacao aqui transformaria um mod
 * opcional do pack numa exigencia do mod -- e a fronteira de integracao opcional
 * e uma das tres regras inegociaveis do projeto.
 *
 * <p><b>A RESPOSTA E CACHEADA, e por isso {@link #esquecer()} existe.</b> A
 * chamada e por reflexao e acontece dentro do laco de render; refaze-la a cada
 * quadro seria pagar uma busca de metodo sessenta vezes por segundo para uma
 * resposta que muda quando a pessoa troca de pack -- ou seja, na recarga de
 * recurso, que e exatamente quando o cache e esquecido.
 *
 * <p>FALHAR AQUI E RESPONDER "NAO". Se a API mudar de nome, de assinatura ou
 * lancar, a deteccao devolve falso e o passe roda -- o pior caso e um halo
 * desenhado num ambiente em que ele talvez nao fique bom, e nao um cliente que
 * nao abre por causa de um mod que nem esta instalado.
 */
public final class DeteccaoDeShaderPack {

    private static final Logger LOG = LoggerFactory.getLogger(DeteccaoDeShaderPack.class);

    /**
     * Os mods que substituem o pipeline, e a classe de API de cada um.
     *
     * <p>OCULUS E O PORT DO IRIS PARA FORGE e mantem o MESMO pacote de API --
     * conferido, e nao suposto. Por isso a lista tem dois ids de mod e um
     * caminho de classe so.
     */
    private static final String[] MODS = {"iris", "oculus"};
    private static final String CLASSE_DE_API = "net.irisshaders.iris.api.v0.IrisApi";

    private static Boolean cache;

    private DeteccaoDeShaderPack() {
    }

    /** Se ha um pack de shader ativo agora. */
    public static boolean pipelineSubstituido() {
        Boolean lembrado = cache;
        if (lembrado != null) {
            return lembrado;
        }
        boolean resposta = perguntar();
        cache = resposta;
        return resposta;
    }

    /** Esquece a resposta. Chamado na recarga de recurso, quando ela pode mudar. */
    public static void esquecer() {
        cache = null;
    }

    private static boolean perguntar() {
        if (!algumPresente()) {
            return false;
        }
        try {
            Class<?> api = Class.forName(CLASSE_DE_API);
            Object instancia = api.getMethod("getInstance").invoke(null);
            Method emUso = api.getMethod("isShaderPackInUse");
            boolean ativo = Boolean.TRUE.equals(emUso.invoke(instancia));
            if (ativo) {
                LOG.info("Pack de shader ativo detectado; o brilho da aura usa o caminho"
                        + " FAST, sem alvo de render proprio. A combinacao precisa entrar"
                        + " em docs/testing/compatibility.md.");
            }
            return ativo;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError erro) {
            // RESPONDER "NAO" E O CAMINHO SEGURO. A alternativa -- assumir que
            // ha pack porque a pergunta falhou -- desligaria o passe de brilho
            // para todo mundo no dia em que o Iris renomeasse um metodo.
            LOG.debug("Nao foi possivel perguntar ao Iris se ha pack ativo; assumindo"
                    + " que nao ha.", erro);
            return false;
        }
    }

    private static boolean algumPresente() {
        for (String mod : MODS) {
            if (ModList.get() != null && ModList.get().isLoaded(mod)) {
                return true;
            }
        }
        return false;
    }
}
