# WobbleShop

`WobbleShop` is a GUI-first Paper shop plugin with SQLite persistence and Vault economy support.

It supports both static listings with infinite stock and limited listings with real stock, automatic restock, and admin-side item management through in-game menus.

## Highlights

- GUI-only player shop flow
- Buy and sell support
- Static and limited stock types
- Real stock decrement on purchase
- Automatic and manual restocking
- SQLite-backed item persistence
- Admin GUI for item management
- Vault economy integration

## Requirements

- Java 21
- Paper 1.21.11+
- Vault
- An economy plugin compatible with Vault

## Build

```bash
mvn clean package
```

Output jar:

- `target/WobbleShop-1.0.0.jar`

## Installation

1. Build the plugin with Maven.
2. Install Vault and your economy plugin.
3. Place the jar into your server `plugins/` folder.
4. Start the server once to generate config files.
5. Adjust `config.yml` and `messages.yml`.
6. Restart the server.

## Commands

### Player

- `/shop`

### Admin

- `/shop admin`
- `/shop reload`
- `/shop restock`
- `/shop restock <category>`
- `/shop restock <itemId>`

## Permissions

- `wobble.shop.use`
- `wobble.shop.admin`
- `wobble.shop.reload`
- `wobble.shop.restock`

## Core Features

### Shop Flow

- `/shop` opens the main GUI
- category GUI shows available items
- item action GUI supports:
  - buy `x1`
  - buy `x16`
  - buy `x64`
  - sell `x1`
  - sell `x16`
  - sell `x64`
  - sell all

### Stock Types

#### Static

- infinite stock
- never decreases
- always available while active and enabled

#### Limited

- finite stock
- decreases on successful buy
- unavailable when stock is insufficient
- can be restocked manually or automatically

### Restock

Supported restock flows:

- full restock
- category restock
- single-item restock
- automatic timed restock task

## Configuration Overview

Main sections in `config.yml`:

- `database`
- `shop`
- `restock`
- `economy`
- `gui`
- `sounds`

Important defaults:

- SQLite file: `shop.db`
- restock task enabled: `true`
- restock interval check: `60` seconds
- `allow-sell-above-buy`: `false`

## Persistence

WobbleShop stores shop state in SQLite.

Database file:

- `plugins/WobbleShop/shop.db`

Stored data includes:

- categories
- shop items
- buy and sell prices
- stock type
- stock and max stock
- restock settings
- status and lore

## Admin Editing

The admin GUI supports:

- creating items
- editing category
- editing slot
- editing material
- toggling buy and sell
- changing prices
- changing stock type
- changing stock and max stock
- toggling restock
- changing restock interval
- deleting items

## Notes

- WobbleShop depends on Vault at startup.
- Sell logic is independent from shop stock.
- The plugin is designed around GUI usage, not chat-driven trading.
