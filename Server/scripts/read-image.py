import json
import base64
import requests
from io import BytesIO
from PIL import Image
import matplotlib.pyplot as plt

def display_image_from_json(json_response):
    # Parse the JSON response
    if isinstance(json_response, str):
        data = json.loads(json_response)
    else:
        data = json_response
    
    # Extract the base64 image data (remove the data URL prefix if present)
    base64_data = data['data']['base64encodedImage']
    if base64_data.startswith('data:image'):
        # Remove the data URL prefix (e.g., "data:image/jpeg;base64,")
        base64_data = base64_data.split(',', 1)[1]
    
    # Decode the base64 string
    image_data = base64.b64decode(base64_data)
    
    # Create an image from the bytes
    image = Image.open(BytesIO(image_data))
    
    # Display the image
    plt.imshow(image)
    plt.axis('off')  # Hide axes
    plt.show()

def fetch_image_from_server(well_id, image_number):
    url = f"http://bluebridge.homeonthewater.com/api/wells/{well_id}/images/{image_number}"
    try:
        response = requests.get(url)
        response.raise_for_status()  # Raise an exception for HTTP errors
        return response.json()  # Return the parsed JSON data
    except requests.exceptions.RequestException as e:
        print(f"Error fetching image from server: {e}")
        return None

def main():
    print("Choose an option:")
    print("1. Load image from local JSON file")
    print("2. Fetch image from server using Well ID and Image Number")
    
    choice = input("Enter your choice (1 or 2): ")
    
    if choice == "1":
        file_path = input("Enter the path to the JSON file: ")
        try:
            with open(file_path, 'r') as file:
                json_response = file.read()
            display_image_from_json(json_response)
        except Exception as e:
            print(f"Error loading JSON file: {e}")
    elif choice == "2":
        well_id = input("Enter the Well ID: ")
        image_number = input("Enter the Image Number: ")
        json_response = fetch_image_from_server(well_id, image_number)
        if json_response:
            display_image_from_json(json_response)
    else:
        print("Invalid choice. Please enter 1 or 2.")

if __name__ == "__main__":
    main()