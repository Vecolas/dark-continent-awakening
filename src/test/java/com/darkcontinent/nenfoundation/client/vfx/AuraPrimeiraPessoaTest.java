package com.darkcontinent.nenfoundation.client.vfx;

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
}
