package com.darkcontinent.nenfoundation.enemy.encounter;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

/**
 * A UNICA porta entre o controlador de encontro e o ato de criar entidade.
 *
 * <p>O controlador decide QUANDO spawnar -- e essa e a parte que precisa ser
 * provada contra restart, corrida e abandono. O QUE spawnar muda por criatura e
 * envolve mundo; deixar as duas coisas juntas faria a regra de duplicacao ser
 * reescrita em cada encontro novo, e a decima copia esqueceria de registrar os
 * uuids.</p>
 *
 * <p>Quem implementa DEVE devolver os uuids de tudo que criou. Um spawn que nao
 * volta na lista fica invisivel para o controlador: ao reiniciar o servidor ele
 * conta zero entidades vivas, decide que o episodio nao terminou, e spawna outra
 * leva por cima da primeira. Nao ha erro nisso -- ha dois chefes.</p>
 */
@FunctionalInterface
public interface EncounterSpawner {
    /**
     * @return uuids de TODAS as entidades criadas; vazio significa que o spawn
     *         falhou, e o controlador trata isso como episodio nao iniciado
     */
    List<UUID> spawnar(ServerLevel nivel, EncounterInstance instancia);
}
