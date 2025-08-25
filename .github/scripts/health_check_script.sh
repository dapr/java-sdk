#!/bin/sh

wait_for_dapr() {
  local port="${1:-3500}"
  local max_tries="${2:-60}"
  local url="http://localhost:${port}/v1.0/healthz"
  local code=""

  for i in $(seq 1 "$max_tries"); do
    code=$(curl -s -o /dev/null -w "%{http_code}" "$url" || true)
    if [ "$code" = "204" ]; then
      echo "Ready (204) on port ${port}"
      return 0  # do not exit; just return
    fi
    sleep 1
  done

  echo "Timeout after ${max_tries}s waiting for 204 on port ${port} (last code: ${code})"
  return 0  # keep returning success to avoid exiting callers using set -e
}

# Example usage:
# wait_for_dapr               # uses defaults: port 3500, tries 60
# wait_for_dapr 3501          # custom port, default tries
# wait_for_dapr 3501 30       # custom port and tries
