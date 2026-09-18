package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilDePressao;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraShellPass;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O acabamento de Ren: perfil interpolado, detritos e impulso de camera.
 *
 * <p><b>A TROCA SECA DE PRESET E O DEFEITO CENTRAL AQUI.</b> Ate o AV3, quem
 * desenhava buscava o perfil por {@code estado.mode()} -- e o modo so vira no
 * ULTIMO tick da transicao. O resultado: a intensidade subia suave enquanto o
 * material saltava de Ten para Ren num quadro. Isso nao lanca nada, nao aparece
 * em revisao de codigo, e a pessoa relata como "bug de render". O unico jeito de
 * fechar esse buraco e um teste que ande a transicao passo a passo e exija
 * continuidade.
 */
class AuraRenAcabamentoTest {

    private static final String DIRETORIO = "src/main/resources/assets/nenfoundation/nen_vfx";

    private static AuraPerfilVisual ler(String nome) {
        JsonElement json = JsonParser.parseString(Repo.texto(DIRETORIO + "/" + nome));
        return AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(erro -> new AssertionError(nome + " nao le: " + erro));
    }

    // ------------------------------------------------- perfil interpolado

    @Test
    @DisplayName("a interpolacao chega EXATAMENTE nas duas pontas")
    void interpolacaoFechaNasPontas() {
        AuraPerfilVisual ten = ler("ten.json");
        AuraPerfilVisual ren = ler("ren.json");
        assertEquals(ten, AuraPerfilVisual.interpolar(ten, ren, 0.0F));
        assertEquals(ren, AuraPerfilVisual.interpolar(ten, ren, 1.0F),
                "UM PERFIL QUE NAO FECHA deixa Ren permanentemente um pouco fora do"
                        + " que o arquivo diz -- e ninguem liga isso a uma interpolacao");
    }

    @Test
    @DisplayName("nenhum passo da interpolacao salta: nao ha troca seca de preset")
    void semTrocaSecaDePreset() {
        AuraPerfilVisual ten = ler("ten.json");
        AuraPerfilVisual ren = ler("ren.json");

        // O SALTO MAXIMO ADMITIDO E O DE UM PASSO DA TRANSICAO REAL. Dezoito
        // ticks de ELEVAR, amostrados com folga: se em algum ponto o alpha der
        // um pulo maior que o de um passo linear, e porque houve troca de preset
        // em vez de interpolacao.
        int passos = 180;
        float saltoMaximo = Math.abs(ren.alphaBorda() - ten.alphaBorda()) / passos * 3.0F;
        float anterior = ten.alphaBorda();
        for (int i = 1; i <= passos; i++) {
            float atual = AuraPerfilVisual
                    .interpolar(ten, ren, i / (float) passos).alphaBorda();
            assertTrue(Math.abs(atual - anterior) <= saltoMaximo,
                    "o alpha da borda saltou de " + anterior + " para " + atual
                            + " no passo " + i + ": isso e troca de preset, e nao"
                            + " interpolacao");
            anterior = atual;
        }
    }

    @Test
    @DisplayName("a ordem do Fresnel sobrevive a interpolacao, em todo o caminho")
    void fresnelContinuaDiminuindoParaFora() {
        AuraPerfilVisual ten = ler("ten.json");
        AuraPerfilVisual ren = ler("ren.json");
        // COM OS TRES EXPOENTES IGUAIS as camadas viram uma so mais opaca, e a
        // profundidade que justifica os tres passes desaparece. A propriedade
        // vale por construcao hoje -- interpolacao linear com o mesmo t preserva
        // o sinal da diferenca --, mas deixa de valer no dia em que alguem curvar
        // um dos tres sozinho.
        for (int i = 0; i <= 100; i++) {
            AuraPerfilVisual p = AuraPerfilVisual.interpolar(ten, ren, i / 100.0F);
            assertTrue(p.fresnelInterno() > p.fresnelBorda()
                            && p.fresnelBorda() > p.fresnelExterno(),
                    "o Fresnel chapou no passo " + i + ": " + p.fresnelInterno() + ", "
                            + p.fresnelBorda() + ", " + p.fresnelExterno());
        }
    }

    @Test
    @DisplayName("a borda continua sendo a camada mais forte em todo o caminho")
    void bordaCarregaALeituraSempre() {
        AuraPerfilVisual ten = ler("ten.json");
        AuraPerfilVisual ren = ler("ren.json");
        for (int i = 0; i <= 100; i++) {
            AuraPerfilVisual p = AuraPerfilVisual.interpolar(ten, ren, i / 100.0F);
            assertTrue(p.alphaDe(AuraShellPass.BORDA) > p.alphaDe(AuraShellPass.INTERNA),
                    "no passo " + i + " o filme interno passou a borda");
            assertTrue(p.alphaDe(AuraShellPass.BORDA) > p.alphaDe(AuraShellPass.EXTERNA),
                    "no passo " + i + " o halo passou a borda");
        }
    }

    @Test
    @DisplayName("o perfil apagado zera TAMBEM a pressao e o bloom")
    void apagadoZeraTudo() {
        AuraPerfilVisual apagado = ler("ren.json").apagado();
        assertEquals(0.0F, apagado.bloom(),
                "UM HALO RESIDUAL DE QUEM ESTA SUPRIMIDO entrega justamente quem esta"
                        + " se escondendo -- e o passe de brilho e o mais delator que existe");
        assertEquals(0.0F, apagado.amplitudeDePulso());
        assertEquals(AuraPerfilDePressao.NENHUMA, apagado.pressao(),
                "Zetsu nao deixa anel nem coluna no chao");
        assertEquals(0, apagado.filamentos().quantidade());
        for (AuraShellPass passe : AuraShellPass.values()) {
            assertEquals(0.0F, apagado.alphaDe(passe), passe.toString());
        }
    }

    // ------------------------------------------------- detritos

    private static AuraVisualState emRen(float intensidade, float pressao) {
        return new AuraVisualState(AuraVisualMode.REN, AuraVisualMode.REN, intensidade, 1.0F,
                AuraDistribution.uniforme(), 0xFFFFFFFF, 0xFFFFFFFF,
                new AuraTransitionSample(1.0F, 1.0F, 1.0F, 1.0F, pressao, 0.0F));
    }

    @Test
    @DisplayName("o teto de doze detritos vale, mesmo com densidade absurda")
    void tetoDeDetritosSegura() {
        AuraPerfilVisual ren = ler("ren.json");
        // DENSIDADE ABSURDA E O CASO QUE DEVE REPROVAR: o teto existe para que um
        // numero errado na config nao trave o cliente, e nao como botao de
        // ajuste. Quem quer menos detrito mexe na densidade; quem quer nenhum
        // desliga a chave.
        int emitidos = 0;
        int vivos = 0;
        for (int tick = 0; tick < 200; tick++) {
            int quantos = EmissorDeParticulasDeAura
                    .quantosDetritos(ren, emRen(1.0F, 1.0F), 1000.0D, vivos);
            vivos += quantos;
            emitidos += quantos;
        }
        assertTrue(vivos <= AuraPerfilDePressao.TETO_DE_DETRITOS,
                "a populacao passou do teto: " + vivos);
        assertTrue(emitidos > 0, "com Ren cheio precisa nascer detrito");
    }

    @Test
    @DisplayName("no maximo dois detritos por tick: a populacao enche, e nao estoura")
    void doisPorTickNoMaximo() {
        AuraPerfilVisual ren = ler("ren.json");
        // ENCHER A POPULACAO DE UMA VEZ produziria um "puff" na ativacao e nada
        // depois -- e o fragmento levantado pela pressao precisa PARECER
        // continuo, nao pontual.
        assertTrue(EmissorDeParticulasDeAura
                .quantosDetritos(ren, emRen(1.0F, 1.0F), 1.0D, 0) <= 2);
    }

    @Test
    @DisplayName("sem pressao nao ha detrito: Ten nao levanta fragmento")
    void tenNaoLevantaDetrito() {
        AuraPerfilVisual ten = ler("ten.json");
        assertEquals(0, EmissorDeParticulasDeAura
                .quantosDetritos(ten, emRen(1.0F, 1.0F), 1.0D, 0),
                "o perfil de Ten pede zero detrito, e o pedido vem do DADO");

        AuraPerfilVisual ren = ler("ren.json");
        assertEquals(0, EmissorDeParticulasDeAura
                .quantosDetritos(ren, emRen(1.0F, 0.0F), 1.0D, 0),
                "com a fase de pressao ainda em zero, nada nasce -- e isso e o que faz"
                        + " o detrito entrar na janela certa da transicao");
    }

    @Test
    @DisplayName("densidade zero desliga o fragmento por inteiro")
    void densidadeZeroDesliga() {
        AuraPerfilVisual ren = ler("ren.json");
        assertEquals(0, EmissorDeParticulasDeAura
                .quantosDetritos(ren, emRen(1.0F, 1.0F), 0.0D, 0));
    }

    @Test
    @DisplayName("a populacao de detritos acompanha a intensidade, que vem do OUTPUT")
    void populacaoSegueAIntensidade() {
        AuraPerfilVisual ren = ler("ren.json");
        int alvoForte = encher(ren, 1.0F);
        int alvoFraco = encher(ren, 0.25F);
        assertTrue(alvoFraco < alvoForte,
                "REN FRACO PRECISA LEVANTAR MENOS. Aura e o que esta sendo liberado:"
                        + " ligar isto a reserva faria o efeito APAGAR justamente enquanto"
                        + " o jogador gasta. Forte=" + alvoForte + " fraco=" + alvoFraco);
    }

    private static int encher(AuraPerfilVisual perfil, float intensidade) {
        int vivos = 0;
        for (int tick = 0; tick < 100; tick++) {
            vivos += EmissorDeParticulasDeAura
                    .quantosDetritos(perfil, emRen(intensidade, 1.0F), 1.0D, vivos);
        }
        return vivos;
    }

    // ------------------------------------------------- impulso de camera

    @Test
    @DisplayName("o impulso comeca em zero, chega ao pico e VOLTA a zero")
    void impulsoComecaETerminaEmZero() {
        assertEquals(0.0F, ImpulsoDeCamera.deslocamentoEm(ImpulsoDeCamera.DURACAO_EM_TICKS, 0.0F),
                1.0e-6F, "no primeiro quadro a camera ainda nao se moveu");
        assertEquals(0.0F, ImpulsoDeCamera.deslocamentoEm(0.0F, 0.0F), 0.0F,
                "UM IMPULSO QUE NAO FECHA deixa a mira deslocada para sempre");
        assertEquals(0.0F, ImpulsoDeCamera.deslocamentoEm(-3.0F, 0.0F), 0.0F);
    }

    @Test
    @DisplayName("o pico fica entre 0,1 e 0,25 grau, como a direcao de arte pede")
    void picoDentroDaFaixa() {
        float pico = 0.0F;
        for (int i = 0; i <= 600; i++) {
            float restante = ImpulsoDeCamera.DURACAO_EM_TICKS * (1.0F - i / 600.0F);
            pico = Math.max(pico, ImpulsoDeCamera.deslocamentoEm(restante, 0.0F));
        }
        assertTrue(pico >= 0.1F && pico <= 0.25F,
                "UM IMPULSO DE DOIS GRAUS NAO E 'MAIS IMPACTO' -- e uma arma"
                        + " disparando. Veio " + pico + " grau");
        assertEquals(ImpulsoDeCamera.GRAUS, pico, 1.0e-4F,
                "o pico precisa ser o NUMERO do codigo, e nao o que uma formula produzir");
    }

    @Test
    @DisplayName("o impulso nao oscila: um empurrao, e nao tremor")
    void impulsoNaoOscila() {
        // MAIS DE UMA IDA E VOLTA EM 300 MS E TREMOR, e tremor e exatamente o que
        // o criterio de aceite reprova: ativar Ren vinte vezes seguidas sem que
        // incomode.
        int inversoes = 0;
        int sentido = 0;
        float anterior = ImpulsoDeCamera.deslocamentoEm(ImpulsoDeCamera.DURACAO_EM_TICKS, 0.0F);
        for (int i = 1; i <= 240; i++) {
            float restante = ImpulsoDeCamera.DURACAO_EM_TICKS * (1.0F - i / 240.0F);
            float atual = ImpulsoDeCamera.deslocamentoEm(restante, 0.0F);
            if (Math.abs(atual - anterior) > 1.0e-7F) {
                int novo = atual > anterior ? 1 : -1;
                if (sentido != 0 && novo != sentido) {
                    inversoes++;
                }
                sentido = novo;
            }
            anterior = atual;
        }
        assertTrue(inversoes <= 1, "o impulso inverteu o sentido " + inversoes + " vezes");
    }

    @Test
    @DisplayName("o impulso morre sozinho, e ligar e desligar vinte vezes nao acumula")
    void impulsoNaoAcumula() {
        ImpulsoDeCamera.limpar();
        for (int i = 0; i < 20; i++) {
            ImpulsoDeCamera.disparar();
            for (int t = 0; t < ImpulsoDeCamera.DURACAO_EM_TICKS; t++) {
                ImpulsoDeCamera.aoTick();
            }
            assertTrue(!ImpulsoDeCamera.ativo(),
                    "o impulso sobreviveu ao proprio prazo na rodada " + i);
        }
        ImpulsoDeCamera.limpar();
    }

    @Test
    @DisplayName("quem liga, desliga: limpar mata um impulso em curso")
    void limparMataOImpulso() {
        ImpulsoDeCamera.disparar();
        assertTrue(ImpulsoDeCamera.ativo());
        ImpulsoDeCamera.limpar();
        assertTrue(!ImpulsoDeCamera.ativo(),
                "o impulso do mundo anterior nao pode sobreviver ao logout");
    }
}
