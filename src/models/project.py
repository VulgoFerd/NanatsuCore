"""Project model with CRUD operations."""
import json
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Optional
import aiosqlite


@dataclass
class Project:
    """Project entity."""
    id: Optional[int]
    name: str
    working_directory: str
    description: Optional[str] = None
    settings: dict = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    def __post_init__(self):
        if self.settings is None:
            self.settings = {}


class ProjectModel:
    """CRUD operations for projects."""

    def __init__(self, db):
        self.db = db

    async def create(self, name: str, working_directory: str, description: str = None, settings: dict = None) -> Project:
        """Create a new project."""
        if settings is None:
            settings = {}
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(
                """INSERT INTO projects (name, working_directory, description, settings)
                   VALUES (?, ?, ?, ?)""",
                (name, working_directory, description, json.dumps(settings))
            )
            await conn.commit()
            project_id = cursor.lastrowid
            return await self.get(project_id)

    async def get(self, project_id: int) -> Optional[Project]:
        """Get a project by ID."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(
                "SELECT * FROM projects WHERE id = ?", (project_id,)
            )
            row = await cursor.fetchone()
            if row:
                return self._row_to_project(row)
            return None

    async def update(self, project_id: int, **kwargs) -> Optional[Project]:
        """Update a project."""
        allowed_fields = {"name", "working_directory", "description", "settings"}
        updates = {k: v for k, v in kwargs.items() if k in allowed_fields}
        if not updates:
            return await self.get(project_id)

        if "settings" in updates:
            updates["settings"] = json.dumps(updates["settings"])

        updates["updated_at"] = datetime.now().isoformat()
        set_clause = ", ".join(f"{k} = ?" for k in updates.keys())
        values = list(updates.values()) + [project_id]

        async with await self.db.get_connection() as conn:
            await conn.execute(
                f"UPDATE projects SET {set_clause} WHERE id = ?", values
            )
            await conn.commit()
            return await self.get(project_id)

    async def delete(self, project_id: int) -> bool:
        """Delete a project."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute("DELETE FROM projects WHERE id = ?", (project_id,))
            await conn.commit()
            return cursor.rowcount > 0

    async def list(self, limit: int = 100, offset: int = 0) -> list[Project]:
        """List projects with pagination."""
        async with await self.db.get_connection() as conn:
            cursor = await conn.execute(
                "SELECT * FROM projects ORDER BY created_at DESC LIMIT ? OFFSET ?",
                (limit, offset)
            )
            rows = await cursor.fetchall()
            return [self._row_to_project(row) for row in rows]

    def _row_to_project(self, row: aiosqlite.Row) -> Project:
        """Convert database row to Project."""
        return Project(
            id=row["id"],
            name=row["name"],
            working_directory=row["working_directory"],
            description=row["description"],
            settings=json.loads(row["settings"]) if row["settings"] else {},
            created_at=datetime.fromisoformat(row["created_at"]) if row["created_at"] else None,
            updated_at=datetime.fromisoformat(row["updated_at"]) if row["updated_at"] else None,
        )