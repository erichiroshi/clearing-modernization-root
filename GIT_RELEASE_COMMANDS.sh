#!/usr/bin/env bash
# Release v0.2.0 — fecha Épico 1 (Infraestrutura) + Épico 2 (Core Transacional e Ingestão)
#
# Rodar da raiz do repositório, com a develop já atualizada e com o CI
# passando no último commit dela.
set -euo pipefail

VERSION="v0.2.0"
TITULO="v0.2.0 — Core transacional e ingestão"

echo "==> Atualizando main e develop locais"
git checkout main
git pull origin main
git checkout develop
git pull origin develop

echo "==> Mesclando develop -> main (fecha o Épico 2)"
git checkout main
git merge --no-ff develop -m "release: ${VERSION} — Épico 1 (Infraestrutura) e Épico 2 (Core Transacional e Ingestão)"
git push origin main

echo "==> Criando e publicando a tag ${VERSION}"
git tag -a "${VERSION}" -m "Release ${VERSION}"
git push origin "${VERSION}"

echo "==> Criando o GitHub Release (via gh CLI, não workflow)"
gh release create "${VERSION}" \
  --title "${TITULO}" \
  --notes-file about/v0.2.0.md

echo "==> Voltando pra develop"
git checkout develop

echo "==> Release ${VERSION} publicado."
