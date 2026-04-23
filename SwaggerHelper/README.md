# Exalate API Batch Tool

A modular Python tool designed to execute GET, POST, and PUT requests against an Exalate node and Azure DevOps. Supports single-request lookups, batch processing via CSV, and automated daily sync triggering via a cron-compatible CRON mode.

## Why this tool exists

Exalate has a native trigger system, but it is designed for live syncs — it fires when a ticket is created or updated based on a condition you define (e.g. a label, status, or field value). It is not built to run on a schedule at a specific time of day.

This tool fills that gap. For Azure DevOps specifically, it automatically fetches all work items created on the current day and triggers an Exalate sync for each one — no manual CSV export needed. This makes it ideal for a daily end-of-day cron job to ensure nothing created that day was missed.

CSV-based processing is still fully supported for cases where you need to manually control which tickets are synced.

---

## 📖 1. API & Swagger Documentation

This tool interacts with the Exalate REST API 4.0. You can explore all available endpoints, required parameters, and response models via the Swagger UI on your specific node.

**Swagger URL:** `https://<your-node-url>/swagger`  
**Authentication:** Requests are authenticated via the `X-exalate-jwt` header. The script handles this automatically — see Section 3.

---

## 🔐 2. Authentication & JWT Handling

The script handles JWT authentication automatically. The behaviour depends on what is provided in your `.env` file.

### Priority order

| Scenario | Behaviour |
|---|---|
| `EXALATE_JWT` present and not expired | Used directly — no network call made |
| `EXALATE_JWT` present but expired | Fresh token fetched dynamically via platform credentials |
| `EXALATE_JWT` absent | Fresh token fetched dynamically via platform credentials |
| No `EXALATE_JWT` and no platform credentials | Error — cannot authenticate |

### How dynamic fetching works

When no valid static JWT is available, the script detects your platform automatically based on which credential variables are present in `.env`, then authenticates against the Exalate node via:

```
POST /rest/issuehub/4.0/authenticate
```

The JWT is extracted from the response and used for all subsequent requests in that run. Nothing is written back to your `.env` — the token lives only in memory for the duration of the script.

### Supported platforms for dynamic fetch

| Platform | Required `.env` variables | Auth method |
|---|---|---|
| **Azure DevOps** | `ADO_PAT` | PAT sent as password, empty user |
| **ServiceNow** | `SNOW_TOKEN` *(preferred)* | Node env token, empty user |
| **ServiceNow** | `SNOW_USER` + `SNOW_PWD` | Username + password |

> **ServiceNow token vs credentials:** If both `SNOW_TOKEN` and `SNOW_USER`/`SNOW_PWD` are present, `SNOW_TOKEN` takes priority.

> **Other node types (Jira, Zendesk, Freshservice):** Dynamic fetch is not yet implemented. Provide `EXALATE_JWT` directly in your `.env` for these platforms.

### Example terminal output

When `EXALATE_JWT` is present and valid:
```
[ℹ️  INFO] Token valid until: 2026-04-02 06:19:15 UTC
✅ [AUTH] Using EXALATE_JWT from .env — still valid.
```

When fetching dynamically via ADO PAT:
```
⚠️  [AUTH] No EXALATE_JWT in .env — fetching token dynamically...
⚙️  [AUTH] Detected platform: ADO
🔐 [AUTH] Authenticating via ADO PAT...
📡 [AUTH] Response status: 200
✅ [AUTH] Successfully authenticated — JWT fetched via ADO PAT.
```

When fetching dynamically via ServiceNow:
```
⚠️  [AUTH] No EXALATE_JWT in .env — fetching token dynamically...
⚙️  [AUTH] Detected platform: ServiceNow
🔐 [AUTH] Authenticating via ServiceNow node env token (SNOW_TOKEN)...
📡 [AUTH] Response status: 200
✅ [AUTH] Successfully authenticated — JWT fetched via SNOW node token.
```

---

## 🛠 3. Installation (macOS Setup)

macOS prevents installing Python packages globally to protect the system. You must use a Virtual Environment (`venv`) to install the required libraries.

### Step 1: Create a Virtual Environment

Navigate to your project folder in the terminal and run:

```bash
python3 -m venv venv
```

### Step 2: Activate the Environment

> **Note:** You must run this every time you open a new terminal window to run the script.

```bash
source venv/bin/activate
```

Your terminal prompt will now show `(venv)` at the beginning.

### Step 3: Install Dependencies

```bash
pip install -r requirements.txt
```

---

## 🐧 4. Installation (Linux Server Setup)

On most Linux servers Python 3 is already installed. You can verify with:

```bash
python3 --version
```

### Option A: Virtual Environment (recommended, mirrors macOS setup)

```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

When running via cron, reference the venv Python directly so the correct packages are used:

```bash
0 18 * * * cd /path/to/project && /path/to/project/venv/bin/python3 init.py >> /path/to/logs/cron.log 2>&1
```

### Option B: System-wide pip install (simpler, no venv)

```bash
pip install requests python-dotenv
```

Cron job:

```bash
0 18 * * * cd /path/to/project && python3 init.py >> /path/to/logs/cron.log 2>&1
```

### Credentials on Linux

**Option 1 — `.env` file** (same as macOS, no changes needed to the script):

Create a `.env` file in the project root exactly as described in Section 5 below.

**Option 2 — System environment variables** (no `.env` file needed):

Add your credentials to `~/.bashrc` or `~/.profile`:

```bash
export NODE_URL="https://your-node.exalate.cloud"
export EXALATE_JWT="your_jwt_token_here"
export ADO_ORG="your-ado-org-name"
export ADO_PAT="your-ado-personal-access-token"
export ADO_PROJECT="your-ado-project-name"
```

Then reload:

```bash
source ~/.bashrc
```

`python-dotenv` will automatically pick up real environment variables if no `.env` file is present — no changes to the script required either way.

---

## 🔧 5. Configuration (`.env`)

Create a file named `.env` in the root folder. This file stores your private credentials.

### ADO node — dynamic JWT fetch via PAT

Use this when your node is an `azurenode`. No `EXALATE_JWT` needed — the script fetches it automatically using your `ADO_PAT`.

```env
# Exalate
NODE_URL="https://azurenode-xxxx-xxxx-xxxx-xxxx.exalate.cloud"
# EXALATE_JWT intentionally omitted — fetched dynamically via ADO_PAT

# ADO
ADO_ORG="your-ado-org-name"
ADO_PAT="your-azure-devops-pat-here"
ADO_PROJECT="your-project-name"
```

### ServiceNow node — dynamic JWT fetch via node env token *(recommended)*

Use this when your node is a `snownode` and you have a token set in the Exalate node's environment variables panel.

```env
# Exalate
NODE_URL="https://snownode-xxxx-xxxx-xxxx-xxxx.exalate.cloud"
# EXALATE_JWT intentionally omitted — fetched dynamically via SNOW_TOKEN

# ServiceNow
SNOW_TOKEN="your-snow-node-env-token-here"
```

### ServiceNow node — dynamic JWT fetch via username/password

Use this if you don't have a node env token and prefer to authenticate with standard ServiceNow credentials.

```env
# Exalate
NODE_URL="https://snownode-xxxx-xxxx-xxxx-xxxx.exalate.cloud"
# EXALATE_JWT intentionally omitted — fetched dynamically via SNOW credentials

# ServiceNow
SNOW_USER="your-servicenow-username"
SNOW_PWD="your-servicenow-password"
```

### All other node types — static JWT

Use this for `jcloudnode`, `zendesknode`, and any non-ADO/non-SNOW node. Provide `EXALATE_JWT` directly.

```env
# Exalate
NODE_URL="https://jcloudnode-xxxx-xxxx-xxxx-xxxx.exalate.cloud"
EXALATE_JWT="your_hardcoded_jwt_here"
```

> ADO variables (`ADO_ORG`, `ADO_PAT`, `ADO_PROJECT`) are only required when using `REQUEST_METHOD = "CRON"`. All other modes work without them.

---

## 🚀 6. Usage Guide

The script's behavior is controlled by `REQUEST_METHOD` at the top of `init.py`. There are four modes.

### Scenario A: List Connections

Use this to find your `CONNECTION_ID`, check node connectivity, or verify the remote instance a connection is linked to. No CSV file is required.

Pass `True` as a CLI argument when running the script:

```bash
python3 init.py True
```

This will print a dynamically formatted table of all connections:

```
✅ Found 2 Connection(s):
ID   | CONNECTION NAME          | STATUS      | CONNECTED TO
--------------------------------------------------------------
154  | ADO <> Jira Prod         | ACTIVE      | jcloudnode
155  | ADO <> Zendesk Staging   | DEACTIVATED | zendesknode
```

Valid CLI arguments:

| Command | Behaviour |
|---|---|
| `python3 init.py` | Runs normally (default mode) |
| `python3 init.py False` | Runs normally (explicit) |
| `python3 init.py True` | Prints connections table & exits |
| `python3 init.py "True"` | ❌ Invalid — exits with error |
| `python3 init.py 1` | ❌ Invalid — exits with error |

### Scenario B: CRON Mode — Auto Sync Today's ADO Work Items

This is the primary mode for daily automation. It queries Azure DevOps for all work items created today using WIQL, then triggers an Exalate sync for each one via POST.

Requires ADO credentials (`ADO_ORG`, `ADO_PAT`, `ADO_PROJECT`) in your `.env`.

1. Set `REQUEST_METHOD = "CRON"` in `init.py`
2. Set `CONNECTION_ID = "154"` (found via Scenario A)
3. Run manually or via cron:

```bash
python3 init.py
```

To schedule as a daily cron job (e.g. every day at 6 PM):

```bash
0 18 * * * cd /path/to/project && python3 init.py >> /path/to/logs/cron.log 2>&1
```

If using a venv, replace `python3` with the full venv path: `/path/to/project/venv/bin/python3`

### Scenario C: Batch Trigger Sync via CSV

Use this to trigger syncs in bulk from a CSV file (e.g. a Jira or Zendesk export). ADO credentials are not required for this mode.

1. Prepare CSV: ensure your file has a column named `ID` (configurable via `COLUMN_NAME` in `init.py`)
2. Set `REQUEST_METHOD = "BATCH_CSV"` in `init.py`
3. Set `CONNECTION_ID = "154"`
4. Run:

```bash
python3 init.py
```

### Scenario D: Single GET Request

Use this to inspect a specific Exalate endpoint directly. ADO credentials are not required for this mode.

1. Set `REQUEST_METHOD = "GET"`
2. Set `ENDPOINT_TEMPLATE` to the desired path (e.g. `"/rest/issuehub/4.0/connections"`)
3. Run:

```bash
python3 init.py
```

---

## 📊 7. Results & Reporting

### Terminal Feedback

The script provides real-time output as it processes tickets:

```
🔌 Connecting to Azure DevOps...
✅ Authenticated with ADO Org: my-org
🔍 Querying ADO for work items created today...
✅ Found 3 work item(s) created today: ['101', '102', '103']

🚀 Starting Exalate sync for 3 ticket(s)...

  Processing 101... ✅
  Processing 102... ✅
  Processing 103... ❌ (Status: 404)

--- Batch Complete ---
✅ Success: 2
❌ Failed:  1
📄 Report saved to: exalate_report_20260331_180000.csv
```

### CSV Report

After every batch run, a timestamped report is generated for troubleshooting:

| Ticket ID | Status | HTTP Code | Error Reason | Timestamp |
|---|---|---|---|---|
| 101 | SUCCESS | 200 | | 2026-03-31 18:00:01 |
| 102 | SUCCESS | 200 | | 2026-03-31 18:00:02 |
| 103 | FAILED | 404 | Issue '103' not found | 2026-03-31 18:00:03 |

---

## 📂 8. Project Structure

```
exalate-api-automation/
├── .env                            # Private credentials (ignored by Git)
├── .gitignore                      # Prevents pushing secrets to GitHub
├── tickets.csv                     # Required source data for BATCH_CSV mode
├── init.py                         # Main entry point (The Runner)
├── requirements.txt                # Library dependencies
├── exalate_report_<timestamp>.csv  # Auto-generated report after each run
└── client/
    ├── __init__.py                 # Defines Python package
    ├── api_client.py               # ExalateClient — handles Exalate HTTP requests
    ├── ado_client.py               # ADOClient — handles Azure DevOps HTTP requests
    └── fetch_jwt.py                # Dynamic JWT fetching — auto-detects platform from .env
```

---

## 🛡 9. GitHub Safety

Ensure your `.gitignore` is set up to prevent leaking sensitive data:

```
venv/
.env
__pycache__/
.DS_Store
exalate_report_*.csv
```