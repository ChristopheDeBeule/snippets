# fetch_jwt.py
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


def _post_authenticate(base_url: str, payload: dict, label: str) -> str | None:
    """
    Shared POST to /rest/issuehub/4.0/authenticate.
    Used by all dynamic auth strategies — only the payload differs.
    """
    try:
        response = requests.post(
            f"{base_url}/rest/issuehub/4.0/authenticate",
            json=payload,
            headers={
                "Content-Type": "application/json",
                "Accept":       "application/json, text/plain, */*",
                "Origin":       base_url,
                "Referer":      base_url + "/"
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
                print(f"✅ [AUTH] Successfully authenticated — JWT fetched via {label}.")
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


def _fetch_jwt_via_ado_pat(base_url: str) -> str | None:
    """ADO node auth — uses ADO_PAT with an empty user field."""
    pat = os.getenv("ADO_PAT")
    if not pat:
        print("❌ [ERROR] ADO_PAT not found in .env — cannot authenticate.")
        return None

    print("🔐 [AUTH] Authenticating via ADO PAT...")
    return _post_authenticate(
        base_url,
        payload={"authProtocol": "basic", "password": pat, "user": ""},
        label="ADO PAT"
    )


def _fetch_jwt_via_snow(base_url: str) -> str | None:
    """
    ServiceNow node auth — supports two strategies:

    Strategy A — SNOW_TOKEN (node env token, takes priority):
        Set in the Exalate node's environment variables panel.
        Sent as the password with an empty user field.

    Strategy B — SNOW_USER + SNOW_PWD (username/password):
        Standard ServiceNow credentials.
    """
    token  = os.getenv("SNOW_TOKEN")
    user   = os.getenv("SNOW_USER")
    pwd    = os.getenv("SNOW_PWD")

    if token:
        print("🔐 [AUTH] Authenticating via ServiceNow node env token (SNOW_TOKEN)...")
        return _post_authenticate(
            base_url,
            payload={"authProtocol": "basic", "password": token, "user": ""},
            label="SNOW node token"
        )

    if user and pwd:
        print("🔐 [AUTH] Authenticating via ServiceNow username/password...")
        return _post_authenticate(
            base_url,
            payload={"authProtocol": "basic", "password": pwd, "user": user},
            label="SNOW credentials"
        )

    print("❌ [ERROR] No ServiceNow credentials found.")
    print("   Provide one of:")
    print("     SNOW_TOKEN=<node-env-token>")
    print("     SNOW_USER=<username> + SNOW_PWD=<password>")
    return None


# ── Auth strategy registry ───────────────────────────────────────────────────
# Add new platforms here. Each entry maps to a detector and a fetcher.
# detector: callable() -> bool  (True if this platform's creds are present)
# fetcher:  callable(base_url) -> str | None

_AUTH_STRATEGIES = [
    {
        "platform": "ADO",
        "detector": lambda: bool(os.getenv("ADO_PAT")),
        "fetcher":  _fetch_jwt_via_ado_pat,
    },
    {
        "platform": "ServiceNow",
        "detector": lambda: bool(os.getenv("SNOW_TOKEN") or (os.getenv("SNOW_USER") and os.getenv("SNOW_PWD"))),
        "fetcher":  _fetch_jwt_via_snow,
    },
    # Future platforms — uncomment and implement as needed:
    # {
    #     "platform": "Jira",
    #     "detector": lambda: bool(os.getenv("JIRA_USER") and os.getenv("JIRA_TOKEN")),
    #     "fetcher":  _fetch_jwt_via_jira,
    # },
    # {
    #     "platform": "Zendesk",
    #     "detector": lambda: bool(os.getenv("ZD_USER") and os.getenv("ZD_TOKEN")),
    #     "fetcher":  _fetch_jwt_via_zendesk,
    # },
    # {
    #     "platform": "Freshservice",
    #     "detector": lambda: bool(os.getenv("FS_API_KEY")),
    #     "fetcher":  _fetch_jwt_via_freshservice,
    # },
]


def _detect_and_fetch_jwt(base_url: str) -> str | None:
    """
    Walks through _AUTH_STRATEGIES in order and uses the first platform
    whose credentials are present in the environment.
    """
    for strategy in _AUTH_STRATEGIES:
        if strategy["detector"]():
            print(f"⚙️  [AUTH] Detected platform: {strategy['platform']}")
            return strategy["fetcher"](base_url)

    print("❌ [ERROR] No supported credentials found in .env.")
    print("   Provide one of the following sets of variables:")
    print("     ADO:           ADO_PAT")
    print("     ServiceNow:    SNOW_TOKEN  or  SNOW_USER + SNOW_PWD")
    return None


def get_dynamic_jwt(target_url: str) -> str | None:
    """
    Public entry point called by ExalateClient.

    Priority:
      1. EXALATE_JWT in .env and still valid → use it directly
      2. EXALATE_JWT present but expired → auto-fetch via detected platform
      3. EXALATE_JWT absent → auto-fetch via detected platform
    """
    base_url = target_url.rstrip("/")
    static_jwt = os.getenv("EXALATE_JWT")

    if static_jwt:
        if not _is_jwt_expired(static_jwt):
            print("✅ [AUTH] Using EXALATE_JWT from .env — still valid.")
            return static_jwt
        print("⚠️  [AUTH] EXALATE_JWT in .env has expired — fetching fresh token...")
        return _detect_and_fetch_jwt(base_url)

    print("⚠️  [AUTH] No EXALATE_JWT in .env — fetching token dynamically...")
    return _detect_and_fetch_jwt(base_url)