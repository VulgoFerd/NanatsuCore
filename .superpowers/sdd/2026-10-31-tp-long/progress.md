# Ledger — TP 64 bits (2026-10-31)

Plano: `docs/superpowers/plans/2026-10-31-tp-long.md`
Spec: `docs/superpowers/specs/2026-10-31-tp-long-design.md`

## Rulings de setup

- Não há git no projeto. Sem worktree, sem commit. Entrega = `artifacts/<jar>-<tag>.jar`
  + SHA-256 em `artifacts/SHA256SUMS.txt`.
- Base do JRMCore: `artifacts/JRMCore-v1.3.51-maxTP-clean.jar`
  (já contém o fix de `getMaxTP`), SHA-256 `B29C0D74...09E864`.
- DBC e npcdbc: cópias dos originais de `nntdarkmp_copy/mods/`, nunca patchados antes.
- Rollback dos três jars é tudo-ou-nada.

## Progresso

| Task | Estado | Evidência |
|---|---|---|
| 1 | **complete** | VerifyJar valida frames + refs `curTP:I`; baseline rodado no maxTP-clean: 345 classes, 3677 métodos, 0 frames inválidos, 21 refs `curTP:I` com `--expect-long-curTP` (exit 1) |
| 2 | **em andamento** | Escrevendo `tools/TpPatch.java` — patcher ASM para storage 64-bit em JRMCoreH (parcial, precisa completar) |

## Task 1 — Detalhes completados

- `tools/VerifyJar.java`: check de frames (COMPUTE_FRAMES com getCommonSuperClass→Object) + lista refs `curTP` + flag `--expect-long-curTP`
- `tools/FrameCheck.java`: aceita jar inteiro ou classe única
- Baseline registrado em `artifacts/SHA256SUMS.txt` (3 jars originais)
- ClassDiff confirmado reprodutível (0 classes diferentes self-vs-self)

## Task 2 — Estado atual

`tools/TpPatch.java` **iniciado mas incompleto**. O arquivo existe em `C:\Users\Fernando\Desktop\NanatsuCore\tools\TpPatch.java` mas precisa ser finalizado com todos os patches:

**Faltando completar no TpPatch.java:**

1. **JRMCoreH** — métodos auxiliares (tpShort precisa correção de stack), barreira setInt, handleStats2, expPls, jrmcExp (2 blocos), tpalgn (2 blocos)
2. **PacHanS** — 8 sítios de ganho → addTP/addTPToPlayer (handleData 993-1000, handleQuad 1603-1610 e 1924-1931)
3. **PacHanC** — handleData dat11 parse (Integer.parseInt → Long.parseLong + clampTP)
4. **ComTickH** — dat11 (getTPTotal + append(J)) + migração (jrmcTpint → jrmcTplong)
5. **PStats2** — curTP:I → curTP:J, construtor (JIB[I)V, toBytes writeLong, fromBytes readLong
6. **PStats2$Handler** — handleClientMessage para long
7. **GUI classes** (3) — 21 sítios display: getstatic curTP:I + I2L + numSep → getstatic curTPLong:J + tpShort
8. **Patch genérico** — 13 sítios NBT diretos "jrmcTpint" → applyTPWindow

## Próximos passos para próxima sessão

```bash
# 1. Completar TpPatch.java com todos os patches acima
# 2. Compilar
cd C:\Users\Fernando\AppData\Local\Temp\opencode\dbc-re
javac --release 8 -cp "asm.jar;asm-tree.jar;asm-util.jar" -d classes ..\tools\TpPatch.java

# 3. Gerar jar tp-long
java -cp "classes;asm.jar;asm-tree.jar;asm-util.jar" TpPatch ^
  "C:\Users\Fernando\Desktop\NanatsuCore\artifacts\JRMCore-v1.3.51-maxTP-clean.jar" ^
  "C:\Users\Fernando\Desktop\NanatsuCore\artifacts\JRMCore-v1.3.51-tp-long.jar"

# 4. Validar
java -cp "classes;asm.jar;asm-tree.jar;asm-util.jar" VerifyJar "C:\Users\Fernando\Desktop\NanatsuCore\artifacts\JRMCore-v1.3.51-tp-long.jar" --expect-long-curTP
java -cp "classes;asm.jar;asm-tree.jar;asm-util.jar" FrameCheck "C:\Users\Fernando\Desktop\NanatsuCore\artifacts\JRMCore-v1.3.51-tp-long.jar"
java -cp "classes;asm.jar;asm-tree.jar;asm-util.jar" ClassDiff "C:\Users\Fernando\Desktop\NanatsuCore\artifacts\JRMCore-v1.3.51-maxTP-clean.jar" "C:\Users\Fernando\Desktop\NanatsuCore\artifacts\JRMCore-v1.3.51-tp-long.jar"
```

## Arquivos de referência em `C:\Users\Fernando\AppData\Local\Temp\opencode\dbc-re\`

- `h-full.txt` — javap -p -c de JRMCoreH (setInt, expPls, jrmcExp, tpalgn, Stats2, numSep, getMaxTP)
- `pacs-full.txt` — javap -p -c de JRMCorePacHanS (handleData, handleQuad, offsets de ganho/clamp)
- `pacc-full.txt` — javap -p -c de JRMCorePacHanC (handleStats2, parse dat11)
- `tick-full.txt` — javap -p -c de JRMCoreComTickH (migração 2134-2160, ganho 2396-2409, dat11 4124-4144)
- `tp-report.txt` — inventário legível de 566 linhas com todos os sítios de TP ±3 instruções
- `curTP-sites.txt` — 68 ocorrências de `getstatic curTP:I` categorizadas por classe/método/offset