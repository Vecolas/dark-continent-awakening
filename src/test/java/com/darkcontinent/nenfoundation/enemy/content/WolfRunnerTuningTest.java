package com.darkcontinent.nenfoundation.enemy.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkcontinent.nenfoundation.Repo;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeFlanco;
import com.darkcontinent.nenfoundation.enemy.ai.RegrasDeRastro;
import com.darkcontinent.nenfoundation.enemy.ai.squad.SquadRules;
import com.darkcontinent.nenfoundation.enemy.combat.AttackHitbox;
import com.darkcontinent.nenfoundation.enemy.combat.RegrasDeSalto;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Portao da ficha do Wolf Runner: o rastro, o flanco, o bote e o focinho.
 *
 * <p>Todos os defeitos que ele segura tem a mesma assinatura -- compilam,
 * spawnam, atacam, dropam loot e passam em todo o resto. O bicho continua sendo
 * um mob; ele so deixa de ser ESTE mob.</p>
 *
 * <p><b>Por que a metade final le o {@code .geo.json}.</b> O gerador em
 * {@code art-source/enemies/wolf_runner/} ja cobra as mesmas medidas, e cobra
 * melhor -- ele conhece a tabela de caixas. Mas ele so roda quando alguem o
 * roda, e o {@code .geo.json} e um arquivo VERSIONADO: quem editar o modelo a
 * mao, ou quem mexer em {@link WolfRunnerTuning} sem reabrir o gerador, nao
 * encontra regua nenhuma. Este teste roda em {@code ./gradlew build}, que e a
 * unica coisa que sempre acontece.</p>
 */
class WolfRunnerTuningTest {

    private static final String GEO =
            "src/main/resources/assets/nenfoundation/geo/entity/wolf_runner.geo.json";
    private static final double PX_POR_BLOCO = 16.0D;

    // ------------------------------------------------- as regras nascem daqui

    @Test
    @DisplayName("as tres regras saem das constantes deste arquivo, e nao de numeros paralelos")
    void asRegrasNascemDasConstantes() {
        RegrasDeRastro rastro = WolfRunnerTuning.rastro();
        assertEquals(WolfRunnerTuning.TICKS_DE_RASTRO, rastro.ticksDeRastro());
        assertEquals(WolfRunnerTuning.MEMORIA_DE_ALVO_TICKS, rastro.ticksDeMemoriaDoAlvo(),
                "o prazo da memoria tem de ser o MESMO que a entidade instala na ThreatMemory:"
                        + " escrito de novo aqui, ele divergiria na primeira vez que alguem"
                        + " ajustasse um lado so, e os dois prazos discordariam em silencio");

        RegrasDeFlanco flanco = WolfRunnerTuning.flanco();
        assertEquals(WolfRunnerTuning.ANGULO_DE_FLANCO_EM_GRAUS, flanco.anguloDeFlancoEmGraus(),
                0.0D);
        assertEquals(WolfRunnerTuning.ALCANCE_DA_MORDIDA, flanco.distanciaDeInvestida(), 0.0D,
                "a distancia em que ele fecha e a MESMA que a Goal da mordida usa: com dois"
                        + " numeros, o flanco autorizaria a investida a uma distancia em que a"
                        + " boca nao alcanca, e o esquadrao erraria sozinho");
        assertEquals(SquadRules.esquadrao(), flanco.bando(),
                "teto, espacamento e moral saem inteiros de SquadRules: redeclarados aqui, o"
                        + " numero deste arquivo venceria em metade dos caminhos e o de la na"
                        + " outra metade");

        RegrasDeSalto salto = WolfRunnerTuning.salto();
        assertEquals(WolfRunnerTuning.WINDUP_DO_BOTE, salto.ticksDeTelegrafo(),
                "o telegrafo do salto e o windup do proprio ataque: numeros separados fariam o"
                        + " aviso durar uma coisa e a regra cobrar outra");
    }

    @Test
    @DisplayName("o dano dos dois golpes sai do atributo publicado, e nao de um literal")
    void oDanoSaiDoAtributo() {
        float atributo = ChimeraProfiles.wolfRunner().attributes().attackDamage();
        assertEquals(atributo, WolfRunnerTuning.mordida().damage(), 0.0F);
        assertEquals(atributo, WolfRunnerTuning.bote().damage(), 0.0F,
                "o bote compra DISTANCIA, e nao dano. Um salto que tambem machucasse mais tornaria"
                        + " a mordida curta irrelevante, e o mob ficaria com um golpe so e um"
                        + " telegrafo so");
    }

    // --------------------------------------------------------- forma do golpe

    @Test
    @DisplayName("o bote avisa por mais tempo que a mordida, e os dois avisam mais do que ferem")
    void aFormaDoTelegrafo() {
        assertTrue(WolfRunnerTuning.WINDUP_DA_MORDIDA > WolfRunnerTuning.JANELA_DA_MORDIDA,
                "aviso mais curto que a janela nao e telegrafo: e um mob que bate sem avisar, com"
                        + " o mesmo dano e o mesmo log limpo");
        assertTrue(WolfRunnerTuning.WINDUP_DO_BOTE > WolfRunnerTuning.JANELA_DO_BOTE);
        assertTrue(WolfRunnerTuning.WINDUP_DO_BOTE > WolfRunnerTuning.WINDUP_DA_MORDIDA,
                "o bote desloca o corpo inteiro e por isso avisa mais: telegrafo e deslocamento"
                        + " andam juntos, e e essa proporcao que impede o salto de virar"
                        + " teleporte");
        assertTrue(WolfRunnerTuning.RECUPERACAO_DO_BOTE >= WolfRunnerTuning.RECUPERACAO_DA_MORDIDA,
                "a aterrissagem e a maior janela de punicao do mob; encurta-la abaixo da mordida"
                        + " faria o golpe caro ser tambem o mais seguro");
        assertTrue(WolfRunnerTuning.RECARGA_APOS_INTERRUPCAO > ChimeraProfiles.wolfRunnerRecarga(),
                "interromper tem de VALER: sem recarga extra, o bicho recomeca no tick seguinte e"
                        + " o jogador aprende a ignorar o cambaleio");
    }

    @Test
    @DisplayName("o bote so e autorizado ALEM do alcance da mordida")
    void oBoteComecaOndeAMordidaTermina() {
        assertTrue(WolfRunnerTuning.ALCANCE_MINIMO_DO_BOTE > WolfRunnerTuning.ALCANCE_DA_MORDIDA,
                "com a faixa do bote comecando dentro do alcance da boca, o bicho escolheria o"
                        + " golpe caro quando o barato ja chega: gastaria "
                        + WolfRunnerTuning.WINDUP_DO_BOTE + " ticks de agachamento para viajar"
                        + " zero blocos, e na tela seria um mob que se prepara por um segundo e"
                        + " morde exatamente onde ja estava");
        assertTrue(WolfRunnerTuning.ALCANCE_MAXIMO_DO_BOTE
                        <= ChimeraProfiles.wolfRunner().attributes().followRange(),
                "autorizar o bote alem do alcance de perseguicao daria um salto contra alguem que"
                        + " o mob nem deveria estar seguindo");
    }

    @Test
    @DisplayName("a distancia de decisao cabe na caixa, medida da BORDA do alvo")
    void aDistanciaDeDecisaoCabeNaCaixa() {
        double limite = WolfRunnerTuning.caixaDaMordida().maxZ()
                + WolfRunnerTuning.MEIA_LARGURA_DE_UM_ALVO;
        assertTrue(WolfRunnerTuning.ALCANCE_DA_MORDIDA <= limite,
                "ele decide morder a " + WolfRunnerTuning.ALCANCE_DA_MORDIDA + " (centro a centro)"
                        + " e a caixa so alcanca " + limite + " ate a borda do alvo. Como a"
                        + " navegacao trava durante o golpe, a mordida no limite da distancia"
                        + " NUNCA acertaria -- um esquadrao que erra sozinho e parece quebrado");
    }

    @Test
    @DisplayName("a caixa da mordida aponta para a FRENTE do mundo, e nao para tras")
    void aCaixaApontaParaAFrente() {
        // A confusao entre as duas convencoes de frente ja custou um bug neste
        // repositorio: na geometria Bedrock a frente e -Z, e na matematica de
        // mundo, com yaw 0, o olhar aponta para +Z. Com o sinal trocado o mob
        // ataca, anima e nao encosta em quem esta na frente -- quem esta pelas
        // costas e que apanha. Fase certa, cooldown certo, log limpo.
        AABB frente = WolfRunnerTuning.caixaDaMordida().noMundo(Vec3.ZERO, 0.0F);
        assertTrue(frente.minZ > 0.0D,
                "com yaw 0 a caixa inteira tem de ficar em +Z e ela comeca em " + frente.minZ);

        AABB atras = WolfRunnerTuning.caixaDaMordida().noMundo(Vec3.ZERO, 180.0F);
        assertTrue(atras.maxZ < 0.0D,
                "girando o mob 180 graus a caixa tem de ir junto; se ela ficasse parada, o golpe"
                        + " acertaria sempre o mesmo lado do mundo");
    }

    // ------------------------------------- o desenho e a regra, do lado de ca

    @Test
    @DisplayName("o focinho desenhado mais o avanco cobrem a caixa da mordida")
    void oFocinhoDesenhadoCobreACaixa() {
        double pontaEmPx = cubo("jaw").getAsJsonArray("origin").get(2).getAsDouble();
        double focinhoEmBlocos = -pontaEmPx / PX_POR_BLOCO;

        assertEquals(WolfRunnerTuning.ALCANCE_DESENHADO_DO_FOCINHO, focinhoEmBlocos, 1.0E-9D,
                "a constante ALCANCE_DESENHADO_DO_FOCINHO e uma copia DECLARADA do modelo. Quando"
                        + " ela e o geo discordam, quem esta certo e o geo -- e o que a"
                        + " divergencia produz nao e erro nenhum, e uma conta de alcance feita"
                        + " sobre um focinho que nao existe mais");

        double alcanceTotal = focinhoEmBlocos + WolfRunnerTuning.AVANCO_DA_INVESTIDA;
        assertTrue(alcanceTotal + 1.0E-9D >= WolfRunnerTuning.caixaDaMordida().maxZ(),
                "o focinho alcanca " + focinhoEmBlocos + " e o avanco paga mais "
                        + WolfRunnerTuning.AVANCO_DA_INVESTIDA + ", somando " + alcanceTotal
                        + "; a caixa reivindica " + WolfRunnerTuning.caixaDaMordida().maxZ()
                        + ". O jogador apanharia de uma boca que, na tela, parou antes dele --"
                        + " dano certo, cooldown certo, log limpo, e a unica leitura que um"
                        + " corpo-a-corpo oferece quebrada");
    }

    @Test
    @DisplayName("o modelo tem os ossos que pagam os traits que o molde pode sortear")
    void oModeloTemOsOssosDosTraits() {
        // ChimeraPeonDefinitions.wolfRunner() sorteia entre SPEED, LEAP, CLAWS e
        // TAIL. A issue #120 proibe skeleton procedural em runtime: um trait sem
        // osso e guardado na identidade, salvo no NBT, contado pela colonia -- e
        // invisivel na tela. O jogador descreve isso como aleatoriedade.
        var molde = ChimeraProfiles.wolfRunnerMolde();
        assertTrue(molde.traitsPossiveis().size() >= 2,
                "gene pool vazio faria esta regua passar sem medir nada");

        for (String osso : new String[] {"tail", "leg_front_left", "leg_front_right",
                "leg_back_left", "leg_back_right", "paw_front_left", "paw_front_right",
                "paw_back_left", "paw_back_right"}) {
            assertTrue(temOsso(osso),
                    "o modelo nao tem o osso '" + osso + "', e o molde pode sortear o trait que"
                            + " ele paga. A formiga nasceria com o trait no save e sem ele na"
                            + " tela.");
        }
    }

    // ------------------------------------------- os casos que DEVEM reprovar

    @Test
    @DisplayName("PORTAO: as reguas mordem -- a ficha invertida e recusada nas tres regras")
    void asReguasMordem() {
        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeRastro(WolfRunnerTuning.MEMORIA_DE_ALVO_TICKS + 1,
                        WolfRunnerTuning.RAIO_DE_CHEGADA_DO_RASTRO,
                        WolfRunnerTuning.MEMORIA_DE_ALVO_TICKS),
                "rastro que sobrevive a memoria da ameaca faz o bicho perseguir alguem de quem ele"
                        + " ja esqueceu");

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeFlanco(SquadRules.esquadrao(),
                        WolfRunnerTuning.MEMBROS_PARA_FLANQUEAR,
                        WolfRunnerTuning.ARCO_FRONTAL_DO_ALVO_EM_GRAUS - 1.0D,
                        WolfRunnerTuning.ARCO_FRONTAL_DO_ALVO_EM_GRAUS,
                        WolfRunnerTuning.ALCANCE_DA_MORDIDA, WolfRunnerTuning.RAIO_DO_CONTORNO),
                "posto dentro do arco frontal poe o bicho num lugar de onde ele nunca recebe"
                        + " permissao para investir: ele gira e nunca ataca");

        assertThrows(IllegalArgumentException.class,
                () -> new RegrasDeSalto(RegrasDeSalto.TELEGRAFO_MINIMO - 1,
                        WolfRunnerTuning.ALCANCE_MINIMO_DO_BOTE,
                        WolfRunnerTuning.ALCANCE_MAXIMO_DO_BOTE,
                        WolfRunnerTuning.DESNIVEL_MAXIMO_DO_BOTE,
                        WolfRunnerTuning.IMPULSO_HORIZONTAL_DO_BOTE,
                        WolfRunnerTuning.IMPULSO_VERTICAL_DO_BOTE),
                "abaixo do telegrafo minimo o salto vira teleporte com dano");

        // E a ficha em uso tem de montar sem reclamar: uma regua que so reprova e
        // tao inutil quanto uma que so aprova.
        assertEquals(WolfRunnerTuning.TICKS_DE_RASTRO, WolfRunnerTuning.rastro().ticksDeRastro());
        assertEquals(WolfRunnerTuning.WINDUP_DO_BOTE, WolfRunnerTuning.salto().ticksDeTelegrafo());
        assertEquals(WolfRunnerTuning.RAIO_DO_CONTORNO, WolfRunnerTuning.flanco().raioDoContorno(),
                0.0D);
    }

    @Test
    @DisplayName("PORTAO: uma caixa de mordida mais longa que o desenho seria recusada")
    void umaCaixaMaisLongaQueODesenhoSeriaRecusada() {
        // O caso que DEVE reprovar, montado a mao: a mesma conta do teste acima,
        // com a caixa esticada meio bloco. Sem este caso, o teste de cima poderia
        // estar comparando duas constantes que ninguem jamais vai violar, e o
        // portao estaria imprimindo verde sem nunca ter mordido.
        AttackHitbox esticada = new AttackHitbox(-0.4D, 0.0D, 0.25D, 0.4D, 0.9D, 1.6D);
        double alcanceDesenhado = WolfRunnerTuning.ALCANCE_DESENHADO_DO_FOCINHO
                + WolfRunnerTuning.AVANCO_DA_INVESTIDA;
        assertTrue(alcanceDesenhado < esticada.maxZ(),
                "a conta tem de reprovar uma caixa de " + esticada.maxZ() + " contra um alcance"
                        + " desenhado de " + alcanceDesenhado + "; se ela aprovasse isto, ela nao"
                        + " estaria medindo nada");
    }

    // ------------------------------------------------------------- utilitario

    private static JsonObject geometria() {
        return JsonParser.parseString(Repo.texto(GEO)).getAsJsonObject()
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    private static JsonObject cubo(String osso) {
        for (JsonElement elemento : geometria().getAsJsonArray("bones")) {
            JsonObject b = elemento.getAsJsonObject();
            if (b.get("name").getAsString().equals(osso) && b.has("cubes")) {
                return b.getAsJsonArray("cubes").get(0).getAsJsonObject();
            }
        }
        throw new AssertionError("o geo do wolf_runner nao tem cubo no osso '" + osso + "'");
    }

    private static boolean temOsso(String nome) {
        for (JsonElement elemento : geometria().getAsJsonArray("bones")) {
            JsonObject b = elemento.getAsJsonObject();
            if (b.get("name").getAsString().equals(nome) && b.has("cubes")) return true;
        }
        return false;
    }
}
