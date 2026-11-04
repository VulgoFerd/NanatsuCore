# AI Collaboration Bridge - Design Document

## Project Overview
Build a local multi-agent collaboration system enabling OpenCode and Claude Code to work together on software projects through a web-based interface with shared memory, real-time communication, and security controls.

## Environment Analysis
- **Python**: 3.11.9
- **Node.js**: v26.4.0
- **npm**: 12.0.1
- **OpenCode**: v2.2.5 (supports `opencode run --format json`, `opencode serve`)
- **Claude Code**: v2.1.314 (supports `claude -p --output-format json`)

## Architecture

### Core Components
1. **Central Coordinator** - Python async service managing agents, tasks, rounds, context sharing
2. **OpenCode Agent** - Wrapper around `opencode run --format json` CLI
3. **Claude Code Agent** - Wrapper around `claude -p --output-format json` CLI
4. **Communication System** - WebSocket-based real-time messaging with SQLite persistence
5. **Web Frontend** - Single-page application with dashboard, chat, task manager, controls
6. **Security Layer** - Path validation, Git-backed backups, approval workflows

### Data Flow
```
User → Web UI → Coordinator → Agent (OpenCode/Claude) → Coordinator → Web UI
                    ↓
              SQLite Database (projects, conversations, messages, tasks, evidence, decisions)
```

## Database Schema (SQLite)

### Tables
- `projects` - Project metadata, working directory, settings
- `conversations` - Conversation threads per project/task
- `messages` - Individual messages with sender, recipient, content, references, state
- `tasks` - Task definitions, assignments, status, limits
- `evidence` - File references, diffs, test results, investigation outputs
- `decisions` - Recorded decisions with rationale
- `modifications` - File change history with diffs and approvals
- `errors` - Error logs with context
- `test_runs` - Test execution records
- `agent_sessions` - Active agent sessions with state

## Agent Integration Strategy

### OpenCode Agent
- Use `opencode run --format json --session <id> --prompt "<message>"`
- Maintain session IDs per project/task for context continuity
- Parse JSON output for structured responses

### Claude Code Agent
- Use `claude -p --output-format json --session-id <uuid> --add-dir <project_dir> "<prompt>"`
- Use `--allowed-tools` to restrict to safe operations
- Maintain session IDs for context continuity

## Communication Protocol

### Message Types
- `task_assigned` - Coordinator → Agent
- `agent_response` - Agent → Coordinator
- `agent_to_agent` - Agent → Agent (via Coordinator)
- `clarification_request` - Agent → Agent
- `evidence_shared` - Agent → All
- `review_request` - Agent → Agent
- `implementation_request` - Coordinator → Implementer Agent
- `review_complete` - Reviewer → Coordinator
- `test_result` - Agent → Coordinator
- `user_approval_required` - Coordinator → User
- `user_decision` - User → Coordinator
- `round_complete` - Coordinator → All
- `task_complete` - Coordinator → User

## Execution Controls
- Configurable max rounds per task (default: 10)
- Configurable timeout per agent (default: 5 min)
- Configurable timeout per task (default: 30 min)
- Max messages per discussion (default: 50)
- Estimated token/cost tracking
- Graceful cancellation with cleanup

## Security Model
- **Path confinement**: Agents restricted to project working directory
- **Git integration**: Auto-commit before changes, diff recording
- **Backup system**: Timestamped backups before modifications
- **Approval workflow**: Destructive operations require explicit user approval
- **No credential sharing**: Each agent uses its own authentication

## Frontend Structure
- Single HTML file with embedded CSS/JS (or separate files)
- WebSocket connection to backend
- Real-time updates via WebSocket
- REST API for initial load and configuration

## Implementation Phases

### Phase 1: Foundation
- Project structure, database, basic coordinator
- Agent wrapper classes with CLI integration
- Basic WebSocket server

### Phase 2: Communication & Coordination
- Message routing, conversation management
- Round control, limit enforcement
- Task lifecycle management

### Phase 3: Frontend
- Dashboard, chat, task manager, controls
- Real-time updates

### Phase 4: Security & Polish
- Path validation, Git integration, backups
- Approval workflows
- Error recovery, logging

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Backend | Python 3.11 + asyncio + aiohttp | Native async, good WebSocket support |
| Database | SQLite (aiosqlite) | Zero-config, portable, sufficient for local use |
| Agent Integration | CLI subprocess with JSON I/O | Official, stable, no API keys needed |
| Frontend | Vanilla HTML/CSS/JS | No build step, portable, fast |
| Session Management | Per-project persistent sessions | Context continuity across restarts |
| Message Format | JSON over WebSocket | Structured, parseable, extensible |

## Approaches Considered

### Approach 1: CLI Subprocess (Selected)
- Direct CLI invocation with JSON I/O
- Pros: Official, no API costs, uses existing auth, simple
- Cons: Process overhead, no streaming (except Claude stream-json)

### Approach 2: OpenCode HTTP API + Claude SDK
- Pros: Streaming, more control
- Cons: OpenCode API undocumented/unstable, Claude SDK requires API key

### Approach 3: MCP Servers
- Pros: Standard protocol
- Cons: Overkill, both tools have CLI that works well

**Selected**: Approach 1 - CLI subprocess with JSON output. Most reliable, zero-cost, uses existing authentication.

## Open Questions for User
1. Default project directory for new projects?
2. Preferred port for web UI (default: 8765)?
3. Should the system auto-start OpenCode/Claude background services?
4. Any specific models to default to for each agent?