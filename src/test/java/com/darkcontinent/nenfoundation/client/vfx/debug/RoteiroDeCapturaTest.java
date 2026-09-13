package com.darkcontinent.nenfoundation.client.vfx.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.client.vfx.AuraVisualMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A maquina do lote de capturas.
 *
 * <p>ELA E A UNICA PARTE DO MODO DE CAPTURA QUE DA PARA PROVAR SEM TELA, e e
 * tambem a que falha em silencio: um lote que fotografa o quadro errado e
 * nomeia certo produz seis imagens que parecem um conjunto valido. Ninguem
 * descobre olhando as imagens -- descobre-se meses depois, comparando com uma
 * referencia e nao entendendo por que nao bate.
 */
class RoteiroDeCapturaTest {

    /** Roda o lote inteiro e devolve a sequencia de acoes, com o passo de cada uma. */
    private static List<String> rodarAteOFim(RoteiroDeCaptura roteiro, int limiteDeQuadros) {
        List<String> trilha = new ArrayList<>();
        for (int i = 0; i < limiteDeQuadros; i++) {
            RoteiroDeCaptura.Resultado r = roteiro.aoQuadro();
            trilha.add(r.acao() + (r.passo() == null ? "" : ":" + r.passo().nome()));
            if (r.acao() == RoteiroDeCaptura.Acao.ENCERRAR) {
                break;
            }
        }
        return trilha;
    }

    @Test
    @DisplayName("cada passo produz exatamente uma captura, e na ordem declarada")
    void umaCapturaPorPasso() {
        RoteiroDeCaptura roteiro = new RoteiroDeCaptura();
        roteiro.iniciar();

        List<String> trilha = rodarAteOFim(roteiro, 500);
        List<String> capturas = trilha.stream()
                .filter(passo -> passo.startsWith("CAPTURAR:"))
                .map(passo -> passo.substring("CAPTURAR:".length()))
                .toList();

        List<String> esperadas = RoteiroDeCaptura.CONJUNTO_MINIMO.stream()
                .map(RoteiroDeCaptura.Passo::nome)
                .toList();
        assertEquals(esperadas, capturas,
                "um passo a mais ou a menos desloca o nome de todas as capturas seguintes");
    }

    @Test
    @DisplayName("aplicar vem antes da captura, e com espera no meio")
    void aplicaEsperaEFotografa() {
        RoteiroDeCaptura roteiro = new RoteiroDeCaptura(
                List.of(new RoteiroDeCaptura.Passo("ten", AuraVisualMode.TEN, -1.0F)));
        roteiro.iniciar();

        assertEquals(RoteiroDeCaptura.Acao.APLICAR, roteiro.aoQuadro().acao());
        for (int i = 1; i < RoteiroDeCaptura.QUADROS_DE_ESPERA; i++) {
            assertEquals(RoteiroDeCaptura.Acao.NADA, roteiro.aoQuadro().acao(),
                    "fotografar antes do estado assentar entrega a imagem do estado ANTERIOR"
                            + " com o nome do novo");
        }
        assertEquals(RoteiroDeCaptura.Acao.CAPTURAR, roteiro.aoQuadro().acao());
        assertEquals(RoteiroDeCaptura.Acao.ENCERRAR, roteiro.aoQuadro().acao());
    }

    @Test
    @DisplayName("o lote acaba, e nao fica devolvendo acao para sempre")
    void encerraEFicaQuieto() {
        RoteiroDeCaptura roteiro = new RoteiroDeCaptura();
        roteiro.iniciar();
        rodarAteOFim(roteiro, 500);

        assertFalse(roteiro.ativo(), "um lote que nao termina trava o tick para sempre");
        assertEquals(RoteiroDeCaptura.Acao.NADA, roteiro.aoQuadro().acao());
    }

    @Test
    @DisplayName("cancelar para no meio e zera o progresso")
    void cancelarPara() {
        RoteiroDeCaptura roteiro = new RoteiroDeCaptura();
        roteiro.iniciar();
        roteiro.aoQuadro();
        roteiro.aoQuadro();
        roteiro.cancelar();

        assertFalse(roteiro.ativo());
        assertEquals(0, roteiro.concluidos());
        assertEquals(RoteiroDeCaptura.Acao.NADA, roteiro.aoQuadro().acao());
    }

    @Test
    @DisplayName("sem iniciar, nada acontece")
    void naoRodaSemIniciar() {
        assertEquals(RoteiroDeCaptura.Acao.NADA, new RoteiroDeCaptura().aoQuadro().acao());
    }

    @Test
    @DisplayName("o conjunto minimo tem a linha de base SEM aura, e ela vem por ultimo")
    void temALinhaDeBase() {
        List<RoteiroDeCaptura.Passo> passos = RoteiroDeCaptura.CONJUNTO_MINIMO;
        RoteiroDeCaptura.Passo ultimo = passos.get(passos.size() - 1);

        assertEquals(AuraVisualMode.OFF, ultimo.modo(),
                "o criterio do ADR-015 e 'reprovado se parece o jogador normal' -- e"
                        + " essa frase so se julga com a foto do jogador normal do lado");
        assertTrue(passos.stream().anyMatch(p -> p.nome().equals("ten_sem_particulas")),
                "a captura que decide o gate #176 e a de Ten SEM particula nenhuma");
    }

    @Test
    @DisplayName("um roteiro vazio e recusado, em vez de aprovar o nada")
    void roteiroVazioERecusado() {
        assertThrows(IllegalArgumentException.class, () -> new RoteiroDeCaptura(List.of()),
                "um lote de zero passos capturaria nada e relataria sucesso");
    }
}
