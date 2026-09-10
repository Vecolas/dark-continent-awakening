# Migracao de saves

Um mundo de Minecraft e o unico artefato deste projeto que **nao tem backup por
padrao e nao pode ser regerado**. Codigo se reescreve; as quarenta horas de
alguem, nao.

---

## As tres regras

**1. Todo dado persistido carrega `schemaVersion`, desde a v1.**

Ele existe agora, com nada para migrar. Criar versionamento depois que saves
reais existem obriga a adivinhar de que versao cada save veio — e adivinhar
errado corrompe em silencio.

**2. Save de versao FUTURA e recusado alto.**

Abrir um mundo com um JAR mais antigo que o save regrava os campos que o JAR
antigo nao conhece. O jogador perde progresso e nada acusa.
`NenProfileMigrator` lanca excecao com instrucao de atualizar o mod.

**3. Cada degrau e uma funcao propria, com teste proprio.**

Nao existe "migracao generica". `de1Para2`, `de2Para3`, em ordem, uma por
linha, cada uma com um caso de teste que parte de um dado real da versao
anterior.

---

## Como acrescentar um campo

Ordem obrigatoria:

1. Acrescente o campo ao record, **no fim**.
2. No codec, use `optionalFieldOf` com o valor **neutro**. Save antigo sem o
   campo tem de ler como neutro, nao dar erro. Campo com default util faz todo
   dado antigo mentir.
3. Se o neutro nao servir — se o campo precisar ser calculado a partir dos
   outros — suba `SCHEMA_ATUAL` e escreva o degrau em `NenProfileMigrator`.
4. Guarde uma **fixture** do formato antigo em
   `src/test/resources/saves/v<N>/`.
5. Escreva o teste que le a fixture antiga e verifica os valores depois da
   migracao.

Passo 4 e o que costuma faltar. Sem a fixture, o teste de migracao testa o
codigo contra ele mesmo.

---

## Como remover ou renomear um campo

Muito mais caro que acrescentar. Antes de comecar, confirme que vale a pena.

- **Renomear** e remover mais acrescentar. Precisa de degrau que le o nome
  antigo e escreve o novo, e a fixture do nome antigo fica no repositorio para
  sempre.
- **Remover** exige decidir o que acontece com o dado. Descartar em silencio e
  a opcao errada por padrao.
- Ids de categoria, tecnica e habilidade estao **congelados**
  ([ADR-004](../adr/ADR-004-identidade-congelada.md)). Mexer neles exige um ADR
  novo apontando aquele.

---

## Teste de regressao de save

A cada release:

1. Abrir um mundo da versao **anterior**.
2. Verificar que a categoria, os unlocks e os marcos continuam la, com os
   mesmos valores.
3. Verificar que `schemaVersion` foi atualizado no disco.
4. Fechar, reabrir, verificar de novo — a migracao tem de ser idempotente.

Guarde o mundo de teste. Ele e a fixture mais valiosa do projeto, e ela precisa
crescer: um mundo com jogador desperto, categoria revelada, tecnicas
desbloqueadas e proficiencia acumulada prova muito mais que um mundo novo.

---

## O que ainda nao existe

| | Estado |
| --- | --- |
| Fixtures de save em `src/test/resources/saves/` | **existe** — `v1/perfil-completo.snbt` |
| Teste que le NBT real, nao so JSON | **existe** — `FixturesDeSaveTest` |
| Portao que exige fixture por versao de schema | **existe** — reprova quando `SCHEMA_ATUAL` sobe sem fixture |
| Ida e volta em NBT binario comprimido, no disco | **existe** |
| Mundo de regressao guardado | falta — depende do ciclo de vida do jogador (issue #2) |
| Teste do attachment gravando num save real | falta — precisa de gametest (M1) |
| Suite completa de migracao | falta — M7 |

Hoje o `NenProfileMigratorTest` cobre apenas a **recusa** de versao invalida e
futura. Ele nao prova que uma migracao real preserva um mundo real, porque
ainda nao ha migracao real. Ver
[o-que-nao-provamos.md](o-que-nao-provamos.md).
