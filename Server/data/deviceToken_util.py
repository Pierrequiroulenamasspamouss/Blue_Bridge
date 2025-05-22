#!/usr/bin/env python3
import json
import sqlite3
from datetime import datetime
from database_manager import DatabaseManager

db = DatabaseManager()

def get_db_connection():
    return db._get_connection('deviceTokens')

def display_tokens(tokens):
    print("\nList of Device Tokens:")
    print("-" * 80)
    print(f"{'Index':<5} | {'User ID':<30} | {'Token':<30} | {'Last Used':<20} | {'Active'}")
    print("-" * 80)
    for idx, token in enumerate(tokens, 1):
        print(f"{idx:<5} | {token.get('userId', '')[:30]:<30} | {token.get('token', '')[:30]:<30} | {token.get('lastUsed', '')[:20]:<20} | {token.get('isActive', '')}")
    print("-" * 80)

def interactive_menu():
    print("\nOptions:")
    print("1 - List all device tokens")
    print("2 - List tokens for a specific user")
    print("3 - Add a new device token")
    print("4 - Update a device token")
    print("5 - Delete a device token")
    print("6 - Verify a device token")
    print("7 - Exit")

def select_token(tokens):
    while True:
        try:
            choice = int(input("Select token by number (0 to cancel): "))
            if choice == 0:
                return None
            if 1 <= choice <= len(tokens):
                return tokens[choice - 1]
            print("Invalid selection. Please try again.")
        except ValueError:
            print("Please enter a valid number.")

def main():
    while True:
        interactive_menu()
        choice = input("Choose an action: ").strip()

        if choice == "1":
            tokens = db.deviceTokens().get_all_tokens()
            if tokens:
                display_tokens(tokens)
            else:
                print("No device tokens found in the database.")

        elif choice == "2":
            user_id = input("Enter user ID: ").strip()
            tokens = db.deviceTokens().get_tokens_by_user(user_id)
            if tokens:
                display_tokens(tokens)
            else:
                print(f"No device tokens found for user {user_id}.")

        elif choice == "3":
            user_id = input("Enter user ID: ").strip()
            token = input("Enter device token: ").strip()
            device_type = input("Enter device type (default: android): ").strip() or "android"
            
            if db.deviceTokens().add_token(user_id, token, device_type):
                print("Device token added successfully.")
            else:
                print("Failed to add device token.")

        elif choice == "4":
            tokens = db.deviceTokens().get_all_tokens()
            if not tokens:
                print("No device tokens found in the database.")
                continue
                
            display_tokens(tokens)
            selected_token = select_token(tokens)
            if not selected_token:
                continue
                
            new_token = input("Enter new token: ").strip()
            if db.deviceTokens().update_token(selected_token['userId'], selected_token['token'], new_token):
                print("Device token updated successfully.")
            else:
                print("Failed to update device token.")

        elif choice == "5":
            tokens = db.deviceTokens().get_all_tokens()
            if not tokens:
                print("No device tokens found in the database.")
                continue
                
            display_tokens(tokens)
            selected_token = select_token(tokens)
            if not selected_token:
                continue
                
            confirm = input(f"Are you sure you want to delete this token? (y/n): ").strip().lower()
            if confirm == 'y':
                if db.deviceTokens().delete_token(selected_token['userId'], selected_token['token']):
                    print("Device token deleted successfully.")
                else:
                    print("Failed to delete device token.")

        elif choice == "6":
            user_id = input("Enter user ID: ").strip()
            token = input("Enter token to verify: ").strip()
            
            if db.deviceTokens().verify_token(user_id, token):
                print("Token verification successful.")
            else:
                print("Token verification failed.")

        elif choice == "7":
            print("Goodbye!")
            break

        else:
            print("Invalid choice.")

if __name__ == "__main__":
    main() 