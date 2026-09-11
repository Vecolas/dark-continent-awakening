package com.darkcontinent.nenfoundation.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import com.darkcontinent.nenfoundation.network.handler.Recebedores;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import com.darkcontinent.nenfoundation.network.payload.FeedbackDeErroS2C;
import com.darkcontinent.nenfoundation.network.payload.FxDeHabilidadeS2C;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao do cache de cliente.
 *
 * <p>Ele roda sem o jogo carregado porque o cache nao depende do jogo: o tick
 * do cliente entra por um supplier. Foi por isso que ele entrou por um
 * supplier — logica que so pode ser exercitada com o Minecraft aberto nao e
 * exercitada.
 */
class NenClientCacheTest {

    private final AtomicLong tick = new AtomicLong(100L);
    private final NenClientCache cache = new NenClientCache(this.tick::get, () -> false);

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }

    private static SnapshotDePerfilS2C snapshot(NenCategory categoria) {
        return new SnapshotDePerfilS2C(categoria, Set.of(id("ten")), Set.of(), Set.of());
    }

    private static DeltaDeRuntimeS2C deltaComAura(float aura) {
        return new DeltaDeRuntimeS2C(aura, 100.0F, 1.0F, Set.of(), Map.of());
    }

    @AfterEach
    void limparRegistro() {
        Recebedores.limpar();
    }

    // ------------------------------------------- zero nao e "ainda nao sei"

    @Test
    @DisplayName("cache novo distingue 'nunca recebi' de 'e zero'")
    void nuncaRecebidoNaoEZero() {
        assertFalse(this.cache.recebeuAlgumDelta(),
                "Cache novo nao pode dizer que recebeu delta.");
        assertEquals(0.0F, this.cache.auraOuZero(),
                "auraOuZero devolve zero, mas quem desenha PRECISA conferir"
                        + " recebeuAlgumDelta antes: barra vazia por 'ainda nao sei'"
                        + " mente para o jogador no login.");

        this.cache.aoReceberDelta(deltaComAura(0.0F));

        assertTrue(this.cache.recebeuAlgumDelta());
        assertEquals(0.0F, this.cache.auraOuZero(),
                "Agora zero significa zero de verdade, e as duas situacoes"
                        + " continuam distinguiveis.");
    }

    @Test
    @DisplayName("sem snapshot, a categoria visivel e o NEUTRO")
    void semSnapshotCategoriaENeutra() {
        assertSame(NenCategory.UNDETERMINED, this.cache.categoriaVisivel());
        assertFalse(this.cache.recebeuAlgumSnapshot());
    }

    // ---------------------------------------------------------- recepcao

    @Test
    @DisplayName("snapshot novo substitui o anterior por inteiro")
    void snapshotSubstitui() {
        this.cache.aoReceberSnapshot(snapshot(NenCategory.EMISSION));
        assertSame(NenCategory.EMISSION, this.cache.categoriaVisivel());

        this.cache.aoReceberSnapshot(snapshot(NenCategory.CONJURATION));
        assertSame(NenCategory.CONJURATION, this.cache.categoriaVisivel());
        assertEquals(2, this.cache.snapshotsRecebidos());
    }

    @Test
    @DisplayName("o tick do ultimo delta e lido na hora em que o delta chega")
    void tickDoDeltaELidoNaHora() {
        this.tick.set(500L);
        this.cache.aoReceberDelta(deltaComAura(10.0F));

        this.tick.set(512L);
        assertEquals(12L, this.cache.ticksDesdeOUltimoDelta(),
                "Se o cache tivesse guardado o tick calculado em vez de perguntar"
                        + " na hora, esta conta daria zero para sempre.");
    }

    @Test
    @DisplayName("sem nenhum delta, ticksDesdeOUltimoDelta e -1 e nao 0")
    void semDeltaOTempoENegativo() {
        assertEquals(-1L, this.cache.ticksDesdeOUltimoDelta(),
                "Zero aqui leria como 'chegou agora mesmo', que e o oposto da"
                        + " verdade. Sentinela de 'nao configurado' e negativa"
                        + " quando zero e valor valido.");
    }

    @Test
    @DisplayName("FX conta, mas nao muda estado")
    void fxNaoMudaEstado() {
        this.cache.aoReceberSnapshot(snapshot(NenCategory.ENHANCEMENT));
        this.cache.aoReceberDelta(deltaComAura(50.0F));

        this.cache.aoReceberFx(new FxDeHabilidadeS2C(id("disparo_de_aura"), Vec3.ZERO, 0));

        assertEquals(1, this.cache.fxRecebidos());
        assertSame(NenCategory.ENHANCEMENT, this.cache.categoriaVisivel());
        assertEquals(50.0F, this.cache.auraOuZero(),
                "Um FX nao pode alterar nenhum estado. Se puder, o estado esta"
                        + " no lugar errado e um FX perdido causa divergencia.");
    }

    @Test
    @DisplayName("o ultimo erro fica disponivel, como chave de traducao")
    void erroFicaDisponivel() {
        this.cache.aoReceberErro(new FeedbackDeErroS2C("nenfoundation.recusa.aura_insuficiente"));
        assertEquals("nenfoundation.recusa.aura_insuficiente",
                this.cache.ultimoErro().orElseThrow());
    }

    // ------------------------------------------------------------ limpeza

    @Test
    @DisplayName("limpar esquece tudo, inclusive os contadores")
    void limparEsqueceTudo() {
        this.cache.aoReceberSnapshot(snapshot(NenCategory.MANIPULATION));
        this.cache.aoReceberDelta(deltaComAura(30.0F));
        this.cache.aoReceberFx(new FxDeHabilidadeS2C(id("x"), Vec3.ZERO, 0));
        this.cache.aoReceberErro(new FeedbackDeErroS2C("x"));

        this.cache.limpar();

        assertFalse(this.cache.recebeuAlgumSnapshot(),
                "Trocar de servidor nao pode deixar o perfil do anterior visivel.");
        assertFalse(this.cache.recebeuAlgumDelta());
        assertSame(NenCategory.UNDETERMINED, this.cache.categoriaVisivel());
        assertTrue(this.cache.ultimoErro().isEmpty());
        assertEquals(-1L, this.cache.ticksDesdeOUltimoDelta());

        assertEquals(0, this.cache.snapshotsRecebidos(),
                "Contador que atravessa sessoes faz o overlay dizer que os pacotes"
                        + " estao chegando quando nao esta chegando nenhum.");
        assertEquals(0, this.cache.deltasRecebidos());
        assertEquals(0, this.cache.fxRecebidos());
        assertEquals(0, this.cache.errosRecebidos());
    }

    // -------------------------------------------------- registro de papel

    @Test
    @DisplayName("sem recebedor registrado, o no-op responde e nao ha NPE")
    void semRecebedorNaoEstoura() {
        assertFalse(Recebedores.temRecebedor());
        // E o que acontece no servidor dedicado: o handler roda e nao ha cache.
        Recebedores.atual().aoReceberDelta(deltaComAura(1.0F));
        Recebedores.atual().aoReceberSnapshot(snapshot(NenCategory.EMISSION));
        Recebedores.atual().aoReceberFx(new FxDeHabilidadeS2C(id("x"), Vec3.ZERO, 0));
        Recebedores.atual().aoReceberErro(new FeedbackDeErroS2C("x"));
    }

    @Test
    @DisplayName("registrar dois recebedores lanca")
    void doisRecebedoresLancam() {
        Recebedores.registrar(this.cache);
        assertTrue(Recebedores.temRecebedor());

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Recebedores.registrar(new NenClientCache(() -> 0L, () -> false)));
        assertTrue(e.getMessage().contains("Ja existe"),
                "Dois recebedores fazem duas telas mostrarem valores diferentes do"
                        + " mesmo jogador, sem nada acusar.");
    }
}
