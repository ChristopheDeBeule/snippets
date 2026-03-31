# Exalate API Batch Tool

A modular Python tool designed to execute GET, POST, and PUT requests against an Exalate node and Azure DevOps. Supports single-request lookups, batch processing via CSV, and automated daily sync triggering via a cron-compatible CRON mode.

### Why this tool exists

Exalate has a native trigger system, but it is designed for **live syncs** - it fires when a ticket is created or updated based on a condition you define (e.g. a label, status, or field value). It is not built to run on a schedule at a specific time of day.

This tool fills that gap. For **Azure DevOps specifically**, it automatically fetches all work items created on the current day and triggers an Exalate sync for each one - no manual CSV export needed. This makes it ideal for a daily end-of-day cron job to ensure nothing created that day was missed.

CSV-based processing is still fully supported for cases where you need to manually control which tickets are synced.

---

## 📖 1. API & Swagger Documentation

This tool interacts with the Exalate REST API 4.0. You can explore all available endpoints, required parameters, and response models via the Swagger UI on your specific node.

- **Swagger URL:** `https://<your-node-url>/swagger`
- **Authentication:** Requests are authenticated via the `X-exalate-jwt` header. The script handles this automatically using the token in your `.env` file.
  - To get the `X-exalate-jwt`, follow these docs (Exalate internal use only):
  - [Get your token](https://exalate.atlassian.net/wiki/spaces/~62b96bf4c9f2df7b6089fc61/history/1560576084/API+request+on+Exalate+node+Swagger)

---

## 🛠 2. Installation (macOS Setup)

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

## 🐧 3. Installation (Linux Server Setup)

On most Linux servers Python 3 is already installed. You can verify with:

```bash
python3 --version
```

You have two options for installing dependencies:

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

If you prefer not to use a venv:

```bash
pip install requests python-dotenv
```

Then your cron job is simply:

```bash
0 18 * * * cd /path/to/project && python3 init.py >> /path/to/logs/cron.log 2>&1
```

### Credentials on Linux

**Option 1 - `.env` file** (same as macOS, no changes needed to the script):

Create a `.env` file in the project root exactly as described in Section 4 below.

**Option 2 - System environment variables** (no `.env` file needed):

Add your credentials to `~/.bashrc` or `~/.profile`:

```bash
export EXALATE_JWT="your_jwt_token_here"
export NODE_URL="https://your-node.exalate.cloud"
export ADO_ORG="your-ado-org-name"
export ADO_PAT="your-ado-personal-access-token"
export ADO_PROJECT="your-ado-project-name"
```

Then reload:

```bash
source ~/.bashrc
```

`python-dotenv` will automatically pick up real environment variables if no `.env` file is present - no changes to the script required either way.

---

## 🔐 4. Configuration (`.env`)

Create a file named `.env` in the root folder. This file stores your private credentials.

```plaintext
EXALATE_JWT="your_jwt_token_here"
NODE_URL="https://zendesknode-aaaa-bbbb-cccc-dddd.exalate.cloud"

# Only required when using CRON mode (Azure DevOps auto-fetch)
ADO_ORG="your-ado-org-name"
ADO_PAT="your-ado-personal-access-token"
ADO_PROJECT="your-ado-project-name"
```

> **ADO variables are optional.** If you are not using ADO, you can leave `ADO_ORG`, `ADO_PAT`, and `ADO_PROJECT` out of your `.env` entirely. The script only initializes the ADO client when `REQUEST_METHOD = "CRON"` is set - so GET, BATCH_CSV, and GET_CONNECTIONS modes will work without them.

---

## 🚀 5. Usage Guide

The script's behavior is controlled by `REQUEST_METHOD` at the top of `init.py`. There are four modes.

---

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

**Valid CLI arguments:**

| Command                  | Behaviour                        |
|--------------------------|----------------------------------|
| `python3 init.py`        | Runs normally (default mode)     |
| `python3 init.py False`  | Runs normally (explicit)         |
| `python3 init.py True`   | Prints connections table & exits |
| `python3 init.py "True"` | ❌ Invalid - exits with error    |
| `python3 init.py 1`      | ❌ Invalid - exits with error    |

---

### Scenario B: CRON Mode - Auto Sync Today's ADO Work Items

This is the primary mode for daily automation. It queries Azure DevOps for all work items created today using WIQL, then triggers an Exalate sync for each one via POST.

> **Requires ADO credentials** in your `.env` or system environment variables.

1. Set `REQUEST_METHOD = "CRON"` in `init.py`
2. Set `CONNECTION_ID = "154"` (found via Scenario A above)
3. Run manually or via cron job:

```bash
python3 init.py
```

**To schedule as a daily cron job** (e.g. every day at 6 PM):

```bash
0 18 * * * cd /path/to/project && python3 init.py >> /path/to/logs/cron.log 2>&1
```

> If using a venv, replace `python3` with the full venv path: `/path/to/project/venv/bin/python3`

---

### Scenario C: Batch Trigger Sync via CSV

Use this to trigger syncs in bulk from a CSV file (e.g. a Jira or Zendesk export). ADO credentials are not required for this mode.

1. **Prepare CSV:** Ensure your file has a column named `ID` (configurable via `COLUMN_NAME` in `init.py`)
2. Set `REQUEST_METHOD = "BATCH_CSV"` in `init.py`
3. Set `CONNECTION_ID = "154"`
4. Set `ENDPOINT_TEMPLATE = "/rest/issuehub/4.0/entity/{ticket_id}/connection/" + CONNECTION_ID + "/exalate"`
5. Run:

```bash
python3 init.py
```

---

### Scenario D: Single GET Request

Use this to inspect a specific Exalate endpoint directly. ADO credentials are not required for this mode.

1. Set `REQUEST_METHOD = "GET"`
2. Set `ENDPOINT_TEMPLATE` to the desired path (e.g. `"/rest/issuehub/4.0/connections"`)
3. Run:

```bash
python3 init.py
```

---

## 📊 6. Results & Reporting

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

| Ticket ID | Status  | HTTP Code | Error Reason               | Timestamp           |
|-----------|---------|-----------|----------------------------|---------------------|
| 101       | SUCCESS | 200       |                            | 2026-03-31 18:00:01 |
| 102       | SUCCESS | 200       |                            | 2026-03-31 18:00:02 |
| 103       | FAILED  | 404       | Issue '103' not found      | 2026-03-31 18:00:03 |

---

## 📂 7. Project Structure

```plaintext
exalate-api-automation/
├── .env                            # Private credentials (ignored by Git)
├── .gitignore                      # Prevents pushing secrets to GitHub
├── tickets.csv                     # Required source data for BATCH_CSV mode
├── init.py                         # Main entry point (The Runner)
├── requirements.txt                # Library dependencies
├── exalate_report_<timestamp>.csv  # Auto-generated report after each run
└── client/
    ├── __init__.py                 # Defines Python package
    ├── api_client.py               # ExalateClient - handles Exalate HTTP requests
    └── ado_client.py               # ADOClient - handles Azure DevOps HTTP requests
```

---

## 🛡 8. GitHub Safety

Ensure your `.gitignore` is set up to prevent leaking sensitive data:

```plaintext
venv/
.env
__pycache__/
.DS_Store
exalate_report_*.csv
```