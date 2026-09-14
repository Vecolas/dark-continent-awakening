# EN0–EN16: onde a trilha de inimigos está

Fonte de verdade do estágio de **[INIMIGOS]**, como
[`marcos.md`](../processo/marcos.md) é dos marcos M0–M8. Os dois prefixos
existem separados de propósito: `EN` não colide com `M`, e as duas trilhas
avançam por dependência técnica própria.

> **Não deduza entrega a partir de código compilável.** Um mob registrado que
> anda, percebe e ataca passa por todos os sinais que o repositório sabe ler. O
> que separa "funciona" de "entregue" está na
> [diretriz de mobs customizados](mobs-customizados.md) e no
> [ADR-017](../adr/ADR-017-mob-vanilla-e-andaime-nao-entrega.md).

---

## O quadro

| Estágio | O que é | Estado |
| --- | --- | --- |
| EN0 | baseline Java/NeoForge/GeckoLib | ✅ entregue |
| EN1 | fundação: contratos, brain, percepção, combate, stagger, facções, spawn, debug, dummy | ✅ **código completo**; gate (#139) pendente de jogo |
| EN2 | Great Stamp + Bestiário | ✅ entregue pela outra frente |
| EN3 | Foxbear, Frog-In-Waiting, GrabController | ✅ **código completo**; gate (#123) pendente de jogo |
| EN4 | fundação de encontro persistente | ✅ código completo; gate pendente de restart real |
| EN5 | Greed Island core, captura, card | ✅ código completo; gate pendente de corrida com dois jogadores |
| EN6 | sete criaturas de Greed Island | 🟡 registradas; **corpo e comportamento em andamento** |
| EN7 | Squad e Pack | ✅ código completo; sem consumidor em jogo |
| EN8 | Chimera core + três peons | 🟡 domínio completo; **sem entidade** |
| EN9 | colônia persistente e simulação offline | 🟡 domínio completo; **sem SavedData e sem entidade** |
| EN10 | Nen de Chimera sobre o Nen real | 🟡 decisão tática completa; **não ativa técnica nenhuma** |
| EN11 | seis officers e squadron leaders | ⬜ não iniciado |
| EN12 | integração de mundo e segurança de spawn | 🟡 parcial: os cinco perfis e as duas trancas existem |
| EN13 | passe de áudio, VFX e legibilidade | ⬜ não iniciado — **e o bloqueio de ferramenta caiu** |
| EN14 | hardening multiplayer | ⬜ não iniciado |
| EN15 | balanceamento e telemetria local | ⬜ não iniciado |
| EN16 | release candidate dos 23 | ⬜ não iniciado |

---

## O que a fundação EN1 passou a garantir

Cada item existe por causa de uma falha que **não levanta exceção**.

| Peça | A falha que ela fecha |
| --- | --- |
| `PerceptionBudget` | um mob que varre o mundo todo tick funciona sozinho e derruba o TPS em manada — sem log, sem erro, só "o servidor fica lento com o tempo" |
| `ThreatMemory` | memória sem prazo transforma "perdi de vista" em "persigo para sempre", e o mob nunca volta para casa |
| `HearingEvent` | ouvir por varredura custa o mesmo que ver, sem nenhum dos limites da visão — e ninguém procura gasto em "audição" |
| `TargetEvaluator` | decidir alvo por `instanceof` espalha a regra por cada mob e garante que o décimo esqueça um caso |
| `StaggerState` | usar knockback como interrupção funciona até alguém dar resistência a knockback ao mob, e então a interrupção some junto |
| `SpawnProfile` | natural, structure-only e encounter-only no mesmo caminho fazem um chefe nascer no mato — e cada instância é uma entidade legítima |
| `SpawnCaps` | sem teto, a lista de bioma continua valendo a cada tentativa e o vale vira parede de carne, tudo dentro das regras |
| `EnemyRuntime` | limpeza espalhada pelos pontos de saída: o esquecido deixa um mob perseguindo um fantasma até o restart |
| `EnemyCatalog` | uma segunda família de mobs faz o portão antigo seguir **verde varrendo menos** — o falso verde mais barato que existe |
| `GrabController` | um ponto de saída esquecido deixa um jogador preso dentro de um bicho até o servidor reiniciar |
| `SafeReleaseSpot` | soltar a vítima "na frente" mata quem foi agarrado contra a parede, e só acontece quando o predador encurralou alguém |
| `EncounterTransitions` | `COMPLETED → ACTIVE` paga a recompensa duas vezes, e isso aparece como item duplicado no baú |
| `RewardLedger` | a corrida que ele impede é a que **sobrevive ao restart**: em memória, a trava morre com o processo |
| `ChimeraTrackingBudget` | colônia que produz mais do que morre deixa o mundo mais lento a cada hora, e nada aponta para a causa |
| `SimulacaoOffline` | "ficou três dias fora, então cresceu três dias" materializa centenas de entidades quando o chunk carrega |

---

## O que o Boneco de Treino provou — e o que ele achou

O `dummy_enemy` (#138) existe para usar a fundação inteira antes que qualquer
mob de conteúdo dependa dela. Ele é **feio de propósito**: um saco de estopa num
poste não pertence ao bestiário de Hunter x Hunter, e no dia em que alguém
pensar em promovê-lo a conteúdo, a aparência responde antes da discussão.

Escrever os onze GameTests dele achou **dois defeitos reais**, e nenhum dava
erro:

1. **A caixa de golpe apontava para trás.** Ela usava `-Z` como frente, que é a
   convenção da *geometria* Bedrock, contra a convenção de *mundo* que
   `AttackHitbox.noMundo` segue (`yaw 0 → +Z`). O boneco atacava, animava e não
   encostava em quem estava na frente.
2. **A morte não publicava o repouso.** Depois de morto o passo de IA não roda
   mais, e a fase ficava congelada no cliente em `WINDUP`/`ACTIVE` — o boneco
   morria de braços erguidos. Morte por golpe escapava por acidente; morte por
   fogo ou queda, não.

E os portões acharam mais dois, antes de qualquer teste:

3. **A régua de arte reprovou o alvo pintado.** O anel vermelho fica em `0.46`
   da altura e o `WeakPointResolver` dizia `0.55`, copiado de outro mob. O
   desenho prometeria um acerto que a regra não paga.
4. **`CoerenciaDeGeckoLibTest` reprovou o atalho de loop.** No GeckoLib 4.8.3 o
   `LoopType` do Java vence o JSON, e cravá-lo em código transformaria o
   dicionário de loops do gerador em documentação que discorda do comportamento.

---

## A biblioteca comum de arte

Os sete primeiros mobs foram escritos com o pincel, o ruído, o layout de caixa e
os portões **copiados** de arquivo em arquivo: 21 geradores, 18.814 linhas. Isso
estava certo enquanto eram poucos — e o gatilho de troca estava escrito no
próprio `spider_eagle_textura.py`: *"se um quinto mob precisar do mesmo pincel,
ele vira módulo"*.

O que decidiu a extração não foi o tamanho, e sim que **a duplicação já havia
divergido**:

| Régua | Quem tinha | O que o resto não sabia que tinha |
| --- | --- | --- |
| `valida_sem_buraco` | 3 de 7 | face pintada 5 de 6 — buraco visto de um ângulo só |
| `valida_pivots` | 1 de 7 | membro girando em torno de um ponto que não existe |
| `valida_faces_coplanares` | 1 de 7 | z-fighting, que o jogador lê como bug de driver |
| geo obrigatório | 6 de 7 | geo ausente virando verde |

`art-source/comum/` sobe o que é do **formato** e deixa no arquivo do mob tudo
que é do **bicho** — as tabelas de ossos e caixas, a paleta, e as validações
semânticas que ligam a arte à regra do servidor. Essas validações são o valor do
projeto, e a biblioteca existe para que elas caibam no arquivo do mob em vez de
disputarem espaço com seiscentas linhas de encanamento.

> **Aviso honesto:** quando um mob antigo for migrado para a biblioteca, as
> réguas que ele não tinha passam a rodar, e é **esperado** que alguma folha
> reprove na primeira execução. Isso é a régua encontrando o que já estava lá.

---

## Os dois gates que não fecham com código

`#139` (EN1) e `#123` (EN3) pedem `runClient`, `runServer`, dois jogadores e
olho humano. Nenhum deles fecha por teste verde, e nenhum foi executado nesta
trilha. O que existe é a **evidência negativa escrita**:
[`o-que-nao-provamos.md`](../testing/o-que-nao-provamos.md) ganhou onze linhas
novas nesta passagem, e elas são a parte do relato que costuma faltar.
