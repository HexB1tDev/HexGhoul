## 2.0.6 (HexGhoul): children have a chance at one kagune or both
HexKagune 2.0.6 for Forge 1.20.1. Install on client and server (**protocol 73**, unchanged). Built and tested by CI. **Not yet tried in game**: please report anything that looks or feels wrong.

### From your report

- **Why your children were Ukaku-only:** you were on the Ukaku path, and Touka is Ukaku too. Two Ukaku parents can only pass on wings, so this wasn't a drawing bug. Both kagunes are drawn separately from what the child inherited.
- **Now it's a chance**, as you chose. When the parents have different kinds, the child gets:
  - 40% the mother's kind alone
  - 40% the father's kind alone
  - 20% both (a Rinkaku tentacle and one Ukaku wing; with Eto, her flesh and the other kind)
  - A child with one kind alone takes that parent's kagune colour.
- **Unchanged:** the same kind from both parents (Touka with an Ukaku player: wings), or one parent with none (Touka with a human: her wings).
- **Children already born keep theirs.** It's decided at conception and saved.

### Who passes what (unchanged)

- **Touka:** Ukaku wings. **Ken:** a Rinkaku tail. **Eto:** her flesh (the Owl).
- **A player on the Ukaku path:** wings. **On any other path** (Rinkaku, undecided, Owl, Graft): a Rinkaku tail. **A human:** nothing.
- To get a child with a tail and a wing with Touka, the other parent has to be off the Ukaku path, and then it's a 20% chance.
- `/kagune children` near your kids prints what each one inherited.
