### Task 1: Project Structure & Database Schema

**Files:**
- Create: `src/db/schema.sql`
- Create: `src/db/database.py`
- Create: `src/models/__init__.py`
- Create: `src/models/project.py`
- Create: `src/models/conversation.py`
- Create: `src/models/message.py`
- Create: `src/models/task.py`
- Create: `src/models/evidence.py`
- Create: `src/models/decision.py`
- Create: `src/models/modification.py`
- Create: `src/models/error.py`
- Create: `src/models/test_run.py`
- Create: `src/models/agent_session.py`

**Interfaces:**
- Produces: `Database` class with `initialize()`, `get_connection()`; model classes with CRUD methods

- [ ] **Step 1: Write failing test for database initialization**

```python
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pytest tests/test_database.py::test_database_initializes_all_tables -v`
Expected: FAIL - module not found

- [ ] **Step 3: Implement database schema and Database class**

`src/db/schema.sql` - CREATE TABLE statements for all 10 tables with proper indexes
`src/db/database.py` - Database class with initialize(), connection management

- [ ] **Step 4: Run test to verify it passes**

Run: `pytest tests/test_database.py::test_database_initializes_all_tables -v`
Expected: PASS

- [ ] **Step 5: Implement model classes with basic CRUD**

Each model in `src/models/` with create, get, update, delete, list methods using parameterized queries

- [ ] **Step 6: Test model CRUD operations**

Run: `pytest tests/test_models.py -v`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/db/ src/models/ tests/test_database.py tests/test_models.py
git commit -m "feat: database schema and models"
```

---

