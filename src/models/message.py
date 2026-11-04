"""Message model with CRUD operations."""
import json
from dataclasses import dataclass
from datetime import datetime
from typing import Optional, List
import aiosqlite


@dataclass
class Message:
    """Message entity."""
    id: Optional[int]
    conversation_id: int
    sender: str
    recipient: Optional[str]
    content: str
    message_type: str = "text"
    refs: List = None
    state: str = "sent"
    created_at: Optional[datetime] = None

    def __post_init__(self):
        if self.refs is None:
            self.refs = []


class MessageModel:
    """CRUD operations for messages."""

    def __init__(self, db):
        self.db = db

    async def create(self, conversation_id: int, sender: str, content: str,
                     recipient: str = None, message_type: str = "text",
                     refs: List = None, state: str = "sent") -> Message:
        """Create a new message."""
        if refs is None:
            refs = []
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(
                """INSERT INTO messages (conversation_id, sender, recipient, content, message_type, refs, state)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (conversation_id, sender, recipient, content, message_type, json.dumps(refs), state)
            )
            await conn.commit()
            message_id = cursor.lastrowid
            return await self.get(message_id)

    async def get(self, message_id: int) -> Optional[Message]:
        """Get a message by ID."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(
                "SELECT * FROM messages WHERE id = ?", (message_id,)
            )
            row = await cursor.fetchone()
            if row:
                return self._row_to_message(row)
            return None

    async def update(self, message_id: int, **kwargs) -> Optional[Message]:
        """Update a message."""
        allowed_fields = {"recipient", "content", "message_type", "refs", "state"}
        updates = {k: v for k, v in kwargs.items() if k in allowed_fields}
        if not updates:
            return await self.get(message_id)

        if "refs" in updates:
            updates["refs"] = json.dumps(updates["refs"])

        set_clause = ", ".join(f"{k} = ?" for k in updates.keys())
        values = list(updates.values()) + [message_id]

        async with await self.db.get_connection() as conn:
            await conn.execute(
                f"UPDATE messages SET {set_clause} WHERE id = ?", values
            )
            await conn.commit()
            return await self.get(message_id)

    async def delete(self, message_id: int) -> bool:
        """Delete a message."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute("DELETE FROM messages WHERE id = ?", (message_id,))
            await conn.commit()
            return cursor.rowcount > 0

    async def list(self, conversation_id: int = None, sender: str = None,
                   recipient: str = None, limit: int = 100, offset: int = 0) -> list[Message]:
        """List messages with optional filters."""
        query = "SELECT * FROM messages WHERE 1=1"
        params = []
        if conversation_id is not None:
            query += " AND conversation_id = ?"
            params.append(conversation_id)
        if sender is not None:
            query += " AND sender = ?"
            params.append(sender)
        if recipient is not None:
            query += " AND recipient = ?"
            params.append(recipient)
        query += " ORDER BY created_at ASC LIMIT ? OFFSET ?"
        params.extend([limit, offset])

        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(query, params)
            rows = await cursor.fetchall()
            return [self._row_to_message(row) for row in rows]

    def _row_to_message(self, row: aiosqlite.Row) -> Message:
        """Convert database row to Message."""
        return Message(
            id=row["id"],
            conversation_id=row["conversation_id"],
            sender=row["sender"],
            recipient=row["recipient"],
            content=row["content"],
            message_type=row["message_type"],
            refs=json.loads(row["refs"]) if row["refs"] else [],
            state=row["state"],
            created_at=datetime.fromisoformat(row["created_at"]) if row["created_at"] else None,
        )