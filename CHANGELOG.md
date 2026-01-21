# FluidCows (REBORN) Changelog


## 🔄 Changes in v1.0.3

### 🐣 Baby Growth Time Fix

- Baby cows now properly use the growth_time_ticks value from their fluid's config
- Previously all babies used the default 20 minutes regardless of config settings

### 🎯 Cow Snatcher Improvements

- Adults only - Cow Snatcher now only works on fully grown cows
- Milk cooldown preserved - Captured cows retain their milk cooldown
- Breeding cooldown preserved - Captured cows retain their breeding cooldown
- Cooldowns tick in inventory - Both cooldowns continue counting down while stored as an item
- Live tooltip - Hovering over captured cow shows current cooldown timers

### 🔧 Config System Improvements

- Config values (growth time, breeding cooldown, bucket cooldown) now read fresh from disk
- Live config changes apply immediately without caching issues
- Added debug logging for baby growth time assignment

### 🐛 Bug Fixes

- Fixed vanilla setBaby() overwriting custom growth times
- Fixed config cache preventing live updates from taking effect


---


## 🔄 Changes in v1.0.2

### 🎮 In-Game Config Editor

- Press F7 to open the Fluid Cows configuration GUI
- Edit all cow settings live without restarting:
  - Enable/disable cows
  - Spawn weight
  - Breeding cooldown, growth time, milk cooldown
  - Breeding item (searchable dropdown with all game items)
  - Parent cows for breeding recipes
  - Breeding success chance
- Changes apply immediately (JEI requires world rejoin)

### 🐄 New Item: Cow Snatcher

- Craftable tool to capture fluid cows
- 64 durability
- Right-click cow to pick up

### ✨ Visual Improvements

- Custom fluid drip particles using actual fluid textures
- Particles respect Minecraft particle settings (All/Decreased/Minimal)
- Fixed cow names showing unlocalized keys

### 🔧 New Debug Commands

- /fluidcows debug info <fluid> - Detailed fluid information
- /fluidcows debug nearby [radius] - List fluid cows in range
- /fluidcows debug validate - Check all enabled fluids for issues
- /fluidcows debug problematic - Quick list of fluids to disable

### 🐛 Bug Fixes

- Fixed bucket interaction with stacked buckets
- Fixed Breeding offspring percent chance to actually reflect the config value, instead of complete random


---


## 🔄 Changes in v1.0.1

- Switched project license to MIT
- Source code is now available on GitHub
- Pull Requests are now welcome
- Updated JEI integration for NeoForge 1.21.1:
  - Replaced deprecated ingredient APIs with item stack APIs
  - Fixed deprecated lambda parameter types
  - Removed obsolete suppression annotations
- Removed unused JSON field from breeding manager

### 🔧 Internal Improvements

- Removed legacy NBT_LOCK system (dead test code)
- Deleted 3 unused config generator methods
- Replaced 7 debug prints with proper logging
- Reduced code size by 13.6%


---


## 🎉 v1.0.0 - Initial Release

### 🐄 Core Features

- Fluid Cows that produce any registered fluid
- Configurable spawn weights, cooldowns, and growth times
- Auto-generates config files for all registered fluids

### 🔗 Mod Integration

- JEI integration for breeding recipes and cow info
- Jade/HWYLA integration for cow tooltips

### ⚙️ Configuration

- Per-fluid JSON config files
- Customizable breeding recipes with parent requirements
