---
name: git-workflow
description: Git and GitHub collaboration workflow for two developers working concurrently on this private mod repository. Use when branching, committing, rebasing, resolving conflicts, opening PRs, merging, or coordinating parallel work.
---

# Git Workflow for Two Developers

The goal is to let two developers and their Claude Code sessions work in parallel without destabilizing `main` or overwriting each other's work.

## Main rule

`main` is the integration branch and should stay buildable.

Do not develop substantial features directly on `main`.

## Branch naming

Use short-lived branches with a predictable prefix:

```text
feature/nen-aura-foundation
feature/ten-zetsu-ren
feature/nen-hud
fix/aura-sync
refactor/ability-registry
test/nen-gametests
docs/nen-rules
```

One branch should have one primary responsibility.

## Before starting work

```bash
git switch main
git pull --ff-only
git switch -c feature/<name>
```

If the developer already has a branch, update intentionally instead of recreating it.

## Parallel work

Before two people start, divide work by subsystem/file ownership when possible.

Good split:

- Developer A: aura state + persistence;
- Developer B: HUD + client rendering.

Risky split:

- both heavily editing the same central class and registry at the same time.

If both tasks need the same shared interface, agree on the interface first, commit it early, then branch from that shared point.

## Worktrees

When one developer or one machine needs multiple concurrent Claude Code tasks, prefer Git worktrees over repeatedly switching a dirty working tree.

Example:

```bash
git worktree add ../mod-aura feature/nen-aura-foundation
git worktree add ../mod-hud feature/nen-hud
```

Never point two worktrees at the same branch.

## Commit discipline

Make small, coherent commits.

Good:

```text
feat(nen): add persistent aura attachment
feat(nen): synchronize aura state to client
fix(nen): preserve affinity across death
```

Bad:

```text
stuff
update
fix everything
```

Before committing:

1. inspect `git status`;
2. inspect `git diff`;
3. run relevant tests/build;
4. stage only intended files;
5. verify generated/IDE files are not accidentally staged.

## Syncing with main

For a private two-person repository, prefer a clean linear feature history unless the project chooses otherwise.

When `main` advances:

```bash
git fetch origin
git rebase origin/main
```

Resolve conflicts locally, test again, then push.

If the branch has already been published and rebased, use `--force-with-lease`, never plain `--force`:

```bash
git push --force-with-lease
```

Do not rewrite someone else's branch without coordination.

## Pull requests

Every non-trivial change should go through a PR.

PR description should include:

- what changed;
- why;
- important architecture/lore decisions;
- how it was tested;
- known limitations/follow-ups.

Keep PRs reviewable. If a PR mixes architecture refactor, new ability, HUD redesign, and unrelated cleanup, split it.

## Main branch protection

Recommended GitHub settings for `main`:

- require a pull request before merging;
- require at least one approval when practical;
- require status checks/build to pass;
- require conversation resolution;
- block force pushes;
- block branch deletion;
- optionally require linear history.

For a two-person project, one required approval means the other developer reviews the change. If this becomes too blocking during early prototyping, keep PRs and status checks even if required approvals are temporarily relaxed.

## Merge strategy

Recommended default: squash merge for small feature branches if the branch contains noisy fixup commits.

Use rebase/merge intentionally if preserving individual commits has value.

Whatever strategy is chosen, use it consistently.

## Conflict resolution

When conflicts occur:

1. identify semantic intent on both branches;
2. do not accept `ours`/`theirs` blindly;
3. rebuild the merged logic;
4. run tests;
5. inspect the final diff against `origin/main`;
6. ask the other developer when the conflict changes shared architecture.

High-risk conflict files:

- Gradle/version files;
- central registries;
- mod entrypoint;
- network registration;
- player Nen data schema;
- shared ability interfaces;
- resource/data indexes.

## Schema/persistence changes

Changes to serialized Nen data deserve special care.

If changing attachment codecs/NBT/data formats:

- call it out in the PR;
- consider migration/backward compatibility;
- test loading an existing world if applicable;
- coordinate before both developers modify the schema concurrently.

## Generated files

Do not hand-merge generated assets/data when they can be regenerated deterministically.

Keep generation commands documented.

Do not commit:

- IDE workspace state unless intentionally shared;
- `build/` outputs;
- local run worlds/logs;
- secrets/tokens;
- machine-specific configuration.

## Claude Code collaboration rule

Before Claude changes code in a shared repository, it should:

1. run/inspect `git status`;
2. identify current branch;
3. avoid touching unrelated uncommitted work;
4. state when it needs to modify a high-conflict shared file;
5. keep commits/task changes scoped.

Claude must never discard another developer's changes to make a build pass.

## Emergency recovery

Prefer safe inspection commands first:

```bash
git status
git log --oneline --graph --decorate --all
git reflog
```

Avoid destructive commands such as `git reset --hard`, `git clean -fd`, or deleting branches unless the user explicitly understands what will be lost.

## GitHub reference

GitHub supports protected branches/rulesets that can require PR reviews, passing status checks, resolved conversations, linear history, and restrictions on direct pushes/force pushes.

Reference: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
