# Relatório de bugs — nntdark-CustomNPC-Plus-1.11.1.jar

Instância analisada: `C:\Users\Fernando\nntdarkmp` (cliente Forge 1.7.10, servidor NNTDark/RedeDark).

> O jar `nntdark-CustomNPC-Plus-1.11.1.jar` é um build modificado por terceiros sobre o `CustomNPC-Plus-1.11.1.jar` oficial (KAMKEEL, 13/03/2026). O oficial tem 17.0 MB; o nntdark tem 23.6 MB — contém add-ons próprios (npcdbc, RPresenceCore-NNTDark, ClientCore, CrashAssistant).

---

## 1. Bugs confirmados na instância

### 1.1 Conflito de ID de encantamento com Ars Magica 2 (a cada boot)
- **Evidência:** `logs/CustomNPCs-1.log:2`
  ```
  java.lang.IllegalArgumentException: Duplicate enchantment id! class noppes.npcs.enchants.EnchantDamage and class am2.enchantments.EnchantMagicResist Enchantment ID:100
  ```
- **Causa:** `config/CustomNpcs.cfg:28` → `EnchantStartId=100`, que colide com o AM2 (ambos usam slot 100).
- **Impacto:** log SEVERE em toda inicialização; o jogo continua, mas encantamentos do CNPC+ podem não registrar corretamente.
- **Fix:** trocar `EnchantStartId` para um slot livre (ex.: 199) e reiniciar.

### 1.2 Crash de render de item nulo — "Rendering item" (2x em 24/08/2026)
- **Evidência:** `crash-reports/crash-2026-08-24_21.09.56-client.txt` e `crash-2026-08-24_21.39.42-client.txt`
  - NPE em `net.minecraft.item.ItemStack.func_77960_j(ItemStack.java:217)` no render da GUI criativa.
  - Stack com `mixins.customnpcs.json:MixinItemStack`.
- **Causa:** item nulo/corrompido (NBT) entrando no caminho de render do `MixinItemStack` do CNPC+ (GUI criativa). Mesmo caminho de render é usado pela GUI do trader (ver §2.2).
- **Impacto:** crash puro do cliente ao renderizar slot vazio.
- **Fix:** limpar a aba/`NEI` de itens com NBT corrompido; o wrapper do gerenciador de itens do CustomNPC+ deve estar deletado. Se recorrente, reportar no issue tracker.

### 1.3 Skin por URL falha com NPE — ImageBufferDownloadAlt (49+ ocorrências)
- **Evidência:** logs (picos em 2026-08-27-1, 2026-09-03-1, 2026-09-09-1)
  - `Couldn't download http texture` → NPE em `noppes.npcs.client.renderer.ImageBufferDownloadAlt.func_78432_a(ImageBufferDownloadAlt.java:30)`.
- **Impacto:** skins de NPCs via URL (imgur etc.) não carregam; spam no log. Não derruba o jogo.
- **Fix:** usar texturas locais/username em vez de URL; atualizar quando KAMKEEL fechar o download-fallback do URL64.

### 1.4 Autocomplete do Script Editor quebra no boot (AnalyzerException)
- **Evidência:** 350 linhas em logs de boot
  - `AnalyzerException: Error at instruction 30371: null` (MixinExtras/LocalVariableDiscriminator) dentro do `ClassIndex.buildPackageIndex`.
- **Impacto:** o indexador de classes do editor de scripts falha, o que pode dar os "Script Syntax Bugs" e quebra de autocomplete que a 1.11.1 dizia ter corrigido.
- **Fix:** sem fix público; reportar ao KAMKEEL com log, se o autocomplete não funcionar.

### 1.5 Event bus de scripting falha — ScriptForgeEventHandler (3x)
- **Evidência:** `[Client thread/ERROR]: 12: ASM: noppes.npcs.ScriptForgeEventHandler@... forgeEntity` (logs 1θ/)
- **Impacto:** eventos Forge de entidade (forgeEntity) do scripting podem não ser entregues — afeta scripts de trader/loot que escutam esses eventos.
- **Fix:** verificar se os scripts usam `entityEvent`; se usarem `forgeEntity`, testar sem.

---

## 2. BUGS CONHECIDOS — NPCs de TRADER

### 2.1 #148 KAMKEEL/CustomNPC-Plus — TRADER QUEBRA APÓS MORRER (ABERTO, não corrigido)
> "After dying a few times on a server you cannot use trader npcs and have to relog. Also sometimes npcs just glitch into the ground for some reason."
> URL: https://github.com/KAMKEEL/CustomNPC-Plus/issues/148 (aberta desde 10/03/2025, **sem fix**)

- **Sintoma:** após algumas mortes, o jogador não consegue mais abrir/interagir com traders — precisa relogar.
- **Causa provável:** estado da sessão de GUI do trader/interação ficando inconsistente após respawn/pacote de morte do jogador (cliente move o jogador e quebra sessões de abrir GUI). O "Entity packet loss fix" da 1.11 era para NPCs invisíveis, não para a sessão de trader.
- **Relevância p/ NNTDark:** é o bug de trader mais reportado no CNPC+; a instância tem 7 crash-reports e várias mortes no log, cenário idêntico ao da issue.
- **Mitigação:** manter `EntityRespawn`/death respawn estável; se o jogador prender, `relog`. Reportar no issue tracker do KAMKEEL.

### 2.2 Crash "Rendering item" pode atingir a GUI do trader (visto na instância)
- O NPE de render (→ §1.2) usa o mesmo `MixinItemStack`. Se um item da GUI do trader (item vendido/comprado com NBT) estiver nulo/corrompido, a GUI tende a crashar igual às do criativo (crash 24/08).
- **Mitigação:** não colocar itens com NBT complexo (itens "scripted"/soulstone) na listagem do trader.

### 2.3 Novo stock system (1.11) pode ter bugs de restock
- A 1.11 adicionou **"Stock system: Global or Per-Player modes with configurable restock timers"** para traders.
- **Riscos:** restock por timer global vs por jogador pode divergir (like BO's-Easy-NPC #221: limite reseta ao reabrir a GUI). Sem issue aberta no CNPC+, mas recurso novo → menor cobertura de testes.

### 2.4 Bug pós-respawn: estoque/inventário do trader pode resetar
- Relacionado ao bug de respawn (#101) e ao JobSpawner (#285 — "loses lifecycle tracking after legacy world conversion" — **aberto**, afeta entity replacement em conversões legadas, comum neste servidor).
- Trader configurado por JobSpawner ou que morre e re-spawna pode perder o inventário/estoque.
- **Mitigação:** testar um trader com stock global após respawn; conferir save (`client`/`server` em `./customnpcs/data`).

### 2.5 Scripting de trader (forgeEntity ASM) — falha vista nos logs
- Se scripts de trader usam os eventos `forgeEntity` (→ §1.5), pode não disparar em entidades, pois o `ScriptForgeEventHandler` lança ERROR no ASM no boot.
- **Mitigação:** preferir eventos nativos do CNPC+ (`traderEvent`) a eventos Forge brutos.

### 2.6 Configuração atual do mercado no servidor NNTDark
- `config/CustomNpcPlus/market.cfg`:
  - **Currency:** nome "Coins", `Vault = false` (usa moeda interna), saldo inicial 0.
  - **Auction:** habilitado, duração 24h, 8 slots de trade, snipe protection 2min, expiração de claim 20 dias.
  - **Blacklist:** bedrock, command_block e ferramentas do CNPC+ (npcWand, npcScripter, etc.).
- **Observação:** os avisos de expirado no chat ("Mercado >>> Your product ... has expired") nos logs (2026-08-23-2) são do sistema de Auction do servidor — comportamento normal, não bug.

---

## 3. Referências upstream
- Repo/issues: https://github.com/KAMKEEL/CustomNPC-Plus/issues
- Issues abertas relevantes: #289, #288, #285, #270, #259, #258, #199, #148, #101, #99
- Release 1.11.1 (CurseForge 7753130, 13/03/2026): "Script Editor Fixes + Styling Changes; Improved FONT; Auto Complete / Script Syntax Bugs; Fix Sound Selection GUI Reset; Accept Quest / Marks Data Crash; ..."