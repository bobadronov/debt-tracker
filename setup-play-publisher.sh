#!/usr/bin/env bash
# Run this in Google Cloud Shell (console.cloud.google.com -> Cloud Shell icon,
# top right). Sets up a service account for automated Google Play publishing
# from DebtTracker's release.yml GitHub Actions workflow.
#
# Usage:
#   ./setup-play-publisher.sh [PROJECT_ID]
#
# If PROJECT_ID is omitted, uses your Cloud Shell's current gcloud project
# (gcloud config get-value project). Pass one explicitly to create/use a
# dedicated project instead.
#
# What it does:
#   1. Enables the Google Play Android Developer API on the project.
#   2. Creates a service account (idempotent - safe to re-run).
#   3. Generates a JSON key for it and saves it to ~/debttracker-play-key.json.
#
# What it does NOT do (still needs Play Console, by hand):
#   - Invite the service account's email under Play Console -> Users and
#     permissions, and grant it release access to the DebtTracker app.
#   - The very first .aab upload to Play Console (Play Developer API can't
#     create an app or do its first release).

set -euo pipefail

SA_NAME="debttracker-play-publisher"
SA_DISPLAY_NAME="DebtTracker Play Publisher"
KEY_OUT="$HOME/debttracker-play-key.json"

PROJECT_ID="${1:-$(gcloud config get-value project 2>/dev/null)}"
if [[ -z "$PROJECT_ID" || "$PROJECT_ID" == "(unset)" ]]; then
  echo "No project set. Pass one explicitly: ./setup-play-publisher.sh my-project-id" >&2
  echo "Or run: gcloud config set project my-project-id" >&2
  exit 1
fi

echo "Using project: $PROJECT_ID"
gcloud config set project "$PROJECT_ID" >/dev/null

echo "Enabling Google Play Android Developer API..."
gcloud services enable androidpublisher.googleapis.com --project="$PROJECT_ID"

SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

if gcloud iam service-accounts describe "$SA_EMAIL" --project="$PROJECT_ID" >/dev/null 2>&1; then
  echo "Service account already exists: $SA_EMAIL"
else
  echo "Creating service account: $SA_EMAIL"
  gcloud iam service-accounts create "$SA_NAME" \
    --project="$PROJECT_ID" \
    --display-name="$SA_DISPLAY_NAME"
fi

echo "Generating JSON key -> $KEY_OUT"
gcloud iam service-accounts keys create "$KEY_OUT" \
  --iam-account="$SA_EMAIL" \
  --project="$PROJECT_ID"

echo
echo "============================================================"
echo "Done. Service account email (invite this in Play Console):"
echo "  $SA_EMAIL"
echo
echo "Key file saved to: $KEY_OUT"
echo "Download it from Cloud Shell (⋮ menu -> Download File -> paste that"
echo "path), then in Play Console -> Users and permissions -> Invite new"
echo "users, paste the email above and grant release permissions for the"
echo "DebtTracker app."
echo
echo "After downloading, delete it from Cloud Shell:"
echo "  rm $KEY_OUT"
echo "============================================================"
