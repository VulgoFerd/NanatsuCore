-- Database Schema for AI Collaboration Bridge
-- 10 tables with proper indexes

-- Projects table
CREATE TABLE IF NOT EXISTS projects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    working_directory TEXT NOT NULL,
    description TEXT,
    settings TEXT DEFAULT '{}',  -- JSON settings
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_projects_name ON projects(name);
CREATE INDEX IF NOT EXISTS idx_projects_working_directory ON projects(working_directory);

-- Conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    title TEXT,
    task_id INTEGER,  -- Optional link to a task
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_conversations_project_id ON conversations(project_id);
CREATE INDEX IF NOT EXISTS idx_conversations_task_id ON conversations(task_id);

-- Messages table
CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conversation_id INTEGER NOT NULL,
    sender TEXT NOT NULL,  -- 'user', 'opencode', 'claude', 'coordinator'
    recipient TEXT,  -- 'user', 'opencode', 'claude', 'coordinator', 'all'
    content TEXT NOT NULL,
    message_type TEXT NOT NULL DEFAULT 'text',  -- 'text', 'task_assigned', 'agent_response', 'clarification_request', 'evidence_shared', etc.
    refs TEXT DEFAULT '[]',  -- JSON array of references (file paths, evidence IDs, etc.)
    state TEXT DEFAULT 'sent',  -- 'sent', 'delivered', 'read', 'failed'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender);
CREATE INDEX IF NOT EXISTS idx_messages_recipient ON messages(recipient);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at);

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'pending',  -- 'pending', 'in_progress', 'review', 'completed', 'failed', 'cancelled'
    assigned_agents TEXT DEFAULT '[]',  -- JSON array of agent names
    max_rounds INTEGER DEFAULT 10,
    current_round INTEGER DEFAULT 0,
    priority INTEGER DEFAULT 0,
    metadata TEXT DEFAULT '{}',  -- JSON for additional data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks(created_at);

-- Evidence table
CREATE TABLE IF NOT EXISTS evidence (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER,
    project_id INTEGER NOT NULL,
    conversation_id INTEGER,
    message_id INTEGER,
    title TEXT NOT NULL,
    type TEXT NOT NULL,  -- 'file_reference', 'diff', 'test_result', 'investigation_output', 'log'
    content TEXT,  -- JSON content or text
    file_path TEXT,  -- Optional file path reference
    metadata TEXT DEFAULT '{}',  -- JSON for additional data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE SET NULL,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_evidence_task_id ON evidence(task_id);
CREATE INDEX IF NOT EXISTS idx_evidence_project_id ON evidence(project_id);
CREATE INDEX IF NOT EXISTS idx_evidence_type ON evidence(type);
CREATE INDEX IF NOT EXISTS idx_evidence_created_at ON evidence(created_at);

-- Decisions table
CREATE TABLE IF NOT EXISTS decisions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER,
    project_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    rationale TEXT,
    decision TEXT NOT NULL,  -- The actual decision made
    alternatives TEXT DEFAULT '[]',  -- JSON array of considered alternatives
    made_by TEXT NOT NULL,  -- 'user', 'opencode', 'claude', 'coordinator'
    status TEXT DEFAULT 'active',  -- 'active', 'superseded', 'reverted'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_decisions_task_id ON decisions(task_id);
CREATE INDEX IF NOT EXISTS idx_decisions_project_id ON decisions(project_id);
CREATE INDEX IF NOT EXISTS idx_decisions_status ON decisions(status);

-- Modifications table
CREATE TABLE IF NOT EXISTS modifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER,
    project_id INTEGER NOT NULL,
    file_path TEXT NOT NULL,
    action TEXT NOT NULL,  -- 'create', 'update', 'delete', 'move'
    old_content TEXT,  -- Previous content (for diff)
    new_content TEXT,  -- New content (for diff)
    diff TEXT,  -- Unified diff
    approved_by TEXT,  -- 'user', 'opencode', 'claude', 'coordinator', or NULL if pending
    approval_status TEXT DEFAULT 'pending',  -- 'pending', 'approved', 'rejected'
    applied BOOLEAN DEFAULT FALSE,
    git_commit_hash TEXT,  -- Hash of commit if applied via git
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    applied_at TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_modifications_task_id ON modifications(task_id);
CREATE INDEX IF NOT EXISTS idx_modifications_project_id ON modifications(project_id);
CREATE INDEX IF NOT EXISTS idx_modifications_file_path ON modifications(file_path);
CREATE INDEX IF NOT EXISTS idx_modifications_approval_status ON modifications(approval_status);

-- Errors table
CREATE TABLE IF NOT EXISTS errors (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER,
    project_id INTEGER NOT NULL,
    agent TEXT,  -- 'opencode', 'claude', 'coordinator', 'system'
    error_type TEXT NOT NULL,  -- 'execution', 'timeout', 'validation', 'communication', 'system'
    message TEXT NOT NULL,
    stack_trace TEXT,
    context TEXT DEFAULT '{}',  -- JSON context data
    severity TEXT DEFAULT 'error',  -- 'warning', 'error', 'critical'
    resolved BOOLEAN DEFAULT FALSE,
    resolution TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_errors_task_id ON errors(task_id);
CREATE INDEX IF NOT EXISTS idx_errors_project_id ON errors(project_id);
CREATE INDEX IF NOT EXISTS idx_errors_agent ON errors(agent);
CREATE INDEX IF NOT EXISTS idx_errors_severity ON errors(severity);
CREATE INDEX IF NOT EXISTS idx_errors_created_at ON errors(created_at);

-- Test Runs table
CREATE TABLE IF NOT EXISTS test_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER,
    project_id INTEGER NOT NULL,
    command TEXT NOT NULL,  -- Test command executed
    output TEXT,  -- Test output
    exit_code INTEGER,
    passed INTEGER DEFAULT 0,
    failed INTEGER DEFAULT 0,
    skipped INTEGER DEFAULT 0,
    duration_ms INTEGER,  -- Duration in milliseconds
    metadata TEXT DEFAULT '{}',  -- JSON for additional data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_test_runs_task_id ON test_runs(task_id);
CREATE INDEX IF NOT EXISTS idx_test_runs_project_id ON test_runs(project_id);
CREATE INDEX IF NOT EXISTS idx_test_runs_created_at ON test_runs(created_at);

-- Agent Sessions table
CREATE TABLE IF NOT EXISTS agent_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER NOT NULL,
    agent_name TEXT NOT NULL,  -- 'opencode', 'claude'
    session_id TEXT NOT NULL,  -- External session ID from the agent CLI
    task_id INTEGER,  -- Optional link to current task
    status TEXT DEFAULT 'active',  -- 'active', 'idle', 'terminated', 'error'
    working_directory TEXT,
    metadata TEXT DEFAULT '{}',  -- JSON for additional data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    UNIQUE(agent_name, session_id)
);

CREATE INDEX IF NOT EXISTS idx_agent_sessions_project_id ON agent_sessions(project_id);
CREATE INDEX IF NOT EXISTS idx_agent_sessions_agent_name ON agent_sessions(agent_name);
CREATE INDEX IF NOT EXISTS idx_agent_sessions_session_id ON agent_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_agent_sessions_task_id ON agent_sessions(task_id);
CREATE INDEX IF NOT EXISTS idx_agent_sessions_status ON agent_sessions(status);