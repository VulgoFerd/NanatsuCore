"""Database connection management and initialization."""
import aiosqlite
from pathlib import Path
from typing import Optional


class Database:
    """Manages SQLite database connection and initialization."""

    def __init__(self, db_path: str):
        """Initialize database with path.

        Args:
            db_path: Path to SQLite database file.
        """
        self.db_path = db_path
        self._connection: Optional[aiosqlite.Connection] = None

    async def initialize(self) -> None:
        """Initialize database by creating all tables from schema.sql."""
        # Ensure directory exists
        Path(self.db_path).parent.mkdir(parents=True, exist_ok=True)

        # Read schema
        schema_path = Path(__file__).parent / "schema.sql"
        schema = schema_path.read_text(encoding="utf-8")

        # Execute schema
        async with aiosqlite.connect(self.db_path) as conn:
            await conn.executescript(schema)
            await conn.commit()

    async def get_connection(self) -> aiosqlite.Connection:
        """Get a database connection.

        Returns:
            An aiosqlite connection with row_factory set to aiosqlite.Row.
        """
        conn = await aiosqlite.connect(self.db_path)
        conn.row_factory = aiosqlite.Row
        return conn

    async def close(self) -> None:
        """Close any held connection."""
        if self._connection:
            await self._connection.close()
            self._connection = None

    async def __aenter__(self) -> "Database":
        """Async context manager entry."""
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb) -> None:
        """Async context manager exit."""
        await self.close()