=== PROJETO: Remocao de Limites Numericos DBC ===
Data: 30/10/2026
Usuario: Fernando

Artefatos salvos:
- docs/INSTALACAO.md: Instrucoes de instalacao e rollback
- docs/LIMITES-NUMERICOS.md: Mapas de limites numericos
- docs/report_tp.txt: Inventario de 59 sites jrmcTpint
- artifacts/PatchBig.java: Patcher ASM (getMaxTP alterado)
- artifacts/JRMCore-v1.3.51-bignum.jar: Jar patchado (getMaxTP -> Integer.MAX_VALUE)
- artifacts/JRMCore-v1.3.51-original.jar: Jar original para comparacao
- config/jinryuujrmcore_original.cfg: Config original (Max=500, atcm=0.75)
- config/jinryuujrmcore_nova.cfg: Config proposta (Max=100000, atcm=0.015)

Pasta original nao modificada: C:\Users\Fernando\nntdarkmp
Pasta de teste: C:\Users\Fernando\Desktop\NanatsuCore\nntdarkmp_copy
