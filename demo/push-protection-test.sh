#!/bin/bash
# ============================================================
# DEMO ONLY — Push Protection Test
# This file contains a fake secret to trigger push protection.
# ============================================================

# This fake AWS key will trigger GitHub push protection
export AWS_ACCESS_KEY_ID="AKIAIOSFODNN7EXAMPLEBN"
export AWS_SECRET_ACCESS_KEY="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEYGHHG"

echo "Connecting to database..."

# Additional test secret — GitHub Personal Access Token
export GITHUB_TOKEN="ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdef12"
