#!/usr/bin/env python3
"""
FCM Messaging Tool for BlueBridge
This tool allows testing FCM messaging functionality by sending test messages to devices.
"""

import requests
import json
import argparse
import sys
from datetime import datetime
from typing import Dict, Any, Optional

class FCMMessagingTool:
    def __init__(self, server_key: str):
        self.server_key = server_key
        self.fcm_url = "https://fcm.googleapis.com/fcm/send"
        self.headers = {
            "Authorization": f"key={server_key}",
            "Content-Type": "application/json"
        }
    
    def send_message(
        self, 
        to_token: str, 
        title: str, 
        body: str, 
        data: Optional[Dict[str, str]] = None
    ) -> Dict[str, Any]:
        """
        Send a message via FCM
        
        Args:
            to_token: The FCM token of the target device
            title: Notification title
            body: Notification body
            data: Optional data payload
            
        Returns:
            Response from FCM API
        """
        payload = {
            "to": to_token,
            "notification": {
                "title": title,
                "body": body,
                "sound": "default"
            }
        }
        
        if data:
            payload["data"] = data
        
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
    
    def send_chat_message(
        self, 
        to_token: str, 
        sender_name: str, 
        message_content: str,
        sender_id: str = "test_sender",
        message_id: str = None
    ) -> Dict[str, Any]:
        """
        Send a chat message via FCM
        
        Args:
            to_token: The FCM token of the target device
            sender_name: Name of the sender
            message_content: Content of the message
            sender_id: ID of the sender
            message_id: Optional message ID
            
        Returns:
            Response from FCM API
        """
        if message_id is None:
            message_id = f"msg_{int(datetime.now().timestamp())}"
        
        data = {
            "type": "chat_message",
            "messageId": message_id,
            "senderId": sender_id,
            "senderName": sender_name,
            "content": message_content,
            "timestamp": str(int(datetime.now().timestamp() * 1000)),
            "messageType": "TEXT"
        }
        
        return self.send_message(
            to_token=to_token,
            title=sender_name,
            body=message_content,
            data=data
        )

def main():
    parser = argparse.ArgumentParser(description="FCM Messaging Tool for BlueBridge")
    parser.add_argument("--server-key", required=True, help="FCM Server Key")
    parser.add_argument("--token", required=True, help="Target device FCM token")
    parser.add_argument("--title", default="Test Message", help="Message title")
    parser.add_argument("--body", default="This is a test message", help="Message body")
    parser.add_argument("--sender-name", default="Test User", help="Sender name for chat messages")
    parser.add_argument("--chat", action="store_true", help="Send as chat message")
    parser.add_argument("--sender-id", default="test_sender", help="Sender ID for chat messages")
    parser.add_argument("--message-id", help="Message ID for chat messages")
    
    args = parser.parse_args()
    
    tool = FCMMessagingTool(args.server_key)
    
    if args.chat:
        print("Sending chat message...")
        result = tool.send_chat_message(
            to_token=args.token,
            sender_name=args.sender_name,
            message_content=args.body,
            sender_id=args.sender_id,
            message_id=args.message_id
        )
    else:
        print("Sending notification message...")
        result = tool.send_message(
            to_token=args.token,
            title=args.title,
            body=args.body
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

# Example usage:
# python fcm_messaging_tool.py --server-key "YOUR_FCM_SERVER_KEY" --token "DEVICE_TOKEN" --chat --sender-name "John" --body "Hello from Python tool!" 