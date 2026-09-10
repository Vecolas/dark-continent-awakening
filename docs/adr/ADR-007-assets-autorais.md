# ADR-007 — Todo asset e autoral; nada e extraido da obra

- **Status:** aceita
- **Data:** 2026-09-10
- **Marco:** M0

## Contexto

Ha tres licencas empilhadas neste projeto, e elas sao independentes:

1. **Mojang para o mod.** Criar e distribuir mod e permitido dentro das regras
   da Mojang.
2. **Autor de mod para o modpack.** Cada mod de terceiro tem a propria licenca.
   Varios sao *All Rights Reserved*, o que nao impede inclusao por manifesto mas
   impede modificar ou redistribuir o JAR.
3. **Hunter x Hunter para nos.** Esta nao e coberta por nenhuma das outras. A
   Shueisha trata a obra como propriedade protegida, e uma orientacao de
   copyright nao equivale a licenca para projeto de fa. "Fan made" no README nao
   muda isso.

## Decisao

1. **Nada extraido da obra.** Nenhum frame, nenhuma trilha, nenhuma voz, nenhum
   recorte de arte. Textura, modelo, VFX, som e interface sao feitos por nos ou
   licenciados com permissao registrada.
2. **JAR de terceiro nao entra no repositorio.** O pack se descreve por
   manifesto e version-lock; o repositorio nao e um mirror.
3. **Toda dependencia entra em `docs/modpack/supported-mods.md`** com pacote,
   versao, licenca, motivo e URL.
4. **Distribuicao publica passa por uma revisao de permissao** antes de
   acontecer, mod a mod.
5. **O nucleo e projetado para sobreviver a uma retematizacao.** Categorias,
   aura e tecnicas sao mecanicas; os NOMES exibidos saem de arquivos de idioma.
   Se um dia o projeto precisar virar um universo original inspirado no
   conceito, isso e trabalho de traducao e arte — nao de arquitetura.

## Custo assumido

- **Arte propria e cara.** Fazer VFX e modelo do zero e mais lento que
  aproveitar material existente, e a qualidade inicial vai ser menor.
- **Menos fidelidade visual.** O pack nao vai parecer o anime. Nao pode.
- **O risco nao chega a zero.** Estas medidas reduzem exposicao; elas nao
  produzem uma licenca. Um projeto gratuito entre amigos e uma situacao pratica
  muito diferente de uma distribuicao publica, e essa diferenca esta reconhecida
  aqui em vez de escondida.
- **Revisao antes de publicar e uma etapa a mais** no caminho de qualquer
  release.

## O que NAO muda

- O projeto continua sendo um projeto de Hunter x Hunter em intencao e em
  vocabulario. Esta decisao trata de ARQUIVOS, nao de tema.
- Ela nao proibe nome de personagem em quest ou em lore escrita por nos.
- Ela nao antecipa a decisao sobre distribuir publicamente. Essa continua em
  aberto, e o item 4 e o portao dela.

## Em aberto

- A licenca do CODIGO deste repositorio ainda e a padrao *All Rights Reserved*.
  Escolher uma licenca de verdade e uma decisao dos dois desenvolvedores e vai
  virar issue.
