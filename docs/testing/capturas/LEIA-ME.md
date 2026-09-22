# Capturas da trilha AV — o que falta, e como tirar

Este diretório existe porque **dez gates da trilha AV estão abertos por falta de
evidência, e não por falta de código.** Até agora ele não existia, e "quais
capturas faltam" era uma pergunta que só se respondia relendo dez issues.

> **Código compilável não é entrega de gate, e teste verde não é aprovação
> visual.** O que fecha um gate desta trilha é imagem arquivada, comparada com a
> referência, por gente olhando.

---

## O estado, de uma vez

| Gate | Issue | Capturas | Feito |
| --- | --- | --- | --- |
| AV0 — a shell acompanha as animações | #169 | **0 — aposentadas** | — |
| AV1 — Ten convincente sem partícula | #176 | 17 | 0 |
| AV2 — filamentos nascem na superfície | #181 | 13 | 0 |
| AV3 — TEN aprovado | #187 | 22 | 0 |
| AV4 — REN aprovado, mundo intacto | #193 | 20 | 0 |
| AV5 — o halo respeita parede | #198 | 13 | 0 |
| AV6 — ausência para observadores | #201 | 7 | 0 |
| AV7 — dois clientes, armadura e poses | #205, #104 | 21 | 0 |
| AV8 — orçamento e release | #209, #206, #207 | perfis, não imagens | 0 |

**São 113 itens de evidência visual (2026-09-22), e nenhum existe.** Eram 127
até o AV0 dispensar as suas catorze — a inspeção visual ao vivo foi considerada
suficiente, e o custo da decisão está em [`AV0/DIVIDAS.md`](AV0/DIVIDAS.md).
Três dos 113 são vídeo quadro a quadro — a transição Ten→Ren no AV4 e as duas de
Zetsu no AV6. O total não promete 113 PNGs.

> **O AV0 é o único gate desta trilha sem evidência visual arquivada**, e isso é
> decisão registrada, não esquecimento. A evidência dele é o julgamento humano
> em [`AV0/PERGUNTAS.md`](AV0/PERGUNTAS.md), datado e preso a um commit.

**Este número é derivado.** A fonte de verdade de quais evidências cada gate
exige continua sendo o `LEIA-ME.md` do gate, e a issue. Refaça a soma em vez de
confiar nela: é assim que o 75 abaixo aconteceu.

> A contagem desta tabela já disse **75**. Aquele número somava só os gates cuja
> linha trazia um número; AV1, AV2 e AV3 apareciam como "ver issue" e ficavam de
> fora da soma — uma subcontagem de 52 capturas que ninguém tinha como ver.
> As três listas foram lidas das issues em 2026-09-21 e os números entraram na
> tabela.

A ordem de execução, a montagem de dois clientes e o checklist por gate estão em
[`CAMPANHA-EVIDENCIAS.md`](../CAMPANHA-EVIDENCIAS.md).

Isso está declarado também em
[`o-que-nao-provamos.md`](../o-que-nao-provamos.md), e não deve ser deduzido de
código que compila.

---

## A bancada rodou, e funciona

O AV0 (#168) entregou `/nenvfx`, o overlay em **F6**, os sliders e o lote de
capturas nomeadas com data, commit e nível de bloom. Por meses isso teve apenas
teste unitário da lógica, e **nenhuma linha tinha sido digitada num cliente** —
era o ponto cego mais antigo da trilha.

**Em 2026-09-21 ela foi validada**, numa sessão que existia só para isso e que
deliberadamente não arquivou captura nenhuma. Comando, overlay, sliders, lote,
nomenclatura e restauração de câmera passaram item a item — os detalhes estão em
[`../CAMPANHA-EVIDENCIAS.md`](../CAMPANHA-EVIDENCIAS.md) seção 5.

A sessão custou três defeitos: #299 (o cliente morria ao ligar Ren), #300 (toda
captura saía com o mesmo sufixo de bloom) e #301 (a shell de primeira pessoa
desenhava fora do braço). **Nenhum deles aparecia em 1.549 testes verdes.**

Foi exatamente o que a sessão existia para descobrir — e por isso ela vinha
antes das evidências, e não junto delas.

---

## Antes de começar

1. **Atualize a instância.** Mod novo mergeado exige `scripts/instancia.ps1
   atualizar`, senão o teste manual roda o JAR velho — em silêncio.
2. **Servidor dedicado, nunca singleplayer.** Singleplayer roda um servidor
   interno no mesmo processo do cliente e **não** pega classe client-only
   vazada (erro nº 10 do `CLAUDE.md`).
3. **Dois clientes reais** onde o gate pedir. `./gradlew runClient
   -Pjogador=Steve` e `-Pjogador=Alex` usam diretórios separados — sem isso os
   dois brigam pelo `options.txt` e o segundo sobrescreve o log do primeiro.
4. **`runServer` é recurso de dono único.** Combine a janela antes de subir
   ([`fronteira-de-arquivos.md`](../../processo/fronteira-de-arquivos.md) §5).

---

## A regra que invalida uma captura

O overlay **F6** grita `OVERRIDE ATIVO` sempre que há qualquer sobreposição:
`/nenvfx off`, `freeze`, estado forçado, output forçado, densidade forçada, LOD
forçado, contagem de ribbons, um slider de perfil, ou o modo `permissivo` de
visibilidade.

> **Captura com o aviso aceso não vale como aprovação.** Ela mostra um jogo que
> ninguém joga. O aviso existe exatamente porque essa é a forma mais fácil de
> aprovar um gate contra uma imagem que mente.

---

## Nomes de arquivo

O modo de captura já nomeia com data, commit e nível de bloom. **O commit é o do
build, e não o da árvore** — com mudança não commitada em cima, a imagem aponta
para um código que não é exatamente o que a gerou. É custo aceito por não haver
git em runtime, e está declarado.

Cada gate tem um `LEIA-ME.md` no diretório dele com a lista exata e o critério
de cada imagem.
