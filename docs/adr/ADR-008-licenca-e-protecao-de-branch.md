# ADR-008 — Licenca do codigo e ausencia de protecao de branch

- **Status:** aceita
- **Data:** 2026-09-10
- **Marco:** M1
- **Altera:** fecha as duas perguntas que o [ADR-007](ADR-007-assets-autorais.md)
  deixou explicitamente em aberto.

## Contexto

Duas decisoes ficaram pendentes desde o bootstrap, e as duas estavam
registradas como bloqueio:

1. A licenca do codigo era a padrao do MDK — *All Rights Reserved* — por
   omissao, e nao por escolha.
2. Nao ha protecao de branch nem CODEOWNERS. Em repositorio privado, o GitHub
   exige plano pago para aplicar qualquer um dos dois.

Decisao pendente registrada como bloqueio nao e problema; decisao pendente que
ninguem fecha vira decisao tomada por inercia. Estas duas ficam fechadas aqui.

## Decisao

### 1. O codigo continua *All Rights Reserved*

Agora por escolha. O projeto nao aceita contribuicao externa hoje, e a licenca
mais conservadora e a compativel com o risco de IP que o ADR-007 descreve.

### 2. A ausencia de protecao de branch e um risco ACEITO

Nao vamos pagar por plano do GitHub nem abrir o repositorio para conseguir a
trava. A regra "ninguem commita na `main`" continua valendo e continua
dependendo **de disciplina, nao de mecanismo**.

Consequencia pratica, escrita para nao ser esquecida: **um push direto na
`main` nao produz nenhum aviso.** Ele nao e recusado, nao dispara alerta e nao
aparece em lugar nenhum ate alguem olhar o historico.

O que resta como defesa:

- todo trabalho passa por PR, por acordo entre as duas pessoas;
- o CI roda em `pull_request` **e** em `push` para `main`, entao um push direto
  que quebre o build fica vermelho — depois de ja estar na `main`;
- o historico e o portao final: `git log --first-parent main` mostra o que
  entrou sem PR.

## Custo assumido

- **A `main` pode quebrar, e nada impede.** A defesa e humana. O historico de
  qualquer projeto mostra o que acontece sem trava, e este nao e excecao — so
  esta escolhendo conscientemente.
- **Nao ha revisao obrigatoria.** Uma das duas pessoas pode mergear o proprio
  PR sem a outra olhar. Isso ja aconteceu nesta sessao, varias vezes, e foi
  aceitavel porque o trabalho era de bootstrap; deixa de ser aceitavel quando
  as duas frentes estiverem tocando o mesmo codigo.
- **Ninguem de fora pode contribuir com clareza** sobre o que esta concedendo,
  por causa da licenca. Isso e deliberado enquanto o projeto for de duas
  pessoas.
- **A licenca fecha a porta para distribuicao aberta** sem uma decisao nova.

## O que NAO muda

- **O ADR-007 continua inteiro.** Licenca de codigo e IP da obra sao camadas
  independentes: escolher *All Rights Reserved* para o nosso codigo nao diz
  nada sobre Hunter x Hunter, e nao reduz o risco descrito la.
- **Nada aqui autoriza distribuicao publica.** Ela continua dependendo da
  revisao de permissao mod a mod do ADR-007.
- **A regra de PR continua valendo.** Aceitar a ausencia de trava nao e
  autorizar push direto; e reconhecer que a regra nao tem mecanismo por tras.
- **Estas duas decisoes podem ser revistas a custo baixo.** Trocar a licenca
  antes de haver contribuicao externa e barato; ligar protecao de branch e uma
  configuracao. O que seria caro e descobrir daqui a meses que ninguem tinha
  decidido.
