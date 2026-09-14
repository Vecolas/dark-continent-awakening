package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Regua automatica da ativacao sonora de Ten; audicao humana fica no gate AV3. */
class AudioDeTenTest {

    @Test
    @DisplayName("OFF para TEN dispara uma vez, sem empilhar a cada tick")
    void ativacaoLocalEhUmaBorda() {
        SessaoDeVfxDeAura sessao = new SessaoDeVfxDeAura();

        sessao.aoTick(Set.of(Ten.ID), 0.5F, 0xFFFFFFFF, 1.0F);
        assertTrue(sessao.consumirAtivacaoDeTen());
        assertFalse(sessao.consumirAtivacaoDeTen());

        for (int i = 0; i < 20; i++) {
            sessao.aoTick(Set.of(Ten.ID), 0.5F, 0xFFFFFFFF, 1.0F);
            assertFalse(sessao.consumirAtivacaoDeTen(),
                    "Ten sustentado tentou tocar a ativacao outra vez no tick " + i);
        }
    }

    @Test
    @DisplayName("uma nova ativacao so existe depois de sair de Ten")
    void reativaDepoisDeDesligar() {
        SessaoDeVfxDeAura sessao = new SessaoDeVfxDeAura();
        sessao.aoTick(Set.of(Ten.ID), 0.5F, 0xFFFFFFFF, 1.0F);
        assertTrue(sessao.consumirAtivacaoDeTen());

        sessao.aoTick(Set.of(), 0.0F, 0xFFFFFFFF, 1.0F);
        sessao.aoTick(Set.of(Ten.ID), 0.5F, 0xFFFFFFFF, 1.0F);
        assertTrue(sessao.consumirAtivacaoDeTen());
    }

    @Test
    @DisplayName("ver alguem ja em Ten nao inventa ativacao; a borda seguinte toca")
    void ativacaoRemotaExigeBordaObservada() {
        DetectorDeAtivacaoDeTen detector = new DetectorDeAtivacaoDeTen();
        assertFalse(detector.atualizar(7, SinalDeAura.TEN));
        assertFalse(detector.atualizar(7, SinalDeAura.NENHUM));
        assertTrue(detector.atualizar(7, SinalDeAura.TEN));
        assertFalse(detector.atualizar(7, SinalDeAura.TEN));
        assertFalse(detector.atualizar(7, SinalDeAura.REN));
        assertFalse(detector.atualizar(7, SinalDeAura.TEN),
                "voltar de Ren para Ten nao e ligar Ten a partir do zero");
    }

    @Test
    @DisplayName("asset e mono Ogg autoral estao ligados ao SoundEvent e a legenda")
    void contratoDoAsset() throws IOException {
        Path raiz = Repo.raiz().resolve("src/main/resources/assets/nenfoundation");
        Path ogg = raiz.resolve("sounds/vfx/nen/ten_activate.ogg");
        byte[] cabecalho = Files.readAllBytes(ogg);
        assertTrue(cabecalho.length > 4_000, "o asset parece vazio ou truncado");
        assertTrue(cabecalho[0] == 'O' && cabecalho[1] == 'g'
                && cabecalho[2] == 'g' && cabecalho[3] == 'S', "nao e um Ogg valido");
        int identificacaoVorbis = indiceDe(cabecalho,
                new byte[] {1, 'v', 'o', 'r', 'b', 'i', 's'});
        assertTrue(identificacaoVorbis >= 0, "cabecalho de identificacao Vorbis ausente");
        assertTrue(cabecalho[identificacaoVorbis + 11] == 1,
                "a ativacao precisa ser mono; canais=" + cabecalho[identificacaoVorbis + 11]);
        long amostrasPorSegundo = inteiroSemSinalLittleEndian(cabecalho,
                identificacaoVorbis + 12, 4);
        long totalDeAmostras = ultimaGranulacaoOgg(cabecalho);
        double duracao = (double) totalDeAmostras / amostrasPorSegundo;
        assertTrue(duracao >= 0.4D && duracao <= 0.9D,
                "duracao fora do contrato de 0.4–0.9 s: " + duracao);

        var sons = JsonParser.parseString(Files.readString(raiz.resolve("sounds.json")))
                .getAsJsonObject().getAsJsonObject("vfx.nen.ten_activate");
        assertTrue(sons.get("subtitle").getAsString().equals(
                "subtitles.nenfoundation.vfx.nen.ten_activate"));
        var definicao = sons.getAsJsonArray("sounds").get(0).getAsJsonObject();
        assertTrue(definicao.get("name").getAsString().equals(
                "nenfoundation:vfx/nen/ten_activate"));
        assertEquals(10, definicao.get("attenuation_distance").getAsInt());
        assertFalse(definicao.get("stream").getAsBoolean(),
                "efeito curto nao deve ocupar um stream continuo");
        for (String idioma : Set.of("pt_br.json", "en_us.json")) {
            var lang = JsonParser.parseString(Files.readString(raiz.resolve("lang/" + idioma)))
                    .getAsJsonObject();
            assertTrue(lang.has("subtitles.nenfoundation.vfx.nen.ten_activate"),
                    "legenda ausente em " + idioma);
        }
    }

    private static int indiceDe(byte[] dados, byte[] procurado) {
        for (int i = 0; i <= dados.length - procurado.length; i++) {
            boolean igual = true;
            for (int j = 0; j < procurado.length; j++) {
                if (dados[i + j] != procurado[j]) {
                    igual = false;
                    break;
                }
            }
            if (igual) {
                return i;
            }
        }
        return -1;
    }

    private static long ultimaGranulacaoOgg(byte[] dados) {
        int pagina = 0;
        long ultimaGranulacao = -1L;
        while (pagina < dados.length) {
            assertTrue(pagina + 27 <= dados.length, "pagina Ogg truncada");
            assertTrue(dados[pagina] == 'O' && dados[pagina + 1] == 'g'
                    && dados[pagina + 2] == 'g' && dados[pagina + 3] == 'S',
                    "captura Ogg ausente no offset " + pagina);
            int segmentos = Byte.toUnsignedInt(dados[pagina + 26]);
            assertTrue(pagina + 27 + segmentos <= dados.length,
                    "tabela de segmentos Ogg truncada");
            int corpo = 0;
            for (int i = 0; i < segmentos; i++) {
                corpo += Byte.toUnsignedInt(dados[pagina + 27 + i]);
            }
            ultimaGranulacao = inteiroSemSinalLittleEndian(dados, pagina + 6, 8);
            pagina += 27 + segmentos + corpo;
        }
        assertEquals(dados.length, pagina, "bytes sobrando depois da ultima pagina Ogg");
        assertTrue(ultimaGranulacao > 0, "granulacao final Ogg ausente");
        return ultimaGranulacao;
    }

    private static long inteiroSemSinalLittleEndian(byte[] dados, int inicio, int bytes) {
        long valor = 0L;
        for (int i = 0; i < bytes; i++) {
            valor |= (long) Byte.toUnsignedInt(dados[inicio + i]) << (i * 8);
        }
        return valor;
    }
}
