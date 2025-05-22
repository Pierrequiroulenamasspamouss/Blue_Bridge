from database_manager import DatabaseManager
import json
import re
import uuid
import random
from datetime import datetime

db = DatabaseManager()

def connect_db():
    return db._get_connection('users')

def get_user_by_email(email):
    return db.users().get_user_by_email(email)

def update_user_field(email, field_name, new_value):
    user = get_user_by_email(email)
    if user:
        return db.users().update_user(user['userId'], {field_name: new_value})
    return False

def delete_user(email):
    user = get_user_by_email(email)
    if user:
        return db.users().delete_user(user['userId'])
    return False

def generate_random_user():
    user_data = {
        'userId': str(uuid.uuid4()),
        'email': f"user_{uuid.uuid4().hex[:6]}@example.com",
        'password': str(uuid.uuid4()),
        'firstName': f"User{uuid.uuid4().hex[:4]}",
        'lastName': f"Test{uuid.uuid4().hex[:4]}",
        'username': f"user_{uuid.uuid4().hex[:6]}",
        'role': 'user',
        'location': json.dumps({'latitude': random.uniform(-90, 90), 'longitude': random.uniform(-180, 180)}),
        'waterNeeds': json.dumps({'type': 'drinking', 'amount': random.randint(1, 100)}),
        'lastActive': datetime.now().isoformat(),
        'registrationDate': datetime.now().isoformat(),
        'notificationPreferences': json.dumps({
            'weatherAlerts': True,
            'wellUpdates': True,
            'nearbyUsers': True
        })
    }
    return db.users().create_user(user_data)

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

def display_users(users):
    print("\nList of Users:")
    print("-" * 80)
    print(f"{'Index':<5} | {'Email':<30} | {'Name':<20} | {'Role':<10} | {'Last Active'}")
    print("-" * 80)
    for idx, user in enumerate(users, 1):
        name = f"{user.get('firstName', '')} {user.get('lastName', '')}"
        print(f"{idx:<5} | {user.get('email', '')[:30]:<30} | {name[:20]:<20} | {user.get('role', ''):<10} | {user.get('lastActive', '')}")
    print("-" * 80)

def interactive_menu():
    print("\nOptions:")
    print("1 - List all users")
    print("2 - Edit a user")
    print("3 - Delete a user")
    print("4 - Create a random user")
    print("5 - Exit")

def select_user(users):
    while True:
        try:
            choice = int(input("Select user by number (0 to cancel): "))
            if choice == 0:
                return None
            if 1 <= choice <= len(users):
                return users[choice - 1]
            print("Invalid selection. Please try again.")
        except ValueError:
            print("Please enter a valid number.")

def select_field(user):
    fields = list(user.keys())
    print("\nAvailable fields:")
    for idx, field in enumerate(fields, 1):
        print(f"{idx}. {field}")
    
    while True:
        try:
            choice = int(input("Select field by number (0 to cancel): "))
            if choice == 0:
                return None
            if 1 <= choice <= len(fields):
                return fields[choice - 1]
            print("Invalid selection. Please try again.")
        except ValueError:
            print("Please enter a valid number.")

def main():
    while True:
        interactive_menu()
        choice = input("Choose an action: ").strip()

        if choice == "1":
            users = db.users().get_all_users()
            if users:
                display_users(users)
            else:
                print("No users found in the database.")

        elif choice == "2":
            users = db.users().get_all_users()
            if not users:
                print("No users found in the database.")
                continue
                
            display_users(users)
            selected_user = select_user(users)
            if not selected_user:
                continue
                
            selected_field = select_field(selected_user)
            if not selected_field:
                continue
                
            value = selected_user[selected_field]
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

            update_user_field(selected_user['email'], selected_field, json.dumps(parsed))

        elif choice == "3":
            users = db.users().get_all_users()
            if not users:
                print("No users found in the database.")
                continue
                
            display_users(users)
            selected_user = select_user(users)
            if not selected_user:
                continue
                
            confirm = input(f"Are you sure you want to delete {selected_user['email']}? (y/n): ").strip().lower()
            if confirm == 'y':
                if delete_user(selected_user['email']):
                    print(f"User '{selected_user['email']}' has been deleted.")
                else:
                    print(f"Failed to delete user '{selected_user['email']}'.")

        elif choice == "4":
            if generate_random_user():
                print("Random user created successfully.")
            else:
                print("Failed to create random user.")

        elif choice == "5":
            print("Goodbye!")
            break

        else:
            print("Invalid choice.")

if __name__ == "__main__":
    main()