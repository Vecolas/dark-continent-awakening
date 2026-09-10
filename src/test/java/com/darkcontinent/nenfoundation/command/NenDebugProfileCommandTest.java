package com.darkcontinent.nenfoundation.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NenDebugProfileCommandTest {

    @Test
    @DisplayName("comando de perfil exige permissao de operador")
    void comandoEPermissionado() {
        String fonte = Repo.texto(
                "src/main/java/com/darkcontinent/nenfoundation/command/NenDebugProfileCommand.java");

        assertEquals(2, NenDebugProfileCommand.NIVEL_DE_OPERADOR);
        assertTrue(fonte.contains(".requires(fonte -> fonte.hasPermission(NIVEL_DE_OPERADOR))"),
                "Comando de debug sem requires expoe dados internos a jogador comum.");
    }

    @Test
    @DisplayName("dump do perfil e deterministico e inclui todo o estado persistente resumido")
    void formatoEEstavel() {
        ResourceLocation ten = id("ten");
        ResourceLocation ren = id("ren");
        PersistentNenData perfil = new PersistentNenData(
                1, true, NenCategory.ENHANCEMENT, true,
                12.5D, 0.25D, 0.75D,
                Map.of(ten, 0.4D),
                Set.of(ren, ten),
                Set.of(id("punho_reforcado")),
                Set.of(id("despertou")));

        String texto = NenDebugProfileCommand.formatar(perfil);

        assertTrue(texto.contains("schema=1"));
        assertTrue(texto.contains("awakened=true"));
        assertTrue(texto.contains("category=enhancement"));
        assertTrue(texto.contains("revealed=true"));
        assertTrue(texto.contains("auraPotential=12.500"));
        assertTrue(texto.contains("proficiencies={nenfoundation:ten=0.400}"));
        assertTrue(texto.contains("techniques=[nenfoundation:ren, nenfoundation:ten]"),
                "Ids devem sair ordenados para o relato ser comparavel entre execucoes.");
        assertTrue(texto.contains("abilities=[nenfoundation:punho_reforcado]"));
        assertTrue(texto.contains("flags=[nenfoundation:despertou]"));
    }

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }
}
