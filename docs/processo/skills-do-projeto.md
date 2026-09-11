# Skills compartilhadas: Codex e Claude Code

As seis skills fornecidas pelo responsável foram incorporadas sem alterar seu
conteúdo. A fonte versionada fica em `.claude/skills/<nome>/SKILL.md`.
Os pontos de entrada em `.agents/skills` encaminham o Codex à mesma fonte.
Não editar duas cópias: alterar a fonte e manter nome/description do encaminhador.

## Uso nas implementações

Antes de atuar, ler integralmente as skills aplicáveis e anunciar seu uso:

| Trabalho | Skills |
| --- | --- |
| Java, APIs, eventos, dados e rede | `neoforge-1-21-1` |
| Regras de Nen e mecânicas | `nen-lore-rules` e `nen-architecture` |
| Branch, commit, PR e merge | `git-workflow` |
| Validação e regressão | `testing` |
| Revisão antes de merge | `code-review` (inclui arquitetura e lore) |

`disciplina-de-engenharia` continua obrigatória conforme AGENTS.md. Não carregar
seis textos em toda pergunta simples: o uso é obrigatório quando o escopo se
aplica, e não uma autorização para implementar marcos futuros.

## Precedência e divergências conhecidas

Instalar uma skill **não altera uma decisão arquitetural**. Prevalecem as
instruções explícitas do responsável, os ADRs aprovados, os contratos congelados
e as convenções do repositório. Exemplos genéricos das skills são referências,
não migrações automáticas de código ou de save.

- **Persistência:** a lista genérica em `neoforge-1-21-1` menciona Aura atual,
  estado ativo e cooldowns como candidatos a attachment. Aqui o ADR-002 separa
  perfil persistente de runtime: Aura atual/estados de combate não são salvos.
- **Valores derivados:** os exemplos de cache nas skills técnicas não
  autorizam congelar afinidade, capacidade, output ou multiplicadores. A regra
  de `disciplina-de-engenharia` é ler a fonte no instante de uso; definições
  imutáveis e caches com invalidação explícita são outro assunto.
- **Git:** usamos `feat/`, não o exemplo `feature/`; commits em português,
  squash por PR, stage explícito e nenhuma alteração da branch de outra pessoa.
  A recomendação genérica de proteção de branch não revoga o ADR-008.
- **Lore:** `nen-lore-rules` orienta pesquisa e desenho; não substitui os ADRs
  nem transforma interpretação comunitária em cânone comprovado. Incerteza
  deve ser declarada e verificada em fonte primária antes de mudar mecânicas.
- **Aura:** uma barra especial; Reserva + Output + Controle + Eficiência.
  Mudança nesse modelo exige decisão dos dois desenvolvedores registrada no
  ADR-009. Nenhum agente assina pelos dois nem trata a instalação como aprovação.
- **Escopo:** as ordens sugeridas e checklists de técnicas futuras nas skills
  não antecipam M3–M8. O marco autorizado continua em `marcos.md`.

## Descoberta e verificação

Codex procura skills de projeto em `.agents/skills`; seleção implícita fica
habilitada por padrão. Não foi criada configuração global nem desabilitação.
As skills ficam disponíveis no próximo turno; se a lista não atualizar,
reinicie a sessão. Confira com `/skills` ou `$nome` no Codex.
Fonte: [documentação oficial de skills](https://learn.chatgpt.com/docs/build-skills).

Claude usa `.claude/skills` conforme o pacote fornecido. AGENTS.md e CLAUDE.md
também exigem a leitura desta política, inclusive para sessões já abertas.
Não houve teste da interface de uma sessão Claude que já estava em execução.

O pacote original em `skills/` no clone antigo do OneDrive foi preservado.
A instalação foi feita no clone ativo em `C:\dev\dark-continent-awakening`;
outros clones recebem os arquivos pelo fluxo normal de Git, não por sobrescrita.
