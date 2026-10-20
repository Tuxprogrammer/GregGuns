# ICBM spec conventions

This spec follows the [Techguns spec conventions](../techguns-spec/CONVENTIONS.md): ground rules, entity sheet, lang and area index. This page lists only where it differs.

## Differences from the Techguns conventions

- **Source citations.** Cite ICBM classes as `path/Class.java:line`, relative to `src/main/java/icbm/classic/` at commit `9c1d985a`. The link script doesn't know ICBM paths, so use permalinks where a link helps.
- **MIT-portable sections.** A formula or algorithm may be ported under the [code policy](README.md#code-policy). Head its section `MIT-portable` and give a permalink pinned to `9c1d985a`. The section still describes the algorithm in prose or pseudocode, so the reader can judge it without opening the source.
- **Assets are catalog depth.** Each texture, model, sound and lang key gets one catalog row: path, size or duration, and what uses it. Full asset entries (palette, layout, envelope) are written only for assets GregGuns decides to keep.
- **Version differences.** When ICBM 2 or 1.7.10 does something notably different, link the appendix sheet ([A1 or A2](README.md#appendices)) instead of describing it inline.
- **Bug IDs** are `ICBM-<area>-<nn>`. Severity is one of crash, gameplay, visual or cosmetic, as in the Techguns spec. Each bug notes whether it is a 1.12-only problem (it doesn't matter to a 1.7.10 rebuild) or a logic bug we could inherit.
- **1.12-only mechanics.** Mark capabilities, blockstates, JSON models, the registry events and Forge Energy as *1.12-only mechanism*. Describe the behavior they produce, not the plumbing, because GregGuns rebuilds that plumbing on 1.7.10 and GT.
