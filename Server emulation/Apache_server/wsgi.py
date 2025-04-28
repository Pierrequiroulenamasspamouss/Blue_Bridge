#!/usr/bin/env python

import os
import sys

# Add the current directory to the Python path
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, current_dir)

# Import the application from the apache_server module
try:
    from apache_server import application
except ImportError as e:
    import traceback
    traceback.print_exc()
    raise e

# The application variable is used by the WSGI server to access the Flask app
if __name__ == '__main__':
    # If run directly, initialize the Flask development server
    from apache_server import app
    app.run(debug=True) 