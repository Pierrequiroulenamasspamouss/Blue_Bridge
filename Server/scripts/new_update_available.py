import requests

# ==== CONFIGURATION ====
API_URL = "http://bluebridge.homeonthewater.com/api/notifications/send"  # Change this to your actual server URL
NOTIFICATION_TITLE = "Update Available"
NOTIFICATION_MESSAGE = "The app got a new update: Download the latest version to enjoy the latest features"
TARGET_USER_IDS = []  # Example: [1, 2, 3] — leave empty for all users

# ==== SEND FUNCTION ====
def send_notification(title, message, target_user_ids=None):
    payload = {
        "title": title,
        "message": message,
        "targetUserIds": target_user_ids or []
    }

    try:
        response = requests.post(API_URL, json=payload)
        response.raise_for_status()
        data = response.json()
        print(f"✅ Notification sent: {data}")
    except requests.exceptions.RequestException as e:
        print(f"❌ Request failed: {e}")
    except ValueError:
        print(f"❌ Invalid JSON response: {response.text}")

# ==== EXECUTION ====
if __name__ == "__main__":
    send_notification(NOTIFICATION_TITLE, NOTIFICATION_MESSAGE, TARGET_USER_IDS)
