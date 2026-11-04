# tests/test_database.py
import pytest
import aiosqlite
from src.db.database import Database


@pytest.mark.asyncio
async def test_database_initializes_all_tables(tmp_path):
    db_path = tmp_path / "test.db"
    db = Database(str(db_path))
    await db.initialize()

    async with aiosqlite.connect(str(db_path)) as conn:
        cursor = await conn.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = {row[0] for row in await cursor.fetchall()}

    expected = {'projects', 'conversations', 'messages', 'tasks', 'evidence',
                'decisions', 'modifications', 'errors', 'test_runs', 'agent_sessions'}
    assert expected.issubset(tables)