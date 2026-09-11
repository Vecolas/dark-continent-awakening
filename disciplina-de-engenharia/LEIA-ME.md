# Por que esta pasta existe

Isto e uma **copia versionada** da skill `disciplina-de-engenharia`, que na
maquina de quem a escreveu vive em `~/.claude/skills/disciplina-de-engenharia/`.

## A decisao, e o que ela custa

O `CLAUDE.md` e o `CONVENCOES.md` dizem que as regras gerais de engenharia nao
sao duplicadas no repositorio, porque duas fontes para a mesma verdade
divergem — e a que vale costuma ser a errada.

Copiar a skill para ca **e** essa duplicacao. Ela foi feita mesmo assim, por um
motivo concreto: **um agente de codificacao rodando neste repositorio pode nao
ter acesso a `~/.claude/skills/`.** Sem a copia, ele improvisa as regras ou
trabalha sem elas — e o custo disso e maior que o custo da duplicacao.

## Qual copia vale

**Esta.** A copia do repositorio e a fonte de verdade para quem trabalha neste
projeto.

Se a versao em `~/.claude/skills/` mudar, a mudanca **precisa** ser trazida
para ca num commit proprio, com `docs:` no titulo. Enquanto isso nao acontecer,
a de la nao vale aqui.

Isso e o unico jeito de a duplicacao nao virar divergencia: uma das duas copias
tem de ganhar por regra, e nao por acaso.

## O que fazer se as duas discordarem

O que esta commitado aqui ganha, e a diferenca vira commit. Nunca o contrario,
e nunca em silencio.
