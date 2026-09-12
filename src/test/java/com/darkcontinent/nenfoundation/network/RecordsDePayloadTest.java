package com.darkcontinent.nenfoundation.network;

import com.darkcontinent.nenfoundation.nen.aura.AlocacaoDeAura;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.network.payload.AjustarOutputC2S;
import com.darkcontinent.nenfoundation.network.payload.AtivarHabilidadeC2S;
import com.darkcontinent.nenfoundation.network.payload.AtivarTecnicaC2S;
import com.darkcontinent.nenfoundation.network.payload.DeltaDeRuntimeS2C;
import com.darkcontinent.nenfoundation.network.payload.FeedbackDeErroS2C;
import com.darkcontinent.nenfoundation.network.payload.NenPayloads;
import com.darkcontinent.nenfoundation.network.payload.SnapshotDePerfilS2C;
import com.darkcontinent.nenfoundation.nen.category.NenCategory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao dos records de payload.
 *
 * <p>Ele cruza tres coisas que precisam dizer a mesma verdade: a tabela
 * congelada em {@link NenProtocol}, os records em Java, e o catalogo em
 * {@link NenPayloads}.
 *
 * <p>POR QUE ISSO IMPORTA: a tabela declara o que cada payload PODE carregar, e
 * outro portao ({@code ProtocoloCongeladoTest}) usa essa declaracao para
 * garantir que nenhum payload C2S carregue estado do servidor. Se o record
 * puder ganhar um campo sem a tabela ganhar a linha correspondente, aquela
 * garantia vira decoracao — alguem acrescenta {@code auraGasta} ao record e o
 * portao continua verde, porque ele so olha a tabela.
 *
 * <p>Ele varre as duas FONTES (a tabela e o catalogo) e morde dos dois lados.
 */
class RecordsDePayloadTest {

    // ------------------------------------------------ tabela x implementacao

    @Test
    @DisplayName("todo payload da tabela tem record, e todo record esta na tabela")
    void tabelaECatalogoCasam() {
        Set<ResourceLocation> naTabela = new LinkedHashSet<>();
        for (NenProtocol.Registro r : NenProtocol.TABELA) {
            naTabela.add(r.id());
        }
        Set<ResourceLocation> implementados = new LinkedHashSet<>();
        for (NenPayloads.Implementado i : NenPayloads.TODOS) {
            implementados.add(i.tipo().id());
        }

        assertFalse(naTabela.isEmpty(), "NenProtocol.TABELA vazia.");
        assertFalse(implementados.isEmpty(), "NenPayloads.TODOS vazio.");

        Set<ResourceLocation> semRecord = new LinkedHashSet<>(naTabela);
        semRecord.removeAll(implementados);
        assertTrue(semRecord.isEmpty(),
                "Payload declarado na tabela e sem record: " + semRecord);

        Set<ResourceLocation> foraDaTabela = new LinkedHashSet<>(implementados);
        foraDaTabela.removeAll(naTabela);
        assertTrue(foraDaTabela.isEmpty(),
                "Record de payload sem entrada na tabela: " + foraDaTabela
                        + ". Sem a entrada, o portao que proibe campo de servidor"
                        + " em payload C2S nao enxerga este payload.");

        assertEquals(NenProtocol.TABELA.size(), NenPayloads.TODOS.size(),
                "Tabela e catalogo com tamanhos diferentes — ha id duplicado num dos dois.");
    }

    @Test
    @DisplayName("a direcao do catalogo e a mesma da tabela")
    void direcoesCasam() {
        Map<ResourceLocation, Direcao> naTabela = new LinkedHashMap<>();
        for (NenProtocol.Registro r : NenProtocol.TABELA) {
            naTabela.put(r.id(), r.direcao());
        }
        for (NenPayloads.Implementado i : NenPayloads.TODOS) {
            assertEquals(naTabela.get(i.tipo().id()), i.direcao(),
                    "Direcao divergente para " + i.tipo().id()
                            + ". Um payload registrado na direcao errada e o buraco"
                            + " por onde o cliente passa a enviar o que so o servidor"
                            + " deveria enviar.");
        }
    }

    @Test
    @DisplayName("os componentes de cada record sao exatamente os campos declarados")
    void componentesCasamComOsCamposDeclarados() {
        Map<ResourceLocation, List<String>> declarados = new LinkedHashMap<>();
        for (NenProtocol.Registro r : NenProtocol.TABELA) {
            declarados.put(r.id(), r.campos());
        }

        int conferidos = 0;
        for (NenPayloads.Implementado i : NenPayloads.TODOS) {
            Class<? extends CustomPacketPayload> classe = i.classe();
            assertTrue(classe.isRecord(),
                    classe.getSimpleName() + " nao e um record. O portao le os"
                            + " componentes por reflexao; uma classe comum passaria"
                            + " sem ser conferida.");

            List<String> reais = new ArrayList<>();
            for (RecordComponent rc : classe.getRecordComponents()) {
                reais.add(rc.getName());
            }

            assertEquals(declarados.get(i.tipo().id()), reais,
                    "Os componentes de " + classe.getSimpleName() + " nao batem com"
                            + " os campos declarados na tabela para " + i.tipo().id()
                            + ".\nA ordem tambem importa: ela e a ordem do StreamCodec,"
                            + " e trocar duas posicoes do mesmo tipo produz bytes"
                            + " validos com significado trocado.\nAtualize"
                            + " NenProtocol.TABELA e docs/multiplayer/protocol.md junto.");
            conferidos++;
        }
        assertTrue(conferidos > 0, "Nenhum record conferido.");
    }

    @Test
    @DisplayName("o portao de componentes reprova quando alimentado com um record errado")
    void oPortaoDeComponentesMorde() {
        // Caso de controle: um record cujos componentes NAO batem com nenhuma
        // declaracao. Regua que nunca reprovou e carimbo.
        record PayloadDeControle(ResourceLocation tecnicaId, float auraGasta) {
        }

        List<String> reais = new ArrayList<>();
        for (RecordComponent rc : PayloadDeControle.class.getRecordComponents()) {
            reais.add(rc.getName());
        }
        List<String> declarados = NenProtocol.TABELA.stream()
                .filter(r -> r.id().getPath().equals("activate_technique_request"))
                .findFirst()
                .orElseThrow()
                .campos();

        assertFalse(declarados.equals(reais),
                "O criterio de comparacao aceitou um record com um campo a mais"
                        + " ('auraGasta'). O portao de cima nao esta medindo nada.");
    }

    // ----------------------------------------------- ida e volta na rede

    @Test
    @DisplayName("cada payload faz ida e volta pelo proprio StreamCodec")
    void idaEVoltaNaRede() {
        idaEVolta(AtivarTecnicaC2S.STREAM_CODEC,
                new AtivarTecnicaC2S(id("ren")));

        idaEVolta(AjustarOutputC2S.STREAM_CODEC,
                new AjustarOutputC2S(+0.10F));

        idaEVolta(AtivarHabilidadeC2S.STREAM_CODEC,
                new AtivarHabilidadeC2S(id("disparo_de_aura"), 3,
                        OptionalInt.of(0), Optional.of(new Vec3(1.5D, -64.0D, 2048.25D))));

        idaEVolta(AtivarHabilidadeC2S.STREAM_CODEC,
                new AtivarHabilidadeC2S(id("impacto_reforcado"), 0,
                        OptionalInt.empty(), Optional.empty()));

        idaEVolta(SnapshotDePerfilS2C.STREAM_CODEC,
                new SnapshotDePerfilS2C(NenCategory.SPECIALIZATION,
                        Set.of(id("ten"), id("gyo")), Set.of(id("analise_de_aura")),
                        Set.of(id("despertou"))));

        idaEVolta(SnapshotDePerfilS2C.STREAM_CODEC,
                new SnapshotDePerfilS2C(NenCategory.UNDETERMINED,
                        Set.of(), Set.of(), Set.of()));

        idaEVolta(DeltaDeRuntimeS2C.STREAM_CODEC,
                new DeltaDeRuntimeS2C(12.5F, 80.0F, 1.0F, Set.of(id("ren")),
                        Map.of(id("disparo_de_aura"), 40), AlocacaoDeAura.uniforme()));

        idaEVolta(FeedbackDeErroS2C.STREAM_CODEC,
                new FeedbackDeErroS2C("nenfoundation.recusa.aura_insuficiente"));
    }

    @Test
    @DisplayName("id de entidade ZERO sobrevive, e nao vira ausente")
    void idDeEntidadeZeroNaoViraAusente() {
        AtivarHabilidadeC2S comAlvoZero = new AtivarHabilidadeC2S(
                id("marca_de_controle"), 1, OptionalInt.of(0), Optional.empty());

        AtivarHabilidadeC2S volta = pelaRede(AtivarHabilidadeC2S.STREAM_CODEC, comAlvoZero);

        assertTrue(volta.alvoIdCandidato().isPresent(),
                "O alvo com id 0 chegou como AUSENTE. Id de entidade zero e valido;"
                        + " se o codec usasse um sentinela numerico, um alvo legitimo"
                        + " viraria 'sem alvo' em silencio.");
        assertEquals(0, volta.alvoIdCandidato().getAsInt());
    }

    @Test
    @DisplayName("categoria desconhecida na rede falha alto, e nao vira UNDETERMINED")
    void categoriaDesconhecidaFalhaAlto() {
        ByteBuf buf = Unpooled.buffer();
        // Escreve manualmente um snapshot com um nome de categoria que nao existe.
        net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.encode(buf, "nen_do_futuro");
        net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(buf, 0); // tecnicas
        net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(buf, 0); // habilidades
        net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(buf, 0); // marcos

        IllegalArgumentException e = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SnapshotDePerfilS2C.STREAM_CODEC.decode(buf));

        assertTrue(e.getMessage().contains("nen_do_futuro"),
                "A excecao precisa nomear o valor recebido — sem isso o diagnostico"
                        + " de uma divergencia de versao vira adivinhacao.");
    }

    // -------------------------------------------------------------- apoio

    private static ResourceLocation id(String caminho) {
        return ResourceLocation.fromNamespaceAndPath("nenfoundation", caminho);
    }

    private static <T> T pelaRede(StreamCodec<ByteBuf, T> codec, T original) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, original);
        T volta = codec.decode(buf);
        assertEquals(0, buf.readableBytes(),
                "Sobraram bytes no buffer depois de decodificar. O codec le menos do"
                        + " que escreve, e o proximo payload do mesmo pacote comecaria"
                        + " no lugar errado.");
        return volta;
    }

    private static <T> void idaEVolta(StreamCodec<ByteBuf, T> codec, T original) {
        assertEquals(original, pelaRede(codec, original),
                "Ida e volta alterou o payload: " + original);
    }
}
