#!/usr/bin/env bash
# Compares the endpoints declared in the Spring controllers with the endpoints
# documented in docs/api.md. Exits 0 when they match, 1 when they differ.
#
# Usage (from the repo root): bash .claude/skills/write-docs/scripts/check-endpoints.sh
set -euo pipefail

CONTROLLER_DIR="backend/src/main/java"
API_DOC="docs/api.md"

if [ ! -f "$API_DOC" ]; then
  echo "ERROR: $API_DOC not found. Run from the repo root." >&2
  exit 2
fi

# Code side: "VERB /full/path" per controller method (class-level @RequestMapping + method mapping).
code=$(grep -rl '@RestController' "$CONTROLLER_DIR" | while read -r f; do
  awk '
    /@RequestMapping\(/ && !base {
      match($0, /"[^"]*"/); base = substr($0, RSTART + 1, RLENGTH - 2)
    }
    /@(Get|Post|Put|Patch|Delete)Mapping/ {
      verb = $0; sub(/.*@/, "", verb); sub(/Mapping.*/, "", verb); verb = toupper(verb)
      sub_path = ""
      if (match($0, /"[^"]*"/)) sub_path = substr($0, RSTART + 1, RLENGTH - 2)
      print verb " " base sub_path
    }
  ' "$f"
done | sort -u)

# Doc side: headings of the form  ### `VERB /path`
doc=$(grep -oE '^### `(GET|POST|PUT|PATCH|DELETE) [^`]+`' "$API_DOC" \
  | sed -E 's/^### `//; s/`$//' | sort -u)

only_code=$(comm -23 <(echo "$code") <(echo "$doc") || true)
only_doc=$(comm -13 <(echo "$code") <(echo "$doc") || true)

status=0
if [ -n "$only_code" ]; then
  echo "In controllers but NOT documented in $API_DOC:"
  echo "$only_code" | sed 's/^/  /'
  status=1
fi
if [ -n "$only_doc" ]; then
  echo "Documented in $API_DOC but NOT found in controllers:"
  echo "$only_doc" | sed 's/^/  /'
  status=1
fi
if [ "$status" -eq 0 ]; then
  echo "OK: $(echo "$code" | wc -l | tr -d ' ') endpoints, controllers and $API_DOC match."
fi
exit "$status"
