#!/usr/bin/env python3
"""
FCM Messaging Tool for BlueBridge (text only)
This tool allows testing FCM messaging functionality by sending test messages to devices.
"""

import requests
import json
import argparse
import sys
import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext
from datetime import datetime

class FCMMessagingTool:
    def __init__(self, server_key: str):
        self.server_key = server_key
        self.fcm_url = "http://localhost:80/api/chats"
        self.headers = {
            "Authorization": f"key={server_key}",
            "Content-Type": "application/json"
        }
    
    def send_message(
        self, 
        to_token: str, 
        sender_id: str, 
        sender_name: str, 
        message_content: str
    ) -> dict:
        payload = {
            "to": to_token,
            "data": {
                "type": "chat_message",
                "messageId": f"msg_{int(datetime.now().timestamp())}",
                "senderId": sender_id,
                "receiverId": "",
                "content": message_content,
                "timestamp": str(int(datetime.now().timestamp() * 1000)),
                "messageType": "TEXT"
            },
            "notification": {
                "title": sender_name,
                "body": message_content,
                "sound": "default"
            }
        }
        try:
            response = requests.post(
                self.fcm_url,
                headers=self.headers,
                json=payload,
                timeout=10
            )
            return {
                "success": response.status_code == 200,
                "status_code": response.status_code,
                "response": response.json() if response.status_code == 200 else response.text,
                "timestamp": datetime.now().isoformat()
            }
        except requests.exceptions.RequestException as e:
            return {
                "success": False,
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            }

class FCMGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("FCM Messaging Tool - BlueBridge (Text Only)")
        self.root.geometry("700x400")
        self.server_key = tk.StringVar(value="BJ9Mf0mMKZO8pnRYwL-jdWcCM2iqQUmzsgXJnIT5yjrrfrgF_2rmjkM9gUv1QbWSlLcG2cyTA93qIHmSyvx2c6o")
        self.device_token = tk.StringVar(value="d9MrrVpSRL6ou_-Oq6FmW_:APA91bFnnfuJ25QyYuhDXYFsoI9iv-6nI3x7pXdqI0rrvdRKA74vtsUw5gyVy85uvOg2qggEq770K-F_WhqMEy_fd_HevEdLzTjGaYOZRRfj3Q75gyoOzqg")
        self.sender_name = tk.StringVar(value="Test User")
        self.sender_id = tk.StringVar(value="test_sender")
        self.message_content = tk.StringVar()
        self.create_widgets()
    
    def create_widgets(self):
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)
        main_frame.columnconfigure(1, weight=1)
        ttk.Label(main_frame, text="FCM Server Key:").grid(row=0, column=0, sticky=tk.W, pady=5)
        server_key_entry = ttk.Entry(main_frame, textvariable=self.server_key, width=60)
        server_key_entry.grid(row=0, column=1, sticky=(tk.W, tk.E), pady=5)
        ttk.Label(main_frame, text="Device Token:").grid(row=1, column=0, sticky=tk.W, pady=5)
        device_token_entry = ttk.Entry(main_frame, textvariable=self.device_token, width=60)
        device_token_entry.grid(row=1, column=1, sticky=(tk.W, tk.E), pady=5)
        ttk.Label(main_frame, text="Sender Name:").grid(row=2, column=0, sticky=tk.W, pady=5)
        sender_name_entry = ttk.Entry(main_frame, textvariable=self.sender_name, width=30)
        sender_name_entry.grid(row=2, column=1, sticky=tk.W, pady=5)
        ttk.Label(main_frame, text="Sender ID:").grid(row=3, column=0, sticky=tk.W, pady=5)
        sender_id_entry = ttk.Entry(main_frame, textvariable=self.sender_id, width=30)
        sender_id_entry.grid(row=3, column=1, sticky=tk.W, pady=5)
        ttk.Label(main_frame, text="Message:").grid(row=4, column=0, sticky=tk.W, pady=5)
        message_entry = ttk.Entry(main_frame, textvariable=self.message_content, width=50)
        message_entry.grid(row=4, column=1, sticky=(tk.W, tk.E), pady=5)
        send_message_btn = ttk.Button(main_frame, text="Send Message", command=self.send_message)
        send_message_btn.grid(row=5, column=0, columnspan=2, pady=15)
        ttk.Label(main_frame, text="Log:").grid(row=6, column=0, sticky=tk.W, pady=(10, 5))
        self.log_text = scrolledtext.ScrolledText(main_frame, height=8, width=80)
        self.log_text.grid(row=7, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=5)
        main_frame.rowconfigure(7, weight=1)
    def log_message(self, message):
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.log_text.insert(tk.END, f"[{timestamp}] {message}\n")
        self.log_text.see(tk.END)
        self.root.update()
    def send_message(self):
        if not self.server_key.get() or not self.device_token.get():
            messagebox.showerror("Error", "Please enter both Server Key and Device Token")
            return
        if not self.message_content.get():
            messagebox.showerror("Error", "Please enter a message")
            return
        self.log_message("Sending chat message...")
        try:
            tool = FCMMessagingTool(self.server_key.get())
            result = tool.send_message(
                to_token=self.device_token.get(),
                sender_id=self.sender_id.get(),
                sender_name=self.sender_name.get(),
                message_content=self.message_content.get()
            )
            if result.get("success"):
                self.log_message("✅ Message sent successfully!")
            else:
                self.log_message(f"❌ Failed to send message: {result.get('error', result.get('response', 'Unknown error'))}")
            self.log_message(f"Response: {json.dumps(result, indent=2)}")
        except Exception as e:
            self.log_message(f"❌ Error: {str(e)}")

def main():
    parser = argparse.ArgumentParser(description="FCM Messaging Tool for BlueBridge (text only)")
    parser.add_argument("--server-key", help="FCM Server Key")
    parser.add_argument("--token", help="Target device FCM token")
    parser.add_argument("--sender-name", default="Test User", help="Sender name for chat messages")
    parser.add_argument("--sender-id", default="test_sender", help="Sender ID for chat messages")
    parser.add_argument("--body", default="This is a test message", help="Message body")
    parser.add_argument("--gui", action="store_true", help="Launch GUI interface")
    args = parser.parse_args()
    if args.gui:
        root = tk.Tk()
        app = FCMGUI(root)
        if args.server_key:
            app.server_key.set(args.server_key)
        if args.token:
            app.device_token.set(args.token)
        if args.sender_name:
            app.sender_name.set(args.sender_name)
        if args.sender_id:
            app.sender_id.set(args.sender_id)
        if args.body:
            app.message_content.set(args.body)
        root.mainloop()
    else:
        if not args.server_key or not args.token:
            print("Error: --server-key and --token are required for command line mode")
            sys.exit(1)
        tool = FCMMessagingTool(args.server_key)
        print("Sending chat message...")
        result = tool.send_message(
            to_token=args.token,
            sender_id=args.sender_id,
            sender_name=args.sender_name,
            message_content=args.body
        )
        print(f"Result: {json.dumps(result, indent=2)}")
        if result.get("success"):
            print("✅ Message sent successfully!")
            sys.exit(0)
        else:
            print("❌ Failed to send message")
            sys.exit(1)

if __name__ == "__main__":
    main() 