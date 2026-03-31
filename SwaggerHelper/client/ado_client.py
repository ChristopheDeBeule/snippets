import os
import requests
import base64
import sys
from dotenv import load_dotenv

load_dotenv()

class ADOClient:
    def __init__(self):
        self.org = os.getenv("ADO_ORG")
        self.pat = os.getenv("ADO_PAT")

        if not self.pat or not self.org:
            print("❌ ERROR: Missing ADO_ORG or ADO_PAT in .env file.")
            sys.exit(1)

        # ADO requires Basic Auth: Base64(":{PAT}")
        auth_str = f":{self.pat}"
        encoded_pat = base64.b64encode(auth_str.encode()).decode()

        self.base_url = f"https://dev.azure.com/{self.org}"
        self.headers = {
            "Content-Type": "application/json",
            "Authorization": f"Basic {encoded_pat}"
        }

    def execute(self, method, path, params=None, data=None):
        if not path.startswith('/'):
            path = f"/{path}"

        if "api-version" not in path:
            separator = "&" if "?" in path else "?"
            path = f"{path}{separator}api-version=7.1"

        url = f"{self.base_url}{path}"

        try:
            response = requests.request(
                method.upper(),
                url,
                headers=self.headers,
                json=data,
                params=params
            )
            return {
                "success": response.ok,
                "status": response.status_code,
                "data": response.json() if response.content else {},
                "error_body": response.text if not response.ok else None
            }
        except Exception as e:
            return {
                "success": False,
                "status": "Exception",
                "data": {},
                "error_body": str(e)
            }