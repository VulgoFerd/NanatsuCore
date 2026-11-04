# Graph Report - NanatsuCore  (2026-11-03)

## Corpus Check
- 155 files · ~2,600,248 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 5221 file(s) not represented in the graph (top: .vertex 4242, .gz 208, .cfg 157)

## Summary
- 1006 nodes · 2509 edges · 58 communities (19 shown, 39 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 28 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- enumeration
- validate-coverage-ledger.cjs
- validate-findings.cjs
- evaluation.py
- org.objectweb.asm.tree.AbstractInsnNode
- TpPatch
- TpPatch
- generate_review.py
- PatchBig
- run_loop.py
- artifacts/PatchBig.java
- Database
- task.py
- ConversationModel
- TPAlgoTest
- run_eval.py
- package_skill.py
- MessageModel
- ProjectModel
- NBTTagCompound
- aggregate_benchmark.py
- tree
- cli.json
- AUDITORIA_DBC/VerifyClasses.java
- PatchBig
- PatchBig
- VerifyJar
- VerifyJar
- instalar.ps1
- .tryCompute
- VerifyASM.java
- ScanTP
- interface
- interface
- InspectMethods.java
- ClassDiff
- ClassDiff
- DumpMethod
- DiffDescriptors.java
- artifacts/DumpMethod.java
- artifacts/InspectPStats2.java
- artifacts/InspectTpSites.java
- artifacts/VerifyClasses.java
- FixMaxTP
- evaluation.py
- AUDITORIA_DBC/InspectPStats2.java
- AUDITORIA_DBC/InspectTpSites.java
- interface
- DebugMethod
- FixMaxTP
- exportar.ps1
- CurTPContext
- VarUse

## God Nodes (most connected - your core abstractions)
1. `TpPatch` - 63 edges
2. `TpPatch` - 63 edges
3. `PatchBig` - 23 edges
4. `NBTTagCompound` - 20 edges
5. `collectUnitErrors()` - 16 edges
6. `TPAlgoTest` - 14 edges
7. `EntityPlayer` - 14 edges
8. `validate()` - 13 edges
9. `MCPConnection` - 13 edges
10. `preflightJsonText()` - 12 edges

## Surprising Connections (you probably didn't know these)
- `test_database_initializes_all_tables()` --uses--> `Database`  [INFERRED]
  tests/test_database.py → src/db/database.py
- `SafeClassWriter` --inherits--> `ClassWriter`  [EXTRACTED]
  artifacts/PatchBig.java → AUDITORIA_DBC/FrameCheck.java
- `FrameSafeClassWriter` --inherits--> `ClassWriter`  [EXTRACTED]
  tools/TpPatch.java → AUDITORIA_DBC/FrameCheck.java
- `unit()` --calls--> `canonicalCoverageId()`  [EXTRACTED]
  .agents/skills/security-audit/validate-coverage-ledger.test.cjs → .agents/skills/security-audit/validate-coverage-ledger.cjs
- `errorsFor()` --calls--> `validateDocument()`  [EXTRACTED]
  .agents/skills/security-audit/validate-coverage-ledger.test.cjs → .agents/skills/security-audit/validate-coverage-ledger.cjs

## Import Cycles
- None detected.

## Communities (58 total, 39 thin omitted)

### Community 0 - "enumeration"
Cohesion: 0.11
Nodes (6): MemberRefs, AnalyzeClass, InspectClass, MemberRefs, ScanLdc, ScanRefs

### Community 1 - "validate-coverage-ledger.cjs"
Cohesion: 0.06
Nodes (65): ATTEMPT_FIELDS, ATTEMPT_STATUSES, canonicalCoverageId(), collectUnitErrors(), createErrorList(), encodeCanonicalRef(), escapeUnsafeDiagnosticCharacters(), fs (+57 more)

### Community 2 - "validate-findings.cjs"
Cohesion: 0.06
Nodes (54): canonicalKey(), append(), encode(), codePointLength(), collect(), collectDataLimitErrors(), collectFindingSemanticErrors(), collectSchemaErrors() (+46 more)

### Community 3 - "evaluation.py"
Cohesion: 0.07
Nodes (4): MCPConnection, MCPConnectionHTTP, MCPConnectionSSE, MCPConnectionStdio

### Community 4 - "org.objectweb.asm.tree.AbstractInsnNode"
Cohesion: 0.07
Nodes (9): DumpRefs, Sites, TpReport, TpShapes, DebugMethodFull, DumpRefs, Sites, TpReport (+1 more)

### Community 7 - "generate_review.py"
Cohesion: 0.12
Nodes (9): build_run(), embed_file(), find_runs(), _find_runs_recursive(), generate_html(), get_mime_type(), load_previous_iteration(), main() (+1 more)

### Community 9 - "run_loop.py"
Cohesion: 0.11
Nodes (9): generate_html(), main(), _call_claude(), improve_description(), main(), main(), run_loop(), split_eval_set() (+1 more)

### Community 10 - "artifacts/PatchBig.java"
Cohesion: 0.10
Nodes (6): SafeClassWriter, FrameCheck, ClassWriter, SafeClassWriter, FrameSafeClassWriter, FrameSafeClassWriter

### Community 12 - "task.py"
Cohesion: 0.16
Nodes (3): Task, TaskModel, TaskStatus

### Community 15 - "run_eval.py"
Cohesion: 0.18
Nodes (5): _kill_port(), find_project_root(), main(), run_eval(), run_single_query()

### Community 16 - "package_skill.py"
Cohesion: 0.20
Nodes (4): main(), package_skill(), should_exclude(), validate_skill()

### Community 19 - "NBTTagCompound"
Cohesion: 0.13
Nodes (6): TpUtil, JRMCoreH, Entity, EntityPlayer, NBTTagCompound, TpUtil

### Community 20 - "aggregate_benchmark.py"
Cohesion: 0.20
Nodes (6): aggregate_results(), calculate_stats(), generate_benchmark(), generate_markdown(), load_run_results(), main()

### Community 21 - "tree"
Cohesion: 0.15
Nodes (6): FindStats2Refs, InspectChain, FindStats2Refs, InspectChain, SafeClassWriter, ExtractMethod

### Community 22 - "cli.json"
Cohesion: 0.18
Nodes (10): animations, diffs, wrap, $schema, session, scrollbar, sidebar, thinking (+2 more)

### Community 34 - "interface"
Cohesion: 0.33
Nodes (5): interface, display_name, short_description, policy, allow_implicit_invocation

### Community 35 - "interface"
Cohesion: 0.33
Nodes (5): interface, display_name, short_description, policy, allow_implicit_invocation

### Community 46 - "evaluation.py"
Cohesion: 0.14
Nodes (9): create_connection(), agent_loop(), evaluate_single_task(), extract_xml_content(), main(), parse_env_vars(), parse_evaluation_file(), parse_headers() (+1 more)

### Community 49 - "interface"
Cohesion: 0.50
Nodes (3): interface, display_name, short_description

## Knowledge Gaps
- **39 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TpPatch` connect `TpPatch` to `enumeration`, `artifacts/PatchBig.java`?**
  _High betweenness centrality (0.101) - this node is a cross-community bridge._
- **Should `enumeration` be split into smaller, more focused modules?**
  _Cohesion score 0.10842490842490843 - nodes in this community are weakly interconnected._
- **Why does `TpPatch` connect `TpPatch` to `enumeration`, `artifacts/PatchBig.java`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Should `validate-coverage-ledger.cjs` be split into smaller, more focused modules?**
  _Cohesion score 0.06316590563165905 - nodes in this community are weakly interconnected._
- **Why does `PatchBig` connect `PatchBig` to `artifacts/PatchBig.java`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Should `validate-findings.cjs` be split into smaller, more focused modules?**
  _Cohesion score 0.05789235639981909 - nodes in this community are weakly interconnected._
- **Should `evaluation.py` be split into smaller, more focused modules?**
  _Cohesion score 0.0748663101604278 - nodes in this community are weakly interconnected._