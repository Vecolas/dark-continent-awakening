package com.darkcontinent.nenfoundation.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do indice de decisoes.
 *
 * <p>POR QUE ELE EXISTE: um indice de decisoes que omite uma decisao e PIOR que
 * nao ter indice, porque quem confia nele conclui que a decisao nao existe e
 * decide de novo, em outro sentido.
 *
 * <p>Ele varre a FONTE -- o diretorio -- e nao a lista. Verificacao que percorre
 * "o que esta na lista" nunca acusa o que nunca entrou nela. E morde dos dois
 * lados: entrada do indice sem arquivo no disco tambem reprova.
 */
class IndiceDeAdrTest {

    private static final String DIR = "docs/adr";
    private static final String INDICE = "docs/adr/index.md";

    @Test
    @DisplayName("todo ADR no disco esta no indice, e todo item do indice existe no disco")
    void indiceEDiscoCasam() {
        Set<String> noDisco = new LinkedHashSet<>();
        for (Path p : Repo.varrer(DIR, ".md")) {
            String nome = p.getFileName().toString();
            if (nome.startsWith("ADR-")) {
                noDisco.add(nome);
            }
        }

        assertFalse(noDisco.isEmpty(),
                "Nenhum ADR encontrado em " + DIR + ". Portao com zero verificacoes"
                        + " reprova: tabela vazia nao e aprovacao.");

        Set<String> noIndice = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\\((ADR-[A-Za-z0-9\\-]+\\.md)\\)").matcher(Repo.texto(INDICE));
        while (m.find()) {
            noIndice.add(m.group(1));
        }

        Set<String> faltandoNoIndice = new LinkedHashSet<>(noDisco);
        faltandoNoIndice.removeAll(noIndice);
        assertTrue(faltandoNoIndice.isEmpty(),
                "ADR no disco e fora do indice: " + faltandoNoIndice
                        + ". Quem ler o indice vai concluir que a decisao nao foi tomada.");

        Set<String> fantasmas = new LinkedHashSet<>(noIndice);
        fantasmas.removeAll(noDisco);
        assertTrue(fantasmas.isEmpty(),
                "Indice aponta para ADR que nao existe: " + fantasmas);
    }

    @Test
    @DisplayName("todo ADR declara o custo assumido e o que NAO muda")
    void adrDeclaraCustoEOQueNaoMuda() {
        for (Path p : Repo.varrer(DIR, ".md")) {
            String nome = p.getFileName().toString();
            if (!nome.startsWith("ADR-")) {
                continue;
            }
            String texto = Repo.texto(DIR + "/" + nome);
            assertTrue(texto.contains("## Custo assumido"),
                    nome + " nao declara o custo. Registrar so o beneficio faz a"
                            + " decisao parecer gratuita, e a proxima pessoa a revisita"
                            + " sem saber o que ja se pagou por ela.");
            assertTrue(texto.contains("## O que NAO muda"),
                    nome + " nao diz o que NAO muda. E a parte que evita erosao:"
                            + " sem ela, um principio que sobrevive a decisao se perde"
                            + " no meio dela.");
        }
    }
}
