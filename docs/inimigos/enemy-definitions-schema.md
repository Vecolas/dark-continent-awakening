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
    "max_nearby_same_faction": 4,
    "profile": "on_ground",
    "caps": {
      "max_por_chunk": 4,
      "distancia_entre_grupos": 48,
      "distancia_de_jogador": 24
    }
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

## `profile` e `caps` (issue #112)

Os dois campos são **obrigatórios**, e um valor padrão aqui seria cômodo e caro:
quem escrevesse a regra de um chefe sem pensar no assunto ganharia "natural" de
graça, e o chefe entraria na lista de bioma sem que nada acusasse.

`profile` responde a uma pergunta só, e é a que decide se a criatura chega ao
mundo sozinha:

| valor | entra na lista de bioma | placement registrado | para quem |
| --- | --- | --- | --- |
| `on_ground` | sim | `ON_GROUND` | fauna terrestre |
| `in_water` | sim | `IN_WATER` | vida aquática |
| `flying_surface_anchor` | sim | `ON_GROUND` | voadora que **nasce pousada** |
| `structure_only` | **não** | `ON_GROUND` | habitante de estrutura |
| `encounter_only` | **não** | **nenhum** | chefe e alvo de encontro |

As três colunas existem porque as três falhas são silenciosas:

- **Lista de bioma errada.** Um `encounter_only` que vaze para o pool nasce pelo
  mundo inteiro; cada instância é uma entidade legítima, então não há duplicata
  para nenhum portão achar. O sintoma é a recompensa do encontro único virando
  farm.
- **Placement errado.** Registrar um peixe com placement de chão reprova todo
  ponto de água funda e o mob simplesmente nunca nasce — sem log, sem erro.
- **Voadora no ar.** Spawnar no ar parece o óbvio para quem voa e ancora o ninho
  no vazio: toda distância medida a partir dele passa a sair de um ponto que
  ninguém alcança.

`caps` são tetos de densidade, e **são** botão de balanceamento — por isso moram
no dado. O que não é botão é a existência do teto: sem ele a lista de bioma
continua valendo a cada tentativa e o vale vira parede de carne, tudo dentro das
regras. `max_por_chunk` igual a zero é **rejeitado**: desligar um mob se faz pelo
perfil `encounter_only`, que diz a intenção, e não por um zero que a esconde.

Duas coerências são cobradas no construtor de `SpawnRule`, e as duas mordem:

1. perfil que entra na lista de bioma **precisa** de ao menos uma `biome_tag` —
   sem tag ele nunca nasce, e isso aparece como um bioma vazio;
2. perfil que **não** entra na lista de bioma não pode declarar `biome_tags` —
   a tag existiria no datapack, o portão a conferiria, e mesmo assim ela não
   colocaria o mob em lugar nenhum. Alarme órfão.
