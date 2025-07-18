import json
import base64
from io import BytesIO
from PIL import Image
import matplotlib.pyplot as plt

def display_image_from_json(json_response):
    # Parse the JSON response
    data = json.loads(json_response)
    
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

response = input("enter the response: ")
display_image_from_json(response)