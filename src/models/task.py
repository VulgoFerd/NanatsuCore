"""Task model with CRUD operations."""
import json
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Optional, List
import aiosqlite


class TaskStatus(str, Enum):
    """Task status enumeration."""
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    REVIEW = "review"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


@dataclass
class Task:
    """Task entity."""
    id: Optional[int]
    project_id: int
    title: str
    description: Optional[str] = None
    status: TaskStatus = TaskStatus.PENDING
    assigned_agents: List[str] = None
    max_rounds: int = 10
    current_round: int = 0
    priority: int = 0
    metadata: dict = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None

    def __post_init__(self):
        if self.assigned_agents is None:
            self.assigned_agents = []
        if self.metadata is None:
            self.metadata = {}


class TaskModel:
    """CRUD operations for tasks."""

    def __init__(self, db):
        self.db = db

    async def create(self, project_id: int, title: str, description: str = None,
                     assigned_agents: List[str] = None, max_rounds: int = 10,
                     priority: int = 0, metadata: dict = None) -> Task:
        """Create a new task."""
        if assigned_agents is None:
            assigned_agents = []
        if metadata is None:
            metadata = {}
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(
                """INSERT INTO tasks (project_id, title, description, assigned_agents, max_rounds, priority, metadata)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (project_id, title, description, json.dumps(assigned_agents), max_rounds, priority, json.dumps(metadata))
            )
            await conn.commit()
            task_id = cursor.lastrowid
            return await self.get(task_id)

    async def get(self, task_id: int) -> Optional[Task]:
        """Get a task by ID."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute("SELECT * FROM tasks WHERE id = ?", (task_id,))
            row = await cursor.fetchone()
            if row:
                return self._row_to_task(row)
            return None

    async def update(self, task_id: int, **kwargs) -> Optional[Task]:
        """Update a task."""
        allowed_fields = {"title", "description", "status", "assigned_agents", "max_rounds",
                          "current_round", "priority", "metadata", "completed_at"}
        updates = {k: v for k, v in kwargs.items() if k in allowed_fields}
        if not updates:
            return await self.get(task_id)

        if "status" in updates and isinstance(updates["status"], TaskStatus):
            updates["status"] = updates["status"].value
        if "assigned_agents" in updates:
            updates["assigned_agents"] = json.dumps(updates["assigned_agents"])
        if "metadata" in updates:
            updates["metadata"] = json.dumps(updates["metadata"])

        updates["updated_at"] = datetime.now().isoformat()
        set_clause = ", ".join(f"{k} = ?" for k in updates.keys())
        values = list(updates.values()) + [task_id]

        async with await self.db.get_connection() as conn:
            await conn.execute(f"UPDATE tasks SET {set_clause} WHERE id = ?", values)
            await conn.commit()
            return await self.get(task_id)

    async def delete(self, task_id: int) -> bool:
        """Delete a task."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute("DELETE FROM tasks WHERE id = ?", (task_id,))
            await conn.commit()
            return cursor.rowcount > 0

    async def list(self, project_id: int = None, status: TaskStatus = None,
                   limit: int = 100, offset: int = 0) -> list[Task]:
        """List tasks with optional filters."""
        query = "SELECT * FROM tasks WHERE 1=1"
        params = []
        if project_id is not None:
            query += " AND project_id = ?"
            params.append(project_id)
        if status is not None:
            query += " AND status = ?"
            params.append(status.value if isinstance(status, TaskStatus) else status)
        query += " ORDER BY created_at DESC LIMIT ? OFFSET ?"
        params.extend([limit, offset])

        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(query, params)
            rows = await cursor.fetchall()
            return [self._row_to_task(row) for row in rows]

    def _row_to_task(self, row: aiosqlite.Row) -> Task:
        """Convert database row to Task."""
        return Task(
            id=row["id"],
            project_id=row["project_id"],
            title=row["title"],
            description=row["description"],
            status=TaskStatus(row["status"]),
            assigned_agents=json.loads(row["assigned_agents"]) if row["assigned_agents"] else [],
            max_rounds=row["max_rounds"],
            current_round=row["current_round"],
            priority=row["priority"],
            metadata=json.loads(row["metadata"]) if row["metadata"] else {},
            created_at=datetime.fromisoformat(row["created_at"]) if row["created_at"] else None,
            updated_at=datetime.fromisoformat(row["updated_at"]) if row["updated_at"] else None,
            completed_at=datetime.fromisoformat(row["completed_at"]) if row["completed_at"] else None,
        )