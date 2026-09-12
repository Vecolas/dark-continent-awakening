package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellMaterial;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O que da para provar do shader SEM uma GPU.
 *
 * <p>E so a coerencia dos parametros -- a APARENCIA nao vira verde aqui, e o
 * gate #176 e quem responde por ela. Mas um Fresnel igual nas tres camadas
 * apagaria a profundidade que justifica os tres passes, e isso da para pegar
 * sem desenhar nada.
 */
class AuraShellMaterialTest {

    @Test
    @DisplayName("o expoente de Fresnel diminui da camada interna para a externa")
    void fresnelAbreParaFora() {
        for (AuraShellMaterial material : new AuraShellMaterial[] {
                AuraShellMaterial.ten(), AuraShellMaterial.ren()}) {
            assertTrue(material.fresnelDe(AuraShellPass.INTERNA)
                    > material.fresnelDe(AuraShellPass.BORDA));
            assertTrue(material.fresnelDe(AuraShellPass.BORDA)
                    > material.fresnelDe(AuraShellPass.EXTERNA));
        }
    }

    @Test
    @DisplayName("Fresnel igual nas tres camadas e recusado: elas virariam uma so")
    void fresnelChapadoERecusado() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuraShellMaterial(2.0F, 2.0F, 2.0F, 0.12F, 4.0F, 0.9F));
        assertThrows(IllegalArgumentException.class,
                () -> new AuraShellMaterial(1.0F, 2.0F, 3.0F, 0.12F, 4.0F, 0.9F),
                "invertido tambem: a borda ficaria mais estreita para fora");
    }

    @Test
    @DisplayName("Ren abre a borda e acelera o fluxo -- e a MESMA shell")
    void renEATenComABordaAberta() {
        AuraShellMaterial ten = AuraShellMaterial.ten();
        AuraShellMaterial ren = AuraShellMaterial.ren();
        for (AuraShellPass passe : AuraShellPass.values()) {
            assertTrue(ren.fresnelDe(passe) < ten.fresnelDe(passe),
                    "expoente MENOR = borda mais espessa, em " + passe);
        }
        assertTrue(ren.velocidadeDeFluxo() > ten.velocidadeDeFluxo());
        assertTrue(ren.reforcoDaBorda() > ten.reforcoDaBorda());
    }

    @Test
    @DisplayName("valores nao-finitos e escala zero sao recusados")
    void valoresInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuraShellMaterial(3.4F, 2.7F, 2.0F, Float.NaN, 4.0F, 0.9F));
        assertThrows(IllegalArgumentException.class,
                () -> new AuraShellMaterial(3.4F, 2.7F, 2.0F, 0.12F, 0.0F, 0.9F),
                "escala zero colapsaria a UV do ruido num ponto so");
        assertThrows(IllegalArgumentException.class,
                () -> new AuraShellMaterial(3.4F, 2.7F, 2.0F, -1.0F, 4.0F, 0.9F));
    }

    @Test
    @DisplayName("fluxo zero e legitimo: aura parada continua sendo aura")
    void fluxoZeroEPermitido() {
        AuraShellMaterial parado = new AuraShellMaterial(3.4F, 2.7F, 2.0F, 0.0F, 4.0F, 0.9F);
        assertTrue(parado.velocidadeDeFluxo() == 0.0F);
    }
}
