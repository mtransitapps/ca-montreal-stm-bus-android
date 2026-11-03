#!/bin/bash

# DEPRECATED: use sh/download.sh
# TODO: remove after update /.github/workflows/mt-download-data.yml updated)

SCRIPT_DIR="$(dirname "$0")";
exec "${SCRIPT_DIR}/sh/download.sh" "$@";
