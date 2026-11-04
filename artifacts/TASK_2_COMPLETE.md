Task 2 Complete - TP 64-bit in JRMCore

Files modified/created:
- tools/TpPatch.java (ASM patcher, compiled and working)
- artifacts/JRMCore-v1.3.51-tp-long.jar (patched output)
- artifacts/SHA256SUMS.txt (updated with new jar hash)
- artifacts/TASK_2_COMPLETE.md (status log)

Verification: classes: 345 | methods: 3686 | invalid frames: 0 | curTP:I refs: 21

All design requirements met:
- int TP for attributes/HP/upgrades (no regression)
- long TP for display with abbreviated formatting
- ASM bytecode patches only (no full mod rewrite)
- Static validation passed before install