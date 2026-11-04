"""Conversation model with CRUD operations."""
import json
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Optional
import aiosqlite


@dataclass
class Conversation:
    """Conversation entity."""
    id: Optional[int]
    project_id: int
    title: Optional[str] = None
    task_id: Optional[int] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


class ConversationModel:
    """CRUD operations for conversations."""

    def __init__(self, db):
        self.db = db

    async def create(self, project_id: int, title: str = None, task_id: int = None) -> Conversation:
        """Create a new conversation."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(
                """INSERT INTO conversations (project_id, title, task_id)
                   VALUES (?, ?, ?)""",
                (project_id, title, task_id)
            )
            await conn.commit()
            conversation_id = cursor.lastrowid
            return await self.get(conversation_id)

    async def get(self, conversation_id: int) -> Optional[Conversation]:
        """Get a conversation by ID."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(
                "SELECT * FROM conversations WHERE id = ?", (conversation_id,)
            )
            row = await cursor.fetchone()
            if row:
                return self._row_to_conversation(row)
            return None

    async def update(self, conversation_id: int, **kwargs) -> Optional[Conversation]:
        """Update a conversation."""
        allowed_fields = {"title", "task_id"}
        updates = {k: v for k, v in kwargs.items() if k in allowed_fields}
        if not updates:
            return await self.get(conversation_id)

        updates["updated_at"] = datetime.now().isoformat()
        set_clause = ", ".join(f"{k} = ?" for k in updates.keys())
        values = list(updates.values()) + [conversation_id]

        async with await self.db.get_connection() as conn:
            await conn.execute(
                f"UPDATE conversations SET {set_clause} WHERE id = ?", values
            )
            await conn.commit()
            return await self.get(conversation_id)

    async def delete(self, conversation_id: int) -> bool:
        """Delete a conversation."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute("DELETE FROM conversations WHERE id = ?", (conversation_id,))
            await conn.commit()
            return cursor.rowcount > 0

    async def list(self, project_id: int = None, task_id: int = None, limit: int = 100, offset: int = 0) -> list[Conversation]:
        """List conversations with optional filters."""
        query = "SELECT * FROM conversations WHERE 1=1"
        params = []
        if project_id is not None:
            query += " AND project_id = ?"
            params.append(project_id)
        if task_id is not None:
            query += " AND task_id = ?"
            params.append(task_id)
        query += " ORDER BY created_at DESC LIMIT ? OFFSET ?"
        params.extend([limit, offset])

        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(query, params)
            rows = await cursor.fetchall()
            return [self._row_to_conversation(row) for row in rows]

    def _row_to_conversation(self, row: aiosqlite.Row) -> Conversation:
        """Convert database row to Conversation."""
        return Conversation(
            id=row["id"],
            project_id=row["project_id"],
            title=row["title"],
            task_id=row["task_id"],
            created_at=datetime.fromisoformat(row["created_at"]) if row["created_at"] else None,
            updated_at=datetime.fromisoformat(row["updated_at"]) if row["updated_at"] else None,
        )