import os
import requests
import sys
from dotenv import load_dotenv
from client.fetch_jwt import get_dynamic_jwt

load_dotenv()

class ExalateClient:
    def __init__(self):
        self.node_url = os.getenv("NODE_URL", "").rstrip('/')
        self.token = os.getenv("EXALATE_JWT")

        if not self.node_url:
            print("❌ ERROR: Missing NODE_URL in .env file.")
            sys.exit(1)

        self.headers = None

    def execute(self, method, path, data=None):
        allowed_methods = ["GET", "POST", "PUT"]

        if not self.token:
            self.token = get_dynamic_jwt(self.node_url)
        self.headers = {
            "Content-Type": "application/json",
            "X-exalate-jwt": self.token
        }
        if method.upper() not in allowed_methods:
            print(f"❌ CRITICAL ERROR: '{method}' is not a permitted request type.")
            print(f"   Please use only: {', '.join(allowed_methods)}")
            sys.exit(1)

        url = f"{self.node_url}{path}"

        try:
            response = requests.request(
                method.upper(),
                url,
                headers=self.headers,
                json=data
            )
            return {
                "success": response.ok,
                "status": response.status_code,
                "data": response.json() if response.content else {"message": "No content returned"},
                "error_body": response.text if not response.ok else None
            }
        except Exception as e:
            return {
                "success": False,
                "status": "Exception",
                "data": {},
                "error_body": str(e)
            }