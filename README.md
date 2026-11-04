PLUGIN NAME:
Enchantment Event

SERVER VERSION:
Spigot 1.12.2

OVERVIEW:
A world-ending event where enchantments become unique artifacts.
Only one copy of each enchantment may exist on the server at any given time.
Players gain power by obtaining, combining, stealing, and protecting enchantments.

==================================================
CORE RULES
==================================================

- Every supported enchantment exists only once globally.
- Ownership is tied to possession.
- Enchantments may be applied to ANY item.
- ALL enchantments are compatible.
- Vanilla mutual exclusivity is removed.
- Enchantments retain normal vanilla effects unless otherwise specified below.

Examples:
- Sharpness V Dirt
- Infinity Steak
- Punch Carrot
- Protection Stick
- Efficiency Sword
- Power Boots

==================================================
EVENT COMMANDS
==================================================

/enchantmentevent start
Starts the event.

/enchantmentevent stop
Stops the event.

/enchantmentevent killroll
The next player kill grants the killer a random enchantment.

/enchantmentevent worldroll
(Optional)
All online players receive a random enchantment.

==================================================
ENCHANTMENT DISCOVERY
==================================================

Enchantments may be obtained through:

- Loot chests
- Event rolls
- Kill Rolls
- Other configured sources

Once an enchantment exists on the server:

Future copies become unstable.

==================================================
DUPLICATE ENCHANTMENTS
==================================================

If a duplicate enchantment is generated:

- Book becomes frozen in place.
- Floats above its location.
- Emits particles.
- Delayed countdown occurs.
- Small explosion destroys the duplicate.

Purpose:
Prevent duplicate ownership.

==================================================
EVENT START ROLL
==================================================

5-15 minutes after event start:

Every online player receives a random enchantment.

If holding an item:
- Enchantment is applied immediately.

If holding nothing:
- Receive enchantment book.

==================================================
KILL ROLL EVENT
==================================================

Admin command:

/enchantmentevent killroll

Effects:

- Next player kill consumes the event.
- Killer receives a random available enchantment.

If holding an item:
- Applied immediately.

If empty-handed:
- Receive book version.

==================================================
ENCHANTMENT DEATH SYSTEM
==================================================

When a player dies:

Unique enchantments are removed from enchanted items.

The enchantments drop as physical books.

Ownership becomes unclaimed.

Players may collect and reuse them.

Purpose:
Enchantments remain in circulation.

==================================================
CUSTOM ENCHANT BEHAVIOR
==================================================

Protection
- Vanilla functionality.

Fire Protection
- Vanilla functionality.

Feather Falling
- Vanilla functionality.

Blast Protection
- Vanilla functionality.

Projectile Protection
- Vanilla functionality.

Respiration
- Vanilla functionality.

Aqua Affinity
- Vanilla functionality.

Thorns
- Vanilla functionality.

Depth Strider
- Vanilla functionality.

Frost Walker
- Functions normally.
- Works while holding enchanted item.

Sharpness
- Vanilla functionality.

Smite
- Vanilla functionality.

Bane of Arthropods
- Vanilla functionality.

Knockback
- Vanilla functionality.

Fire Aspect
- Vanilla functionality.

Looting
- Vanilla functionality.

Sweeping Edge
- Splashes 50% damage to nearby entities (excludes owner).

Efficiency
- Reduces weapon attack cooldown.

Silk Touch
- Vanilla functionality.

Unbreaking
- Vanilla functionality.

Fortune
- Vanilla functionality.

Mending
- Vanilla functionality.

Power
- Increases melee reach distance.

Punch
- Applies knockback regardless of item.

Flame
- Vanilla functionality.

Infinity
- Special behavior.

If applied to:
- Food
- Potions
- Totems
- Blocks
- Consumables

Item becomes infinite-use.

If applied to Shield:
- Never breaks.
- Cannot be disabled by axes.

If applied to Bow:
- Vanilla functionality.

==================================================
REMOVED ENCHANTMENTS
==================================================

Curse of Binding

Curse of Vanishing

Luck of the Sea

Lure

These enchantments cannot generate.

==================================================
THE END PHASE
==================================================

At a configurable point:

The End opens.

This should be one of the few server-wide announcements.

==================================================
DRAGON EGG ARTIFACT
==================================================

When a player obtains the Dragon Egg:

- Egg is consumed.
- Replaced with a unique artifact:

Death Ban

Properties:

- Cannot be enchanted.
- Cannot be combined.
- Exists only once.

Effect:

If holder kills another player:

Victim is permanently death-banned
(or banned until event end, configurable).

Death Ban transfers through death like other artifacts.

==================================================
DATA STORAGE
==================================================

Store:

- Existing enchantments
- Current ownership
- Event state
- Kill roll state
- Dragon Egg owner
- Death Ban owner

==================================================
CONFIGURATION
==================================================

Configurable:

- Roll delay
- Explosion strength
- Death-ban duration
- Broadcast messages
- End opening timing
- Allowed enchantments
- Discovery methods
