import csv
import json
import sys
from datetime import datetime
from client.api_client import ExalateClient

# ==============================================================================
# CONFIGURATION VARIABLES
# ==============================================================================
# 1. Set the method: "GET", "POST", or "PUT"
REQUEST_METHOD = "POST" 

# 2. Set the Connection ID (found via GET Connections)
CONNECTION_ID = "154"

# 3. Set the Endpoint Template. 
# Use {ticket_id} as a placeholder—the script swaps this for CSV values.
ENDPOINT_TEMPLATE = "/rest/issuehub/4.0/entity/{ticket_id}/connection/" + CONNECTION_ID + "/exalate"

# 4. File Configuration
CSV_FILE = 'tickets.csv'
COLUMN_NAME = "ID" # The header name in your CSV
REPORT_FILE = "exalate_report.csv" # The file created after running
# ==============================================================================

def main():
    # Initialize the Engine
    try:
        client = ExalateClient()
    except ValueError as e:
        print(e)
        return

    # --- SCENARIO 1: SINGLE GET REQUEST ---
    if REQUEST_METHOD.upper() == "GET":
        print(f"🔍 Executing GET request to: {ENDPOINT_TEMPLATE}...")
        res = client.execute("GET", ENDPOINT_TEMPLATE)
        
        if res["success"]:
            # Special formatting for the connections list
            if "/connections" in ENDPOINT_TEMPLATE:
                connections = res["data"].get("results", [])
                print(f"\n✅ Found {res['data'].get('total', 0)} Connection(s):")
                print(f"{'ID':<10} | {'CONNECTION NAME'}")
                print("-" * 40)
                for conn in connections:
                    print(f"{conn.get('id'):<10} | {conn.get('name')}")
            else:
                print(f"✅ Success (200):")
                print(json.dumps(res["data"], indent=4))
        else:
            print(f"❌ Failed: {res['status']}")
            print(f"Error Details: {res.get('error_body')}")
        return

    # --- SCENARIO 2: BATCH POST/PUT REQUESTS ---
    print(f"🚀 Starting {REQUEST_METHOD} batch process for Connection {CONNECTION_ID}...")
    
    report_data = [] 
    success_count = 0
    fail_count = 0

    try:
        with open(CSV_FILE, mode='r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            
            if COLUMN_NAME not in reader.fieldnames:
                print(f"❌ ERROR: Could not find column '{COLUMN_NAME}' in {CSV_FILE}")
                return

            for row in reader:
                t_id = row.get(COLUMN_NAME)
                if not t_id:
                    continue

                # DYNAMIC URL REPLACEMENT
                current_path = ENDPOINT_TEMPLATE.replace("{ticket_id}", t_id)
                
                print(f"Processing {t_id}...", end=" ", flush=True)
                
                res = client.execute(REQUEST_METHOD, current_path)
                
                # Logic for status and reporting
                if res["success"]:
                    print("✅")
                    success_count += 1
                    status_text = "SUCCESS"
                    error_msg = ""
                else:
                    print(f"❌ (Status: {res['status']})")
                    fail_count += 1
                    status_text = "FAILED"
                    # Try to grab the error message from the response body
                    error_msg = res.get("data", {}).get("message") or res.get("error_body", "Unknown Error")

                # Add to report list
                report_data.append({
                    "Ticket Number": t_id,
                    "Status": status_text,
                    "HTTP Code": res["status"],
                    "Error Reason": error_msg,
                    "Timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                })

        # --- WRITE THE CSV REPORT ---
        with open(REPORT_FILE, mode='w', newline='', encoding='utf-8') as rf:
            fieldnames = ["Ticket Number", "Status", "HTTP Code", "Error Reason", "Timestamp"]
            writer = csv.DictWriter(rf, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(report_data)

        print(f"\n--- Batch Complete ---")
        print(f"✅ Success: {success_count}")
        print(f"❌ Failed:  {fail_count}")
        print(f"📄 Report saved to: {REPORT_FILE}")

    except FileNotFoundError:
        print(f"❌ ERROR: File '{CSV_FILE}' not found.")
    except Exception as e:
        print(f"❌ An unexpected error occurred: {e}")

if __name__ == "__main__":
    main()