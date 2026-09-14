package com.darkcontinent.nenfoundation.enemy.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.enemy.chimera.nen.TacticalNenIntent;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da traducao INTENCAO -> POSTURA, e da fronteira que ela nao cruza.
 *
 * <p>O defeito que este arquivo segura e o mais silencioso da ligacao com Nen:
 * uma intencao que o corpo IGNORA. A formiga decide se defender, o controlador
 * registra a decisao, o debug mostra a intencao certa -- e ela continua correndo
 * para cima do jogador como se nao tivesse decidido nada. Nenhum log, nenhuma
 * excecao, e o unico sintoma e um bicho que "nao reage".</p>
 */
class PosturaDeCacaTest {

    // ------------------------------------------------------------ caso normal

    @Test
    @DisplayName("cada intencao tem UMA postura, e as seis estao cobertas")
    void asSeisIntencoesTemPostura() {
        assertEquals(PosturaDeCaca.CACAR, PosturaDeCaca.de(TacticalNenIntent.NENHUMA),
                "NENHUMA e o padrao de quase toda formiga: ela caca normalmente. Tratar a ausencia"
                        + " de Nen como hesitacao apagaria o comportamento de todas as formigas"
                        + " dormentes de uma vez");
        assertEquals(PosturaDeCaca.CACAR, PosturaDeCaca.de(TacticalNenIntent.MANTER_TEN));
        assertEquals(PosturaDeCaca.CACAR, PosturaDeCaca.de(TacticalNenIntent.ELEVAR_REN),
                "Ren e ofensiva: quem elevou a aura para atacar nao para de fechar distancia");
        assertEquals(PosturaDeCaca.GUARDAR, PosturaDeCaca.de(TacticalNenIntent.USAR_GYO),
                "Gyo e procurar: arrancar cegamente enquanto procura gasta o ciclo inteiro para"
                        + " chegar fatigada no lugar errado");
        assertEquals(PosturaDeCaca.GUARDAR, PosturaDeCaca.de(TacticalNenIntent.MANTER_KEN),
                "Ken e defesa cara: quem esta se defendendo nao decidiu que o problema e distancia");
        assertEquals(PosturaDeCaca.ROMPER, PosturaDeCaca.de(TacticalNenIntent.ENTRAR_EM_ZETSU),
                "Zetsu e a decisao de SUMIR. Um bicho que decide sumir e continua correndo na"
                        + " direcao do jogador contradiz a propria decisao na tela");
    }

    @Test
    @DisplayName("os dois predicados sao exclusivos: ninguem fecha e rompe ao mesmo tempo")
    void fecharERomperNaoConvivem() {
        for (PosturaDeCaca postura : PosturaDeCaca.values()) {
            assertFalse(postura.fechaDistancia() && postura.rompeContato(),
                    postura + " fecha distancia E rompe contato: com os dois verdadeiros, o"
                            + " destino do passo dependeria de qual 'if' o codigo consultasse"
                            + " primeiro, e o bicho andaria para os dois lados em ticks"
                            + " alternados");
        }
        assertTrue(PosturaDeCaca.CACAR.fechaDistancia());
        assertTrue(PosturaDeCaca.ROMPER.rompeContato());
        assertFalse(PosturaDeCaca.GUARDAR.fechaDistancia(),
                "GUARDAR nao autoriza gastar o arranque -- e isso e a diferenca inteira entre ela"
                        + " e CACAR");
        assertFalse(PosturaDeCaca.GUARDAR.rompeContato());
    }

    // --------------------------------------------------------------- recusado

    @Test
    @DisplayName("intencao ausente e recusa, e nunca o padrao")
    void intencaoAusenteNaoViraCacar() {
        assertThrows(IllegalArgumentException.class, () -> PosturaDeCaca.de(null),
                "devolver CACAR para uma intencao nula esconderia a ausencia atras de um bicho que"
                        + " persegue normalmente, e a decisao de Nen que faltou nunca apareceria");
    }

    // ------------------------------------------- o caso que DEVE reprovar

    @Test
    @DisplayName("PORTAO: toda intencao do nucleo tem de estar mapeada aqui")
    void nenhumaIntencaoFicaDeForaDoMapa() {
        // O switch de PosturaDeCaca.de e EXAUSTIVO, entao uma intencao nova no
        // nucleo quebra a COMPILACAO deste repositorio antes de quebrar qualquer
        // teste -- que e o portao mais forte que existe para este defeito. Esta
        // varredura cobre o caso que a compilacao nao cobre: alguem trocar o
        // switch exaustivo por um com `default`, e passar a mapear intencoes novas
        // para CACAR em silencio.
        Set<PosturaDeCaca> alcancadas = EnumSet.noneOf(PosturaDeCaca.class);
        for (TacticalNenIntent intencao : TacticalNenIntent.values()) {
            PosturaDeCaca postura = PosturaDeCaca.de(intencao);
            assertNotNull(postura, intencao + " nao tem postura");
            alcancadas.add(postura);
        }
        assertEquals(EnumSet.allOf(PosturaDeCaca.class), alcancadas,
                "Alguma postura nao e alcancada por intencao nenhuma. Alcancadas: " + alcancadas
                        + ". Postura inalcancavel e codigo que parece cobrir um caso e nao cobre --"
                        + " e a proxima pessoa vai confiar nela.");
    }

    @Test
    @DisplayName("PORTAO: intencao que usa Nen nao implica postura diferente da normal")
    void usarNenNaoObrigaAMudarDeCorpo() {
        // Esta e a fronteira do CLAUDE.md escrita como assercao: MANTER_TEN USA
        // Nen e mesmo assim mantem o corpo cacando. Se um dia toda intencao com
        // tecnica passasse a mudar a postura, a postura teria virado um espelho da
        // aura -- e o inimigo estaria lendo Nen para decidir o proprio corpo, que e
        // a segunda autoridade que o projeto proibe.
        assertTrue(TacticalNenIntent.MANTER_TEN.usaNen());
        assertEquals(PosturaDeCaca.de(TacticalNenIntent.NENHUMA),
                PosturaDeCaca.de(TacticalNenIntent.MANTER_TEN),
                "Ten e defesa passiva constante: ela nao muda para onde o bicho anda. Uma postura"
                        + " diferente aqui faria o jogador LER a aura da formiga pelo jeito de"
                        + " andar dela, sem que o nucleo tivesse publicado nada");
    }
}
