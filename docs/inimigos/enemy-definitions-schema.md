# Schema de `enemy_definitions`

Este é o contrato da primeira camada data-driven de inimigos. Os arquivos ficam
em `data/<namespace>/enemy_definitions/<nome>.json`. O listener lê todos os
arquivos e só troca o catálogo quando todos são válidos; um erro preserva o
snapshot anterior.

```json
{
  "metadata": {
    "id": "example:field_beast",
    "canon_level": "ORIGINAL_COMPATIBLE",
    "faction": "WILDLIFE",
    "threat_tier": "LOW",
    "territorial": false,
    "social": false,
    "visual_id": "example:field_beast"
  },
  "attributes": {
    "max_health": 10.0,
    "movement_speed": 0.2,
    "attack_damage": 1.0,
    "armor": 0.0,
    "follow_range": 8.0,
    "knockback_resistance": 0.0
  },
  "spawn": {
    "biome_tags": ["#example:field_biomes"],
    "dimensions": ["minecraft:overworld"],
    "min_light": 0,
    "max_light": 15,
    "require_ground": true,
    "allow_water": false,
    "require_sky": false,
    "max_nearby_same_faction": 4
  },
  "audio_id": "example:entity/field_beast",
  "timings": {
    "strike": {
      "windup_ticks": 8,
      "active_ticks": 4,
      "recovery_ticks": 12,
      "interruptible_windup": true,
      "interruptible_active": false,
      "interruptible_recovery": true
    }
  },
  "schema_version": 1
}
```

Enumerações usam os nomes Java atuais e são rejeitadas quando não existem.
`biome_tags` exige `#namespace:path`; `dimensions` e `audio_id` exigem
`namespace:path`. `timings` contém apenas janelas temporais; dano, knockback e
regras de alvo não entram nele para evitar uma segunda fonte de balanceamento.
`schema_version` é aceito como 1; versões futuras são rejeitadas e nunca
aplicadas parcialmente. Os perfis existentes em `HunterExamProfiles` ainda são
legado Java e usam `audio_id` derivado do próprio id até uma migração explícita;
este schema não declara balanceamento final dos 23 encounters.
