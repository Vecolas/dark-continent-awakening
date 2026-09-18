package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.client.vfx.AuraRenderLod;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualState;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualSystem;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDeBrilho;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfis;
import com.darkcontinent.nenfoundation.client.vfx.shader.AuraPostProcess;
import com.darkcontinent.nenfoundation.config.NenClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Onde o passe de brilho se encaixa no quadro.
 *
 * <p>DOIS MOMENTOS, E A ORDEM DELES E O EFEITO INTEIRO:
 *
 * <ol>
 *   <li><b>Depois dos blocos solidos, antes das entidades</b> -- e aqui que a
 *       profundidade da cena e copiada para o alvo. Ela precisa estar la ANTES
 *       de a aura ser desenhada, porque e ela a mascara de oclusao. Um quadro
 *       depois seria um quadro de halo atravessando parede.</li>
 *   <li><b>Depois de tudo</b> -- a cadeia roda e soma o resultado na cena.</li>
 * </ol>
 *
 * <p><b>O PREDICADO DE PULO E VERIFICADO NO PRIMEIRO MOMENTO, e nao no
 * segundo.</b> Sem aura na tela o passe inteiro e pulado antes de qualquer
 * {@code bind} de framebuffer: nenhum alvo e criado, nenhuma profundidade e
 * copiada, e a layer nem monta a segunda emissao. Custo ZERO, e nao "custo
 * pequeno" -- e a diferenca entre os dois e exatamente o que o AV8 vai medir.
 *
 * <p><b>O PESO E O RAIO VEM DA AURA MAIS FORTE DA TELA.</b> Um alvo
 * compartilhado para a cena inteira significa um peso so, e escolher o MAIOR e
 * a unica opcao que nao apaga alguem: com a media, um Ren ao lado de cinco Ten
 * perderia o halo dele; com o do jogador local, a aura dos outros ficaria refem
 * do estado de quem esta olhando. <b>Nunca um framebuffer por entidade</b> --
 * dez jogadores em Ren sao dez contribuicoes no mesmo alvo.
 *
 * <p>NADA DE GAMEPLAY AQUI. O passe le estado visual e desenha; ele nao altera
 * {@code packedLight}, nao acende bloco e nao ilumina o cenario.
 */
public final class AuraBloomRenderer {

    /** Ate onde vale procurar aura para alimentar o alvo, em blocos. */
    private static final double DISTANCIA_MAXIMA = 96.0D;

    /** O peso e o raio do quadro corrente, decididos na preparacao. */
    private float peso;
    private float raio;

    /** Decide o pulo, garante o alvo e copia a profundidade da cena. */
    public void aoPreparar(RenderLevelStageEvent evento) {
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        this.peso = 0.0F;
        this.raio = 0.0F;
        if (mc.level == null || mc.player == null
                || !NenClientConfig.qualidade().teto().visivel()) {
            AuraPostProcess.prepararQuadro(mc, false);
            return;
        }

        AuraPerfilDeBrilho maior = AuraPerfilDeBrilho.NENHUM;
        for (Player jogador : mc.level.players()) {
            if (jogador.isInvisible() || jogador.isSpectator()) {
                continue;
            }
            double distancia = mc.player.distanceTo(jogador);
            if (distancia > DISTANCIA_MAXIMA
                    || !AuraRenderLod.porDistancia(distancia).visivel()) {
                continue;
            }
            AuraVisualState estado = AuraVisualSystem.estadoDe(jogador);
            if (!estado.enabled()) {
                continue;
            }
            // O PESO ACOMPANHA A INTENSIDADE: um Ren com output baixo brilha
            // menos, como a shell dele. Ligar o halo so ao modo faria o brilho
            // ignorar o botao de output, e o sintoma seria "o bloom nao reage".
            AuraPerfilDeBrilho brilho = AuraPerfis.de(estado).brilho();
            float forca = brilho.forca() * estado.intensity() * estado.fases().borda();
            if (forca > maior.forca()) {
                maior = new AuraPerfilDeBrilho(Math.clamp(forca, 0.0F, 1.0F), brilho.raio());
            }
        }

        boolean haAura = maior.existe();
        AuraPostProcess.prepararQuadro(mc, haAura);
        if (haAura) {
            this.peso = maior.forca() * NenClientConfig.intensidadeDoBloom();
            this.raio = maior.raio();
        }
    }

    /** Roda a cadeia e soma o resultado na cena. */
    public void aoAplicar(RenderLevelStageEvent evento) {
        // DEPOIS DO CLIMA E O ULTIMO ESTAGIO DO MUNDO. Rodar antes deixaria
        // chuva e neve desenhadas POR CIMA do halo; rodar na interface somaria
        // luz por cima do HUD, e a UI nunca entra no alvo nem recebe dele.
        if (evento.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        AuraPostProcess.aplicar(Minecraft.getInstance(), this.peso, this.raio);
        this.peso = 0.0F;
        this.raio = 0.0F;
    }

    /** Quem liga, desliga: sair do servidor solta os alvos e zera as reguas. */
    public void limpar() {
        AuraPostProcess.liberar();
        AuraPostProcess.limparContadores();
        this.peso = 0.0F;
        this.raio = 0.0F;
    }
}
