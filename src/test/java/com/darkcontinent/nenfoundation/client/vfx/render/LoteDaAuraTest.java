package com.darkcontinent.nenfoundation.client.vfx.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A aura nunca segura dois consumidores de vertice ao mesmo tempo.
 *
 * <p><b>ESTE PORTAO EXISTE POR CAUSA DE UM CRASH REAL, e ele so apareceu na
 * PRIMEIRA sessao de cliente da trilha AV (#299).</b> {@code
 * AuraPlayerRenderLayer.desenharFilamentos} pedia o buffer de {@code ribbon()}
 * e o de {@code ribbonDeBrilho()} antes do laco, e alternava entre os dois a
 * cada filamento.
 *
 * <p>Um {@code MultiBufferSource.BufferSource} guarda UM buffer compartilhado
 * para todo {@code RenderType} que nao esteja na tabela de buffers fixos -- e
 * nenhum tipo da aura esta. Pedir o segundo tipo ENCERRA o primeiro por dentro;
 * o consumidor que ja estava na mao vira referencia morta, e o vertice seguinte
 * levanta {@code IllegalStateException: Not building!}, derrubando o cliente no
 * meio do quadro.
 *
 * <p><b>NENHUM VERDE ACUSAVA.</b> O segundo consumidor so existe quando
 * {@code AuraPostProcess.capturando()} e verdadeiro, e o passe de brilho nunca
 * tinha rodado numa GPU -- essa exata frase estava em
 * {@code o-que-nao-provamos.md} como ponto cego declarado. Compilava, os testes
 * passavam, e o jogo morria ao ligar Ren com dois jogadores na tela.
 *
 * <p><b>O QUE ESTE ARQUIVO PROVA, E O QUE ELE NAO PROVA.</b> Ele prova uma
 * propriedade ESTRUTURAL do texto do arquivo: entre dois {@code getBuffer} tem
 * de existir um {@code descarregar}. Ele NAO executa render, nao abre janela e
 * nao sabe o que a GPU faz -- isso continua sendo o gate #198, com olho humano.
 * O que ele impede e a reintroducao do padrao, que e como o defeito nasceu.
 */
class LoteDaAuraTest {

    private static final String ALVO =
            "src/main/java/com/darkcontinent/nenfoundation/client/vfx/render/"
            + "AuraPlayerRenderLayer.java";

    /** Um pedido de buffer, ou uma descarga, na ordem em que aparecem no arquivo. */
    private record Evento(int linha, boolean pedido, String trecho) { }

    private static String fonte() {
        Path p = Repo.raiz().resolve(ALVO);
        assertTrue(Files.exists(p), "arquivo alvo sumiu ou mudou de lugar: " + ALVO);
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Evento> eventos(String fonte) {
        Pattern padrao = Pattern.compile("buffers\\.getBuffer\\(|descarregar\\(buffers");
        List<Evento> achados = new ArrayList<>();
        Matcher m = padrao.matcher(fonte);
        while (m.find()) {
            boolean pedido = m.group().startsWith("buffers.getBuffer");
            int linha = (int) fonte.substring(0, m.start()).lines().count() + 1;
            achados.add(new Evento(linha, pedido, m.group()));
        }
        return achados;
    }

    @Test
    @DisplayName("nunca ha dois getBuffer sem um descarregar entre eles")
    void nuncaDoisConsumidoresVivos() {
        List<Evento> eventos = eventos(fonte());
        assertFalse(eventos.isEmpty(),
                "nenhum getBuffer encontrado: o teste deixou de medir o que mede");

        Evento pendente = null;
        for (Evento e : eventos) {
            if (e.pedido()) {
                assertTrue(pendente == null,
                        "DOIS CONSUMIDORES VIVOS AO MESMO TEMPO em " + ALVO + ": o buffer da "
                        + "linha " + (pendente == null ? -1 : pendente.linha()) + " continua na "
                        + "mao quando a linha " + e.linha() + " pede outro. O BufferSource "
                        + "encerra o primeiro por dentro, e o proximo vertice levanta "
                        + "\"Not building!\" -- foi assim que #299 derrubou o cliente. "
                        + "Descarregue antes de pedir o proximo.");
                pendente = e;
            } else {
                pendente = null;
            }
        }
    }

    @Test
    @DisplayName("todo buffer pedido e descarregado antes do fim do metodo")
    void todoBufferEDescarregado() {
        List<Evento> eventos = eventos(fonte());
        int pedidos = (int) eventos.stream().filter(Evento::pedido).count();
        int descargas = eventos.size() - pedidos;
        assertTrue(descargas >= pedidos,
                "sobrou buffer sem descarga em " + ALVO + ": " + pedidos + " pedido(s) e "
                + descargas + " descarga(s). Lote nao descarregado sai com os uniformes do "
                + "passe seguinte, e as camadas de Fresnel ficam identicas -- o defeito e "
                + "visual e silencioso, nao um crash.");
    }
}
