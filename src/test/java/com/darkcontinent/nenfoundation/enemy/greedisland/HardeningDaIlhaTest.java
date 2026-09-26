package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O portao da fase G13: o que impede um mundo de virar lixo silencioso.
 *
 * <p>Nenhum dos casos aqui levanta excecao em jogo. Um mundo que nasceu no
 * disco de andaime e carregado com a geografia nova gera chunks novos que nao
 * combinam com os antigos -- e a costura fica visivel para sempre, sem que
 * nada no log fale nisso.
 */
class HardeningDaIlhaTest {

    @Test
    @DisplayName("o layout de hoje E prototipo, e diz isso em voz alta")
    void aMarcaDeProtótipoExiste() {
        assertTrue(GreedIslandLayoutVersion.ehPrototipo(),
                "o layout foi declarado congelado sem as cidades existirem em bloco");
        assertTrue(GreedIslandLayoutVersion.AVISO_DE_PROTOTIPO.contains("scaffold"),
                "a secao 129 exige a nota literal, para o disco nao virar DoD");
    }

    @Test
    @DisplayName("save sem versao cai em ZERO -- e nao na versao de hoje")
    void oSaveAntigoNaoSeDisfarcaDeNovo() {
        // O caso silencioso: um mundo gravado antes deste arquivo existir. Se
        // a leitura assumisse "nasceu na versao atual", ele seria tratado como
        // compativel -- e a geografia dele e o disco de andaime, que nao
        // existe mais.
        var lido = GreedIslandSavedData.ler(new net.minecraft.nbt.CompoundTag(), null);
        assertEquals(0, lido.versaoDoLayout(),
                "campo ausente virou a versao de hoje: um mundo de andaime passaria"
                        + " por mundo novo, e a costura de terreno ficaria para sempre");
        assertEquals("desconhecida", lido.versaoDoMod());
    }

    @Test
    @DisplayName("um mundo de versao diferente e DETECTADO, e nao consertado")
    void aDivergenciaEAvisada() {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("versaoDoLayout", GreedIslandLayoutVersion.ATUAL + 7);
        var futuro = GreedIslandSavedData.ler(tag, null);

        assertTrue(!futuro.geografiaCombina(),
                "um mundo de outra versao de layout passou como compativel");
        // Consertar seria pior: reescrever chunk gerado deixa metade do mapa
        // mudada e a outra metade nao.
    }

    @Test
    @DisplayName("nenhum mundo de Greed Island e permanente enquanto o layout for zero")
    void nadaEPermanenteAinda() {
        var novo = GreedIslandSavedData.novo();
        assertEquals(GreedIslandLayoutVersion.ATUAL, novo.versaoDoLayout());
        assertTrue(!novo.ehPermanente(),
                "um mundo criado hoje se declarou permanente: a secao 128 manda apagar"
                        + " e regenerar durante o desenvolvimento, e ninguem faria isso"
                        + " num mundo que o codigo chama de definitivo");
    }

    @Test
    @DisplayName("o disco guarda BYTES, e nao o mapa de terreno")
    void oUsoDeDiscoEMinusculo() {
        // A secao 89 proibe guardar o terreno, e a 106 limita o disco. O
        // layout e uma FUNCAO: guardar a ilha seria salvar um numero que ja
        // se sabe -- e uma ilha de 80.000 x 70.000 tem bilhoes de colunas.
        var tag = new net.minecraft.nbt.CompoundTag();
        GreedIslandSavedData.novo().save(tag, null);
        assertTrue(tag.getAllKeys().size() <= 4,
                "o SavedData guarda " + tag.getAllKeys() + ": o layout e recalculavel,"
                        + " e o que entra aqui e so o que NAO e");
    }
}
