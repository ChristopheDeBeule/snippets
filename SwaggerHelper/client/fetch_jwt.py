# jwt_extractor.py
import re
import json
import base64
import requests
from datetime import datetime, timezone
from dotenv import load_dotenv
import os

load_dotenv()

JWT_PATTERN = re.compile(r'eyJ[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+')


def _is_jwt_expired(token: str) -> bool:
    try:
        payload = token.split(".")[1]
        payload += "=" * (4 - len(payload) % 4)
        decoded = json.loads(base64.urlsafe_b64decode(payload))
        exp = decoded.get("exp")
        if not exp:
            return True
        expired = datetime.now(timezone.utc).timestamp() > exp
        if not expired:
            exp_dt = datetime.fromtimestamp(exp, tz=timezone.utc)
            print(f"[ℹ️  INFO] Token valid until: {exp_dt.strftime('%Y-%m-%d %H:%M:%S UTC')}")
        return expired
    except Exception:
        return True


def _fetch_jwt_via_pat(base_url: str) -> str | None:
    pat = os.getenv("ADO_PAT")
    if not pat:
        print("❌ [ERROR] No EXALATE_JWT in .env and no ADO_PAT found — cannot authenticate.")
        return None

    print("🔐 [AUTH] Authenticating via PAT...")

    try:
        response = requests.post(
            f"{base_url}/rest/issuehub/4.0/authenticate",
            json={"authProtocol": "basic", "password": pat, "user": ""},
            headers={
                "Content-Type": "application/json",
                "Accept": "application/json, text/plain, */*",
                "Origin": base_url,
                "Referer": base_url + "/"
            },
            timeout=10,
        )

        print(f"📡 [AUTH] Response status: {response.status_code}")

        if not response.ok:
            print(f"❌ [ERROR] Authentication failed — HTTP {response.status_code}: {response.text[:200]}")
            return None

        for source in (response.headers.get("X-Exalate-Jwt", ""), response.text):
            match = JWT_PATTERN.search(source)
            if match:
                print("✅ [AUTH] Successfully authenticated — JWT fetched via PAT.")
                return match.group()

        print("❌ [ERROR] Authenticated but no JWT found in response.")
        print(f"🔍 [DEBUG] Headers: {dict(response.headers)}")
        print(f"🔍 [DEBUG] Body: {response.text[:300]}")
        return None

    except requests.exceptions.ConnectionError:
        print(f"❌ [ERROR] Could not connect to {base_url}.")
        return None
    except requests.exceptions.Timeout:
        print("❌ [ERROR] Request timed out.")
        return None
    except Exception as e:
        print(f"❌ [ERROR] Unexpected error: {e}")
        return None


def get_dynamic_jwt(target_url: str) -> str | None:
    base_url = target_url.rstrip("/")
    static_jwt = os.getenv("EXALATE_JWT")

    if static_jwt:
        if not _is_jwt_expired(static_jwt):
            print("✅ [AUTH] Using EXALATE_JWT from .env — still valid.")
            return static_jwt
        print("⚠️  [AUTH] EXALATE_JWT in .env has expired — fetching fresh token via PAT...")
        return _fetch_jwt_via_pat(base_url)

    print("⚠️  [AUTH] No EXALATE_JWT in .env — fetching token via PAT...")
    return _fetch_jwt_via_pat(base_url)