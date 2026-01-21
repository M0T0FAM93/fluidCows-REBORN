# FluidCows (REBORN)

Cows that produce any fluid in the game. Milk them with buckets, breed them for new types, and capture them with the Cow Snatcher. Every cow displays its fluid texture directly on the model — no flat colors.

Works automatically with any mod that registers fluids. No setup required.

**Minecraft:** 1.21.1  
**Loader:** NeoForge

---

## Features

**Fluid Cows** spawn naturally. Each one produces a specific fluid. Right-click with an empty bucket to collect it. Cows have a configurable cooldown between milkings.

**Textured Overlays** show the actual fluid texture mapped onto the cow's spots. These generate automatically for every registered fluid. Resource packs can override them.

**Breeding** lets you combine two cow types to create offspring. Configure which parents produce which child, the breeding item required, success chance, and how long babies take to grow up.

**Cow Snatcher** is a craftable tool that picks up adult cows as items. Captured cows keep their cooldowns, which tick down while sitting in your inventory. The tooltip shows time remaining.

**In-Game Config (F7)** lets operators edit everything without restarting. Enable or disable cows, adjust spawn weights, set cooldowns, configure breeding recipes. Changes apply immediately.

---

## Mod Support

**JEI** — Browse all fluid cows and their breeding recipes

**Jade** — See fluid type and cooldown timers when looking at a cow

---

## Configuration

JSON configs generate automatically for every fluid on first launch. Each file controls:

- Spawn weight
- Bucket cooldown
- Breeding cooldown
- Growth time
- Breeding parents
- Breeding item
- Success chance

Edit through the F7 menu or directly in the config files at `config/fluidcows/`.

---

## Commands

All commands require operator permissions.

- `/fluidcows spawn <fluid> [count]` - Spawn fluid cows
- `/fluidcows list` - List all enabled fluids
- `/fluidcows reload` - Reload configs from disk
- `/fluidcows debug info <fluid>` - Detailed fluid information
- `/fluidcows debug nearby [radius]` - List fluid cows in range
- `/fluidcows debug validate` - Check all enabled fluids for issues
- `/fluidcows debug problematic` - Quick list of fluids to disable

---

## Installation

1. Download the jar from [Releases](https://github.com/M0T0FAM93/fluidCows-REBORN/releases) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fluid-cows-reborn)
2. Place it in your `mods` folder
3. Launch Minecraft with NeoForge
4. Enjoy

---

## Contributing

Contributions are welcome.

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

For large changes please open an issue first to discuss the design.

---

## License

This project is licensed under the **MIT License**.  
Free to use, modify, distribute, and include in modpacks.

---

## Author

**Motofam93**
