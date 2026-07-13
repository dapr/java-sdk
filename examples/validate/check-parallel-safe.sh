#!/usr/bin/env bash
# check-parallel-safe.sh
#
# Statically checks that Dapr example READMEs follow the "parallel-safe" standard
# documented in examples/validate/PARALLEL_STANDARD.md, so they can all run
# concurrently against one shared Dapr runtime.
#
# Rules enforced (see PARALLEL_STANDARD.md "Enforcement" section):
#   1. Every --app-id must start with "<slug>-" or equal "<slug>" exactly, where
#      <slug> is the README's path under examples/src/main/java/io/dapr/examples/
#      with '/' replaced by '-' and the trailing /README.md removed.
#   2. No line may use the default --dapr-http-port 3500 or --dapr-grpc-port 50001.
#   3. Every --app-port value must be unique across all READMEs in the list.
#
# Portable: macOS bash 3.2 and Linux bash. No associative arrays, no mapfile.

set -u

# ---------------------------------------------------------------------------
# Resolve repo root / examples dir from this script's own location, not cwd.
# Script lives at: examples/validate/check-parallel-safe.sh
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
EXAMPLES_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

LIST_FILE="$SCRIPT_DIR/readmes.txt"

usage() {
    echo "Usage: $0 [-f FILE] [README_PATH ...]" >&2
    echo "  -f FILE   override the list of README paths (default: $LIST_FILE)" >&2
    echo "  README_PATH ... optional positional README paths (relative to examples/)" >&2
}

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------
while getopts "f:h" opt; do
    case "$opt" in
        f)
            LIST_FILE="$OPTARG"
            ;;
        h)
            usage
            exit 0
            ;;
        *)
            usage
            exit 2
            ;;
    esac
done
shift $((OPTIND - 1))

# ---------------------------------------------------------------------------
# Build the list of README paths (relative to $EXAMPLES_DIR) to check.
# Positional args (if any) take precedence over the list file.
# ---------------------------------------------------------------------------
README_LIST_TMP="$(mktemp)"
trap 'rm -f "$README_LIST_TMP"' EXIT

if [ "$#" -gt 0 ]; then
    for arg in "$@"; do
        printf '%s\n' "$arg" >> "$README_LIST_TMP"
    done
else
    if [ ! -f "$LIST_FILE" ]; then
        echo "error: list file not found: $LIST_FILE" >&2
        exit 2
    fi
    # Strip blank lines and '#' comment lines.
    while IFS= read -r line || [ -n "$line" ]; do
        # Trim leading/trailing whitespace.
        trimmed="$(printf '%s' "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
        [ -z "$trimmed" ] && continue
        case "$trimmed" in
            \#*) continue ;;
        esac
        printf '%s\n' "$trimmed" >> "$README_LIST_TMP"
    done < "$LIST_FILE"
fi

if [ ! -s "$README_LIST_TMP" ]; then
    echo "error: no README paths to check" >&2
    exit 2
fi

# ---------------------------------------------------------------------------
# Derive slug from a README path (relative to examples/src/main/java/io/dapr/examples/).
#   e.g. pubsub/README.md            -> pubsub
#        invoke/http/README.md       -> invoke-http
#        pubsub/stream/README.md     -> pubsub-stream
# ---------------------------------------------------------------------------
slug_for() {
    rel_path="$1"
    prefix="src/main/java/io/dapr/examples/"
    case "$rel_path" in
        "$prefix"*)
            trimmed="${rel_path#"$prefix"}"
            ;;
        *)
            trimmed="$rel_path"
            ;;
    esac
    trimmed="${trimmed%/README.md}"
    printf '%s\n' "$trimmed" | tr '/' '-'
}

# ---------------------------------------------------------------------------
# Pass 1: for each README, extract every "--app-port <n>" occurrence (with
# line number) into a global scratch file, tagged with the README path.
# Format per line: "<readme_path>\t<line_no>\t<port>"
# ---------------------------------------------------------------------------
APP_PORTS_TMP="$(mktemp)"
trap 'rm -f "$README_LIST_TMP" "$APP_PORTS_TMP"' EXIT

VIOLATIONS_TMP="$(mktemp)"
trap 'rm -f "$README_LIST_TMP" "$APP_PORTS_TMP" "$VIOLATIONS_TMP"' EXIT

VALID_README_LIST_TMP="$(mktemp)"
trap 'rm -f "$README_LIST_TMP" "$APP_PORTS_TMP" "$VIOLATIONS_TMP" "$VALID_README_LIST_TMP"' EXIT

violation_count=0
affected_files_tmp="$(mktemp)"
trap 'rm -f "$README_LIST_TMP" "$APP_PORTS_TMP" "$VIOLATIONS_TMP" "$VALID_README_LIST_TMP" "$affected_files_tmp"' EXIT

add_violation() {
    printf '%s\n' "$1" >> "$VIOLATIONS_TMP"
    violation_count=$((violation_count + 1))
}

while IFS= read -r readme_rel || [ -n "$readme_rel" ]; do
    [ -z "$readme_rel" ] && continue
    readme_abs="$EXAMPLES_DIR/$readme_rel"
    if [ ! -f "$readme_abs" ]; then
        echo "warning: README not found, skipping: $readme_rel" >&2
        continue
    fi
    printf '%s\n' "$readme_rel" >> "$VALID_README_LIST_TMP"

    # --- Rule 1: app-id prefix -------------------------------------------
    slug="$(slug_for "$readme_rel")"

    while IFS=: read -r line_no line_content; do
        [ -z "$line_no" ] && continue
        # Pull each "--app-id <token>" occurrence out of the line.
        remainder="$line_content"
        while :; do
            case "$remainder" in
                *--app-id*)
                    after="${remainder#*--app-id}"
                    # Skip leading whitespace (spaces or '=').
                    after_trimmed="$(printf '%s' "$after" | sed -e 's/^[[:space:]=]*//')"
                    app_id="$(printf '%s' "$after_trimmed" | awk '{print $1}')"
                    # Strip surrounding markdown backticks/punctuation, e.g. from
                    # prose like "with distinct `--app-id` and `--dapr-grpc-port`".
                    app_id="$(printf '%s' "$app_id" | sed -e 's/^`*//' -e 's/[`,.;:)]*$//')"
                    if [ -n "$app_id" ]; then
                        if [ "$app_id" = "$slug" ]; then
                            :
                        else
                            case "$app_id" in
                                "$slug"-*) : ;;
                                *)
                                    add_violation "$readme_rel:$line_no: app-id '$app_id' must start with '$slug-'"
                                    ;;
                            esac
                        fi
                    fi
                    remainder="$after_trimmed"
                    ;;
                *)
                    break
                    ;;
            esac
        done
    done < <(grep -n -- "--app-id" "$readme_abs" 2>/dev/null)

    # --- Rule 2: no default dapr ports -----------------------------------
    while IFS=: read -r line_no line_content; do
        [ -z "$line_no" ] && continue
        case "$line_content" in
            *--dapr-http-port\ 3500*)
                add_violation "$readme_rel:$line_no: default dapr port 3500 collides under concurrency; use a unique port or omit the flag"
                ;;
        esac
    done < <(grep -n -- "--dapr-http-port 3500" "$readme_abs" 2>/dev/null)

    while IFS=: read -r line_no line_content; do
        [ -z "$line_no" ] && continue
        case "$line_content" in
            *--dapr-grpc-port\ 50001*)
                add_violation "$readme_rel:$line_no: default dapr port 50001 collides under concurrency; use a unique port or omit the flag"
                ;;
        esac
    done < <(grep -n -- "--dapr-grpc-port 50001" "$readme_abs" 2>/dev/null)

    # --- Collect app-port occurrences for the cross-README uniqueness check.
    while IFS=: read -r line_no line_content; do
        [ -z "$line_no" ] && continue
        remainder="$line_content"
        while :; do
            case "$remainder" in
                *--app-port*)
                    after="${remainder#*--app-port}"
                    after_trimmed="$(printf '%s' "$after" | sed -e 's/^[[:space:]=]*//')"
                    port="$(printf '%s' "$after_trimmed" | awk '{print $1}')"
                    if [ -n "$port" ]; then
                        printf '%s\t%s\t%s\n' "$readme_rel" "$line_no" "$port" >> "$APP_PORTS_TMP"
                    fi
                    remainder="$after_trimmed"
                    ;;
                *)
                    break
                    ;;
            esac
        done
    done < <(grep -n -- "--app-port" "$readme_abs" 2>/dev/null)

done < "$README_LIST_TMP"

# ---------------------------------------------------------------------------
# Rule 3: --app-port cross-README uniqueness.
# For every distinct port value, find the set of distinct READMEs that use it.
# If more than one README uses it, every occurrence of that port (in every
# README that uses it) is a violation, listing the *other* README(s).
# ---------------------------------------------------------------------------
if [ -s "$APP_PORTS_TMP" ]; then
    distinct_ports="$(cut -f3 "$APP_PORTS_TMP" | sort -u)"
    while IFS= read -r port; do
        [ -z "$port" ] && continue
        readmes_for_port="$(awk -F'\t' -v p="$port" '$3 == p {print $1}' "$APP_PORTS_TMP" | sort -u)"
        readme_count="$(printf '%s\n' "$readmes_for_port" | sed '/^$/d' | wc -l | tr -d ' ')"
        if [ "$readme_count" -gt 1 ]; then
            while IFS=$'\t' read -r r_readme r_line r_port; do
                [ -z "$r_readme" ] && continue
                [ "$r_port" != "$port" ] && continue
                others="$(printf '%s\n' "$readmes_for_port" | grep -v -F -x "$r_readme" | sort -u | tr '\n' ',' | sed 's/,$//' | sed 's/,/, /g')"
                add_violation "$r_readme:$r_line: --app-port $port is also used by $others"
            done < "$APP_PORTS_TMP"
        fi
    done <<EOF
$distinct_ports
EOF
fi

# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------
if [ -s "$VIOLATIONS_TMP" ]; then
    sort -t: -k1,1 -k2,2n "$VIOLATIONS_TMP"
    cut -d: -f1 "$VIOLATIONS_TMP" | sort -u > "$affected_files_tmp"
    affected_count="$(wc -l < "$affected_files_tmp" | tr -d ' ')"
    echo ""
    echo "Summary: $violation_count violation(s) across $affected_count README(s)."
    exit 1
else
    echo "Summary: 0 violations. All checked READMEs are parallel-safe."
    exit 0
fi
