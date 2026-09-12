package com.darkcontinent.nenfoundation.server;

import com.darkcontinent.nenfoundation.NenFoundation;
import com.darkcontinent.nenfoundation.network.handler.PedidosC2S;
import com.darkcontinent.nenfoundation.config.ConferenciaDeBalanceamento;
import com.darkcontinent.nenfoundation.config.NenConfig;
import com.darkcontinent.nenfoundation.nen.technique.RegistroDeTecnicas;
import com.darkcontinent.nenfoundation.nen.technique.Ren;
import com.darkcontinent.nenfoundation.nen.technique.Ten;
import com.darkcontinent.nenfoundation.nen.technique.Gyo;
import com.darkcontinent.nenfoundation.nen.technique.Ken;
import com.darkcontinent.nenfoundation.nen.technique.Shu;
import com.darkcontinent.nenfoundation.nen.technique.Zetsu;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Limpa estado estatico quando uma instancia de servidor termina. */
@EventBusSubscriber(modid = NenFoundation.MOD_ID)
public final class NenServerLifecycle {
    private static NenTickScheduler.Registro registroDeAura;
    private static NenTickScheduler.Registro registroDePresenca;
    private static NenTickScheduler.Registro registroDeTecnicas;

    private NenServerLifecycle() {
    }

    @SubscribeEvent
    public static void aoIniciarServidor(ServerAboutToStartEvent evento) {
        // AS TECNICAS SAO SELADAS AQUI, uma vez por servidor. O selamento
        // confere a simetria das exclusoes e RECUSA um registro torto -- e
        // recusar na subida e o ponto: uma exclusao pela metade descoberta em
        // jogo ja e uma combinacao ilegal que alguem usou.
        // OS NUMEROS QUE O JOGO CARREGOU, e nao os padroes do codigo. O portao
        // do item 6 do ADR-010 le o codigo-fonte por regex; ele nunca viu o
        // toml que este servidor abriu, e os dois divergem sozinhos -- o
        // NeoForge nao sobrescreve valor existente quando o padrao muda.
        ConferenciaDeBalanceamento.conferirConfigCarregada();

        NenTechniqueService.instalarTetoDeRepouso(NenConfig::tetoDeOutputEmRepouso);
        NenTechniqueService.instalar(RegistroDeTecnicas.selar(List.of(
                new Ten(NenConfig::tenCustoPorSegundo,
                        NenConfig::tenMultiplicadorDeRegeneracao,
                        NenConfig::tenProtecaoBase),
                new Ren(NenConfig::renCustoPorSegundo,
                        NenConfig::renTetoDeOutput),
                new Zetsu(NenConfig::zetsuCustoPorSegundo,
                        NenConfig::zetsuMultiplicadorDeRegeneracao,
                        NenConfig::zetsuTetoDeOutput),
                // A REGIAO NAO ENTRA NO CONSTRUTOR: ela e escolha de CADA
                // jogador e chega como argumento no recalculo. A implementacao
                // e compartilhada por todo o servidor.
                new Gyo(NenConfig::gyoCustoPorSegundo,
                        NenConfig::gyoFracaoConcentrada),
                new Shu(NenConfig::shuCustoPorSegundo,
                        NenConfig::shuFracaoConcentrada),
                new Ken(NenConfig::kenCustoPorSegundo,
                        NenConfig::kenTetoDeOutput,
                        NenConfig::kenProtecaoBase))));

        if (registroDeAura == null) registroDeAura = NenTickScheduler.registrar(NenAuraService::tick);
        // AURA PRIMEIRO, TECNICA DEPOIS, e a ordem importa: a tecnica gasta a
        // aura que o motor acabou de regenerar neste mesmo tick. Invertida, a
        // tecnica decidiria com o saldo do tick anterior -- e o erro apareceria
        // so como "as vezes Ren desliga uma fracao de segundo antes".
        if (registroDeTecnicas == null) {
            registroDeTecnicas = NenTickScheduler.registrar(NenTechniqueService::tick);
        }
        // PRESENCA POR ULTIMO, e no MESMO laco. Ela anuncia o resultado do
        // tick, entao precisa rodar depois de aura e tecnica -- anunciar antes
        // contaria o estado do tick anterior.
        //
        // E MORA AQUI, e nao espalhada pelos pontos onde tecnica muda. Sao
        // muitos: ativar, desligar, conflito, queda por falta de aura, morte,
        // logout, dimensao. Marcar "sujo" em cada um deles e o erro numero 3 da
        // lista do CLAUDE.md -- um vai faltar, e o sinal fica velho sem nada
        // acusar. Um unico ponto que RECALCULA nao tem como esquecer nenhum.
        if (registroDePresenca == null) {
            registroDePresenca = NenTickScheduler.registrar(
                    (jogador, estado) -> NenPresencaService.anunciarSeMudou(jogador));
        }
    }

    @SubscribeEvent
    public static void aoEncerrarServidor(ServerStoppedEvent evento) {
        if (registroDeTecnicas != null) {
            registroDeTecnicas.close();
            registroDeTecnicas = null;
        }
        if (registroDeAura != null) {
            registroDeAura.close();
            registroDeAura = null;
        }
        if (registroDePresenca != null) {
            registroDePresenca.close();
            registroDePresenca = null;
        }
        NenPresencaService.limpar();
        NenGyoService.limparTudo();
        NenRuntimeService.encerrarTodasAsSessoes();
        PedidosC2S.limpar();
        NenSyncService.limparMetricas();
    }
}
