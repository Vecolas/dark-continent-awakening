package com.darkcontinent.nenfoundation.enemy.combat;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Estado server-side de um ataque: janela, instância, atingidos e cooldown.
 *
 * <p>A classe não recebe entidade nem região declarada pelo cliente. O chamador
 * deve fornecer os ids e as caixas que o servidor mediu; a classe apenas cobra
 * a janela ACTIVE, transforma a hitbox local e impede double hit por instância.</p>
 */
public final class AttackController {
    private final AttackTimeline timeline = new AttackTimeline();
    private final int cooldownTicks;
    private final Set<Integer> atingidos = new HashSet<>();
    private long proximaInstancia = 1L;
    private long instanciaAtual;
    private int cooldownRestante;

    public AttackController(int cooldownTicks) {
        if (cooldownTicks < 0) throw new IllegalArgumentException("cooldown invalido");
        this.cooldownTicks = cooldownTicks;
    }

    public AttackPhase phase() { return timeline.phase(); }
    public int remainingTicks() { return timeline.remainingTicks(); }
    public int cooldownRemaining() { return cooldownRestante; }
    public long attackInstanceId() { return instanciaAtual; }

    public boolean canStart() {
        return cooldownRestante == 0
                && (timeline.phase() == AttackPhase.IDLE || timeline.phase() == AttackPhase.COMPLETE);
    }

    public long start(AttackDefinition definition) {
        if (!canStart()) throw new IllegalStateException("ataque ainda nao pode iniciar");
        timeline.start(definition);
        danoDaInstancia = definition.damage();
        instanciaAtual = proximaInstancia++;
        atingidos.clear();
        return instanciaAtual;
    }

    /** Avança um tick; o cooldown só começa depois da janela RECOVERY. */
    public void tick() {
        if (cooldownRestante > 0) cooldownRestante--;
        AttackPhase antes = timeline.phase();
        timeline.tick();
        if (antes != AttackPhase.COMPLETE && timeline.phase() == AttackPhase.COMPLETE) {
            cooldownRestante = cooldownTicks;
        }
    }

    public Optional<AttackHit> tryHit(int targetId, AABB alvo, AttackHitbox hitbox,
            Vec3 origem, float yawGraus, String region) {
        if (targetId < 0 || alvo == null || region == null || region.isBlank()) {
            throw new IllegalArgumentException("alvo de ataque invalido");
        }
        if (timeline.phase() != AttackPhase.ACTIVE || atingidos.contains(targetId)) {
            return Optional.empty();
        }
        if (!hitbox.noMundo(origem, yawGraus).intersects(alvo)) return Optional.empty();
        atingidos.add(targetId);
        return Optional.of(new AttackHit(targetId, instanciaAtual, region,
                timelineDefinitionDamage()));
    }

    private float timelineDefinitionDamage() {
        // AttackTimeline expõe a janela, não sua definição. O controller recebe
        // o dano no início da instância para não consultar um valor mutável no
        // meio do golpe; a cópia é específica da instância, não um multiplicador derivado.
        return danoDaInstancia;
    }

    private float danoDaInstancia;

}
