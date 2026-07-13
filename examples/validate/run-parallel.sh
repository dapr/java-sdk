#!/usr/bin/env bash
#
# run-parallel.sh — run Dapr example READMEs through mm.py (mechanical-markdown)
# concurrently, from the correct working directory, with retries and a
# summary report at the end.
#
# Usage:
#   run-parallel.sh [-j N] [-r R] [-f FILE] [--dry-run] [README ...]
#
#   -j N       Max concurrent READMEs (default 4).
#   -r R       Per-README retry count on failure (default 2, i.e. up to
#              3 attempts total).
#   -f FILE    README list file (default: examples/validate/readmes.txt
#              resolved relative to the repo root).
#   --dry-run  Do not run mm.py; print the exact command that would run
#              for each README instead.
#   README ...  Positional README paths (relative to the examples dir).
#               If given, these override the list file.
#
# Portable: must work on macOS bash 3.2 and Linux bash. No associative
# arrays, no `wait -n`.

set -u

# ---------------------------------------------------------------------------
# Hidden subcommand: __run_one
#
# This script re-execs itself via `xargs -P N -I{} bash "$0" __run_one {} ...`
# to get a portable concurrency pool (no associative arrays / `wait -n`
# needed). When invoked this way, run a single README (with retries) and
# write its result to a per-README result file, then exit.
# ---------------------------------------------------------------------------
if [ "${1:-}" = "__run_one" ]; then
    shift
    readme="$1"
    examples_dir="$2"
    retries="$3"
    log_dir="$4"

    slug=$(printf '%s' "$readme" | tr '/' '_')
    log_file="$log_dir/${slug}.log"
    result_file="$log_dir/${slug}.result"

    attempts=0
    max_attempts=$((retries + 1))
    start_ts=$(date +%s)
    status="FAIL"

    : > "$log_file"

    while [ "$attempts" -lt "$max_attempts" ]; do
        attempts=$((attempts + 1))
        {
            echo "=== attempt $attempts/$max_attempts: (cd $examples_dir && mm.py $readme) ==="
        } >> "$log_file"

        if (cd "$examples_dir" && mm.py "$readme") >> "$log_file" 2>&1; then
            status="PASS"
            break
        fi
    done

    end_ts=$(date +%s)
    duration=$((end_ts - start_ts))

    printf '%s\t%s\t%s\t%s\t%s\n' "$readme" "$status" "$attempts" "$duration" "$log_file" > "$result_file"

    if [ "$status" = "PASS" ]; then
        exit 0
    else
        exit 1
    fi
fi

# ---------------------------------------------------------------------------
# Main entry point
# ---------------------------------------------------------------------------

usage() {
    cat <<'EOF'
Usage: run-parallel.sh [-j N] [-r R] [-f FILE] [--dry-run] [README ...]

  -j N        Max concurrent READMEs (default 4).
  -r R        Per-README retry count on failure (default 1, i.e. up to
              2 attempts total).
  -f FILE     README list file (default: examples/validate/readmes.txt
              resolved relative to the repo root).
  --dry-run   Do not run mm.py; print the exact command that would run
              for each README instead.
  README ...  Positional README paths (relative to the examples dir).
              If given, these override the list file.
EOF
}

# Resolve script location, examples dir, and repo root from the script's
# own path -- not from the caller's cwd.
script_source="$0"
script_dir=$(cd "$(dirname "$script_source")" && pwd)
# script is at <repo_root>/examples/validate/run-parallel.sh
repo_root=$(cd "$script_dir/../.." && pwd)
examples_dir="$repo_root/examples"

jobs=4
retries=2
list_file="$examples_dir/validate/readmes.txt"
dry_run=0
positional_readmes=()

while [ "$#" -gt 0 ]; do
    case "$1" in
        -j)
            [ "$#" -ge 2 ] || { echo "run-parallel.sh: -j requires an argument" >&2; exit 2; }
            jobs="$2"
            shift 2
            ;;
        -r)
            [ "$#" -ge 2 ] || { echo "run-parallel.sh: -r requires an argument" >&2; exit 2; }
            retries="$2"
            shift 2
            ;;
        -f)
            [ "$#" -ge 2 ] || { echo "run-parallel.sh: -f requires an argument" >&2; exit 2; }
            list_file="$2"
            shift 2
            ;;
        --dry-run)
            dry_run=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        --)
            shift
            while [ "$#" -gt 0 ]; do
                positional_readmes+=("$1")
                shift
            done
            ;;
        -*)
            echo "run-parallel.sh: unknown option: $1" >&2
            usage >&2
            exit 2
            ;;
        *)
            positional_readmes+=("$1")
            shift
            ;;
    esac
done

# Build the list of READMEs to validate.
readmes=()
if [ "${#positional_readmes[@]}" -gt 0 ]; then
    readmes=("${positional_readmes[@]}")
else
    if [ ! -f "$list_file" ]; then
        echo "run-parallel.sh: README list file not found: $list_file" >&2
        exit 2
    fi
    while IFS= read -r line || [ -n "$line" ]; do
        # Trim leading/trailing whitespace.
        trimmed=$(printf '%s' "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
        [ -z "$trimmed" ] && continue
        case "$trimmed" in
            \#*) continue ;;
        esac
        readmes+=("$trimmed")
    done < "$list_file"
fi

if [ "${#readmes[@]}" -eq 0 ]; then
    echo "run-parallel.sh: no READMEs to validate" >&2
    exit 2
fi

if [ "$dry_run" -eq 1 ]; then
    for readme in "${readmes[@]}"; do
        echo "(cd $examples_dir && mm.py $readme)"
    done
    exit 0
fi

log_dir=$(mktemp -d "${TMPDIR:-/tmp}/run-parallel.XXXXXX")

# Run every README through the hidden __run_one subcommand, in parallel,
# via xargs -P. This avoids associative arrays and `wait -n`, keeping the
# script portable to macOS bash 3.2.
printf '%s\n' "${readmes[@]}" | \
    xargs -I{} -P "$jobs" bash "$script_source" __run_one {} "$examples_dir" "$retries" "$log_dir"

# Collect results. Order the summary the same as the input list.
overall_status=0
total_pass=0
total_fail=0

echo
echo "==================== SUMMARY ===================="
printf '%-6s  %-60s  %-8s  %-10s\n' "STATUS" "README" "ATTEMPTS" "DURATION(s)"

for readme in "${readmes[@]}"; do
    slug=$(printf '%s' "$readme" | tr '/' '_')
    result_file="$log_dir/${slug}.result"

    if [ -f "$result_file" ]; then
        IFS=$'\t' read -r r_readme r_status r_attempts r_duration r_log < "$result_file"
    else
        r_readme="$readme"
        r_status="FAIL"
        r_attempts="0"
        r_duration="0"
        r_log="$log_dir/${slug}.log"
    fi

    printf '%-6s  %-60s  %-8s  %-10s\n' "$r_status" "$r_readme" "$r_attempts" "$r_duration"

    if [ "$r_status" = "PASS" ]; then
        total_pass=$((total_pass + 1))
    else
        total_fail=$((total_fail + 1))
        overall_status=1

        echo "  --- log: $r_log"
        if [ -f "$r_log" ]; then
            echo "  --- last 20 lines of $r_log ---"
            tail -n 20 "$r_log" | sed 's/^/  /'
        else
            echo "  (no log captured)"
        fi
    fi
done

echo "==================================================="
echo "Total: ${#readmes[@]}  Pass: $total_pass  Fail: $total_fail"
echo "Logs directory: $log_dir"

exit "$overall_status"
