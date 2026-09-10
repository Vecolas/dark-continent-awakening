# Propriedade intelectual: o que este projeto pode e nao pode fazer

Este documento resume a situacao pratica. Ele **nao e parecer juridico**, e
nenhuma linha dele substitui consultar alguem que entenda do assunto antes de
uma distribuicao publica.

A decisao correspondente e o [ADR-007](../adr/ADR-007-assets-autorais.md).

---

## Tres licencas empilhadas, e elas sao independentes

### 1. Mojang para o mod

Criar e distribuir mods de Minecraft e permitido dentro das regras da Mojang.
Esta e a camada menos problematica.

### 2. Autor de mod para o modpack

Cada mod de terceiro tem a propria licenca. Varios dos candidatos deste pack
sao **All Rights Reserved**.

Isso normalmente **nao** impede inclui-los num pack por manifesto (que e como
CurseForge e Modrinth funcionam), mas **impede** assumir que se pode modificar
ou redistribuir o JAR.

Consequencia pratica, ja aplicada:

- JAR de terceiro **nao entra neste repositorio**;
- a coluna de licenca de
  [supported-mods.md](../modpack/supported-mods.md) precisa estar preenchida
  antes de qualquer distribuicao — lida da pagina, nao de memoria.

### 3. Hunter x Hunter para nos

**Esta e a camada que nao e coberta por nenhuma das outras.**

A Shueisha trata a obra como propriedade protegida. Uma orientacao de copyright
nao equivale a licenca para projeto de fa, e um aviso de "fan made" no README
nao muda a situacao juridica — ele so declara intencao.

---

## O que fazemos por causa disso

| Regra | Motivo |
| --- | --- |
| Nenhum frame, trilha, voz ou recorte de arte extraido da obra | e o uso mais dificil de defender |
| Textura, modelo, VFX, som e UI **autorais** | ver acima |
| Nomes de personagem so em quest e lore escritas por nos | referencia textual e diferente de copia de asset |
| Revisao de permissao, mod a mod, antes de distribuir publicamente | camada 2 |
| O nucleo sobrevive a uma retematizacao | ver abaixo |

---

## A saida arquitetural

O nucleo foi projetado para que **retematizar seja trabalho de traducao e arte,
nao de arquitetura**.

Categorias, aura, tecnicas e habilidades sao mecanicas. Os nomes exibidos saem
de arquivos de idioma; os ids internos sao neutros o suficiente
(`nenfoundation:enhancement`, e nao `nenfoundation:gon`).

Se um dia o projeto precisar virar um universo original inspirado no conceito
de Hunters e de uma energia com categorias, isso e uma troca de textos e de
assets. Nao e reescrever o mod.

Essa possibilidade nao existe por acaso — ela e o principal motivo pelo qual as
habilidades de prova do M5 **nao** representam personagens canonicos.

---

## O risco pratico varia muito

Vale dizer com todas as letras, porque a diferenca e grande:

- **Projeto gratuito, entre duas pessoas e alguns amigos, num servidor
  privado:** risco pratico baixo.
- **Distribuicao publica gratuita:** risco maior, e a revisao de permissao do
  ADR-007 e o portao.
- **Qualquer coisa comercial:** outra conversa inteira, que este documento nao
  cobre.

O projeto hoje esta na primeira situacao. Mudar de faixa e uma decisao
consciente, com issue.

---

## Em aberto

| Questao | Quem decide |
| --- | --- |
| Licenca do **codigo** deste repositorio (hoje: `All Rights Reserved`, o padrao do MDK) | as duas pessoas |
| Se havera distribuicao publica, e em que plataforma | as duas pessoas |
| Preencher a coluna de licenca de cada mod candidato | Dev B, antes do M6 |
