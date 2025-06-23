"""
python migrate.py users_database.sqlite --table users --add-column lastActive --type TEXT --default "datetime('now')"
python migrate.py users_database.sqlite --table users --update-values


"""


import sqlite3
from typing import List, Dict, Optional
import argparse
import sys
from pathlib import Path

class DatabaseMigrator:
    def __init__(self, db_path: str):
        self.db_path = db_path
        self.conn = sqlite3.connect(db_path)
        self.conn.row_factory = sqlite3.Row  # Enable dictionary-like access

    def get_table_info(self, table_name: str) -> List[Dict]:
        """Get schema information for a table"""
        cursor = self.conn.cursor()
        cursor.execute(f"PRAGMA table_info({table_name})")
        return [dict(row) for row in cursor.fetchall()]

    def column_exists(self, table_name: str, column_name: str) -> bool:
        """Check if a column exists in a table"""
        columns = self.get_table_info(table_name)
        return any(col['name'] == column_name for col in columns)

    def add_column(self, table_name: str, column_name: str, column_type: str, default_value: Optional[str] = None) -> bool:
        """
        Add a column to a table if it doesn't exist

        Args:
            table_name: Name of the table to modify
            column_name: Name of new column
            column_type: SQLite data type (e.g., 'TEXT', 'INTEGER', 'REAL')
            default_value: Optional default value for the column

        Returns:
            True if column was added, False if it already existed
        """
        if self.column_exists(table_name, column_name):
            print(f"Column '{column_name}' already exists in table '{table_name}'")
            return False

        alter_sql = f"ALTER TABLE {table_name} ADD COLUMN {column_name} {column_type}"
        if default_value is not None:
            alter_sql += f" DEFAULT {default_value}"

        try:
            cursor = self.conn.cursor()
            cursor.execute(alter_sql)
            self.conn.commit()
            print(f"Successfully added column '{column_name}' to table '{table_name}'")
            return True
        except sqlite3.Error as e:
            print(f"Error adding column: {e}")
            self.conn.rollback()
            return False

    def update_column_values(self, table_name: str, column_name: str, value_map: Dict[str, str]) -> bool:
        """
        Update specific column values based on conditions

        Args:
            table_name: Table to update
            column_name: Column to modify
            value_map: Dictionary of {condition: value} pairs

        Example:
            migrator.update_column_values(
                'users',
                'lastActive',
                {
                    "email = 'admin@example.com'": "'2023-01-01'",
                    "username LIKE 'test%'": "datetime('now')"
                }
            )
        """
        if not self.column_exists(table_name, column_name):
            print(f"Column '{column_name}' doesn't exist in table '{table_name}'")
            return False

        try:
            cursor = self.conn.cursor()
            for condition, value in value_map.items():
                update_sql = f"UPDATE {table_name} SET {column_name} = {value} WHERE {condition}"
                cursor.execute(update_sql)
                print(f"Updated {cursor.rowcount} rows where {condition}")
            self.conn.commit()
            return True
        except sqlite3.Error as e:
            print(f"Error updating values: {e}")
            self.conn.rollback()
            return False

    def close(self):
        self.conn.close()

def main():
    parser = argparse.ArgumentParser(description='SQLite Database Migration Tool')
    parser.add_argument('database', help='Path to SQLite database file')
    parser.add_argument('--table', required=True, help='Table to modify')
    parser.add_argument('--add-column', help='Column name to add')
    parser.add_argument('--type', help='Data type for new column')
    parser.add_argument('--default', help='Default value for new column')
    parser.add_argument('--update-values', action='store_true',
                        help='Update values in existing column')

    args = parser.parse_args()

    if not Path(args.database).exists():
        print(f"Error: Database file '{args.database}' not found")
        sys.exit(1)

    migrator = DatabaseMigrator(args.database)

    try:
        if args.add_column:
            if not args.type:
                print("Error: --type is required when adding a column")
                sys.exit(1)

            migrator.add_column(
                args.table,
                args.add_column,
                args.type,
                args.default
            )

        if args.update_values:
            # Example usage - modify as needed
            print("\nCurrent table columns:")
            for col in migrator.get_table_info(args.table):
                print(f"{col['name']}: {col['type']}")

            column_name = input("\nEnter column name to update: ")
            if not migrator.column_exists(args.table, column_name):
                print(f"Column '{column_name}' doesn't exist")
                sys.exit(1)

            print("\nEnter update rules (condition=value, one per line, empty to finish):")
            value_map = {}
            while True:
                rule = input("> ").strip()
                if not rule:
                    break
                if '=' not in rule:
                    print("Invalid format. Use 'condition=value'")
                    continue
                condition, value = rule.split('=', 1)
                value_map[condition.strip()] = value.strip()

            if value_map:
                migrator.update_column_values(args.table, column_name, value_map)

    finally:
        migrator.close()

if __name__ == "__main__":
    main()