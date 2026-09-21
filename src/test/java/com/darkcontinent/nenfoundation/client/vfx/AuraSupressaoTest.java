package com.darkcontinent.nenfoundation.client.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.api.SinalDeAura;
import com.darkcontinent.nenfoundation.client.vfx.AuraTransitionProfile.Componente;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisibilityResolver.AlvoDeAura;
import com.darkcontinent.nenfoundation.client.vfx.AuraVisibilityResolver.ObservadorDeAura;
import com.darkcontinent.nenfoundation.client.vfx.model.AuraPerfilVisual;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Zetsu: o zero literal, a visibilidade por observador, e o pulso local.
 *
 * <p><b>O QUE ESTE ARQUIVO PROTEGE E UMA AUSENCIA, e ausencia e a coisa mais
 * facil de quebrar sem perceber.</b> Um alpha de 0,01 num componente qualquer
 * nao aparece em revisao de codigo, nao lanca, e desenha um contorno tenue que
 * num servidor com dois clientes entrega justamente quem esta se escondendo. Por
 * isso os testes aqui comparam com ZERO EXATO, e nao com uma tolerancia.
 *
 * <p>O que NAO da para provar aqui esta no gate #201: que o cliente do outro
 * jogador nao RECEBE o dado. Isso e inspecao do delta com log de payload, e nao
 * teste de unidade -- olhar a tela prova que o cliente escondeu, e nunca que ele
 * nao recebeu.
 */
class AuraSupressaoTest {

    private static final String DIRETORIO = "src/main/resources/assets/nenfoundation/nen_vfx";

    private static final ObservadorDeAura A = new ObservadorDeAura(1);
    private static final AlvoDeAura B = new AlvoDeAura(2);

    @BeforeEach
    @AfterEach
    void semEstadoEntreTestes() {
        AuraVisibilityResolver.limpar();
        PulsoDeSupressao.limpar();
    }

    private static AuraPerfilVisual ler(String nome) {
        JsonElement json = JsonParser.parseString(Repo.texto(DIRETORIO + "/" + nome));
        return AuraPerfilVisual.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(erro -> new AssertionError(nome + " nao le: " + erro));
    }

    // ------------------------------------------------- o perfil de Zetsu

    @Test
    @DisplayName("o perfil de zetsu existe no disco, e ele e o zero literal")
    void zetsuExisteEEZero() {
        // O ARQUIVO EXISTE PARA QUE O ALVO DA INTERPOLACAO SEJA UM OBJETO
        // LEGITIMO, e nao um caso especial espalhado pelo renderer. E ele
        // precisa ser zero de verdade: o codigo forca o apagado, mas um arquivo
        // que discorde do codigo e uma armadilha para quem o ler depois.
        AuraPerfilVisual zetsu = ler("zetsu.json");
        assertEquals(zetsu.apagado(), zetsu,
                "o perfil de zetsu no disco tem valor diferente de zero; a referencia D e"
                        + " LITERAL -- o corpo e o corpo");
    }

    @Test
    @DisplayName("um zetsu.json com brilho seria IGNORADO pelo codigo")
    void zetsuTortoNaoAcende() {
        // O CASO QUE DEVE REPROVAR, e ele vem de um resource pack de terceiro
        // pelo mesmo caminho que o nosso arquivo. A trava e estrutural: o perfil
        // apagado assume, venha o que vier do disco.
        AuraPerfilVisual torto = ler("ren.json");
        assertTrue(torto.brilho().existe(), "o perfil de origem precisa brilhar");
        AuraPerfilVisual apagado = torto.apagado();
        assertTrue(!apagado.brilho().existe(),
                "um Zetsu que alimenta o passe de brilho e o vazamento mais delator que"
                        + " existe");
        assertTrue(!apagado.pressao().existe());
        assertEquals(0, apagado.filamentos().quantidade());
    }

    // ------------------------------------------------- a transicao

    @Test
    @DisplayName("em 0,10 s ainda ha shell; ao fim, TUDO vale exatamente 0.0f")
    void asFasesDaDirecaoDeArte() {
        AuraTransitionProfile fechar = AuraTransicao.SUPRIMIR.fases();
        float duracao = fechar.duracaoMs();

        // Em 100 ms a pelicula ainda esta la -- as fases sao FASES, e nao uma
        // rampa unica. Uma rampa fecharia tudo junto e o gesto se perderia.
        assertTrue(fechar.valorDe(Componente.SHELL, 100.0F) > 0.0F,
                "em 0,10 s a shell ja tinha sumido: isso e rampa, e nao fase");
        assertEquals(1.0F, fechar.valorDe(Componente.BORDA, 100.0F), 1.0e-5F,
                "A BORDA E A ULTIMA A SAIR -- e ela que carrega a leitura");

        // Em 450 ms -- depois do fim, qualquer que seja a duracao escolhida --
        // nao sobra NADA. E zero exato, e nao 0.01.
        for (Componente c : Componente.values()) {
            assertEquals(0.0F, fechar.valorDe(c, Math.max(450.0F, duracao + 1.0F)), 0.0F,
                    c + " sobrou depois do fechamento");
        }
    }

    @Test
    @DisplayName("suprimir continua sendo a transicao mais rapida da tabela")
    void supressaoEAMaisRapida() {
        // ZETSU E UMA DECISAO DE SUMIR, e uma supressao mais lenta que o gesto
        // de LIGAR Ten e uma supressao que nao salva ninguem.
        for (AuraTransicao t : AuraTransicao.values()) {
            assertTrue(AuraTransicao.SUPRIMIR.ticks() <= t.ticks(),
                    "SUPRIMIR precisa ser a mais rapida; " + t + " e mais curta");
        }
    }

    // ------------------------------------------------- o resolvedor

    @Test
    @DisplayName("NENHUM resolve 0: Zetsu e quem nunca despertou sao indistinguiveis")
    void nenhumResolveZero() {
        assertEquals(0.0F, AuraVisibilityResolver.visibilidade(A, B, SinalDeAura.NENHUM,
                false, false));
        assertEquals(0.0F, AuraVisibilityResolver.visibilidade(A, B, null, false, false));
    }

    @Test
    @DisplayName("TEN, REN e KEN resolvem 1 enquanto Gyo e In forem falsos")
    void auraVisivelResolveUm() {
        for (SinalDeAura sinal : new SinalDeAura[] {
                SinalDeAura.TEN, SinalDeAura.REN, SinalDeAura.KEN}) {
            assertEquals(1.0F, AuraVisibilityResolver.visibilidade(A, B, sinal, false, false),
                    sinal + " deveria ser visivel");
        }
    }

    @Test
    @DisplayName("In esconde; Gyo encontra")
    void inEGyo() {
        assertEquals(0.0F, AuraVisibilityResolver.visibilidade(A, B, SinalDeAura.REN,
                false, true), "In precisa esconder de quem nao esta com Gyo");
        assertEquals(1.0F, AuraVisibilityResolver.visibilidade(A, B, SinalDeAura.REN,
                true, true), "Gyo precisa encontrar o que In escondeu");
    }

    @Test
    @DisplayName("voce sempre ve a sua propria aura, inclusive escondida dos outros")
    void aPropriaAuraSempreAparece() {
        // IN ESCONDE DE QUEM OLHA, e nao de quem usa. Sem esta regra, o jogador
        // perderia de vista a propria tecnica no instante em que ela
        // funcionasse -- e "a minha aura sumiu quando eu liguei In" e um relato
        // que ninguem liga a um resolvedor de visibilidade.
        ObservadorDeAura eu = new ObservadorDeAura(7);
        AlvoDeAura euMesmo = new AlvoDeAura(7);
        assertEquals(1.0F, AuraVisibilityResolver.visibilidade(eu, euMesmo, SinalDeAura.REN,
                false, true));
    }

    @Test
    @DisplayName("sem o par, a resposta e ZERO -- a falha vai na direcao segura")
    void semParResolveZero() {
        // VER DE MENOS, E NUNCA VER O QUE DEVERIA ESTAR ESCONDIDO.
        assertEquals(0.0F, AuraVisibilityResolver.visibilidade(null, B, SinalDeAura.REN,
                false, false));
        assertEquals(0.0F, AuraVisibilityResolver.visibilidade(A, null, SinalDeAura.REN,
                false, false));
    }

    @Test
    @DisplayName("o modo permissivo ignora In -- e some no logout")
    void modoPermissivo() {
        assertEquals(0.0F, AuraVisibilityResolver.visibilidade(A, B, SinalDeAura.REN,
                false, true));
        AuraVisibilityResolver.permissivo(true);
        assertEquals(1.0F, AuraVisibilityResolver.visibilidade(A, B, SinalDeAura.REN,
                false, true), "o modo permissivo existe para conferir a geometria de volta");
        // MAS ELE NAO RESSUSCITA QUEM CHEGOU COMO NENHUM. Nao ha o que revelar:
        // o servidor nao mandou nada, e este resolvedor nunca e a desculpa para
        // pedir mais.
        assertEquals(0.0F, AuraVisibilityResolver.visibilidade(A, B, SinalDeAura.NENHUM,
                false, false),
                "NEM O MODO PERMISSIVO PODE INVENTAR AURA QUE NAO CHEGOU");
        AuraVisibilityResolver.limpar();
        assertEquals(0.0F, AuraVisibilityResolver.visibilidade(A, B, SinalDeAura.REN,
                false, true), "o modo permissivo sobreviveu ao logout");
    }

    @Test
    @DisplayName("visibilidade zero e AUSENCIA, e nao uma aura fraquissima")
    void visibilidadeZeroDesligaDeVerdade() {
        AuraVisualState escondido = EstadoVisualDeTerceiro.de(SinalDeAura.REN,
                AuraRenderLod.FULL, 0.0F);
        assertTrue(!escondido.enabled(),
                "UM ALPHA DE 0,001 AINDA DESENHA GEOMETRIA, ainda alimenta o alvo de brilho"
                        + " e ainda aparece contra um fundo escuro");
        assertEquals(AuraVisualMode.OFF, escondido.mode());
    }

    @Test
    @DisplayName("a visibilidade multiplica o LOD, em vez de ser um corte a parte")
    void visibilidadeMultiplicaOLod() {
        float cheia = EstadoVisualDeTerceiro.de(SinalDeAura.REN, AuraRenderLod.FULL, 1.0F)
                .intensity();
        float pelaMetade = EstadoVisualDeTerceiro.de(SinalDeAura.REN, AuraRenderLod.FULL, 0.5F)
                .intensity();
        assertEquals(cheia * 0.5F, pelaMetade, 1.0e-5F,
                "com um corte a parte, 'por que essa aura sumiu' teria duas respostas"
                        + " possiveis quando Gyo e In existirem");
    }

    // ------------------------------------------------- o pulso local

    @Test
    @DisplayName("o pulso dura no maximo 500 ms e termina sozinho")
    void pulsoTerminaSozinho() {
        assertTrue(PulsoDeSupressao.DURACAO_EM_TICKS * 50 <= 500,
                "o teto da issue #199 e 500 ms; veio "
                        + (PulsoDeSupressao.DURACAO_EM_TICKS * 50) + " ms");
        PulsoDeSupressao.disparar();
        assertTrue(PulsoDeSupressao.ativo());
        for (int i = 0; i < PulsoDeSupressao.DURACAO_EM_TICKS; i++) {
            PulsoDeSupressao.aoTick();
        }
        assertTrue(!PulsoDeSupressao.ativo(), "o pulso sobreviveu ao proprio prazo");
        assertEquals(0.0F, PulsoDeSupressao.intensidade(), 0.0F);
    }

    @Test
    @DisplayName("ligar e desligar Zetsu vinte vezes nao deixa estado preso")
    void vinteVezesSemAcumular() {
        for (int rodada = 0; rodada < 20; rodada++) {
            PulsoDeSupressao.disparar();
            for (int i = 0; i < PulsoDeSupressao.DURACAO_EM_TICKS; i++) {
                PulsoDeSupressao.aoTick();
            }
            assertEquals(0.0F, PulsoDeSupressao.restante(), 0.0F,
                    "sobrou estado na rodada " + rodada);
        }
    }

    @Test
    @DisplayName("o pulso e discreto: ele nao pode ser lido como dano recebido")
    void pulsoEDiscreto() {
        float pico = 0.0F;
        for (int i = 0; i <= 200; i++) {
            pico = Math.max(pico, PulsoDeSupressao.intensidade(
                    PulsoDeSupressao.DURACAO_EM_TICKS * i / 200.0F));
        }
        assertEquals(PulsoDeSupressao.PICO, pico, 1.0e-5F);
        assertTrue(pico <= 0.35F,
                "UM FLASH FORTE NA PROPRIA TELA AO SUPRIMIR seria lido como dano recebido,"
                        + " e 'algo me acertou' e a ultima coisa que Zetsu deveria dizer");
    }

    @Test
    @DisplayName("o pulso CAI: entrada rapida, saida longa")
    void pulsoDecai() {
        float anterior = PulsoDeSupressao.intensidade(PulsoDeSupressao.DURACAO_EM_TICKS);
        for (int i = 1; i <= 50; i++) {
            float restante = PulsoDeSupressao.DURACAO_EM_TICKS * (1.0F - i / 50.0F);
            float atual = PulsoDeSupressao.intensidade(restante);
            assertTrue(atual <= anterior + 1.0e-6F,
                    "o pulso subiu de novo em " + restante + " ticks: isso e piscar");
            anterior = atual;
        }
        assertEquals(0.0F, anterior, 1.0e-6F);
    }

    @Test
    @DisplayName("valor invalido nao acende o pulso")
    void valorInvalidoNaoAcende() {
        assertEquals(0.0F, PulsoDeSupressao.intensidade(Float.NaN), 0.0F);
        assertEquals(0.0F, PulsoDeSupressao.intensidade(-3.0F), 0.0F);
    }
}
