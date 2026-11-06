# Moonpaw rules reference

Moonpaw plays like the classic Werewolf party game, without cards. This is our own summary. The source is the [German Wikipedia article](https://de.wikipedia.org/wiki/Die_Werw%C3%B6lfe_von_D%C3%BCsterwald_(Spiel)) on "Die Werwölfe von Düsterwald". Read it before designing a role that isn't listed here.

Use the mechanics only. Names, texts and art of Moonpaw are our own, nothing comes from the card game's name, cards or illustrations.

## Game flow

1. **Setup:** 8 to 18 players. Everyone gets a hidden role. The mod replaces the narrator. 2 to 4 wolves depending on player count, each special role replaces a villager.
2. **Night:** Everyone is "asleep". Roles act one after another. The wolves see each other and agree on a victim. No agreement means no victim.
3. **Day:** The victims are announced, they are out and their role is revealed. Dead players may not communicate with the living. The village discusses.
4. **Vote:** Every living player votes. Most votes is out and their role is revealed. On a tie there is a runoff or the captain decides. Then the next night starts.
5. **End:** The game ends when all wolves or all villagers are dead. A wolf and villager couple only wins as the last two alive.

## Night order

The article only states this: the thief first, Cupid only on the first night, the witch after the wolves chose, the flute player last. The full order is our decision, and so are the role counts per player count and edge cases. The article doesn't cover them.

## Base roles

| Role | Side | Ability |
| --- | --- | --- |
| Villager | Village | None |
| Werewolf | Wolves | Picks a victim with the pack every night |
| Seer | Village | Sees the role of one player every night |
| Witch | Village | One heal and one poison for the whole game, wakes after the wolves. Can heal herself, so a night has 0, 1 or 2 deaths |
| Hunter | Village | Takes one player along when dying |
| Cupid | Village | Links two lovers on the first night, may include himself. If one dies the other dies, and they may never vote against each other |
| Thief | Village, can switch | Wakes first, may swap with one of two leftover roles. Must swap if both leftovers are werewolves |
| Little Girl | Village | May peek during the wolf phase, the wolves may catch her |
| Captain | Public extra title | Drawn or elected, breaks vote ties, passes the title on when dying |

## For later

- **Protector, scapegoat, village idiot, elder, flute player** and event cards (first expansion)
- **Buildings with professions** (second expansion)
- **Big bad wolf, white werewolf, wild child, fox, knight** and more (third expansion)

## Rules for the implementation

- The server decides everything. A client only learns what its player may know, so a hacked client can't reveal roles.
- A Discord status is public, so it never shows hidden information: no roles, no night targets.
