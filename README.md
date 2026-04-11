# WobbleShop

Config-driven GUI shop plugin for Paper 1.21.x servers using Vault economy.

## Goals

- Baseline shop for SMP progression (not pay-to-win)
- Easy long-term rebalancing without code changes
- Conservative defaults that do **not** replace player trading/auction
- Category + item definitions in YAML files

## Requirements

- Java 21
- Paper 1.21.x
- Vault
- Economy plugin (EssentialsX economy supported via Vault)

## Build

```bash
mvn clean package
```

## Core files

- `config.yml`: global settings (titles, sounds, restock task, economy guards)
- `messages.yml`: translatable messages
- `categories.yml`: category layout/icons/slots/enabled flags
- `shop-items.yml`: all shop items, prices, buy/sell modes, stock model, visibility

## Commands

- `/shop` - open player shop
- `/shop admin` - admin GUI tools
- `/shop reload` - reload config + resync catalog from YAML
- `/shop restock [category|itemId]` - manual restock for limited items

## Permissions

- `wobble.shop.use`
- `wobble.shop.admin`
- `wobble.shop.reload`
- `wobble.shop.restock`

## Catalog design

- Categories are loaded from `categories.yml`.
- Items are loaded from `shop-items.yml`.
- Items can be:
  - buy-only
  - sell-only
  - buy+sell
  - disabled
  - hidden (not shown/imported)
- Prices/slots/materials/status/stock are all configurable.

## Important balancing policy in defaults

The default catalog excludes progression-breaking items from the normal shop (elytra, mace, dragon head) by keeping them hidden+disabled in a special category template.
