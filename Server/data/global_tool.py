#!/usr/bin/env python3
import os
import sys
import subprocess
from pathlib import Path

def clear_screen():
    os.system('cls' if os.name == 'nt' else 'clear')

def interactive_menu():
    print("\nBlueBridge Global Management Tool")
    print("=" * 40)
    print("1 - Manage Users")
    print("2 - Manage Wells")
    print("3 - Manage Device Tokens")
    print("4 - Exit")
    print("=" * 40)

def main():
    # Get the directory where this script is located
    script_dir = Path(__file__).parent.absolute()
    
    while True:
        clear_screen()
        interactive_menu()
        choice = input("Choose an action: ").strip()

        if choice == "1":
            clear_screen()
            subprocess.run([sys.executable, str(script_dir / "users_util.py")])
        elif choice == "2":
            clear_screen()
            subprocess.run([sys.executable, str(script_dir / "wells_util.py")])
        elif choice == "3":
            clear_screen()
            subprocess.run([sys.executable, str(script_dir / "deviceToken_util.py")])
        elif choice == "4":
            print("Goodbye!")
            break
        else:
            print("Invalid choice. Press Enter to continue...")
            input()

if __name__ == "__main__":
    main() 