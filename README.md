# WobbleShop

Production-ready, config-driven GUI shop for Paper 1.21.x with Vault economy.

## Core design

- SMP progression-safe defaults (not pay-to-win)
- Fully config-rebalanceable categories and items
- Buy-only / sell-only / buy+sell / disabled / hidden items
- Infinite, limited, and regenerating stock modes
- Optional dynamic pricing and anti-abuse controls

## Requirements

- Java 21
- Paper 1.21.x
- Vault + an economy provider (EssentialsX economy supported)

## Commands

- `/shop`
- `/shop admin`
- `/shop reload`
- `/shop restock all`
- `/shop restock category <name>`
- `/shop restock item <id>`

## Permissions

- `wobble.shop.use` / `shop.use`
- `wobble.shop.admin`
- `wobble.shop.reload`
- `wobble.shop.restock`
- `shop.category.<name>`
- `shop.discount.<group>`
- `shop.bypass.stock`

## Config files

- `config.yml`: global shop/economy/anti-abuse/debug settings
- `messages.yml`: messages
- `categories.yml`: category layout, visibility, permissions, admin-only flags
- `shop-items.yml`: all catalog entries and pricing/stock behavior

## SMP fairness defaults

Elytra, mace, and dragon head remain hidden+disabled by default in the special category template.