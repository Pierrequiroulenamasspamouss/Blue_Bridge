import sqlite3
import json
import re
import uuid
import random

DB_PATH = "database.sqlite"

def connect_db():
    return sqlite3.connect(DB_PATH)

def get_user_by_email(conn, email):
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM users WHERE email = ?", (email,))
    row = cursor.fetchone()
    if row is None:
        print("User not found.")
        return None, None
    columns = [desc[0] for desc in cursor.description]
    return dict(zip(columns, row)), columns

def update_user_field(conn, email, field_name, new_value):
    cursor = conn.cursor()
    cursor.execute(f"UPDATE users SET {field_name} = ? WHERE email = ?", (new_value, email))
    conn.commit()
    print(f"{field_name} updated for {email}.")

def delete_user(conn, email):
    cursor = conn.cursor()
    cursor.execute("DELETE FROM users WHERE email = ?", (email,))
    conn.commit()
    print(f"User '{email}' has been deleted.")

def generate_random_user(conn):
    cursor = conn.cursor()
    email = f"user_{uuid.uuid4().hex[:6]}@example.com"
    token_list = [str(uuid.uuid4()) for _ in range(random.randint(2, 5))]
    json_token = json.dumps(token_list)

    # Fetch columns to determine schema
    cursor.execute("PRAGMA table_info(users)")
    schema = cursor.fetchall()

    # Construct user data with dummy/random values
    values = []
    for col in schema:
        name, type_ = col[1], col[2].upper()
        if name == "email":
            values.append(email)
        elif "CHAR" in type_ or "TEXT" in type_:
            values.append(str(uuid.uuid4()))
        elif "INT" in type_:
            values.append(random.randint(1, 1000))
        elif "JSON" in type_.upper() or name == "token":
            values.append(json_token)
        else:
            values.append(None)

    placeholders = ", ".join("?" for _ in values)
    col_names = ", ".join(col[1] for col in schema)
    cursor.execute(f"INSERT INTO users ({col_names}) VALUES ({placeholders})", values)
    conn.commit()
    print(f"Random user '{email}' created.")

def parse_indices(input_str, list_len):
    input_str = input_str.strip().lower()
    indices = set()

    if input_str == "all":
        return list(range(list_len))
    elif input_str.startswith("last"):
        if input_str == "last":
            return [list_len - 1] if list_len > 0 else []
        match = re.match(r"last-(\d+)", input_str)
        if match:
            count = int(match.group(1))
            return list(range(list_len - count, list_len)) if count <= list_len else list(range(list_len))
        else:
            raise ValueError("Invalid 'last-x' format.")
    else:
        parts = input_str.split(',')
        for part in parts:
            if '-' in part:
                start, end = part.split('-')
                indices.update(range(int(start), int(end) + 1))
            else:
                indices.add(int(part))
    return sorted(i for i in indices if 0 <= i < list_len)

def interactive_menu():
    print("\nOptions:")
    print("1 - Edit a field")
    print("2 - Delete a user")
    print("3 - Create a random user")
    print("4 - Exit")

def main():
    conn = connect_db()

    while True:
        interactive_menu()
        choice = input("Choose an action: ").strip()

        if choice == "1":
            email = input("Enter user email: ").strip()
            user, columns = get_user_by_email(conn, email)

            if user:
                print("\nUser data:")
                for idx, col in enumerate(columns):
                    print(f"{idx}: {col} = {user[col]}")

                try:
                    field_index = int(input("\nEnter field number to edit: ").strip())
                    field_name = columns[field_index]
                except (ValueError, IndexError):
                    print("Invalid field number.")
                    continue

                value = user[field_name]
                try:
                    parsed = json.loads(value)
                    if not isinstance(parsed, list):
                        print("The field is not a JSON list.")
                        continue
                except Exception:
                    print("The field does not contain valid JSON.")
                    continue

                print(f"\nCurrent list ({len(parsed)} items):")
                for i, item in enumerate(parsed):
                    print(f"  {i}: {item}")

                delete_input = input("\nEnter indices to delete (e.g., 1,2,4-6, last-2, all): ").strip()
                try:
                    to_delete = parse_indices(delete_input, len(parsed))
                except Exception as e:
                    print(f"Invalid format: {e}")
                    continue

                for i in reversed(to_delete):
                    removed = parsed.pop(i)
                    print(f"Deleted index {i}: {removed}")

                update_user_field(conn, email, field_name, json.dumps(parsed))

        elif choice == "2":
            email = input("Enter email of user to delete: ").strip()
            delete_user(conn, email)

        elif choice == "3":
            generate_random_user(conn)

        elif choice == "4":
            print("Goodbye!")
            break

        else:
            print("Invalid choice.")

    conn.close()

if __name__ == "__main__":
    main()
