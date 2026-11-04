# SDD ledger — plan: docs/superpowers/plans/2026-10-31-ai-collaboration-bridge.md

Task 1: pending
Task 2: pending
Task 3: pending
Task 4: pending
Task 5: pending
Task 6: pending
Task 7: pending
Task 8: pending
Task 9: pending

---

## Pre-flight Scan Results

| Pair | Shared Interface/File | Task A Produces | Task B Consumes | Finding |
|------|----------------------|-----------------|-----------------|---------|
| 1→2 | Database, models | Database class, 10 model classes | Database, model classes | ✅ Clean dependency |
| 1→3 | Database, models | Database class, 10 model classes | Database, model classes | ✅ Clean dependency |
| 1→4 | Database, models | Database class, 10 model classes | Database, model classes | ✅ Clean dependency |
| 1→5 | Database | Database class | Database | ✅ Clean dependency |
| 1→7 | All modules | Database, models, agents, coordinator, server, security, frontend | All previous modules | ✅ Clean dependency |
| 2→3 | Agent implementations | Agent base, OpenCodeAgent, ClaudeAgent, AgentResult | Agent implementations | ✅ Clean dependency |
| 2→7 | Agent implementations | Agent base, OpenCodeAgent, ClaudeAgent | All previous modules | ✅ Clean dependency |
| 3→4 | Coordinator | Coordinator, TaskManager, RoundController, ContextManager | Coordinator, Database, models | ✅ Clean dependency |
| 3→7 | Coordinator | Coordinator + submodules | All previous modules | ✅ Clean dependency |
| 4→6 | WebSocket API, REST API | WebSocketServer, MessageRouter, API routes | WebSocket API, REST API | ✅ Clean dependency |
| 4→7 | Server modules | WebSocketServer, MessageRouter, API routes | All previous modules | ✅ Clean dependency |
| 5→7 | Security modules | PathValidator, GitManager, BackupManager, ApprovalWorkflow | All previous modules | ✅ Clean dependency |
| 5→3 | PathValidator | PathValidator | (via Coordinator→ContextManager) | ⚠️ Indirect: Coordinator should use PathValidator for file ops |
| 7→8 | Full app | Runnable app | Full application stack | ✅ Clean dependency |
| 7→9 | Complete codebase | Runnable app + config | Complete codebase | ✅ Clean dependency |

### Task Internal Consistency Checks

| Task | Tests Specified | Code Specified | Consistency |
|------|-----------------|----------------|-------------|
| 1 | Database.initialize(), get_connection(); 10 model classes with CRUD | Schema.sql with 10 tables; Database class; model classes with CRUD | ✅ Match |
| 2 | OpenCodeAgent.execute(), ClaudeAgent.execute() → AgentResult(success, output, error, metadata) | Agent base with execute(); AgentResult dataclass; two implementations | ✅ Match |
| 3 | Coordinator.create_task() → Task(id, status); get_task_messages() | Coordinator with start_task, process_round, handle_agent_response, etc. | ✅ Match |
| 4 | WebSocketServer.start(), stop(), port; ping/pong | WebSocketServer with start, broadcast, handle_client | ✅ Match |
| 5 | PathValidator.validate(path) → bool | PathValidator with validate() | ✅ Match |

### Rulings

1. **Task 3 should consume PathValidator from Task 5** — Coordinator's file operations must use PathValidator. Since Task 3 runs before Task 5, add PathValidator import in Task 3 after Task 5 completes, or create a minimal PathValidator stub in Task 1 that Task 5 replaces. **Ruling: Create minimal PathValidator in Task 1 (src/security/path_validator.py with basic validation), Task 5 enhances it.** — Spec mandates path confinement; early availability prevents retrofitting.

2. **Task 4 WebSocketServer constructor expects Coordinator** — Task 3 produces Coordinator, Task 4 consumes it. Order is correct (Task 3 before Task 4). No change needed.

3. **Task 6 frontend served at `/` by Task 7 main.py** — Task 7 must mount static files from src/frontend. Task 6 creates files, Task 7 serves them. Order correct.

4. **Global Constraints check** — All tasks specify Windows-compatible paths, subprocess handling. Task 2 explicitly uses Windows-compatible subprocess. ✅ Compliant.

5. **No external API dependencies** — All tasks use CLI subprocesses, SQLite, aiohttp. ✅ Compliant.

6. **Approval workflow integration** — Task 5 produces ApprovalWorkflow, Task 3 Coordinator needs to call request_user_approval(). Task 3 runs before Task 5. **Ruling: Task 3 Coordinator defines approval callback interface; Task 5 implements it; Task 7 wires them together.** — Spec mandates approval for destructive operations.

Pre-flight scan complete. Proceeding to Task 1.