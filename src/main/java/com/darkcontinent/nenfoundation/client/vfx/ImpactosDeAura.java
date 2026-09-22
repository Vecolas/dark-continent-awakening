package com.darkcontinent.nenfoundation.client.vfx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;

/**
 * Os ripples vivos, por jogador (#103).
 *
 * <p><b>POR QUE UM DONO, e nao um estado derivado na hora.</b> O resto da aura
 * dos outros e derivado do sinal e some sozinho quando o sinal some -- e o
 * javadoc de {@code tickDaAuraDosOutros} explica por que isso e bom. O impacto
 * nao pode ser assim: ele e uma BORDA, dura oito ticks e precisa de alguem que
 * se lembre dela entre um quadro e o seguinte. Derivar na hora daria um ripple
 * de um tick, que na tela e um piscar que ninguem consegue ler.
 *
 * <p><b>E O DONO LIMPA POR PRESENCA.</b> Guardar estado por entidade cria
 * exatamente o problema que o mesmo javadoc descreve -- alguem precisa limpar
 * quando a pessoa sai do alcance, e esse alguem esquece um caminho. A saida e a
 * mesma de {@link DetectorDeAtivacaoDeTen}: a poda e por quem esta A VISTA
 * agora, e nao por evento. Morte, logout e troca de dimensao nao precisam de
 * handler nenhum -- o jogador simplesmente deixa de estar na lista.
 *
 * <p>CLIENT-ONLY. O impacto CHEGA pela rede -- ver {@code ImpactoDeAuraS2C}
 * para por que o ripple e derivado no cliente em vez de vir do servidor.
 */
public final class ImpactosDeAura {

    private final Map<Integer, AuraImpactState> vivos = new HashMap<>();

    /**
     * REUSADO entre ticks, e nao criado a cada um.
     *
     * <p>A auditoria do AV8 mediu alocacao por quadro e tirou do caminho de
     * desenho o record por jogador e o {@code BlockPos} imutavel do anel. Um
     * {@code HashSet} novo por tick e a mesma classe de custo: nao aparece como
     * erro, aparece como o coletor trabalhando durante a luta -- justamente
     * quando ha mais gente na tela e mais pancada acontecendo.
     */
    private final Set<Integer> presentes = new HashSet<>();

    /**
     * Um tick: avanca os ripples vivos, le as bordas novas e poda o resto.
     *
     * <p>A ORDEM IMPORTA. Avancar ANTES de detectar e o que garante que uma
     * pancada nova nasca inteira: invertido, o impacto do tick recem-criado
     * levaria um decremento antes de ser desenhado uma vez.
     */
    public void tick(Iterable<? extends LivingEntity> visiveis) {
        this.presentes.clear();

        this.vivos.replaceAll((id, impacto) -> impacto.avancar());
        this.vivos.values().removeIf(impacto -> !impacto.ativo());

        for (LivingEntity entidade : visiveis) {
            this.presentes.add(entidade.getId());
        }

        // A PODA POR PRESENCA CONTINUA AQUI, e nao no recebimento: quem sai de
        // vista para de existir para o VFX, e um impacto guardado por id de
        // entidade sobreviveria a morte e a troca de dimensao -- exatamente o
        // que `MorteETrocaDeDimensaoTest` existe para impedir.
        this.vivos.keySet().retainAll(this.presentes);
    }

    /**
     * Um impacto que o SERVIDOR anunciou.
     *
     * <p>Antes de 2026-09-22 o impacto era inferido aqui, de uma queda de vida,
     * e a regiao era um palpite fixo no TRONCO -- o cliente nao tinha como saber
     * onde o golpe caiu. Agora {@code ImpactoDeAuraS2C} traz a faixa e a forca
     * ja normalizada, e a inferencia foi aposentada junto do
     * {@code DetectorDeImpacto}.
     *
     * @param entidadeId quem apanhou
     * @param faixa onde o golpe caiu
     * @param forca ja em {@code 0..1}; zero ou menos nao acende nada
     */
    public void registrar(int entidadeId,
            com.darkcontinent.nenfoundation.nen.combat.FaixaDoCorpo faixa, float forca) {
        if (faixa == null || !Float.isFinite(forca) || forca <= 0.0F) {
            return;
        }
        // A PANCADA NOVA SUBSTITUI a que ainda decaia, e nao soma: duas pancadas
        // seguidas sao duas leituras, nao uma aura que acende em degrau ate
        // saturar.
        this.vivos.put(entidadeId, AuraImpactState.iniciar(faixa, Math.min(1.0F, forca)));
    }

    /** O ripple desta entidade agora, ou {@code null} se nao ha nenhum. */
    public AuraImpactState de(int entidadeId) {
        return this.vivos.get(entidadeId);
    }

    /**
     * A distribuicao ja com o ripple desta entidade somado.
     *
     * <p>Existe para que o chamador nao precise conhecer {@link AuraImpactState}:
     * o renderer pergunta "como esta a distribuicao desta pessoa", e o impacto
     * entra ou nao entra sem que ele saiba a diferenca.
     */
    public AuraDistribution aplicar(int entidadeId, AuraDistribution distribuicao) {
        return distribuicao == null ? null : distribuicao.comImpacto(de(entidadeId));
    }

    /** QUEM LIGA, DESLIGA -- chamado no mesmo ponto de saida que limpa o resto do vfx. */
    public void limpar() {
        this.vivos.clear();
        this.presentes.clear();
    }
}
