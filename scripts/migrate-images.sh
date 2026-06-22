#!/bin/bash
# Image Migration Script
# Exports images from PostgreSQL (bytea) to filesystem for REST API serving
# Usage: ./migrate-images.sh [output_directory]
# Default output: /gameLibrary/images

set -e

IMAGES_DIR="${1:-/gameLibrary/images}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-game-library}"
DB_USER="${DB_USER:-library-manager-user}"
DB_PASS="${DB_PASS:-2wq345tgfiNcbBBwee3}"

echo "Migrating images to: $IMAGES_DIR"

mkdir -p "$IMAGES_DIR/games"
mkdir -p "$IMAGES_DIR/avatars"

export PGPASSWORD="$DB_PASS"

# Export game logos
echo "Exporting game logos..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
  "SELECT id FROM library.game_data WHERE logo IS NOT NULL;" | while read -r game_id; do
  if [ -n "$game_id" ]; then
    game_id=$(echo "$game_id" | xargs)
    mkdir -p "$IMAGES_DIR/games/$game_id"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
      "SELECT encode(logo, 'base64') FROM library.game_data WHERE id = $game_id;" | head -1 | base64 -d > "$IMAGES_DIR/games/$game_id/logo.jpg" 2>/dev/null || true
    echo "  Game $game_id: logo exported"
  fi
done

# Export screenshots
echo "Exporting screenshots..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
  "SELECT id, game_id FROM library.game_screenshot WHERE source IS NOT NULL ORDER BY game_id;" | while read -r line; do
  ss_id=$(echo "$line" | cut -d'|' -f1 | xargs)
  game_id=$(echo "$line" | cut -d'|' -f2 | xargs)
  if [ -n "$ss_id" ] && [ -n "$game_id" ]; then
    mkdir -p "$IMAGES_DIR/games/$game_id/screenshots"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
      "SELECT encode(source, 'base64') FROM library.game_screenshot WHERE id = $ss_id;" | head -1 | base64 -d > "$IMAGES_DIR/games/$game_id/screenshots/$ss_id.jpg" 2>/dev/null || true
  fi
done

# Export avatars
echo "Exporting avatars..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
  "SELECT id FROM library.library_user WHERE avatar IS NOT NULL;" | while read -r user_id; do
  if [ -n "$user_id" ]; then
    user_id=$(echo "$user_id" | xargs)
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c \
      "SELECT encode(avatar, 'base64') FROM library.library_user WHERE id = $user_id;" | head -1 | base64 -d > "$IMAGES_DIR/avatars/$user_id.jpg" 2>/dev/null || true
    echo "  User $user_id: avatar exported"
  fi
done

echo "Migration complete!"
echo "Images stored in: $IMAGES_DIR"
