package com.darkcontinent.nenfoundation.client.vfx.render;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import net.minecraft.client.model.geom.ModelLayerLocation;

/**
 * Os seis {@link ModelLayerLocation} da shell: tres passes x dois modelos.
 *
 * <p>SEIS, E NAO TRES. O braco {@code slim} tem um pixel a menos de largura, e
 * uma malha construida para o modelo padrao veste errado quem usa skin Alex. O
 * defeito nao aparece em teste nenhum: aparece quando a segunda pessoa entra no
 * servidor.
 *
 * <p>DERIVADOS DO ENUM, e nao escritos a mao um por um. Um passe novo em
 * {@link AuraShellPass} passa a existir aqui automaticamente -- e o registro
 * varre a mesma fonte, entao nao ha lista para alguem esquecer de atualizar.
 */
public final class AuraModelLayers {

    private AuraModelLayers() {
    }

    /**
     * A camada deste passe, neste modelo de jogador.
     *
     * @param slim {@code true} para o modelo de braco fino (Alex)
     */
    public static ModelLayerLocation de(AuraShellPass passe, boolean slim) {
        String nome = "aura_shell_" + passe.name().toLowerCase(java.util.Locale.ROOT);
        return new ModelLayerLocation(NenFoundation.id(nome), slim ? "slim" : "default");
    }
}
