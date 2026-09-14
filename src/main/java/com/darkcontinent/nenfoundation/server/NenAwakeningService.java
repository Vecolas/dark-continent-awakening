package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.api.event.NenDespertadoEvent;
import com.darkcontinent.nenfoundation.api.event.NenDespertandoEvent;
import com.darkcontinent.nenfoundation.api.event.OrigemDoDespertar;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import com.darkcontinent.nenfoundation.nen.progression.Marcos;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Gyo;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Ko;
import com.darkcontinent.nenfoundation.nen.technique.Shu;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A porta unica do despertar de Nen.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA:
 *
 * <p>1. IDEMPOTENTE. Despertar quem ja despertou nao e erro, nao regrava o
 * perfil e nao dispara o evento informativo de novo. Quest que completa duas
 * vezes, comando repetido e clique duplo existem; o desenho que trata isso
 * como caso excepcional e o desenho que dispara o onboarding duas vezes.
 *
 * <p>2. NAO HA ESTADO PELA METADE. Se o evento cancelavel for cancelado, nada
 * acontece: nem perfil, nem marco, nem evento informativo. Gravar primeiro e
 * perguntar depois produziria um jogador desperto que "nao devia ter
 * despertado", e desfazer isso e outra operacao que ninguem escreveu.
 *
 * <p>3. A ORDEM E CONTRATO: perguntar, gravar, anunciar. O
 * {@link NenDespertadoEvent} so dispara com o perfil ja persistido -- quem o
 * escuta pode ler o perfil e ver o estado novo, e nao o antigo.
 *
 * <p>4. O SYNC VEM DE GRACA. {@link NenProfileService#atualizar} publica o
 * snapshot quando a mudanca e efetiva. Reenviar aqui tambem criaria um segundo
 * caminho para a mesma verdade, e dois snapshots por despertar.
 *
 * <p>5. Cancelamento e registrado em modo dev. Cancelar em silencio produz um
 * jogador que faz tudo certo e nao desperta, sem nenhuma linha no log -- e a
 * investigacao comeca no lugar errado.
 */
public final class NenAwakeningService {

    private static final Logger LOG = LoggerFactory.getLogger(NenAwakeningService.class);

    private NenAwakeningService() {
    }

    /** O que aconteceu na tentativa de despertar. */
    public enum Resultado {
        /** Despertou agora. Perfil gravado, marco posto, evento anunciado. */
        DESPERTOU,

        /** Ja estava desperto. Nada mudou, e isso nao e erro. */
        JA_ESTAVA,

        /** Um listener cancelou o {@link NenDespertandoEvent}. */
        CANCELADO
    }

    /**
     * Desperta o jogador, se ele ainda nao despertou e ninguem impedir.
     *
     * @param jogador quem desperta; sempre server-side
     * @param origem  COMO o despertar foi provocado. Ver
     *                {@link OrigemDoDespertar} -- o cânone separa treino de
     *                despertar forcado, e a API nao deve perder essa distincao
     */
    public static Resultado despertar(ServerPlayer jogador, OrigemDoDespertar origem) {
        Objects.requireNonNull(jogador, "jogador");
        Objects.requireNonNull(origem, "origem");

        // SAIR CEDO AQUI FOI UM BUG, e ele durou ate alguem abrir a roda.
        //
        // A versao anterior devolvia JA_ESTAVA e ia embora. Quando o despertar
        // passou a liberar Ten, todo perfil que ja tinha despertado ANTES
        // dessa mudanca ficou desperto e sem tecnica nenhuma -- para sempre.
        // A roda nascia vazia, /nen awaken respondia "ja estava", e nada em
        // lugar nenhum dizia que faltava algo.
        //
        // E o mesmo defeito que NenCategoryService.revelar ja tinha: quem sai
        // cedo nao conserta o estado pela metade. So o BOOLEANO decide se
        // anuncia; o conserto acontece de qualquer jeito, e em silencio.
        boolean jaEstava = NenProfileService.ler(jogador).awakened();

        if (!jaEstava) {
            NenDespertandoEvent pergunta = new NenDespertandoEvent(jogador, origem);
            if (NeoForge.EVENT_BUS.post(pergunta).isCanceled()) {
                if (NenConfig.devModeAtivo()) {
                    LOG.info("Despertar de {} cancelado por um listener (origem {}).",
                            jogador.getGameProfile().getName(), origem);
                }
                return Resultado.CANCELADO;
            }
        }

        // Grava so depois de ninguem ter impedido. Para quem ja estava
        // desperto, isto e o CONSERTO -- e nao ha o que cancelar num fato que
        // ja aconteceu. O servico de perfil publica o snapshot sozinho quando a
        // mudanca e efetiva, e nao publica quando nao ha mudanca.
        NenProfileService.atualizar(jogador, NenAwakeningService::comDespertar);

        if (jaEstava) {
            return Resultado.JA_ESTAVA;
        }
        NeoForge.EVENT_BUS.post(new NenDespertadoEvent(jogador, origem));
        return Resultado.DESPERTOU;
    }

    /**
     * Liga {@code awakened} e poe o marco, sem tocar em mais nada.
     *
     * <p>Separado e {@code static} para poder ser exercitado sem um servidor
     * de pe: logica que so roda com o jogo aberto nao e exercitada.
     *
     * <p>Note o que ele NAO faz: nao atribui categoria. Despertar e saber a
     * propria categoria sao dois fatos diferentes sobre o jogador, e o schema
     * v1 ja os separa. A atribuicao tem issue e servico proprios.
     */
    static PersistentNenData comDespertar(PersistentNenData antes) {
        if (antes.awakened() && antes.temMarco(Marcos.DESPERTOU)
                && antes.conheceTecnica(Ten.ID) && antes.conheceTecnica(Ren.ID)
                && antes.conheceTecnica(Zetsu.ID)
                && antes.conheceTecnica(Gyo.ID)
                && antes.conheceTecnica(Shu.ID)
                && antes.conheceTecnica(Ken.ID)
                && antes.conheceTecnica(Ko.ID)) {
            return antes;
        }
        Set<ResourceLocation> marcos = new LinkedHashSet<>(antes.progressionFlags());
        marcos.add(Marcos.DESPERTOU);

        // DESPERTAR LIBERA TEN, e nao mais nada.
        //
        // No cânone, Ten e a primeira coisa que se aprende: e o estado que
        // segura a aura, e a base das outras tecnicas. Sem isto, um jogador
        // desperta e nao tem tecnica nenhuma para usar -- a roda nasce vazia,
        // e a unica forma de ter Ten seria um comando de operador.
        //
        // ISTO NAO E PROGRESSAO. Progressao e o M6, com requisito e treino.
        // Aqui e o minimo para que despertar signifique alguma coisa, e esta
        // declarado como decisao em vez de aparecer como efeito colateral.
        Set<ResourceLocation> tecnicas = new LinkedHashSet<>(antes.unlockedTechniques());
        tecnicas.add(Ten.ID);
        tecnicas.add(Ren.ID);
        tecnicas.add(Zetsu.ID);
        tecnicas.add(Gyo.ID);
        // SHU E INTERMEDIARIA NO CANONE -- ela vem depois do dominio dos quatro
        // principios. Entra no despertar mesmo assim porque NAO HA PROGRESSAO
        // (M6): sem ela, Shu so existiria por comando de operador e ninguem a
        // veria em jogo. Divida declarada, e nao descuido.
        tecnicas.add(Shu.ID);
        // KEN E AVANCADA no canone -- vem depois de Gyo, In e Shu. Mesmo
        // motivo de Shu: sem progressao (M6), so existiria por comando de
        // operador. Divida declarada.
        tecnicas.add(Ken.ID);
        tecnicas.add(Ko.ID);

        return new PersistentNenData(
                antes.schemaVersion(),
                true,
                antes.category(),
                antes.categoryRevealed(),
                antes.auraPotential(),
                antes.control(),
                antes.output(),
                antes.techniqueProficiency(),
                tecnicas,
                antes.unlockedAbilities(),
                marcos);
    }
}
