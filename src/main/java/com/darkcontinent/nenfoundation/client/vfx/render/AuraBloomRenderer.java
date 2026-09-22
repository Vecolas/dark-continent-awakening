package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.client.vfx.AuraLodEfetivo;
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
 * <p><b>O RAIO VEM DA AURA MAIS FORTE DA TELA; O PESO, NAO.</b> Um alvo
 * compartilhado significa UM raio de borrao, e escolher o MAIOR e a unica opcao
 * que nao apaga alguem: com a media, um Ren ao lado de cinco Ten perderia o halo
 * dele. <b>Nunca um framebuffer por entidade</b> -- dez jogadores em Ren sao dez
 * contribuicoes no mesmo alvo.
 *
 * <p><b>O PESO JA NAO USA `maiorForca`, e a historia importa.</b> Ele usava, e a
 * forca acabava aplicada DUAS vezes -- uma por jogador na escrita do alvo, outra
 * no composite com o valor de outra pessoa. O sintoma era um relato de jogo:
 * <i>"ativar Ren deixa a tecnica dos outros mais clara"</i>. Estava certo: o Ten
 * de um terceiro ficava 2,75x mais brilhante porque QUEM OLHAVA trocou de
 * tecnica. Corrigido em 2026-09-22; ver o comentario em {@code aoPreparar}.
 *
 * <p><b>O QUE CONTINUA COMPARTILHADO, e e custo assumido:</b> o RAIO. Com um Ren
 * (5,5 px) e um Ten (2,5 px) na tela, o halo do Ten engrossa. E inerente a um
 * alvo so, e o preco de nao ter um framebuffer por entidade.
 *
 * <p><b>E DOIS REN JUNTOS BRILHAM MAIS onde os halos se cruzam.</b> Isso NAO e
 * defeito: o alvo acumula luz de proposito, e duas fontes brilhantes lado a lado
 * somam -- e o que bloom faz. Trocar isso exigiria separar as contribuicoes, que
 * e exatamente o framebuffer por entidade que esta descartado acima.
 *
 * <p>NADA DE GAMEPLAY AQUI. O passe le estado visual e desenha; ele nao altera
 * {@code packedLight}, nao acende bloco e nao ilumina o cenario.
 */
public final class AuraBloomRenderer {

    // ATE ONDE VALE PROCURAR AURA sai de `AuraLodEfetivo.alcanceDeBusca()`, que
    // le a distancia maxima escolhida pelo jogador. Havia aqui um 96 cru, e ele
    // era a terceira copia da mesma decisao -- com um valor que nao batia com
    // nenhuma das outras duas.

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

        // DOIS FLOATS, E NAO UM RECORD POR JOGADOR. Montar um
        // `AuraPerfilDeBrilho` a cada candidato aloca um objeto por jogador com
        // aura, por quadro -- para carregar dois numeros que cabem na pilha.
        float maiorForca = 0.0F;
        float raioDoMaior = 0.0F;
        for (Player jogador : mc.level.players()) {
            if (jogador.isInvisible() || jogador.isSpectator()) {
                continue;
            }
            double distancia = mc.player.distanceTo(jogador);
            if (distancia > AuraLodEfetivo.alcanceDeBusca()
                    || !AuraLodEfetivo.doCliente(distancia).visivel()) {
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
            float forca = Math.clamp(
                    brilho.forca() * estado.intensity() * estado.fases().borda(), 0.0F, 1.0F);
            if (forca > maiorForca) {
                maiorForca = forca;
                raioDoMaior = brilho.raio();
            }
        }

        boolean haAura = maiorForca > 0.0F && raioDoMaior > 0.0F;
        AuraPostProcess.prepararQuadro(mc, haAura);
        if (haAura) {
            // O PESO NAO LEVA `maiorForca`, E ISSO E A CORRECAO DE 2026-09-22.
            //
            // A forca do perfil JA FOI APLICADA, por jogador, na escrita do alvo:
            // `AuraPlayerRenderLayer` monta a segunda emissao com
            // `alphaDoPasse * forcaDoBrilho`, onde a forca e a do perfil DAQUELE
            // jogador. Multiplicar por `maiorForca` aqui aplicava a forca DUAS
            // VEZES -- e, pior, a segunda vez com o valor de OUTRA pessoa.
            //
            // A conta que o defeito produzia, com um Ten na tela:
            //   observador em Ten -> 0.20 (escrita) * 0.20 (composite) = 0.040
            //   observador em Ren -> 0.20 (escrita) * 0.55 (composite) = 0.110
            // O Ten de um terceiro ficava 2,75x mais claro porque QUEM OLHAVA
            // ligou Ren. Nao lancava, nao aparecia em teste, e o relato que
            // chegou foi "ativar Ren deixa a tecnica dos outros mais clara".
            //
            // `maiorForca` continua decidindo duas coisas legitimas: SE ha aura
            // (o predicado de pulo, acima) e o RAIO do borrao. O raio continua
            // sendo o maior da tela, e isso permanece um custo real do alvo
            // compartilhado -- ver o javadoc da classe.
            this.peso = NenClientConfig.intensidadeDoBloom();
            this.raio = raioDoMaior;
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
