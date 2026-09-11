# Marcos M0 a M8: o caminho ate o MVP

Os marcos sao sequenciais **por dependencia tecnica**, nao por calendario. Nao
ha prazo aqui de proposito.

Dentro de cada marco, as duas lanes correm em paralelo. Um marco so fecha
quando o **gate de saida** esta verde em cliente **e** em servidor dedicado.

```
M0 Bootstrap
 └─> M1 Persistencia + sync
      └─> M2 Aura Engine + HUD
           └─> M3 Despertar + categoria
                └─> M4 Tecnicas fundamentais
                     └─> M5 Framework de habilidades
                          └─> M6 Progressao + camada de modpack
                               └─> M7 Hardening
                                    └─> M8 Vertical slice + RC
```

---

## Regras que valem em todos os marcos

1. `main` sempre buildavel.
2. As duas pessoas nao trabalham sobre a mesma implementacao concreta quando um
   contrato pode ser separado primeiro.
3. Cada marco produz **algo testavel no jogo**. Nao acumular infra invisivel por
   varios marcos.
4. Epic Fight fica fora da linha critica ate o M7 ([ADR-006](../adr/ADR-006-epic-fight-fora-da-fundacao.md)).
5. Mudanca em schema de save ou em protocolo recebe versao e migracao
   explicitas.
6. Ideia nova vai para o backlog pos-MVP. Ela so entra em M0–M8 substituindo
   outra entrega do mesmo peso.
7. **Nunca implementar mais de um marco sem instrucao explicita para
   continuar.**

---

## M0 — Fundacao do repositorio e contratos  ✅ ENTREGUE

Estabelecer um projeto NeoForge 1.21.1 que compila, inicia em cliente e
servidor, e ja impoe as regras de colaboracao.

**Dev A:** MDK e Java 21, registries minimos, package e mod_id, skeleton de
api/domain/network, ADR-001, primeiro `runServer`.

**Dev B:** CI, templates de PR e issue, perfis de execucao, estrutura de
cliente/assets/datagen, matriz inicial de mods, workspace do pack de testes.

**Conjunto:** ownership dos arquivos quentes, convencao de branch e commit,
congelamento dos ids das seis categorias e do namespace, checklist de smoke
test.

### Gate de saida

- [x] Estrutura de pacotes criada, cada um com `package-info` declarando o que
      faz e qual decisao carrega.
- [x] ADR-001, ADR-002 e ADR-003 no repositorio (mais 004–007).
- [x] Contratos congelados: `NenCategory`, `PersistentNenData` v1, ids e
      direcoes de protocolo, `NenTechnique`, `NenAbility`.
- [x] Portoes que mordem: ids de categoria, traducao, protocolo x documento,
      indice de ADR, package-info, dominio sem import de cliente.
- [x] CI de build em todo PR.
- [x] **`gradlew build` verde**, com `Testes executados: 21` e JAR gerado.
- [x] **Servidor dedicado inicia com o JAR** — `Done (8.041s)`, sem
      `NoClassDefFoundError`.
- [x] **Nenhuma classe client-only carregada no servidor** — portao estatico
      mais `runServer` real.
- [x] **`runClient` executado** — camada de cliente carrega, sem erro (#18).
- [x] **JDK 21 instalado na maquina** — Temurin 21.0.12; `build` verde sem JDK
      portatil (#17).
- [ ] **CI verde num PR de cada PESSOA** — o CI roda verde em todo PR, mas
      todos foram abertos pela mesma pessoa. Fecha quando a segunda abrir o
      primeiro. E o unico item do M0 que depende de gente, e nao de codigo.

Os limites completos, e os defeitos que a execucao encontrou, estao em
[o-que-nao-provamos.md](../testing/o-que-nao-provamos.md).

---

## M1 — NenProfile persistente + protocolo de sync  (concluído em 2026-09-11)

> **Entregue ate agora:** perfil ligado ao ciclo de vida (#2), `RuntimeNenState`
> e scheduler central (#3), fixtures de save e portao de regressao em NBT (#8),
> cache de cliente e overlay (#6), comandos de debug (#7), records de payload,
> registro S2C e handshake de versao (#4), e o primeiro gametest (#35).
>
> **Entregue:** validação e rate limit dos payloads C2S (#5), mundo de regressão
> (#27), matriz de QA com dois jogadores (#9), e a correção de reenvio após
> mutação (#43). A execução dedicada foi registrada em
> [`qa-matrix.md`](../testing/qa-matrix.md).

Provar save, clone e sincronizacao **antes** de criar gameplay que dependa
disso.

**Dev A:** `AttachmentType` ligado ao ciclo de vida, politica de clone na
morte, API de query e mutacao server-side, `RuntimeNenState`, scheduler central
de tick.

**Dev B:** records de payload e `StreamCodec`, cache somente-leitura no
cliente, comandos de debug permissionados, overlay tecnico, fixtures de save,
teste de restart.

**Conjunto:** handshake de versao de protocolo; testar jogador novo, relog,
restart, morte e volta do End; revisar que nenhum payload C2S carrega valor
autoritativo.

### Gate de saida

- Categoria e unlocks persistem em teste artificial.
- O perfil de um jogador nunca aparece no cache de outro.
- Restart do servidor mantem os dados.
- Morte respeita exatamente a politica definida.

---

## M2 — Aura Engine + HUD

A primeira mecanica jogavel.

**Dev A:** `AuraPool`, regeneracao, output, exaustao, formulas e configs,
dirty flags de sync, testes unitarios dos invariantes.

**Dev B:** barra de aura, feedback de exaustao, interpolacao no cliente,
framework de keybind, teste de frequencia de pacote.

**Conjunto:** definir os invariantes numericos; instrumentar contadores de
sync e de tick em modo dev; primeiro perfil com spark.

### Gate de saida

- Aura nunca negativa, nunca NaN, nunca acima do maximo.
- O HUD reflete o estado depois de lag e de reconexao.
- O servidor nega gasto invalido.
- Aura nao gera pacote desnecessario.

> **Aqui nasce o primeiro numero ajustavel de verdade.** Ele nasce com a regua
> junto: contador em modo dev e um perfil de spark arquivado. Numero sem regua
> vira folclore.

---

## M3 — Despertar, categoria e afinidade

**Dev A:** API e evento de despertar, atribuicao e revelacao de categoria,
matriz de afinidade orientada a dado, regra propria de Specialist, marcos base.

**Dev B:** UI de descoberta, item ou ritual de Water Divination, assets, lang,
tooltips, primeiro hook com FTB Quests no ambiente do pack.

**Conjunto:** garantir que o nucleo funciona sem FTB Quests; comandos de seed
para reproduzir todas as categorias; testar recarga de recurso sem corromper
dado de jogador.

### Gate de saida

- Duas pessoas com categorias diferentes e persistentes.
- Categoria pode existir escondida ate a revelacao.
- Consulta de afinidade e deterministica.
- Quest dispara o despertar por API ou comando, **nunca** escrevendo NBT.

---

## M4 — Tecnicas fundamentais

**Dev A:** ciclo de vida de `NenTechnique`, Ten e Ren server-side,
modificadores defensivos e ofensivos, motor de exclusao e interrupcao.

**Dev B:** Zetsu e Gyo sobre a mesma interface, icones, feedback, keybinds, FX,
testes de percepcao de Gyo.

**Conjunto:** revisao cruzada (A revisa Zetsu/Gyo, B revisa Ten/Ren); testar
todas as combinacoes invalidas; definir `StopReason` e as mensagens de UX.

### Gate de saida

- As quatro funcionam em servidor dedicado.
- Zetsu cancela e impede estados conforme a matriz.
- Aura zero encerra tecnica corretamente.
- Morte e logout nao deixam estado fantasma.

---

## M5 — Framework de Hatsu / habilidades

**Dev A:** `NenAbility`, pipeline de validacao, custo, cooldown e alvo comuns,
instancia e canalizacao. Amostras: Enhancement, Emission, Manipulation.

**Dev B:** registro, slots de UI, feedback de cooldown, FX. Amostras:
Transmutation, Conjuration, Specialization. Documento "como criar uma
habilidade".

**Conjunto:** as duas pessoas criam habilidades sobre a mesma interface **sem
editar o engine da outra**. Depois da sexta, revisao arquitetural procurando
condicionais que precisam virar componente. Testar spoof de alvo e spam de
pacote.

### Gate de saida

- Seis habilidades de arquetipos diferentes funcionam.
- Acrescentar uma habilidade simples **nao** exige editar AuraEngine, registro
  de rede ou HUD.
- Cooldown e custo sobrevivem as tentativas comuns de exploit.
- Projetil e construct sao limpos ao trocar de dimensao e ao deslogar.

---

## M6 — Progressao e camada de modpack

**Dev A:** servico de progressao e proficiencia, anti-spam e retornos
decrescentes, eventos e API para quest e trainer, config inicial de balanco.

**Dev B:** onboarding em FTB Quests, manual em Patchouli, receitas em KubeJS,
loot em LootJS, spawns de teste em In Control, QA de JEI/EMI e Jade.

**Conjunto:** o fluxo jogavel inteiro — sem Nen, despertar, categoria,
tecnicas, habilidade, missao de combate. Verificar que **remover** FTB e KubeJS
ainda deixa o nucleo iniciar. Congelar a lista de versoes testadas do pack.

### Gate de saida

- Onboarding completo sem nenhum comando de admin para o jogador.
- Progressao persiste corretamente.
- Quest nao escreve NBT interno.
- Receita, loot e spawn mudam sem recompilar o nucleo.

---

## M7 — Hardening de multiplayer, saves e compatibilidade

Atacar os problemas que so aparecem depois que o sistema parece pronto.

**Dev A:** validacao de abuso e rate limit, suite de migracao de save, revisao
do pipeline de dano, otimizacao dos pontos quentes, adapter experimental de
Epic Fight.

**Dev B:** matriz de dois ou mais jogadores em servidor dedicado, smoke tests
de compatibilidade, profiling com ModernFix/FerriteCore/Embeddium, varredura de
colisao de keybind, testes de cliente e animacao do Epic Fight.

**Conjunto:** cenarios de lag, relog, exploit de morte, spam de dimensao e spam
de habilidade. Medir com spark antes e depois. **Decisao formal e escrita sobre
o Epic Fight: aprovado, experimental ou fora.**

### Gate de saida

- Nenhum exploit conhecido permite se auto-conceder aura, unlock ou dano.
- Save anterior abre e migra.
- O nucleo nao e o gargalo dominante no cenario de estresse definido.
- A lista de incompatibilidades esta documentada.

---

## M8 — Vertical slice e Release Candidate

**Dev A:** freeze da API 0.x, correcao de P0 e P1, balance pass, build de RC,
changelog.

**Dev B:** questline final do slice, polimento de HUD, manual e feedback,
manifesto e version-lock do pack, QA de instalacao limpa e de servidor.

**Conjunto:** QA completa; mundo novo percorrido do inicio ao fim **sem
comandos**; a mesma jornada com duas pessoas; catalogar o backlog pos-MVP fora
do RC.

### Gate de saida

- Despertar, categoria, aura, quatro tecnicas e habilidade funcionam ponta a
  ponta.
- Logout, restart, morte e dimensao mantem as regras.
- Servidor dedicado estavel no cenario de aceitacao.
- Documentacao de instalacao, compatibilidade e autoria de habilidade pronta.
- Tag de release candidate reproduzivel no Git.

---

## Decisoes que NAO devem ser antecipadas

- O formato final do Hatsu Builder, antes de o M5 revelar quais componentes sao
  realmente comuns.
- Acoplar combate ao Epic Fight antes de o pipeline vanilla passar pela QA do
  M4 e do M5.
- Dezenas de formulas de balanceamento antes de existir o vertical slice do M6.
- Persistencia de narrativa ou de arco dentro do `PersistentNenData`.
- Promessa de compatibilidade ampla antes de o M7 rodar os testes.

---

## Backlog pos-MVP, priorizado

| | Objetivo | Primeiro passo tecnico |
| --- | --- | --- |
| F1 | Ken, Ko, Ryu | mesma `NenTechnique`, usando modifiers e regras de foco/output ja existentes |
| F2 | Shu, En, In | generalizar contexto de item, area e percepcao; criar servico de sensing |
| F3 | Votos e limitacoes v1 | templates declarativos (condicao + bonus + penalidade), sem scripting livre |
| F4 | Percepcao de Nen em multiplayer | o servidor calcula quem percebe o que e envia filtrado por observador |
| F5 | Hatsu Builder v1 | grafo de componentes limitado + orcamento por categoria/afinidade + preview |
| F6 | Trainers e NPCs | API de desbloqueio e treino; conteudo em addon separado |
| F7 | Framework de boss | fases, telegrafos e IA em content mod; GeckoLib para animacao |
| F8 | Exame Hunter | estruturas, objetivos, regras de tentativa, Hunter License |
| F9 | Torre Celestial, Yorknew | modulos narrativos separados |
| F10 | Greed Island | projeto proprio: cartas, regras, dimensao e economia. **Nao** absorver no nucleo |
| F11 | Nen Beasts | owner, ciclo de vida, persistencia, IA e limpeza sobre o framework de construct |
| F12 | API publica 1.0 | versionar contratos, eventos, docs e compatibilidade para addons |
