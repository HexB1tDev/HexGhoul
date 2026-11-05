## 3.0.6 (HexGhoul): The kagune-made weapons are real items

HexKagune 3.0.6 for Forge 1.20.1. Install matching versions on client and server. Not tested in game.

The four kagune-made weapons are real items now. The Tendril Whip, Crystal Spear, Flesh Hammer and Bone Greatsword each have their own case and weapon item: given by /give, in the Combat tab, and dropped by the investigators who carry them (their own case now, not the regular quinque case; a masterwork's stays a masterwork). Right click the case and the weapon tears out of it in your hand (drawn from the same mesh the investigators use). M1 runs its whole string of blows with the investigators' animations, right click is its first move and Shift + right click its second (Bind/Flay, Crystal Javelin/Piercing Charge, Quake/Whirlwind, Leaping Cut/Cleave), each on a cooldown. Put it in another slot and it folds back into its case. Investigators carry the new case at peace too.

- Items: `ControlItems` registers `quinque_case_<key>` and `quinque_<key>` for each `Arm` (`Arm.key/caseId/weaponId/byId`); masterwork tag `ArmMasterwork`. Icons rendered from the real meshes (`tools/ArmIcons.java`), case textures recoloured from the regular kind's (black case, crimson seams; `tools/arm_items.py`), lang lines.
- Server: `PlayerArms` (open, fold away, string with a one-click buffer, the eight moves ported from `AgentArms`, bleeds, falls forgiven after leaps and slams). Network: `C2SArmAction` (50), `S2CArmSwing` (51); protocol "88".
- Client: `WeaponClient` (M1/right click input, every player's blows, the body posed with `WeaponPose`), `ArmItemLayer` (mesh and whip tendril in third person, first-person view with a swing shaped by each blow).
- Investigators: `AgentBrain.arm` keeps the arm's own case in hand at peace; `drop` leaves it.

Nothing tested in game.
