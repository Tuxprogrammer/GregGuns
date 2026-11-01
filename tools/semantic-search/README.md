# Semantic search

Gives Claude Code a `semantic_search` tool over this repository. It finds files by meaning, not by exact name. It covers:

- the Techguns, GregGuns and ICBM specs;
- the decision log, design records and roadmaps;
- the tool READMEs;
- the Java sources and Python tools;
- the `en_US.lang` player text.

Use it when you don't know which file or identifier to look for, such as "where is gun recoil decided", "which spec covers ammo crafting" or "is there already a decision about silo hatches". It is best at "does this exist anywhere" questions, which grep answers badly. Use grep for exact names.

The tool is ported from the same owner's TrainHorizons repository (MIT), adapted for GregGuns.

## How it works

- **Corpus.** Every tracked `.md`, `.java`, `.py`, `.gradle`, `.properties`, `.toml`, `.lang` and `.lua` file, except `THIRD-PARTY-NOTICES.md`.
  - Only tracked files are sent, so untracked notes and everything under `reference/` never leave the machine.
  - JSON data (models, recipes, quests) and the structure layout `.txt` files are left out.
- **Chunks.** Each kind of file is split differently:
  - Markdown at headings. Each chunk keeps its heading path, for example `Decisions > D290`.
  - Java at the members of each top-level type, ignoring braces inside strings and comments.
  - Python at top-level `def` and `class`.
  - Other files are cut into pieces by size.

  Small pieces are merged and long ones split, so a chunk holds at most 6,000 characters. Each chunk is embedded with its path and title.
- **Embeddings.** `voyageai/voyage-code-4` (1,024 dimensions) through OpenRouter, with `input_type` set to `document` or `query` and provider data collection denied. A refresh embeds only chunks whose text changed.
- **Index.** `reference/semantic-search/voyageai_voyage-code-4.npz` in the main working tree. It is git-ignored, written atomically, and shared by every worktree. Vectors are normalized, so a search is one matrix product.
- **Ranking.** Cosine similarity, with at most three hits per file. The code model scores Java above the prose that explains it, so a mixed search keeps about a third of its slots for docs. Use `kind` to search one kind only.

## Setup

1. Put an OpenRouter key in `.env` at the repository root (git-ignored): `OPENROUTER_API_KEY=...`. If the `OPENROUTER_API_KEY` environment variable is set, it is used instead.
2. Build the first index: `python3 -I tools/semantic-search/index.py`.
3. [`.mcp.json`](../../.mcp.json) registers the server for Claude Code. Approve it once on each machine:
   - accept the prompt when `claude` starts; or
   - add `"enabledMcpjsonServers": ["semantic-search"]` to the git-ignored `.claude/settings.local.json`.

The server refreshes the index in the background when it starts, and again when a search runs more than 10 minutes after the last refresh. A lock file keeps concurrent sessions from building at the same time.

## Tools exposed to Claude Code

| Tool | What it does |
|---|---|
| `semantic_search` | Takes `query` (natural language), plus optional `k` (1–25, default 8), `kind` (`any`, `docs`, `java`, `python`, `lang`, `build`) and a `path` prefix. Returns `[n] path:start-end · score · title` and a snippet of up to 1,500 characters. `build` covers the build files and the other non-code files, such as the Lua scripts. |
| `semantic_index_status` | Chunk and file counts, the commit the index was built at compared with HEAD, and whether a refresh is running. |
| `semantic_reindex` | Refreshes now and waits for it to finish. |

## Command line

```sh
python3 -I tools/semantic-search/index.py                 # incremental build
python3 -I tools/semantic-search/index.py --stats         # chunk counts and estimated cost, no API calls
python3 -I tools/semantic-search/index.py --query "where is gun recoil decided" -k 5 --kind java
python3 -I tools/semantic-search/test_semsearch.py        # offline tests (no API calls)
```

## For agent briefs

Agents with specific files in their brief tend to grep and never search. If you want an agent to use this tool, name a concrete use in its brief, for example: "before adding a decision or a spec section, run `semantic_search` for an existing one."

## Limits

- The index follows the main working tree. A worktree's uncommitted edits are not in it, so read the file a hit points to before acting on it.
- Scores compare well within a single kind, but across kinds they are only a rough guide.
- Changing `MODEL` in `semsearch.py` starts a new index file with a full re-embed.
