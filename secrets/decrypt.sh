#!/usr/bin/env bash

decrypt() {
  OUTPUT=$1
  INPUT_DIR=$2 # optional, include trailing slash
  OUTPUT_DIR=$3 # optional, include trailing slash
  gpg --quiet --batch --yes --decrypt --passphrase="$GPG_KEY" --output "$OUTPUT_DIR$OUTPUT" "$INPUT_DIR$OUTPUT.gpg"
}

if [[ -z "$GPG_KEY" ]]; then
  read -p "Pocket Android GPG key: " -r -s
  echo # (optional) move to a new line
  GPG_KEY="$REPLY"
fi

decrypt "secrets/secret.properties"

FONTS_SECRETS=secrets/fonts/
FONTS_ASSETS=pocket-ui/src/main/assets/
FONTS_RES=Pocket/src/main/res/font/
decrypt "blanco_osf_bold.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "blanco_osf_bold_italic.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "blanco_osf_italic.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "blanco_osf_regular.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "doyle_medium.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "graphik_lcg_bold_no_leading.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "graphik_lcg_medium_italic_no_leading.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "graphik_lcg_medium_no_leading.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "graphik_lcg_medium_no_leading.otf" "$FONTS_SECRETS" "$FONTS_RES"
decrypt "graphik_lcg_regular_italic_no_leading.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "graphik_lcg_regular_no_leading.otf" "$FONTS_SECRETS" "$FONTS_ASSETS"
decrypt "graphik_lcg_regular_no_leading.otf" "$FONTS_SECRETS" "$FONTS_RES"
