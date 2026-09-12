package com.darkcontinent.nenfoundation.nen.technique;

import com.darkcontinent.nenfoundation.nen.profile.PersistentNenData;
import net.minecraft.server.level.ServerPlayer;

/**
 * O que uma tecnica ou habilidade pode PERGUNTAR ao nucleo durante seu ciclo
 * de vida.
 *
 * <p>DECISOES QUE ESTE ARQUIVO CARREGA — as duas regras que sustentam o resto:
 *
 * <p>1. Nada aqui devolve valor JA CALCULADO para o chamador guardar. Uma
 * tecnica pergunta {@code auraDisponivel()} no tick em que vai gastar, nunca no
 * tick em que foi ativada. Multiplicador congelado na ativacao ignora todo
 * buff, debuff e ajuste de configuracao que acontecer depois — e ignora em
 * silencio.
 *
 * <p>2. A tecnica NAO navega pela arvore de objetos para achar o que precisa.
 * Ela nao alcanca o AuraEngine, o registro de tecnicas nem o servidor por
 * cadeia de campos. Tudo o que ela pode saber esta neste contrato, e tudo o que
 * ela quer contar sai por evento. E o que permite mover o engine de lugar sem
 * reescrever cinquenta habilidades.
 *
 * <p>A implementacao concreta chega no M2, junto do Aura Engine. Em M0 este e o
 * contrato que permite as duas frentes escreverem contra a mesma coisa.
 */
public interface NenContext {

    /** O jogador dono do efeito. Sempre server-side: nao existe contexto no cliente. */
    ServerPlayer jogador();

    /** O progresso persistente, somente leitura. */
    PersistentNenData perfil();

    /**
     * Aura disponivel NESTE instante.
     *
     * <p>Chame na hora de gastar. Guardar o retorno num campo e o erro que
     * transforma "gastei o que eu tinha" em "gastei o que eu tinha ha trinta
     * segundos".
     */
    double auraDisponivel();

    /**
     * Tenta gastar aura.
     *
     * @return {@code true} se o gasto ocorreu por inteiro. Gasto parcial nunca
     *     acontece: ou a tecnica paga o preco, ou ela nao age. Debito parcial e
     *     a origem classica de aura consumida sem efeito nenhum.
     */
    boolean gastarAura(double quantidade);

    /** Tick atual do servidor. Nao use o do cliente para nada temporal. */
    long tickDoServidor();

    /** Se a tecnica indicada esta ativa agora para este jogador. */
    boolean tecnicaAtiva(net.minecraft.resources.ResourceLocation tecnica);

    /**
     * A tecnica pede o proprio desligamento.
     *
     * <p>EXISTE PORQUE {@link StopReason#TARGET_LOST} ja existia e ninguem
     * podia usa-lo: o enum previa "a tecnica perdeu o que sustentava", e nao
     * havia caminho para a tecnica dizer isso. Shu foi a primeira a precisar --
     * ela cobre o item da mao, e a mao esvazia.
     *
     * <p>CHAMAR DE DENTRO DO {@code serverTick} E SEGURO: o servico itera uma
     * COPIA do conjunto de ativas justamente porque uma tecnica pode desligar a
     * si mesma ou outra, e isso e legitimo.
     *
     * <p>Desligar o que ja esta desligado nao e erro, e desligar tecnica de
     * outro jogador nao e possivel: o contexto e de um jogador so.
     */
    void desligar(net.minecraft.resources.ResourceLocation tecnica, StopReason motivo);
}
