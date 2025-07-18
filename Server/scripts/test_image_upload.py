import os
import requests

# CONFIGURATION
SERVER_URL = 'http://localhost:80'  # Change to your server's address and port
IMAGE_PATH = r'C:\Users\Pierr\Downloads\test.jpg'  # Path to your test image
DEFAULT_WELL_ID = '001'


def upload_image(well_id, image_number, image_path, description=None):
    try:
        url = f"{SERVER_URL}/api/wells/{well_id}/images/{image_number}/upload"

        # Print file info for debugging
        file_size = os.path.getsize(image_path)
        print(f"Uploading file: {image_path}")
        print(f"File size: {file_size} bytes")

        with open(image_path, 'rb') as f:
            files = {'image': (os.path.basename(image_path), f, 'image/jpeg')}
            data = {'description': description or f"Image {image_number}"}

            response = requests.post(url, files=files, data=data)
            print(f"Upload response: {response.status_code} {response.text}")
            return response.ok

    except Exception as e:
        print(f"Upload error: {str(e)}")
        return False
def fetch_images(well_id):
    for i in range(0, 9):
        url = f"{SERVER_URL}/api/wells/{well_id}/images/{i}"
        response = requests.get(url)
        print(f"Fetch images response: {response.status_code}")
        if response.ok:
            images = response.json().get('data', [])
            print(f"Found image for well {well_id}with imageNumber {i}")
        else:
            print(response.text)

if __name__ == '__main__':
    import sys
    well_id = input(f"Enter wellId (default {DEFAULT_WELL_ID}): ").strip() or DEFAULT_WELL_ID
    image_number = input("enter image number: ").strip() or 0
    print(f"Uploading {IMAGE_PATH} as image {image_number} for well {well_id}...")
    if upload_image(well_id, image_number, IMAGE_PATH, description="Test upload from script"):
        print("Image uploaded successfully.")
    else:
        print("Image upload failed.")
    print("Fetching all images for this well:")
    fetch_images(well_id)
