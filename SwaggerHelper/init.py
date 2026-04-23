import csv
import json
import sys
import os
from datetime import datetime
from client.api_client import ExalateClient
from client.ado_client import ADOClient

# ==============================================================================
# CONFIGURATION VARIABLES
# ==============================================================================

# --- MODE ---
# "GET"        → Single Exalate GET (debug/inspect)
# "BATCH_CSV"  → Read IDs from tickets.csv and POST to Exalate
# "CRON"       → Query ADO for today's work items, then POST each to Exalate
#                Note: CRON mode requires ADO credentials (ADO_ORG + ADO_PAT)
REQUEST_METHOD = "BATCH_CSV"

# --- EXALATE ---
CONNECTION_ID = "296"
ENDPOINT_TEMPLATE = "/rest/issuehub/4.0/entity/{ticket_id}/connection/" + CONNECTION_ID + "/exalate"

# --- ADO (only required when REQUEST_METHOD = "CRON") ---
ADO_PROJECT = os.getenv("ADO_PROJECT", "defaultProjectName")

# --- CSV (used only in BATCH_CSV mode) ---
CSV_FILE = 'tickets.csv'
COLUMN_NAME = "ID"

# --- REPORTING ---
REPORT_FILE = f"exalate_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"

# ==============================================================================


def parse_args() -> bool:
    """
    Parses the optional CLI argument for fetching connections.

    Usage:
      python3 init.py          -> GET_CONNECTIONS = False (default, runs REQUEST_METHOD)
      python3 init.py True     -> GET_CONNECTIONS = True  (prints connections table)
      python3 init.py False    -> GET_CONNECTIONS = False (runs REQUEST_METHOD)
      anything else            -> exits with error

    Note: Quoted values like "True" or "False" are intentionally rejected.
    Only bare True or False are valid.
    """
    valid = {"true": True, "false": False}

    if len(sys.argv) == 1:
        return False

    if len(sys.argv) == 2:
        arg = sys.argv[1].strip().lower()
        if arg in valid:
            return valid[arg]

    print("❌ Invalid argument.")
    print("   Usage:  python3 init.py [True|False]")
    print("   Valid parameters: True, False, or no argument at all.")
    sys.exit(1)


def get_connections(ex_client: ExalateClient):
    """Fetch and print all Exalate connections as a formatted table."""
    print("🔍 Fetching Exalate connections...\n")
    res = ex_client.execute("GET", "/rest/issuehub/4.0/connections")

    if res["success"]:
        connections = res["data"].get("results", [])

        max_id_len      = max((len(str(conn.get("id", "")))     for conn in connections), default=2)
        max_name_len    = max((len(str(conn.get("name", "")))   for conn in connections), default=15)
        max_status_len  = max((len(str(conn.get("status", ""))) for conn in connections), default=6)
        max_remote_len  = max((len(parse_remote(conn))          for conn in connections), default=10)

        max_id_len      = max(max_id_len,     len("ID"))
        max_name_len    = max(max_name_len,   len("CONNECTION NAME"))
        max_status_len  = max(max_status_len, len("STATUS"))
        max_remote_len  = max(max_remote_len, len("CONNECTED TO"))

        total_width = max_id_len + max_name_len + max_status_len + max_remote_len + 11

        print(f"✅ Found {res['data'].get('total', 0)} Connection(s):")
        print(f"{'ID':<{max_id_len}} | {'CONNECTION NAME':<{max_name_len}} | {'STATUS':<{max_status_len}} | {'CONNECTED TO':<{max_remote_len}}")
        print("-" * total_width)
        for conn in connections:
            print(f"{str(conn.get('id', '')):<{max_id_len}} | {str(conn.get('name', '')):<{max_name_len}} | {str(conn.get('status', '')):<{max_status_len}} | {parse_remote(conn):<{max_remote_len}}")
    else:
        print(f"❌ Failed ({res['status']}): {res.get('error_body')}")


def parse_remote(conn: dict) -> str:
    """Extract just the subdomain prefix from remoteUrl."""
    remote_url = conn.get("communication", {}).get("remoteUrl", "")
    if not remote_url:
        return "N/A"
    subdomain = remote_url.replace("https://", "").replace("http://", "").split(".")[0]
    return subdomain.split("-")[0]


def _init_ado_client() -> ADOClient | None:
    """
    Initialise ADOClient only when ADO credentials are present.
    Returns None (with a clear message) if ADO_ORG or ADO_PAT are missing.
    CRON mode is the only mode that requires this client.
    """
    ado_org = os.getenv("ADO_ORG")
    ado_pat = os.getenv("ADO_PAT")

    if not ado_org or not ado_pat:
        print("❌ CRON mode requires ADO credentials.")
        print("   Add the following to your .env file:")
        print("     ADO_ORG=<your-ado-org>")
        print("     ADO_PAT=<your-personal-access-token>")
        print("     ADO_PROJECT=<your-project-name>")
        return None

    try:
        return ADOClient()
    except Exception as e:
        print(f"❌ ADO Initialization Error: {e}")
        return None


def preflight_ado_check(ado_client: ADOClient, project: str) -> bool:
    """
    Verify ADO connectivity. A 404 is still a valid auth response
    (the item just doesn't exist), so we treat it as a pass.
    """
    print("🔌 Connecting to Azure DevOps...")
    res = ado_client.execute("GET", f"/{project}/_apis/wit/workitems/1")

    if res["success"] or res["status"] == 404:
        print(f"✅ Authenticated with ADO Org: {os.getenv('ADO_ORG')}")
        return True
    else:
        print(f"❌ ADO Auth Failed (Status {res['status']}). Check your PAT.")
        print(f"   Details: {res.get('error_body')}")
        return False


def fetch_todays_ado_ids(ado_client: ADOClient, project: str) -> list:
    """
    WIQL POST query to get all work items created today.
    Returns a list of ID strings.
    """
    print(f"🔍 Querying ADO for work items created today in project '{project}'...")

    wiql_path = f"/{project}/_apis/wit/wiql"
    wiql_body = {
        "query": "SELECT [System.Id] FROM WorkItems WHERE [System.CreatedDate] >= @Today"
    }

    res = ado_client.execute("POST", wiql_path, data=wiql_body)

    if not res["success"]:
        print(f"❌ WIQL query failed (Status: {res['status']}): {res.get('error_body', 'Unknown error')}")
        return []

    work_item_refs = res["data"].get("workItems", [])

    if not work_item_refs:
        print("ℹ️  No work items created today.")
        return []

    ids = [str(ref["id"]) for ref in work_item_refs]
    print(f"✅ Found {len(ids)} work item(s) created today: {ids}")
    return ids


def exalate_sync(ex_client: ExalateClient, ticket_id: str) -> dict:
    """POST to Exalate to trigger sync for a single ticket ID."""
    path = ENDPOINT_TEMPLATE.replace("{ticket_id}", ticket_id)
    return ex_client.execute("POST", path)


def run_batch(ex_client: ExalateClient, ticket_ids: list) -> list:
    """
    Loop through ticket IDs, trigger Exalate sync on each,
    and return a list of report row dicts.
    """
    report_data = []
    success_count = 0
    fail_count = 0

    print(f"\n🚀 Starting Exalate sync for {len(ticket_ids)} ticket(s)...\n")

    for t_id in ticket_ids:
        print(f"  Processing {t_id}...", end=" ", flush=True)

        res = exalate_sync(ex_client, t_id)

        if res["success"]:
            print("✅")
            success_count += 1
            status_text = "SUCCESS"
            error_msg = ""
        else:
            print(f"❌ (Status: {res['status']})")
            fail_count += 1
            status_text = "FAILED"
            error_msg = (
                res.get("data", {}).get("message")
                or res.get("error_body")
                or "Unknown Error"
            )

        report_data.append({
            "Ticket ID":    t_id,
            "Status":       status_text,
            "HTTP Code":    res["status"],
            "Error Reason": error_msg,
            "Timestamp":    datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        })

    print(f"\n--- Batch Complete ---")
    print(f"✅ Success: {success_count}")
    print(f"❌ Failed:  {fail_count}")
    return report_data


def write_report(report_data: list):
    """Write batch results to a timestamped CSV report file."""
    fieldnames = ["Ticket ID", "Status", "HTTP Code", "Error Reason", "Timestamp"]
    with open(REPORT_FILE, mode='w', newline='', encoding='utf-8') as rf:
        writer = csv.DictWriter(rf, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(report_data)
    print(f"📄 Report saved to: {REPORT_FILE}")


def main():
    GET_CONNECTIONS = parse_args()

    # --- Always init Exalate client ---
    try:
        ex_client = ExalateClient()
    except Exception as e:
        print(f"❌ Initialization Error: {e}")
        return

    # ── MODE: GET CONNECTIONS ──────────────────────────────────────────────────
    if GET_CONNECTIONS:
        get_connections(ex_client)
        return

    # ── MODE: GET ─────────────────────────────────────────────────────────────
    if REQUEST_METHOD.upper() == "GET":
        print(f"🔍 Executing GET request to: {ENDPOINT_TEMPLATE}")
        res = ex_client.execute("GET", ENDPOINT_TEMPLATE)
        if res["success"]:
            print("✅ Success:")
            print(json.dumps(res["data"], indent=4))
        else:
            print(f"❌ Failed ({res['status']}): {res.get('error_body')}")
        return

    # ── MODE: CRON ────────────────────────────────────────────────────────────
    if REQUEST_METHOD.upper() == "CRON":
        # ADO client is only needed here — initialised dynamically based on env vars
        ado_client = _init_ado_client()
        if not ado_client:
            return

        if not preflight_ado_check(ado_client, ADO_PROJECT):
            return

        ticket_ids = fetch_todays_ado_ids(ado_client, ADO_PROJECT)

        if not ticket_ids:
            print("🚫 No tickets to process. Exiting.")
            return

        report_data = run_batch(ex_client, ticket_ids)
        write_report(report_data)
        return

    # ── MODE: BATCH_CSV ───────────────────────────────────────────────────────
    if REQUEST_METHOD.upper() in ("POST", "PUT", "BATCH_CSV"):
        try:
            with open(CSV_FILE, mode='r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                if COLUMN_NAME not in reader.fieldnames:
                    print(f"❌ ERROR: Column '{COLUMN_NAME}' not found in {CSV_FILE}")
                    return
                ticket_ids = [row[COLUMN_NAME] for row in reader if row.get(COLUMN_NAME)]
        except FileNotFoundError:
            print(f"❌ ERROR: File '{CSV_FILE}' not found.")
            return

        report_data = run_batch(ex_client, ticket_ids)
        write_report(report_data)
        return

    print(f"❌ Unknown REQUEST_METHOD: '{REQUEST_METHOD}'. Use CRON, BATCH_CSV, or GET.")


if __name__ == "__main__":
    main()