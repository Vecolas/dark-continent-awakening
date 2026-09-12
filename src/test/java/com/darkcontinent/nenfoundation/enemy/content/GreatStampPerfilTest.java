package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.combat.ChargeRules;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPoint;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointRegistry;
import com.darkcontinent.nenfoundation.enemy.combat.WeakPointResolver;
import org.junit.jupiter.api.Test;

/**
 * Sustenta que o perfil do great stamp e a unica fonte dos numeros da carga, e que o resolver
 * de ponto fraco e o catalogo de weak points nao podem divergir em silencio.
 */
class GreatStampPerfilTest {
    @Test
    void numerosDaCargaMoramNoPerfil() {
        ChargeRules regras = HunterExamProfiles.greatStampChargeRules();
        assertEquals(4.0D, regras.distanciaMinima());
        assertEquals(10.0D, regras.distanciaMaxima());
        assertEquals(60, regras.ticksDeEspera());
        assertEquals(2.35D, regras.multiplicadorDeVelocidade());
        assertEquals(40, regras.ticksDeAtordoamento());
    }

    @Test
    void numerosDoPontoFracoMoramNoPerfil() {
        WeakPointResolver resolver = HunterExamProfiles.greatStampWeakPoint();
        assertEquals("forehead", resolver.regiaoVulneravel());
        assertEquals("body", resolver.regiaoPadrao());
        assertEquals(0.62D, resolver.alturaMinima());
        assertEquals(0.5D, resolver.cossenoMinimo());
    }

    @Test
    void aRegiaoQueOResolverApontaExisteEEstaHabilitadaNoCatalogo() {
        WeakPointResolver resolver = HunterExamProfiles.greatStampWeakPoint();
        WeakPointRegistry catalogo = HunterExamProfiles.greatStampWeakPoints();

        WeakPoint vulneravel = catalogo.all().get(resolver.regiaoVulneravel());
        assertNotNull(vulneravel, "resolver aponta para um id que o catalogo nao conhece");
        assertTrue(vulneravel.enabled(), "ponto fraco desligado tiraria a testa do jogo sem erro nenhum");
        assertTrue(catalogo.multiplier(resolver.regiaoVulneravel()) > 1.0F,
                "a testa precisa doer mais que o corpo");

        assertEquals(1.0F, catalogo.multiplier(resolver.regiaoPadrao()),
                "a regiao padrao nao pode carregar multiplicador");
        assertEquals(resolver.regiaoVulneravel(), resolver.resolver(0.95D, 1.0D),
                "acerto alto e de frente precisa cair no id que o catalogo multiplica");
    }

    /**
     * A REGUA DA CARGA. A janela ACTIVE, a velocidade e a faixa de disparo sao tres
     * numeros em dois arquivos diferentes, e nada em jogo acusa quando eles deixam de
     * fechar: a investida simplesmente para antes do alvo. Este teste e o que acusa.
     */
    @Test
    void aCorridaAlcancaAMaiorDistanciaDeDisparo() {
        ChargeRules regras = HunterExamProfiles.greatStampChargeRules();
        double velocidadeBase = HunterExamProfiles.greatStamp().attributes().movementSpeed();
        double alcanceDaCorrida = HunterExamProfiles.greatStampCharge().activeTicks()
                * velocidadeBase * regras.multiplicadorDeVelocidade();

        assertTrue(alcanceDaCorrida >= regras.distanciaMaxima(),
                "a corrida cobre " + alcanceDaCorrida + " blocos, mas a carga dispara de ate "
                        + regras.distanciaMaxima() + ": o great stamp investe e para antes do alvo");
        assertTrue(regras.distanciaMinima() < regras.distanciaMaxima(),
                "faixa de disparo vazia nunca dispara carga nenhuma");
    }

    @Test
    void aJanelaDeLuzPermiteAManadaNascerDeDia() {
        assertEquals(15, HunterExamProfiles.greatStamp().spawnRule().maxLight(),
                "janela de luz fechada faria a manada nunca nascer");
        assertTrue(HunterExamProfiles.greatStamp().spawnRule().maxNearbySameFaction() > 1,
                "great stamp anda em manada, nao sozinho");
    }
}
