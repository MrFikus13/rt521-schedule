#!/usr/bin/env bash
# Prepare the static PWA for local preview or GitHub Pages.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p web/data
cp data/schedule.json web/data/schedule.json
echo "web/data/schedule.json updated"
