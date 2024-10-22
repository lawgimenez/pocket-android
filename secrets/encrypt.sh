#!/usr/bin/env bash

encrypt() {
  INPUT=$1
  INPUT_DIR=$2 # optional, include trailing slash
  OUTPUT_DIR=$3 # optional, include trailing slash
  gpg --batch --yes --passphrase="$GPG_KEY" --cipher-algo AES256 --symmetric --output "$OUTPUT_DIR$INPUT.gpg" "$INPUT_DIR$INPUT"
}

if [[ -z "$GPG_KEY" ]]; then
  read -p "Pocket Android GPG key: " -r -s
  echo # (optional) move to a new line
  GPG_KEY="$REPLY"
fi

encrypt "secrets/secret.properties"

FONTS_SECRETS=secrets/fonts/
FONTS_ASSETS=pocket-ui/src/main/assets/
mkdir "$FONTS_SECRETS"
encrypt "blanco_osf_bold.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
encrypt "blanco_osf_bold_italic.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
encrypt "blanco_osf_italic.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
encrypt "blanco_osf_regular.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
encrypt "doyle_medium.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
encrypt "graphik_lcg_bold_no_leading.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
encrypt "graphik_lcg_medium_italic_no_leading.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
encrypt "graphik_lcg_medium_no_leading.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
encrypt "graphik_lcg_regular_italic_no_leading.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
encrypt "graphik_lcg_regular_no_leading.otf" "$FONTS_ASSETS" "$FONTS_SECRETS"
