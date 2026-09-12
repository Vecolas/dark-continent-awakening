package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.client.hud.AparenciaDeTecnica;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;

/**
 * O visual de quem NAO e o jogador local.
 *
 * <p>DERIVADO, E NAO GUARDADO. Um controlador por jogador em volta criaria
 * estado por entidade que alguem precisa limpar quando ela sai do alcance,
 * desloga, morre ou troca de dimensao -- quatro caminhos, e um deles sempre
 * fica para tras. Aqui o visual e funcao do sinal: some junto com ele, sem
 * ninguem precisar lembrar.
 *
 * <p>O CUSTO DISSO E NAO HAVER TRANSICAO para terceiros: a aura dos outros
 * aparece e some seco, enquanto a do jogador local suaviza. E uma troca
 * deliberada, e esta declarada aqui em vez de descoberta depois.
 *
 * <p>A COR VEM DE {@link AparenciaDeTecnica}, a mesma que o HUD e a roda usam.
 * Se Ren e laranja no HUD, e laranja no outro jogador tambem.
 */
public final class EstadoVisualDeTerceiro {

    private EstadoVisualDeTerceiro() {
    }

    /**
     * O estado visual para um sinal percebido, no nivel de detalhe pedido.
     *
     * <p>O LOD ENTRA COMO INTENSIDADE, e nao como corte de features. Longe, a
     * aura fica mais fraca; muito longe, quem chama nem pergunta. Isso mantem a
     * leitura -- "aquela pessoa esta em Ren" -- a qualquer distancia em que ela
     * seja visivel.
     */
    public static AuraVisualState de(SinalDeAura sinal, AuraRenderLod lod) {
        // GUARDA REDUNDANTE, e sabidamente: o `switch` abaixo ja manda NENHUM
        // para OFF, e HIDDEN ja zera a intensidade -- `enabled()` recusa os dois
        // de qualquer jeito. Alimentar o portao com o defeito mostrou isso:
        // apagar esta linha nao muda resultado nenhum.
        //
        // Ficou porque diz a intencao em voz alta no caminho mais perigoso do
        // arquivo, e porque custa uma comparacao. Nao conte com ela como se
        // fosse a unica defesa.
        if (sinal == null || sinal == SinalDeAura.NENHUM || lod == AuraRenderLod.HIDDEN) {
            return AuraVisualState.desligado();
        }
        // O SWITCH E EXAUSTIVO DE PROPOSITO: quando KEN entrou em SinalDeAura,
        // o compilador reprovou este arquivo na hora. Um `default` teria
        // engolido o caso novo e desenhado Ken como Ten, sem erro nenhum.
        AuraVisualMode modo = switch (sinal) {
            case TEN -> AuraVisualMode.TEN;
            // KEN DESENHA COMO REN, e isso e escolha e nao preguica: os dois
            // sao envelopes grandes de aura liberada, e o cliente nao tem
            // preset proprio para Ken. O que os separa na tela e a COR, que
            // vem de AparenciaDeTecnica.
            case REN, KEN -> AuraVisualMode.REN;
            case NENHUM -> AuraVisualMode.OFF;
        };
        AuraVisualPreset preset = modo == AuraVisualMode.REN
                ? AuraVisualPreset.renBasic()
                : AuraVisualPreset.tenBasic();
        int cor = AparenciaDeTecnica.de(switch (sinal) {
            case KEN -> Ken.ID;
            case REN -> Ren.ID;
            default -> Ten.ID;
        }).cor();

        return new AuraVisualState(modo, preset, intensidadePara(lod), 1.0F,
                AuraDistribution.uniforme(), cor, cor);
    }

    /**
     * Quanto da aura se mostra em cada nivel de detalhe.
     *
     * <p>A TABELA SAIU DAQUI e passou a morar no proprio {@link AuraRenderLod}.
     * Antes eram duas listas de numeros que precisavam concordar -- e um nivel
     * novo no enum compilaria com este {@code switch} desatualizado.
     */
    private static float intensidadePara(AuraRenderLod lod) {
        return lod.intensidade();
    }
}
