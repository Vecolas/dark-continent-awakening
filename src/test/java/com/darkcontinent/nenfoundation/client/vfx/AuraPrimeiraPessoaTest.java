package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O que da para provar da primeira pessoa SEM camera.
 *
 * <p>E pouco, e o pouco e o que importa: que ela seja MENOS que a terceira. O
 * resto -- se atrapalha a mira, se cansa, se le como aura -- e o gate #187, e
 * so jogando se responde.
 */
class AuraPrimeiraPessoaTest {

    private static AuraPerfilVisual perfil(String nome) {
        return AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                        Repo.texto("src/main/resources/assets/nenfoundation/nen_vfx/" + nome)))
                .getOrThrow(erro -> new AssertionError(nome + ": " + erro));
    }

    @Test
    @DisplayName("a primeira pessoa mostra MENOS aura que a terceira")
    void primeiraPessoaEMaisDiscreta() {
        // O fator vive no renderer porque e limite de design, e nao ajuste: uma
        // shell inteira na frente da camera cobre a mira e cansa em minutos.
        // Este teste existe para que ninguem o suba a 1.0 sem perceber o que
        // isso significa.
        String fonte = Repo.texto(
                "src/main/java/com/darkcontinent/nenfoundation/client/vfx/render"
                        + "/AuraPrimeiraPessoa.java");
        assertTrue(fonte.contains("FATOR = 0.55F"),
                "o fator de primeira pessoa mudou. Se foi de proposito, atualize este"
                        + " teste E o javadoc -- mas ele nao pode chegar a 1.0 sem que"
                        + " alguem tenha decidido que a aura pode cobrir a mira.");
        assertTrue(fonte.contains("AuraShellPass.BORDA"),
                "a primeira pessoa precisa desenhar SO a borda: o filme interno some"
                        + " atras do proprio braco, e o halo externo tao perto da camera"
                        + " vira um veu sobre a tela inteira");
    }

    @Test
    @DisplayName("a borda de Ten em primeira pessoa continua visivel, e discreta")
    void bordaSobreviveAoFator() {
        float borda = perfil("ten.json").alphaDe(AuraShellPass.BORDA);
        float emPrimeiraPessoa = borda * 0.55F;
        assertTrue(emPrimeiraPessoa > 0.01F,
                "a borda sumiu em primeira pessoa: o jogador nao saberia que esta em Ten");
        assertTrue(emPrimeiraPessoa < borda,
                "a primeira pessoa nao pode ser mais forte que a terceira");
    }

    @Test
    @DisplayName("Zetsu nao desenha nada em primeira pessoa")
    void zetsuNaoDesenha() {
        AuraPerfilVisual apagado = perfil("ten.json").apagado();
        for (AuraShellPass passe : AuraShellPass.values()) {
            assertTrue(apagado.alphaDe(passe) == 0.0F,
                    "a ausencia e a informacao, e vale para a camera de dentro tambem");
        }
    }
    /** O fonte do renderer, sem comentarios: o portao mede CODIGO, e nao prosa. */
    private static String codigoDoRenderer() {
        String fonte = Repo.texto(
                "src/main/java/com/darkcontinent/nenfoundation/client/vfx/render"
                        + "/AuraPrimeiraPessoa.java");
        return fonte.replaceAll("(?s)/[*].*?[*]/", " ").replaceAll("(?m)//.*$", " ");
    }

    @Test
    @DisplayName("a posicao da parte NUNCA e zerada")
    void naoZeraAPosicaoDoBraco() {
        // A PRIMEIRA SESSAO DE CLIENTE VIU ISTO. O metodo zerava a POSICAO da
        // parte antes de desenhar, com um comentario dizendo que "o jogo ja
        // posicionou o braco". Ele posiciona a PILHA -- camera e mao --, e nao
        // este ModelPart, que e da aura e nunca passou por setupAnim. Levada a
        // origem do modelo, a shell desenhava no centro do corpo: em Ren
        // aparecia deslocada do braco, e em Ten o alpha menor a escondia.
        assertFalse(codigoDoRenderer().contains("setPos("),
                "alguem zerou a posicao da parte de novo. A posicao padrao do braco"
                        + " (x = +-5, y = 2) e o que o poe onde a mao esta;"
                        + " PlayerRenderer.renderHand zera APENAS xRot.");
    }

    @Test
    @DisplayName("a pose vem de setupAnim, como a vanilla monta")
    void usaSetupAnim() {
        String codigo = codigoDoRenderer();
        assertTrue(codigo.contains("setupAnim("),
                "a pose do braco tem de vir de setupAnim, que e o que"
                        + " PlayerRenderer.renderHand chama");
        assertTrue(codigo.contains("attackTime") && codigo.contains("swimAmount"),
                "a vanilla neutraliza attackTime, crouching e swimAmount ANTES de"
                        + " setupAnim; sem isso a shell herda a animacao de ataque ou de"
                        + " nado que o jogo ja tirou da geometria do braco");
    }

    @Test
    @DisplayName("nunca dois consumidores de vertice vivos ao mesmo tempo")
    void umMaterialPorVez() {
        String codigo = codigoDoRenderer();
        int pedidos = codigo.split("buffers[.]getBuffer[(]", -1).length - 1;
        int descargas = codigo.split("endBatch[(]", -1).length - 1;
        assertEquals(pedidos, descargas,
                "um BufferSource guarda UM buffer compartilhado; pedir o segundo"
                        + " encerra o primeiro por dentro, e o proximo vertice levanta"
                        + " \"Not building!\" -- foi assim que #299 derrubou o cliente");
    }

    @Test
    @DisplayName("as colunas de Ren nao entram em primeira pessoa")
    void semColunasEmPrimeiraPessoa() {
        assertFalse(codigoDoRenderer().contains("desenharColuna"),
                "o gate do AV4 e explicito: em primeira_pessoa_ren \"as colunas NAO"
                        + " podem aparecer\". Uma coluna de dois blocos nascendo no ombro"
                        + " atravessaria a tela inteira a essa distancia da camera.");
    }

    @Test
    @DisplayName("os filamentos do braco existem, e sao poucos")
    void filamentosDentroDaFaixaDoGate() {
        String codigo = codigoDoRenderer();
        assertTrue(codigo.contains("FILAMENTOS_POR_BRACO"),
                "o gate do AV4 pede 2-4 filamentos em primeira pessoa; ate 2026-09-21"
                        + " a classe desenhava so a borda, e nenhum filamento");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("FILAMENTOS_POR_BRACO *= *([0-9]+)").matcher(codigo);
        assertTrue(m.find(), "FILAMENTOS_POR_BRACO deixou de ser uma constante legivel");
        int quantos = Integer.parseInt(m.group(1));
        assertTrue(quantos >= 2 && quantos <= 4,
                "sao " + quantos + " filamentos por braco, e o gate do AV4 pede 2 a 4."
                        + " Mais que isso compete com a mira, que e o que a regra desta"
                        + " classe proibe.");
    }
}
