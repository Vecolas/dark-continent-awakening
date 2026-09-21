package com.darkcontinent.nenfoundation.client.vfx.model;

import com.darkcontinent.nenfoundation.client.vfx.AuraBodyRegion;
import com.darkcontinent.nenfoundation.client.vfx.ribbon.AuraAnchor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.util.RenderUtil;

/**
 * A aura sobre os BONES de um modelo GeckoLib.
 *
 * <p>ELE E O MOTIVO DE O ADAPTADOR EXISTIR. O
 * [ADR-012](docs/adr/ADR-012-geckolib-obrigatorio.md) ja diz que a aura de mob
 * customizado usa os bones do GeckoLib, e nao o modelo de jogador -- e
 * "{@code PlayerModel} em cima de um quadrupede" nao e um defeito que se
 * conserta com ajuste: e um renderer inteiro no lugar errado.
 *
 * <p><b>OS NOMES DE OSSO SAO DECLARADOS PELO INIMIGO</b>
 * ({@link AuraOssosDoInimigo}), e nao constantes aqui. Um mapa fixo neste
 * arquivo funcionaria para o primeiro mob e estaria errado para o segundo -- e o
 * defeito nao apareceria como erro: a aura nasceria no lugar errado, ou nao
 * nasceria.
 *
 * <p><b>OSSO AUSENTE E RECUSA COM MOTIVO, UMA VEZ POR MODELO.</b> Um log por
 * quadro a sessenta hertz enche o arquivo em minutos e esconde justamente a
 * linha que importa. O silencio e pior ainda: deixa a pessoa procurando por que
 * a aura daquele mob nao tem filamento. O conjunto {@link #jaAvisados} e o que
 * torna a linha unica -- e ele guarda NOMES, e nao instancias, porque o mesmo
 * modelo e compartilhado por todas as entidades daquele tipo.
 *
 * <p><b>ESTE ADAPTADOR NAO FOI EXERCITADO EM JOGO.</b> Nenhum inimigo tem Nen
 * antes do EN10, entao nada o chama hoje. Isso esta dito aqui e em
 * {@code docs/testing/o-que-nao-provamos.md}, e nao escondido: o que existe e a
 * COSTURA, testada na resolucao de nome; o desenho sobre um bone que gira e olho
 * humano.
 */
public final class GeoAuraAdapter implements AuraModelAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GeoAuraAdapter.class);

    /** Ossos ja reclamados, para o aviso sair UMA vez. */
    private final Set<String> jaAvisados = new HashSet<>();

    private final GeoModel<? extends software.bernie.geckolib.animatable.GeoAnimatable>
            modelo;
    private final AuraOssosDoInimigo ossos;
    private final String nomeDoModelo;

    public GeoAuraAdapter(
            GeoModel<? extends software.bernie.geckolib.animatable.GeoAnimatable> modelo,
            AuraOssosDoInimigo ossos, String nomeDoModelo) {
        if (modelo == null || ossos == null) {
            throw new NullPointerException("o adaptador de GeckoLib precisa do modelo e da"
                    + " declaracao de ossos");
        }
        this.modelo = modelo;
        this.ossos = ossos;
        this.nomeDoModelo = nomeDoModelo == null ? modelo.getClass().getSimpleName()
                : nomeDoModelo;
    }

    @Override
    public boolean empilharRegiao(PoseStack pilha, AuraBodyRegion regiao) {
        return empilhar(pilha, this.ossos.ossoDe(regiao), regiao);
    }

    @Override
    public boolean empilharAncora(PoseStack pilha, AuraAnchor ancora) {
        return empilhar(pilha, this.ossos.ossoDe(ancora), ancora);
    }

    @Override
    public void desenharRegiao(PoseStack pilha, VertexConsumer vertices, AuraBodyRegion regiao,
            int luz, int argb) {
        // A SHELL DE UM CORPO GECKOLIB NAO E UM MODELO INFLADO PRONTO, e por
        // isso este metodo nao desenha nada ainda. Inflar um `GeoBone` exige
        // reconstruir os cubos dele com folga, e isso e trabalho do EN10 --
        // quando existir um inimigo com Nen para justifica-lo.
        //
        // O QUE FICA DECLARADO: ate la, um mob GeckoLib com aura recebe os
        // FILAMENTOS (que so precisam da transformacao do osso) e nao a shell.
        // Desenhar a malha de jogador aqui seria exatamente o defeito que este
        // adaptador existe para impedir.
    }

    @Override
    public boolean temRegiao(AuraBodyRegion regiao) {
        String osso = this.ossos.ossoDe(regiao);
        return osso != null && buscar(osso).isPresent();
    }

    /** Se este corpo declara alguma ancora. Sem nenhuma, os filamentos nao nascem. */
    public boolean temAncoras() {
        return this.ossos.temAncoras();
    }

    private boolean empilhar(PoseStack pilha, String osso, Object pedido) {
        if (osso == null) {
            // NAO DECLARADO NAO E ERRO. Um quadrupede nao tem braco, e exigir
            // que ele declare um seria obrigar todo corpo a fingir ser
            // humanoide.
            return false;
        }
        Optional<GeoBone> encontrado = buscar(osso);
        if (encontrado.isEmpty()) {
            avisarUmaVez(osso, pedido);
            return false;
        }
        pilha.pushPose();
        RenderUtil.prepMatrixForBone(pilha, encontrado.get());
        return true;
    }

    private Optional<GeoBone> buscar(String osso) {
        try {
            return this.modelo.getBone(osso);
        } catch (RuntimeException erro) {
            // O CAMINHO DE DESENHO NAO PODE LANCAR. Uma excecao aqui derruba o
            // desenho do mundo inteiro por causa de um osso com nome errado.
            avisarUmaVez(osso, erro.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private void avisarUmaVez(String osso, Object pedido) {
        if (this.jaAvisados.add(osso)) {
            LOG.warn("O modelo '{}' nao tem o osso '{}', declarado para {}. A aura desenha"
                    + " sem os filamentos dessa parte. Este aviso sai UMA vez por osso.",
                    this.nomeDoModelo, osso, pedido);
        }
    }
}
