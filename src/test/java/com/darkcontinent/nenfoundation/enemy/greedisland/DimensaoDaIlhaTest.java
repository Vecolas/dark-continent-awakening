package com.darkcontinent.nenfoundation.enemy.greedisland;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao que liga o NOME da ilha ao arquivo que a cria.
 *
 * <p><b>A falha que ele fecha e a mais discreta deste dominio:</b>
 * {@link GreedIslandRegion#DIMENSAO} e uma constante Java, e o datapack e um
 * arquivo com um nome. Eles nao se conhecem. Renomear um dos dois compila,
 * carrega e roda -- e a unica consequencia e que o isolamento passa a conferir
 * uma dimensao que nao existe, e portanto aprova tudo. Ninguem procura por
 * "criatura de Greed Island no Overworld" antes de ver uma.</p>
 *
 * <p>Ele tambem confere o tipo, porque uma dimensao que aponta para um
 * {@code dimension_type} ausente nao carrega -- e a mensagem do jogo fala do
 * arquivo, e nao do mob que deixou de existir por causa disso.</p>
 */
class DimensaoDaIlhaTest {

    private static final String DIMENSOES = "src/main/resources/data/nenfoundation/dimension";
    private static final String TIPOS = "src/main/resources/data/nenfoundation/dimension_type";

    @Test
    @DisplayName("a constante Java aponta para um arquivo de dimensao que EXISTE")
    void aConstanteTemArquivo() {
        var id = GreedIslandRegion.DIMENSAO.location();
        assertEquals("nenfoundation", id.getNamespace(),
                "A ilha fora do namespace do mod dependeria de um datapack que nao e nosso.");

        Path arquivo = Repo.raiz().resolve(DIMENSOES).resolve(id.getPath() + ".json");
        assertTrue(Files.isRegularFile(arquivo),
                "GreedIslandRegion.DIMENSAO aponta para '" + id + "' e o arquivo "
                        + Repo.raiz().relativize(arquivo) + " nao existe. Renomear um dos dois"
                        + " compila, carrega e roda: o isolamento passa a conferir uma dimensao"
                        + " que nao existe e, portanto, APROVA TUDO -- e ninguem procura por"
                        + " criatura de Greed Island no Overworld antes de ver uma.");
    }

    @Test
    @DisplayName("a dimensao aponta para um dimension_type que existe")
    void oTipoDaDimensaoExiste() {
        var id = GreedIslandRegion.DIMENSAO.location();
        String texto = Repo.texto(DIMENSOES + "/" + id.getPath() + ".json");
        JsonObject json = JsonParser.parseString(texto).getAsJsonObject();

        String tipo = json.get("type").getAsString();
        assertTrue(tipo.startsWith("nenfoundation:"),
                "A ilha usa o dimension_type '" + tipo + "', que nao e do mod: um tipo de"
                        + " terceiro pode sumir com a atualizacao dele, e a dimensao para de"
                        + " carregar sem que nada aqui mude.");

        Path arquivoDoTipo = Repo.raiz().resolve(TIPOS)
                .resolve(tipo.substring(tipo.indexOf(':') + 1) + ".json");
        assertTrue(Files.isRegularFile(arquivoDoTipo),
                "A dimensao da ilha aponta para o tipo '" + tipo + "' e o arquivo "
                        + Repo.raiz().relativize(arquivoDoTipo) + " nao existe. A dimensao nao"
                        + " carrega, e a mensagem do jogo fala do arquivo -- nao dos sete mobs"
                        + " que deixaram de ter onde existir.");

        assertTrue(json.has("generator"),
                "Dimensao sem generator nao gera terreno nenhum: o jogador cai no vazio.");
    }

    @Test
    @DisplayName("a ilha tem ceu -- o isolamento nao pode virar um Nether por descuido")
    void aIlhaTemCeu() {
        var id = GreedIslandRegion.DIMENSAO.location();
        String tipo = JsonParser.parseString(Repo.texto(DIMENSOES + "/" + id.getPath() + ".json"))
                .getAsJsonObject().get("type").getAsString();
        JsonObject dados = JsonParser.parseString(
                Repo.texto(TIPOS + "/" + tipo.substring(tipo.indexOf(':') + 1) + ".json"))
                .getAsJsonObject();

        assertTrue(dados.get("has_skylight").getAsBoolean(),
                "Sem ceu, toda criatura de Greed Island nasceria sob luz zero -- e as sete tem"
                        + " faixa de luz ate 15. O sintoma nao seria erro: seria uma ilha vazia.");
        assertTrue(dados.get("natural").getAsBoolean(),
                "Uma ilha nao-natural desliga clima e ciclo de dia; o encontro que depende de"
                        + " ser visto de dia deixaria de acontecer.");
    }
}
