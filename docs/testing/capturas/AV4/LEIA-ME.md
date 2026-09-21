# AV4 — REN aprovado, e o mundo intacto (#193)

O código das cinco issues do AV4 está na branch `feat/av4-av8-aura-visual`.
**Este gate não depende de mais código**: ele depende de imagem arquivada e de
duas medições no mundo.

Referência: [`referencia-c-ren-pressao.png`](../../../aura-art/referencia-c-ren-pressao.png).

---

## As vinte capturas

Mesmo local, mesmo FOV, mesma skin, mesma distância. Hora e clima travados pelo
modo de captura.

```
ren_dia          ren_noite         ren_caverna
ren_2b   ren_5b  ren_10b  ren_20b  ren_40b
ren_slim         ren_armadura      ren_overlay_skin
ren_correndo     ren_agachado      ren_nadando
primeira_pessoa_ren
sem_particulas_ren
transicao_ten_ren            (VÍDEO, quadro a quadro)
chao_antes   chao_depois     (5 min de Ren contínuo)
```

`sem_particulas_ren` sai com `/nenvfx particulas 0` — e **essa é a única
sobreposição que não invalida a captura**, porque a pergunta dela é exatamente
"com densidade zero, ainda se lê Ren?". As outras dezenove saem com o overlay
**F6** sem o aviso `OVERRIDE ATIVO`.

---

## O que cada uma responde

| Captura | A pergunta |
| --- | --- |
| `ren_dia/noite/caverna` | Ren se lê nas três luzes? Na caverna ele vira borrão branco? |
| a série `2b..40b` | **tirada com um SEGUNDO jogador**, senão o LOD nunca morde — a distância do jogador local para a própria câmera é zero |
| `ren_slim` | a aura do braço fica larga demais numa skin Alex? |
| `ren_armadura` | com armadura de ferro completa, ainda se lê Ren? A armadura continua reconhecível? |
| `ren_overlay_skin` | numa skin com segunda camada completa, a aura some por dentro dela? |
| `ren_correndo/agachado/nadando` | a película continua aderida nas articulações? |
| `primeira_pessoa_ren` | **as colunas NÃO podem aparecer** — só borda mais forte, 2–4 filamentos e o pulso de ativação |
| `transicao_ten_ren` | **a contração inicial é visível quadro a quadro?** Sem ela a subida é lida como interpolação, e não como liberação |

---

## As duas medições, que não são imagem

### 1. O mundo continua intacto

Cinco minutos de Ren contínuo sobre **terra, pedra, areia, grama alta e água
rasa**, com `BlockState` comparado antes e depois na área do anel, e contagem de
entidades antes e depois.

> **OS CINCO MINUTOS NAO CABEM NA RESERVA BASE, e a saida e o config do
> servidor de QA.** Com `aura.maximaBase = 100`, sustentar Ren por 5 min
> exigiria custo 1,33/s -- abaixo de Ten. A escada do
> [ADR-018](../../adr/ADR-018-escada-de-custo-em-segundos.md) da a Ren **29
> segundos**, e isso e o desenho, nao um defeito.
>
> Suba `aura.maximaBase` no config do servidor do **mundo de regressao**. Config
> e por servidor; o balanceamento distribuido nao muda.
>
> **Isso e legitimo porque a reserva nao toca o visual** -- `perfis-visuais.md`
> §7: a intensidade vem do *output efetivo*. Um Ren sustentado com reserva 5.000
> e visualmente IDENTICO ao de reserva 100, e a captura continua mostrando o que
> o jogador vera.
>
> **O que NAO vale:** forcar com `/nenvfx state ren`. Isso acende `OVERRIDE
> ATIVO` no overlay, e a captura deixa de valer como aprovacao.

- **zero bloco alterado**
- **zero `ItemEntity` novo**
- **zero entidade órfã**

> No código, isso é afirmação de construção: `hasPhysics = false`, nenhuma
> chamada a `BlockState` durante a vida da partícula, nenhum `ItemEntity` em
> caminho nenhum. **Afirmação de construção não é medição** — é por isso que
> este item existe.

### 2. O impulso de câmera não incomoda

Ativar Ren **vinte vezes seguidas**. A régua é literal: se incomodar, reprova.

E com o **segundo cliente**: o observador **não recebe impulso nenhum**. No
código isso é garantido por construção — o disparo mora no ramo exclusivo do
jogador local —, mas nunca foi verificado com dois clientes.

---

## Reprova se

- as colunas passarem de oito, ou parecerem raio elétrico (zigue-zague)
- alguma coluna flutuar sem origem visível no corpo
- o anel for um círculo perfeito, ou desenhar qualquer forma legível
- a skin deixar de ser legível em Ren
- sobrar anel, coluna, detrito ou loop de áudio após relog, morte ou troca de
  dimensão
- `runServer` acusar `NoClassDefFoundError`

---

## O que este gate NÃO prova

Está em [`o-que-nao-provamos.md`](../../o-que-nao-provamos.md): captura não é
vídeo (só a transição tem vídeo aqui); uma GPU não é o parque; o bloom não
existia quando as referências foram geradas **com** shader pack; skin de teste
não é skin de jogador; dez jogadores em Ren não foram medidos; o ripple de
impacto não foi exercitado, porque não há dano de Nen para disparar (#103,
bloqueada por #127).
