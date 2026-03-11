# Exalate API Batch Tool

A modular Python tool designed to execute GET, POST, and PUT requests against an Exalate node. This tool supports single-request lookups and batch processing via CSV with dynamic URL replacement.

---

## 📖 1. API & Swagger Documentation

This tool interacts with the Exalate REST API 4.0. You can explore all available endpoints, required parameters, and response models via the Swagger UI on your specific node.

- **Swagger URL:** `https://<your-node-url>/swagger`
- **Authentication:** Requests are authenticated via the `X-exalate-jwt` header. The script handles this automatically using the token in your `.env` file.
    - To get the `X-exalate-jwt` Follow these docs (Exalate internal use Only)
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

## 🔐 3. Configuration (`.env`)

Create a file named `.env` in the root folder. This file stores your private credentials.

```plaintext
EXALATE_JWT="your_jwt_token_here"
NODE_URL="https://zendesknode-aaaa-bbbb-cccc-dddd.exalate.cloud"
```

---

## 🚀 4. Usage Guide

The script's behavior is controlled by the variables at the top of `init.py`.

### Scenario A: GET Connections (Single Run)

Use this to find your `CONNECTION_ID` or check node connectivity. No CSV file is required for GET requests.

1. Set `REQUEST_METHOD = "GET"`
2. Set `ENDPOINT_TEMPLATE = "/rest/issuehub/4.0/connections"`
3. Run: `python init.py`

### Scenario B: Batch Trigger Sync (POST/PUT)

To trigger syncs or update entities in bulk, you must provide a CSV dump (default: `Jira.csv`).

1. **Prepare CSV:** Ensure `file.csv` has a column named `Issue id` OR `ID` (Depends on the CSV, `file.csv` is the name of your downloaded csv this depends on the platform you use,[Jira, Zendesk, ADO, ServiceNow, etc..])
2. Set `REQUEST_METHOD = "POST"`
3. Set `CONNECTION_ID = "154"` (Found via the get request in the example above)
4. Set `ENDPOINT_TEMPLATE = "/rest/issuehub/4.0/entity/{ticket_id}/connection/" + CONNECTION_ID + "/exalate"`
5. Run: `python init.py`

---

## 📊 5. Results & Reporting

### Terminal Feedback

The script provides real-time checkmarks as it processes the CSV:

- `Processing JIRA-101... ✅` — Request successful (200/201/202)
- `Processing JIRA-102... ❌ (Status: 404)` — Request failed

### CSV Report (`exalate_report.csv`)

After every batch run, a report is generated for troubleshooting. This captures the exact reason for any failed triggers.

| Ticket Number | Status  | HTTP Code | Error Reason              | Timestamp           |
|---------------|---------|-----------|---------------------------|---------------------|
| JIRA-101      | SUCCESS | 200       |                           | 2026-03-11 12:00:00 |
| JIRA-102      | FAILED  | 404       | Issue 'JIRA-102' not found | 2026-03-11 12:00:05 |

---

## 📂 6. Project Structure

```plaintext
exalate-api-automation/
├── .env                # Private credentials (ignored by Git)
├── .gitignore          # Prevents pushing secrets to GitHub
├── Jira.csv            # Required source data for POST/PUT
├── init.py             # Main entry point (The Runner)
├── requirements.txt    # Library dependencies
├── exalate_report.csv  # Auto-generated report
└── client/
    ├── __init__.py     # Defines Python package
    └── api_client.py   # OOP Engine for HTTP requests
```

---

## 🛡 7. GitHub Safety

Ensure your `.gitignore` is set up to prevent leaking sensitive data:

```plaintext
venv/
.env
__pycache__/
.DS_Store
exalate_report.csv
```
