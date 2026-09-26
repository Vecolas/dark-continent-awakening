package com.darkcontinent.nenfoundation.client.vfx;

import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.client.hud.AparenciaDeTecnica;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import net.minecraft.resources.ResourceLocation;

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
        return de(sinal, lod, 1.0F);
    }

    /** Idem, sem forma declarada: o corpo inteiro. */
    public static AuraVisualState de(SinalDeAura sinal, AuraRenderLod lod,
            float visibilidade) {
        return de(sinal, lod, visibilidade, AuraDistribution.uniforme());
    }

    /**
     * Idem, com a VISIBILIDADE que o resolvedor respondeu.
     *
     * <p>ELA ENTRA COMO MULTIPLICADOR DA INTENSIDADE, e nao como um corte
     * separado. A diferenca importa: multiplicada, ela participa da mesma conta
     * que o nivel de detalhe e some junto com ele; como um {@code if} a parte,
     * ela seria mais um lugar de onde a aura pode desaparecer -- e quando Gyo e
     * In existirem, "por que essa aura sumiu" teria duas respostas possiveis.
     *
     * <p>Hoje a resposta e quase sempre 1,0. O ponto do resolvedor nao e o valor
     * de hoje: e que amanha ele seja um NUMERO, e nao um renderer novo.
     */
    /**
     * Idem, COM a forma que o servidor anunciou.
     *
     * <p><b>A FORMA ENTROU EM 2026-09-26, e a ausencia dela era um defeito
     * visto em jogo.</b> Esta linha era {@code AuraDistribution.uniforme()},
     * chumbada, e o resultado era Gyo lido como Ken e Ko lido como Ren de corpo
     * cheio: quem concentrava via a propria aura no ponto certo, e quem
     * observava via o corpo todo aceso.
     *
     * <p>Ela chega em {@code aura_presence}, quantizada num byte por regiao.
     */
    public static AuraVisualState de(SinalDeAura sinal, AuraRenderLod lod,
            float visibilidade, AuraDistribution forma) {
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
        //
        // ELE NAO DECIDE MAIS O MODO -- so traduz o sinal pobre que chegou pela
        // rede na tecnica que ele representa. O modo sai de ModoVisualCanonico,
        // a mesma tabela que o jogador local consulta. Ate 2026-09-25 este
        // arquivo tinha a propria opiniao sobre Ken, e ela discordava da do
        // outro caminho: aqui Ken desenhava como REN, la caia em OFF.
        ResourceLocation representante = switch (sinal) {
            case KEN -> Ken.ID;
            case REN -> Ren.ID;
            case TEN -> Ten.ID;
            case NENHUM -> null;
        };
        AuraVisualMode modo = ModoVisualCanonico.modoDe(representante)
                .orElse(AuraVisualMode.OFF);
        int cor = AparenciaDeTecnica.de(
                representante == null ? Ten.ID : representante).cor();

        // OS NUMEROS DE ARTE NAO ENTRAM AQUI. Quem desenha busca o perfil pelo
        // MODO -- `AuraPerfis.de(estado.mode())` --, e por isso a aura de um
        // terceiro em Ren usa exatamente o mesmo `ren.json` que a do jogador
        // local. Carregar os numeros dentro do estado abriria a porta para dois
        // caminhos com valores diferentes para o mesmo Ren, e a divergencia
        // apareceria como "a aura dos outros esta mais fraca" sem nenhum erro.
        float intensidade = Math.clamp(intensidadePara(lod) * visibilidade, 0.0F, 1.0F);
        if (intensidade <= 0.0F) {
            // VISIBILIDADE ZERO E AUSENCIA TOTAL, e nao uma aura fraquissima.
            // Um alpha de 0,001 ainda desenha geometria, ainda alimenta o alvo
            // de brilho e ainda aparece contra um fundo escuro.
            return AuraVisualState.desligado();
        }
        return new AuraVisualState(modo, intensidade, 1.0F,
                forma == null ? AuraDistribution.uniforme() : forma, cor, cor);
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
