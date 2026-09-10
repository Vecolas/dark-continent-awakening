package com.darkcontinent.nenfoundation.nen.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao dos identificadores de categoria.
 *
 * <p>Ele afirma a REGRA, nao o gosto: nomes congelados, neutro no ordinal zero
 * e traducao existente para cada categoria em cada idioma. Nenhuma dessas
 * violacoes produz erro em tempo de execucao -- todas produzem um jogador que
 * perde a categoria, ou uma tela mostrando a chave crua.
 */
class NenCategoryTest {

    /**
     * Os nomes serializados, na ordem congelada.
     *
     * <p>Esta lista e a SEGUNDA fonte de proposito: ela existe para discordar do
     * enum se alguem renomear, reordenar ou remover um valor. E o unico caso em
     * que duplicar e correto -- a copia e o portao.
     */
    private static final List<String> CONGELADOS = List.of(
            "undetermined",
            "enhancement",
            "transmutation",
            "emission",
            "conjuration",
            "manipulation",
            "specialization");

    @Test
    @DisplayName("os nomes serializados estao congelados, na ordem congelada")
    void nomesCongelados() {
        List<String> atuais = java.util.Arrays.stream(NenCategory.values())
                .map(NenCategory::getSerializedName)
                .toList();
        assertEquals(CONGELADOS, atuais,
                "Os identificadores de categoria estao gravados em NBT de save, em"
                        + " datapack e em quest. Mudar um deles nao quebra o build: apaga"
                        + " a categoria de quem ja jogou. Se a mudanca e mesmo desejada,"
                        + " ela precisa de um degrau em NenProfileMigrator e de um ADR.");
    }

    @Test
    @DisplayName("o ordinal zero e o NEUTRO, e nao uma categoria real")
    void neutroNoZero() {
        assertSame(NenCategory.UNDETERMINED, NenCategory.values()[0],
                "Quem esquecer de atribuir categoria recebe o valor de ordinal zero."
                        + " Se ele for uma categoria real, todo jogador sem Nen aparece"
                        + " como tendo aquela categoria -- em tela, em log e em quest.");
        assertFalse(NenCategory.UNDETERMINED.eReal());
    }

    @Test
    @DisplayName("REAIS deriva do enum e contem exatamente as seis")
    void seisReais() {
        assertEquals(6, NenCategory.REAIS.size());
        assertFalse(NenCategory.REAIS.contains(NenCategory.UNDETERMINED));
        assertEquals(NenCategory.values().length - 1, NenCategory.REAIS.size(),
                "REAIS tem de ser derivada de values(), nunca uma lista literal.");
    }

    @Test
    @DisplayName("cada categoria tem traducao em pt_br e en_us, e nenhuma sobra")
    void traducoesCasam() {
        Set<String> esperadas = java.util.Arrays.stream(NenCategory.values())
                .map(NenCategory::chaveDeTraducao)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String idioma : List.of("pt_br", "en_us")) {
            String caminho = "src/main/resources/assets/nenfoundation/lang/" + idioma + ".json";
            Set<String> presentes = chavesDeCategoria(Repo.texto(caminho));

            Set<String> faltando = new LinkedHashSet<>(esperadas);
            faltando.removeAll(presentes);
            assertTrue(faltando.isEmpty(),
                    idioma + ": categoria sem traducao " + faltando
                            + ". A tela mostraria a chave crua, sem nenhum erro no log.");

            Set<String> sobrando = new LinkedHashSet<>(presentes);
            sobrando.removeAll(esperadas);
            assertTrue(sobrando.isEmpty(),
                    idioma + ": traducao de categoria que nao existe mais " + sobrando
                            + ". O portao morde dos dois lados de proposito: chave orfa"
                            + " e o rastro de um rename que ficou pela metade.");
        }
    }

    private static Set<String> chavesDeCategoria(String json) {
        Pattern p = Pattern.compile("\"(nenfoundation\\.category\\.[a-z_]+)\"\\s*:");
        Matcher m = p.matcher(json);
        Set<String> achadas = new LinkedHashSet<>();
        while (m.find()) {
            achadas.add(m.group(1));
        }
        return achadas;
    }
}
