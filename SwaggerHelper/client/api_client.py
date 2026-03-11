import os
import requests
import sys
from dotenv import load_dotenv

load_dotenv()

class ExalateClient:
    def __init__(self):
        self.node_url = os.getenv("NODE_URL", "").rstrip('/')
        self.token = os.getenv("EXALATE_JWT")
        
        if not self.token or not self.node_url:
            print("❌ ERROR: Missing EXALATE_JWT or NODE_URL in .env file.")
            sys.exit(1)

        self.headers = {
            "Content-Type": "application/json",
            "X-exalate-jwt": self.token
        }

    def execute(self, method, path, data=None):
        # --- METHOD VALIDATION VARIABLE ---
        allowed_methods = ["GET", "POST", "PUT"]
        
        if method.upper() not in allowed_methods:
            print(f"❌ CRITICAL ERROR: '{method}' is not a permitted request type.")
            print(f"Please use only: {', '.join(allowed_methods)}")
            sys.exit(1) # Throw error and exit

        url = f"{self.node_url}{path}"
        
        try:
            response = requests.request(method.upper(), url, headers=self.headers, json=data)
            return {
                "success": response.ok,
                "status": response.status_code,
                "data": response.json() if response.content else {"message": "No content returned"},
                "error_body": response.text if not response.ok else None
            }
        except Exception as e:
            return {"success": False, "status": "Exception", "message": str(e)}