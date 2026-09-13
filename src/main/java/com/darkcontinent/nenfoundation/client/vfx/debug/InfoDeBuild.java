package com.darkcontinent.nenfoundation.client.vfx.debug;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * De que commit este JAR saiu.
 *
 * <p>ELE EXISTE POR CAUSA DO NOME DAS CAPTURAS. Todo gate da trilha AV exige
 * "data, commit e nivel de bloom no nome do arquivo", e o runtime nao tem como
 * descobrir o commit: quem sabe disso e o build. O valor e escrito em
 * {@code nenfoundation-build.properties} pelo {@code processResources}, e lido
 * aqui uma vez.
 *
 * <p>AUSENCIA TEM NOME PROPRIO. Um clone sem {@code .git}, um ambiente sem git
 * no PATH ou um recurso que nao foi expandido devolvem {@code sem-git} ou
 * {@code desconhecido} -- e nunca uma string vazia. O motivo e o de sempre
 * nesta trilha: um campo vazio no nome do arquivo produz uma captura que PARECE
 * completa e nao e.
 *
 * <p>LE UMA VEZ E GUARDA. O recurso e do JAR: ele nao muda enquanto o jogo
 * roda, e reler a cada captura so acrescentaria um caminho de falha.
 */
public final class InfoDeBuild {

    private static final Logger LOG = LoggerFactory.getLogger(InfoDeBuild.class);

    private static final String RECURSO = "/nenfoundation-build.properties";

    /** O que aparece quando o recurso nao existe ou nao le. */
    public static final String DESCONHECIDO = "desconhecido";

    private static final Properties DADOS = carregar();

    private InfoDeBuild() {
    }

    /** O commit curto, ou {@link #DESCONHECIDO}. */
    public static String commit() {
        return DADOS.getProperty("commit", DESCONHECIDO);
    }

    /** A versao do mod, ou {@link #DESCONHECIDO}. */
    public static String versao() {
        return DADOS.getProperty("versao", DESCONHECIDO);
    }

    private static Properties carregar() {
        Properties lidos = new Properties();
        try (InputStream entrada = InfoDeBuild.class.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                LOG.warn("{} nao esta no JAR; as capturas vao sair com commit '{}'.",
                        RECURSO, DESCONHECIDO);
                return lidos;
            }
            lidos.load(entrada);
        } catch (IOException erro) {
            // NAO PROPAGA. Isto e lido na inicializacao de uma classe de
            // depuracao; uma excecao aqui derrubaria o carregamento do cliente
            // por causa do nome de um arquivo de captura.
            LOG.warn("Falha ao ler {}; as capturas vao sair com commit '{}'.",
                    RECURSO, DESCONHECIDO, erro);
        }
        return lidos;
    }
}
