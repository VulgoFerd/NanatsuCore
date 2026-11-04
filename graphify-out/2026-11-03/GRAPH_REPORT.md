# Graph Report - NanatsuCore  (2026-11-03)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 879 nodes · 2112 edges · 56 communities (18 shown, 38 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 18 edges (avg confidence: 0.86)
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
- json
- aggregate_benchmark.py
- tree
- cli.json
- zip
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
- .tryCompute
- AUDITORIA_DBC/InspectPStats2.java
- AUDITORIA_DBC/InspectTpSites.java
- interface
- DebugMethod
- FixMaxTP
- exportar.ps1

## God Nodes (most connected - your core abstractions)
1. `TpPatch` - 39 edges
2. `TpPatch` - 38 edges
3. `PatchBig` - 23 edges
4. `collectUnitErrors()` - 16 edges
5. `TPAlgoTest` - 14 edges
6. `MCPConnection` - 13 edges
7. `validate()` - 13 edges
8. `preflightJsonText()` - 12 edges
9. `run_loop()` - 12 edges
10. `validateAttempts()` - 11 edges

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

## Communities (56 total, 38 thin omitted)

### Community 0 - "enumeration"
Cohesion: 0.13
Nodes (4): DumpRefs, MemberRefs, InspectClass, MemberRefs

### Community 1 - "validate-coverage-ledger.cjs"
Cohesion: 0.06
Nodes (65): ATTEMPT_FIELDS, ATTEMPT_STATUSES, canonicalCoverageId(), collectUnitErrors(), createErrorList(), encodeCanonicalRef(), escapeUnsafeDiagnosticCharacters(), fs (+57 more)

### Community 2 - "validate-findings.cjs"
Cohesion: 0.06
Nodes (54): canonicalKey(), append(), encode(), codePointLength(), collect(), collectDataLimitErrors(), collectFindingSemanticErrors(), collectSchemaErrors() (+46 more)

### Community 3 - "evaluation.py"
Cohesion: 0.05
Nodes (13): create_connection(), MCPConnection, MCPConnectionHTTP, MCPConnectionSSE, MCPConnectionStdio, agent_loop(), evaluate_single_task(), extract_xml_content() (+5 more)

### Community 4 - "org.objectweb.asm.tree.AbstractInsnNode"
Cohesion: 0.08
Nodes (8): Sites, TpReport, TpShapes, DebugMethodFull, DumpRefs, Sites, TpReport, TpShapes

### Community 7 - "generate_review.py"
Cohesion: 0.12
Nodes (9): build_run(), embed_file(), find_runs(), _find_runs_recursive(), generate_html(), get_mime_type(), load_previous_iteration(), main() (+1 more)

### Community 9 - "run_loop.py"
Cohesion: 0.12
Nodes (7): generate_html(), find_project_root(), run_eval(), main(), run_loop(), split_eval_set(), parse_skill_md()

### Community 10 - "artifacts/PatchBig.java"
Cohesion: 0.11
Nodes (6): SafeClassWriter, ClassWriter, SafeClassWriter, SafeClassWriter, FrameSafeClassWriter, FrameSafeClassWriter

### Community 12 - "task.py"
Cohesion: 0.16
Nodes (3): Task, TaskModel, TaskStatus

### Community 15 - "run_eval.py"
Cohesion: 0.17
Nodes (3): _kill_port(), _call_claude(), run_single_query()

### Community 16 - "package_skill.py"
Cohesion: 0.20
Nodes (4): main(), package_skill(), should_exclude(), validate_skill()

### Community 19 - "json"
Cohesion: 0.29
Nodes (5): main(), main(), improve_description(), main(), main()

### Community 20 - "aggregate_benchmark.py"
Cohesion: 0.20
Nodes (5): aggregate_results(), calculate_stats(), generate_benchmark(), generate_markdown(), load_run_results()

### Community 21 - "tree"
Cohesion: 0.23
Nodes (3): InspectChain, FindStats2Refs, InspectChain

### Community 22 - "cli.json"
Cohesion: 0.18
Nodes (10): animations, diffs, wrap, $schema, session, scrollbar, sidebar, thinking (+2 more)

### Community 34 - "interface"
Cohesion: 0.33
Nodes (5): interface, display_name, short_description, policy, allow_implicit_invocation

### Community 35 - "interface"
Cohesion: 0.33
Nodes (5): interface, display_name, short_description, policy, allow_implicit_invocation

### Community 49 - "interface"
Cohesion: 0.50
Nodes (3): interface, display_name, short_description

## Knowledge Gaps
- **38 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TpPatch` connect `TpPatch` to `enumeration`, `artifacts/PatchBig.java`?**
  _High betweenness centrality (0.067) - this node is a cross-community bridge._
- **Should `enumeration` be split into smaller, more focused modules?**
  _Cohesion score 0.13473684210526315 - nodes in this community are weakly interconnected._
- **Why does `TpPatch` connect `TpPatch` to `enumeration`, `artifacts/PatchBig.java`?**
  _High betweenness centrality (0.066) - this node is a cross-community bridge._
- **Should `validate-coverage-ledger.cjs` be split into smaller, more focused modules?**
  _Cohesion score 0.06316590563165905 - nodes in this community are weakly interconnected._
- **Why does `PatchBig` connect `PatchBig` to `artifacts/PatchBig.java`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **Should `validate-findings.cjs` be split into smaller, more focused modules?**
  _Cohesion score 0.05789235639981909 - nodes in this community are weakly interconnected._
- **Should `evaluation.py` be split into smaller, more focused modules?**
  _Cohesion score 0.05263157894736842 - nodes in this community are weakly interconnected._