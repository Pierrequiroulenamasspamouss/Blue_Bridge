import os
import requests
from urllib.parse import unquote_to_bytes
from time import sleep
import base64 

# CONFIGURATION
LOCAL_SD_URL = 'http://127.0.0.1:7860/sdapi/v1/txt2img'
SERVER_URL = 'http://bluebridge.homeonthewater.com:80'
TEMP_IMAGE_DIR = r'C:\Users\Pierr\AppData\Local\Temp\generated_images'
BACKUP_PROMPT = 'A scenic natural well in a forest, cinematic lighting, high detail'
NEGATIVE_PROMPT = 'blurry, low quality, distorted'
WIDTH, HEIGHT = 512, 512

os.makedirs(TEMP_IMAGE_DIR, exist_ok=True)


def generate_image(prompt, negative_prompt, index):
    try:
        payload = {
            "prompt": prompt,
            "negative_prompt": negative_prompt,
            "steps": 20,
            "width": WIDTH,
            "height": HEIGHT,
            "sampler_name": "Euler a",
            "cfg_scale": 7,
            "batch_size": 1
        }

        print(f"[{index}] Generating image...")
        response = requests.post(LOCAL_SD_URL, json=payload)
        response.raise_for_status()
        img_base64 = response.json()["images"][0]

        # Save image to temp file
        image_path = os.path.join(TEMP_IMAGE_DIR, f"image_{index}.png")
        with open(image_path, "wb") as f:
            f.write(base64.b64decode(img_base64))

        print(f"[{index}] Image saved to: {image_path}")
        return image_path

    except Exception as e:
        print(f"[{index}] Image generation failed: {e}")
        return None


def upload_image(well_id, image_number, image_path, description=None):
    try:
        url = f"{SERVER_URL}/api/wells/{well_id}/images/{image_number}/upload"

        print(f"[{image_number}] Uploading: {os.path.basename(image_path)}")
        with open(image_path, 'rb') as f:
            files = {'image': (os.path.basename(image_path), f, 'image/png')}

            data = {'description': description or f"Image {image_number}"}

            response = requests.post(url, files=files, data=data)
            print(f"[{image_number}] Upload response: {response.status_code} {response.text}")
            return response.ok

    except Exception as e:
        print(f"[{image_number}] Upload error: {e}")
        return False


def fetch_images(well_id):
    for i in range(10):
        url = f"{SERVER_URL}/api/wells/{well_id}/images/{i}"
        response = requests.get(url)
        if response.ok:
            print(f"[{i}] Found image.")
        else:
            print(f"[{i}] Not found or error: {response.status_code}")


if __name__ == '__main__':
    well_id = input("Enter well ID (default 001): ").strip() or "001"
    PROMPT = input("What image would you like to upload to the server? : ").strip() or BACKUP_PROMPT
    for i in range(10):
        image_path = generate_image(PROMPT, NEGATIVE_PROMPT, i)
        if image_path:
            uploaded = upload_image(well_id, i, image_path, description=f"Generated image {i}")
            if not uploaded:
                print(f"[{i}] ❌ Failed to upload.")
        else:
            print(f"[{i}] ❌ Skipped due to generation failure.")
        sleep(1)  # Optional: avoid overloading your SD WebUI or server

    print("Fetching final image status from server:")
    fetch_images(well_id)
