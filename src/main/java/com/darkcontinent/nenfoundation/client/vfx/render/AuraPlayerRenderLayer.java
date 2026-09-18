package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.AuraTransitionSample;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualMode;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualState;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisualSystem;
import com.darkcontinent.nenfoundation.client.vfx.CorDaAura;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraGeometryLadder;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDePressao;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPlayerModel;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfis;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraCurve;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonBatch;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraRibbonProfile;
import com.darkcontinent.nenfoundation.client.vfx.shader.AuraShaders;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * A aura desenhada por cima do jogador, seguindo a pose dele.
 *
 * <p>POR QUE UMA LAYER, E NAO UM RENDERER PARALELO. A layer roda dentro do
 * {@code PlayerRenderer}, entao yaw, pitch, agachar, nadar, atacar, correr e a
 * rotacao da cabeca chegam PRONTOS. Um renderer paralelo teria de reimplementar
 * a pose, e divergiria dela na primeira animacao nova -- inclusive nas que
 * outros mods adicionam.
 *
 * <p>A POSE E COPIADA, E SO. {@code HumanoidModel.copyPropertiesTo} ja copia as
 * ROTACOES de cada parte do modelo do jogador -- {@code head.copyFrom(...)},
 * {@code body.copyFrom(...)} e os quatro membros --, e nao apenas os flags.
 *
 * <p>CHAMAR {@code setupAnim} DEPOIS SERIA UM ERRO, e um erro silencioso:
 * ele DESCARTARIA a pose final que acabou de ser copiada para recalcular uma
 * aproximacao a partir dos parametros crus. A pose do pai ja inclui o que o
 * renderer, as outras layers e outros mods fizeram com ela; recalcular joga
 * tudo isso fora e a aura passa a divergir do corpo em casos especificos --
 * montado, dormindo, com pose de arma de outro mod. E a divergencia nao aparece
 * como erro: aparece como aura levemente fora do lugar.
 *
 * <p>E o mesmo caminho que {@code HumanoidArmorLayer} usa: copia, e desenha.
 *
 * <p>DESENHA PARTE POR PARTE, e nao o modelo inteiro de uma vez. E o que
 * permite multiplicar a intensidade por REGIAO -- e e o que torna Gyo, Ko e Ryu
 * uma mudanca de numero em vez de um renderer novo (ADR-014, ADR-015).
 *
 * <p><b>O QUE O AV4 MUDOU AQUI, em tres frases.</b> O perfil deixou de ser
 * buscado pelo MODO e passou a vir INTERPOLADO entre as duas pontas da
 * transicao, o que acabou com a troca seca de preset. A espessura deixou de ser
 * uma malha so e passou a escolher um degrau da escada assada, o que deu a Ren a
 * geometria que a direcao de arte pede e permitiu a contracao dos primeiros
 * 100 ms. E as COLUNAS entraram -- como ribbons com outra curva, no mesmo lote e
 * no mesmo material, e nao como um componente novo.
 *
 * <p>ATENCAO A VERSAO: 1.21.1 e ANTERIOR ao {@code EntityRenderState}, que
 * chegou em 1.21.2. Exemplo de renderer publicado depois disso nao serve aqui.
 */
public final class AuraPlayerRenderLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Alpha abaixo do qual nem vale montar a geometria. */
    private static final float ALPHA_MINIMO = 0.002F;

    /**
     * Quanto o flash da liberacao soma a borda.
     *
     * <p>CONSTANTE DE DESENHO, e nao chave de perfil: o flash e um GESTO com
     * duracao de 120 ms, e sua forca faz parte do gesto. Um botao aqui
     * convidaria a transformar o estouro num segundo estado permanente.
     */
    private static final float REFORCO_DO_FLASH = 0.45F;

    /**
     * A largura de uma coluna, em fracao da largura do filamento de corpo.
     *
     * <p>A COLUNA E MAIS LARGA, e nao mais grossa: ela sobe ate 2,5 blocos, e
     * com a largura de um filamento de corpo ela sumiria a cinco blocos. O teto
     * de {@code AuraRibbonProfile.LARGURA_MAXIMA} continua valendo -- acima dele
     * qualquer tira vira tubo de neon, que e modo de falha da direcao visual.
     */
    private static final float LARGURA_DA_COLUNA = 1.8F;

    /**
     * As malhas: um modelo por degrau de espessura, por passe.
     *
     * <p>ASSADAS UMA VEZ, no construtor da layer, e nunca no desenho. O custo
     * esta declarado no javadoc de {@code AuraGeometryLadder}.
     */
    private final AuraPlayerModel[][] modelos;

    /** Uma instancia por layer: todos os vetores de trabalho vivem dentro dela. */
    private final AuraRibbonBatch filamentos = new AuraRibbonBatch();

    private final boolean slim;

    public AuraPlayerRenderLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pai,
            EntityModelSet modelos, boolean slim) {
        super(pai);
        this.slim = slim;
        this.modelos = new AuraPlayerModel[AuraGeometryLadder.DEGRAUS][AuraShellPass.values().length];
        for (int degrau = 0; degrau < AuraGeometryLadder.DEGRAUS; degrau++) {
            for (AuraShellPass passe : AuraShellPass.values()) {
                this.modelos[degrau][passe.ordinal()] = new AuraPlayerModel(
                        modelos.bakeLayer(AuraModelLayers.de(passe, slim, degrau)), slim);
            }
        }
    }

    @Override
    public void render(PoseStack pilha, MultiBufferSource buffers, int luzEmpacotada,
            AbstractClientPlayer jogador, float balancoDosMembros, float amplitudeDoBalanco,
            float parcial, float idadeEmTicks, float guinadaDaCabeca, float inclinacaoDaCabeca) {

        AuraVisualState estado = AuraVisualSystem.estadoDe(jogador);
        if (!estado.enabled()) {
            // CUSTO ZERO, e nao custo pequeno: sem aura, nada e montado,
            // nada e alocado e nenhum buffer e pedido.
            return;
        }
        // JOGADOR INVISIVEL NAO GANHA CONTORNO. Sem esta linha, a pocao de
        // invisibilidade passaria a REVELAR quem esta em Ten -- o oposto do que
        // ela faz, e uma informacao que o observador nao deveria ter.
        if (jogador.isInvisible() || jogador.isSpectator()) {
            return;
        }
        // A REGUA CONTA A PARTIR DAQUI, e nao antes das duas guardas: quem nao
        // desenha nao custa, e um contador que somasse invisiveis e espectadores
        // faria o orcamento do AV8 medir gente que nao esta na tela.
        com.darkcontinent.nenfoundation.client.vfx.MedidorDeVfx.jogadorComAura();

        // O PERFIL VEM DO DADO E JA CHEGA INTERPOLADO entre as duas pontas da
        // transicao. Buscar por `estado.mode()` -- que era o que esta linha
        // fazia ate o AV3 -- devolvia o perfil de ORIGEM a transicao inteira e o
        // de DESTINO num quadro so: material que salta sob uma intensidade que
        // sobe suave, que e a troca seca de preset.
        AuraPerfilVisual perfil = AuraPerfis.de(estado);
        AuraTransitionSample fases = estado.fases();
        RenderType tipo = AuraRenderTypes.shell();

        // O DEGRAU DE ESPESSURA SAI DA BORDA, e a borda ja carrega a contracao.
        // Ver `AuraGeometryLadder` para por que a espessura nao pode ser um
        // uniform nem um `poseStack.scale`.
        int degrau = AuraGeometryLadder.degrauPara(espessuraDaBorda(estado, fases));

        // O TEMPO E POR ENTIDADE, e nao global. `idadeEmTicks` conta desde que
        // AQUELA entidade nasceu, entao dois jogadores nunca estao na mesma fase
        // do fluxo -- e sincronia acidental e a coisa mais artificial que um
        // efeito organico pode fazer. Custo declarado: respawn e troca de
        // dimensao recriam a entidade, e o fluxo da um salto.
        float tempo = idadeEmTicks / 20.0F;

        for (AuraShellPass passe : AuraShellPass.values()) {
            float alphaDoPasse = perfil.alphaDe(passe) * estado.intensity() * peso(passe, fases);
            if (alphaDoPasse < ALPHA_MINIMO) {
                continue;
            }
            AuraPlayerModel modelo = this.modelos[degrau][passe.ordinal()];
            // COPIA A POSE FINAL. Ver o javadoc: `setupAnim` aqui DESCARTARIA
            // o que acabou de ser copiado.
            this.getParentModel().copyPropertiesTo(modelo);

            AuraShaders.configurar(tempo, perfil.fresnelDe(passe),
                    perfil.velocidadeDeFluxo(), perfil.escalaDeRuido(),
                    perfil.reforcoDaBorda());

            VertexConsumer vertices = buffers.getBuffer(tipo);
            desenharPorRegiao(modelo, pilha, vertices, luzEmpacotada, estado, alphaDoPasse);

            // DESCARREGA O LOTE AGORA, e nao no fim do quadro. Os uniformes sao
            // do PROGRAMA, e nao do vertice: sem esta descarga, os tres passes
            // seriam desenhados juntos no fim com os uniformes do ULTIMO, e as
            // tres camadas ficariam identicas -- exatamente o que os tres
            // expoentes de Fresnel existem para evitar.
            //
            // O preco e uma chamada de desenho por passe, por jogador. Esta
            // declarado, e e o AV8 que o ataca.
            descarregar(buffers, tipo);
        }

        desenharFilamentos(pilha, buffers, luzEmpacotada, jogador, estado, perfil, fases, tempo,
                degrau);
    }

    /**
     * A espessura de borda que este estado pede, em blocos.
     *
     * <p>DUAS COISAS SE MULTIPLICAM AQUI, e nenhuma das duas cabe sozinha. A
     * primeira e o MODO: Ren e mais espesso que Ten, e durante a transicao a
     * espessura caminha de um para o outro. A segunda e a CONTRACAO: nos
     * primeiros 100 ms da subida, a shell recua 4,5% -- a inspiracao antes do
     * golpe. Sem ela, a subida e uma rampa; e rampa e lida como interpolacao.
     */
    private static float espessuraDaBorda(AuraVisualState estado, AuraTransitionSample fases) {
        float origem = bordaDe(estado.mode());
        float alvo = bordaDe(estado.modoAlvo());
        float caminhada = origem + (alvo - origem) * estado.transitionProgress();
        return caminhada * fases.shell();
    }

    /** A borda assentada de um modo, em blocos. */
    private static float bordaDe(AuraVisualMode modo) {
        // CUSTOM HERDA REN, pelo mesmo motivo de `AuraTransitionSample`: uma
        // tecnica nova que ainda nao disse o que quer aparece INTEIRA, que e
        // visivel e portanto corrigivel.
        int degrau = (modo == AuraVisualMode.REN || modo == AuraVisualMode.CUSTOM)
                ? AuraGeometryLadder.DEGRAU_DE_REN
                : AuraGeometryLadder.DEGRAU_DE_TEN;
        return AuraGeometryLadder.espessuraDaBordaDe(degrau);
    }

    /**
     * O peso de um passe, vindo da linha do tempo da transicao.
     *
     * <p>O FLASH SO TOCA A BORDA. Somado ao filme interno ele clarearia o corpo
     * inteiro por um instante -- que e a leitura de "personagem virou lampada",
     * e nao de "energia estourou no contorno".
     */
    private static float peso(AuraShellPass passe, AuraTransitionSample fases) {
        return switch (passe) {
            case INTERNA, EXTERNA -> fases.shell();
            case BORDA -> fases.borda() + fases.flash() * REFORCO_DO_FLASH;
        };
    }

    /**
     * Os filamentos e as colunas, agrupados POR PARTE do corpo.
     *
     * <p>AGRUPAR IMPORTA: empilhar a transformacao de uma parte custa uma
     * multiplicacao de matriz, e fazer isso por filamento repetiria a conta ate
     * vinte e oito vezes por jogador. Agrupado, sao seis.
     *
     * <p>A POSE E A DA PARTE, e nao a do modelo: e o que faz o filamento nascer
     * na superficie que se ve, e acompanhar o membro quando ele gira. Vale
     * igual para a coluna, e e por isso que ela acompanha o ombro ao correr em
     * vez de flutuar solta -- que e um dos criterios que reprovam a issue #189.
     */
    private void desenharFilamentos(PoseStack pilha, MultiBufferSource buffers, int luz,
            AbstractClientPlayer jogador, AuraVisualState estado, AuraPerfilVisual perfil,
            AuraTransitionSample fases, float tempo, int degrau) {

        AuraRibbonProfile filamento = perfil.filamentos();
        AuraPerfilDePressao pressao = perfil.pressao();
        // A CONTAGEM PASSA PELA SOBREPOSICAO, e o resto do perfil nao: o que a
        // sessao de arte precisa girar e "quantos filamentos", e nao a curva
        // nem o ciclo deles. Zero e um valor legitimo aqui -- e uma das
        // perguntas do AV2 e exatamente "quanto da leitura vem dos filamentos".
        int quantidade = com.darkcontinent.nenfoundation.client.vfx.SobreposicaoDeVfx
                .aplicarNasRibbons(filamento.quantidade());
        int colunas = fases.colunas() > 0.0F ? pressao.colunas() : 0;
        if (quantidade == 0 && colunas == 0) {
            return;
        }
        AuraPlayerModel modelo = this.modelos[degrau][AuraShellPass.BORDA.ordinal()];
        // A FOLGA SAI DA ESPESSURA DA BORDA DO DEGRAU EM USO, e nao de uma
        // constante nem da borda de Ten: com a escada de espessuras, engordar a
        // shell passou a mover a superficie de verdade, e uma folga fixa faria
        // os filamentos de Ren nascerem DENTRO dela -- sumindo, sem erro nenhum.
        float folga = AuraCurve.folgaBase(AuraGeometryLadder.espessuraDaBordaDe(degrau));
        long semeadura = jogador.getUUID().getLeastSignificantBits();
        VertexConsumer buffer = buffers.getBuffer(AuraRenderTypes.ribbon());

        AuraAnchor[] ancoras = AuraAnchor.values();
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            float pesoDaRegiao = estado.distribution().intensidade(regiao);
            if (pesoDaRegiao < 0.02F) {
                continue;
            }
            pilha.pushPose();
            parteDe(modelo, regiao).translateAndRotate(pilha);
            PoseStack.Pose pose = pilha.last();

            for (int i = 0; i < quantidade; i++) {
                AuraAnchor ancora = ancoras[i % ancoras.length];
                if (ancora.regiao() != regiao) {
                    continue;
                }
                // O CICLO E POR FILAMENTO, e defasado pelo indice: sem a
                // defasagem os oito trocariam de curva no MESMO quadro, e a
                // troca simultanea e visivel como um pisco.
                float fase = (i * 0.618F) % 1.0F;
                float t = tempo / filamento.cicloSegundos() + fase;
                int ciclo = (int) Math.floor(t);
                float dentroDoCiclo = t - ciclo;

                // O ENVELOPE ZERA NAS DUAS PONTAS DO CICLO. E o que torna a
                // troca de curva invisivel: o filamento some antes de virar
                // outro, em vez de saltar de uma forma para a seguinte.
                float envelope = (float) Math.sin(Math.PI * dentroDoCiclo);
                float alpha = estado.intensity() * pesoDaRegiao * envelope * 0.85F
                        * fases.filamentos();
                if (alpha < ALPHA_MINIMO) {
                    continue;
                }

                long semente = AuraCurve.semente(semeadura, ancora, i, ciclo);
                com.darkcontinent.nenfoundation.client.vfx.MedidorDeVfx.filamento();
                this.filamentos.desenhar(buffer, pose, ancora, this.slim, semente, folga,
                        filamento.comprimentoDe(semente), filamento.largura(),
                        CorDaAura.comAlpha(estado.primaryColor(), alpha), luz);
            }

            desenharColunas(buffer, pose, regiao, colunas, estado, filamento, pressao, fases,
                    semeadura, folga, tempo, luz, pesoDaRegiao);
            pilha.popPose();
        }
        descarregar(buffers, AuraRenderTypes.ribbon());
    }

    /**
     * As correntes verticais de Ren que nascem NESTA regiao.
     *
     * <p>ELAS SAO RIBBONS, e nao um componente novo. Mesmo lote, mesmo material,
     * mesma tira -- o que muda e a curva, que sobe em vez de enrolar. Se a
     * coluna ganhasse renderer proprio, a proxima tecnica ganharia o quarto, e o
     * ADR-015 secao 6 existe justamente para impedir essa fragmentacao.
     *
     * <p>O CICLO E MAIS LENTO QUE O DA RIBBON DE CORPO, e de proposito: uma
     * coluna de dois blocos trocando de curva tres vezes por segundo pisca. O
     * fator sai do ciclo do proprio perfil, entao ajustar o ritmo do filamento
     * na sessao de arte ajusta o da coluna junto -- em vez de deixar um numero
     * proprio aqui, esquecido.
     */
    private void desenharColunas(VertexConsumer buffer, PoseStack.Pose pose,
            AuraBodyRegion regiao, int colunas, AuraVisualState estado,
            AuraRibbonProfile filamento, AuraPerfilDePressao pressao, AuraTransitionSample fases,
            long semeadura, float folga, float tempo, int luz, float pesoDaRegiao) {

        if (colunas <= 0 || pressao.alturaMaxima() <= 0.0F) {
            return;
        }
        for (int i = 0; i < colunas; i++) {
            AuraAnchor ancora = AuraAnchor.coluna(i);
            if (ancora.regiao() != regiao) {
                continue;
            }
            float fase = (i * 0.382F) % 1.0F;
            float ciclos = tempo / (filamento.cicloSegundos() * 2.0F) + fase;
            int ciclo = (int) Math.floor(ciclos);
            float dentroDoCiclo = ciclos - ciclo;
            float envelope = (float) Math.sin(Math.PI * dentroDoCiclo);

            float alpha = estado.intensity() * pesoDaRegiao * envelope * fases.colunas() * 0.75F;
            if (alpha < ALPHA_MINIMO) {
                continue;
            }
            // A SEMENTE USA UM INDICE DESLOCADO para que a coluna de um ombro
            // nao herde a curva do filamento que nasce na mesma ancora. Sem
            // isso as duas subiriam sobrepostas, e a coluna pareceria um
            // filamento grosso em vez de outra coisa.
            long semente = AuraCurve.semente(semeadura, ancora,
                    i + AuraRibbonProfile.TETO, ciclo);
            // A ALTURA VEM DO PERFIL e e escalada pela INTENSIDADE, que vem do
            // output efetivo -- e nao da reserva. Reserva grande nao e aura
            // grande: aura e o que esta sendo liberado.
            float altura = pressao.alturaDe(semente) * (0.55F + 0.45F * estado.intensity());
            com.darkcontinent.nenfoundation.client.vfx.MedidorDeVfx.coluna();
            this.filamentos.desenharColuna(buffer, pose, ancora, this.slim, semente, folga,
                    altura, filamento.largura() * LARGURA_DA_COLUNA,
                    CorDaAura.comAlpha(estado.primaryColor(), alpha), luz);
        }
    }

    /**
     * O perfil de filamento do modo ativo.
     *
     * <p>ELE VEM DO MESMO ARQUIVO DA SHELL, e nao de um `switch` com constantes
     * ao lado. Havia aqui um caso por modo chamando `AuraRibbonProfile.ten()` e
     * `.ren()` -- numeros de arte no codigo, ao lado de um perfil de dado que ja
     * carregava os da shell. Duas fontes para "como Ren se parece" divergem, e a
     * divergencia aparece como um Ren cuja shell responde ao ajuste e cujos
     * filamentos nao.
     *
     * <p>{@code AuraPerfis.de} ja responde o perfil apagado para ZETSU e OFF, e
     * o apagado ja vem sem filamento nenhum -- entao a ausencia continua escrita
     * no dado, e nao numa guarda daqui.
     */
    static AuraRibbonProfile perfilDeFilamento(AuraVisualState estado) {
        return AuraPerfis.de(estado).filamentos();
    }

    /**
     * Forca a emissao do que ja foi acumulado neste tipo de render.
     *
     * <p>SILENCIOSA QUANDO A FONTE NAO SABE DESCARREGAR. Nem todo
     * {@code MultiBufferSource} e um lote -- capturas de tela e alguns mods
     * passam implementacoes proprias. Nesses casos as tres camadas sairao com os
     * mesmos uniformes, que e feio e nao e quebrado.
     */
    private static void descarregar(MultiBufferSource buffers, RenderType tipo) {
        if (buffers instanceof MultiBufferSource.BufferSource lote) {
            lote.endBatch(tipo);
            // A CONTAGEM FICA AQUI, E NAO ONDE A GEOMETRIA E MONTADA. Uma
            // chamada de desenho acontece quando o lote e descarregado; contar
            // por parte de corpo daria seis vezes o numero real, e o AV8
            // receberia um orcamento inflado sem ninguem perceber.
            com.darkcontinent.nenfoundation.client.vfx.MedidorDeVfx.chamadaDeDesenho();
        }
    }

    /**
     * Desenha as seis partes, cada uma com a intensidade da SUA regiao.
     *
     * <p>A distribuicao vem do servidor (ADR-014) e ja chega como projecao. Em
     * repouso todas valem o mesmo e o resultado e uma aura uniforme; com Gyo,
     * uma regiao acende e as outras recuam, sem nenhum codigo novo.
     */
    private static void desenharPorRegiao(AuraPlayerModel modelo, PoseStack pilha,
            VertexConsumer vertices, int luz, AuraVisualState estado, float alphaDoPasse) {
        for (AuraBodyRegion regiao : AuraBodyRegion.values()) {
            float alpha = alphaDoPasse * estado.distribution().intensidade(regiao);
            if (alpha < ALPHA_MINIMO) {
                continue;
            }
            parteDe(modelo, regiao).render(pilha, vertices, luz, OverlayTexture.NO_OVERLAY,
                    CorDaAura.comAlpha(estado.primaryColor(), alpha));
        }
    }

    private static ModelPart parteDe(AuraPlayerModel modelo, AuraBodyRegion regiao) {
        return switch (regiao) {
            case HEAD -> modelo.head;
            case TORSO -> modelo.body;
            case LEFT_ARM -> modelo.leftArm;
            case RIGHT_ARM -> modelo.rightArm;
            case LEFT_LEG -> modelo.leftLeg;
            case RIGHT_LEG -> modelo.rightLeg;
        };
    }

}
