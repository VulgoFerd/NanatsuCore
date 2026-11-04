# TP Long Upgrade Progress - 2026-11-04

## Current State
- **JAR**: `artifacts/JRMCore-v1.3.51-tp-long-v15.jar` (SHA-256 `A461651CFF1C6C22BBA77368320B8E129B4CF67B1C120F6BEF610D071F4FA12C`)
- **NPCDBC**: `artifacts/npcdbc-1.2.1-hotfix1-tp-long-v2.jar`
- **Static verification**: `VerifyJar --expect-long-curTP --audit-descriptors` → 346 classes, 3707 methods, 0 invalid frames, 0 unexpected `curTP:I` refs, 0 descriptor mismatches
- **Game test**: installed in `nntdarkmp_copy/mods`, NOT yet validated in-game

## Done since v11 (v12-v15)
- Implemented all stub methods in TpPatch.java:
  - `patchGainSiteJrmcExp` - jrmcExp gain sites (2 sites patched)
  - `patchGainSiteTpalgn` - tpalgn gain sites (2 sites patched)
  - `patchPacHanSHandleTechGain` - handleTech gain sites (2 sites patched)
  - `patchComTickHGain` - ComTickH gain sites
  - `patchComTickHSendPlayerData` - sendPlayerData long TP sync
  - `patchHandleStats2Method` - handled (delegate to PacHanC)
  - `patchHandleStats2` - descriptor changed to long
- Rewrote DBCData.loadFromNBT/saveFromNBT using TpUtil.getTP/writeTP (no stack surgery)
- Fixed `TpUtil.setMirror` with `addTPSafe` (saturating addition)
- Added `TpUtil.writeTP` method with NBT type validation and persistent migration
- PStats2: field `curTP:I` → `curTPLong:J`, constructor slots shifted, toBytes/fromBytes updated
- PacHanC.handleStats2: descriptor updated, local vars shifted, field accesses patched
- PStats2$Handler: patched PacHanC.handleStats2 call descriptor
- HierarchyClassWriter integrated for real COMPUTE_FRAMES (no fallback)
- All anchor-on-removed-instruction insertions eliminated
- TpUtil.class recompiled and re-injected

## Next Steps
1. Launch Minecraft profile **"Nanatsu TEST"** (gameDir `nntdarkmp_copy`) and validate:
   - TP above 2,147,483,647 persists after relog
   - Display shows B/T/Q/Qi suffix above 10B
   - `/jrmctp` accepts values above 2.1B and negatives
   - Attribute upgrade cost is deducted above 2.1B
   - No crash when opening stats / upgrading
2. If crash: collect `nntdarkmp_copy/crash-reports` + `logs/latest.log`, extract failing method with `tools/ExtractMethod`
3. Update `CHANGELOG.md` in `AUDITORIA_DBC` with the 1.0.4 entry
4. Patch NPCDBC similarly

## Commands
```bash
tools/compile_tools.bat      # compile TpPatch/VerifyJar/FrameCheck/ClassDiff
tools/run_patch.bat          # original -> v15
tools/run_verify.bat         # VerifyJar on v15
```
Note: `AUDITORIA_DBC/launch_test.bat` is broken — expanded classpath is 11,247 chars, over the cmd 8,191 limit. Use the Minecraft launcher profile instead.

## Key Files
- `tools/TpPatch.java` - patcher (all stubs implemented)
- `tools/TpUtil.java` - injected helper class (setMirror fixed, writeTP added)
- `tools/out/JinRyuu/JRMCore/TpUtil.class` - compiled helper
- `tools/VerifyJar.java` - static validation (+ descriptor audit)
- `tools/VerifyBytecode.java` - strict frame verification (needs asm-analysis.jar)
- `tools/ExtractMethod.java` - dumps one method's bytecode from a jar
- `artifacts/JRMCore-v1.3.51-original.jar` - baseline
