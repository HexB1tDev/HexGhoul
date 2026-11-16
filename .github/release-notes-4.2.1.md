## 4.2.1 (HexGhoul): every quest checked: three that could never be finished, and every "no idea how" fixed

HexKagune 4.2.1 for Forge 1.20.1. Not tested in game. Protocol unchanged (128).

You said (quoted exactly): "Check every request to make sure that there's no issues like the Anteco one. Like, I can't stress this enough.
I only tested one quest so far, and that's like the first one I try. It has that bug that I already described, and it is fixed now. But
check all the other quests, bro."

Every quest, favour, hunt, gang mission, gang daily, companion wish and romance chapter was gone through, each step traced to the code
that counts it and each thing asked for to where it comes from. What was wrong:

**Could never be finished:** "A Friend in the Dark" asks you to do a favour for a ghoul, and nothing ever counted a finished favour. So it
could never be completed, and "Errands of the Night" and "Side by Side" (which come after it) were blocked too. A favour done now counts.

**Tonight's hunt asked for what isn't there:** a hunt lasts one night, but it could ask for husks (deserts only) while you stood in plains,
strays away from the snow, drowned away from water, or vindicators, pillagers, phantoms, cave spiders or iron golems (mansions, raids,
sleepless nights, mineshafts, villages). Now a hunt only asks for what lives where you are tonight. Every prey, in a hunt or a ghoul's
favour (favours have no deadline, so they may still ask for anything), says where it's found: "Kill vindicators (in woodland mansions and
raids)".

**Roma's "Bring Roma the dove's quinque case":** investigators only drop their case now and then (7 to 10 in 100; the ranked ones 2 to 5;
the CCG's ghoul division never), and nothing said so: you could kill a dozen hoping for one. Now the investigator you kill for the job always
leaves its case ("the dove's"; a Blade case if the dove was one of the ghoul division), and the step says so.

**Ken's daily "Brew Anteiku Blend":** the café brew only worked during the shift, and blood sugar was a hidden chain (human meat and a bottle,
then sugar). Now Ken hands you the makings with the job, and a glass bottle brews a cup in the café any time you carry the makings. Every
daily that asks you to bring something says where it comes from (alliums: flower forests and meadows; charcoal: logs in a furnace; a
quinque case: investigators drop them now and then...).

**The dishes asked for:** when Touka, Ken or Eto wish for an Anteiku Blend, Touka for a Blood Latte, Ken for Crimson Tea or Ayato for an RC
Tonic, they now say how it's made, all the way down ("craft 1 glass bottle, 2 cocoa beans and 1 Blood Sugar Cubes together; Blood Sugar Cubes:
craft 1 sugar and 1 Bottled Blood together; Bottled Blood: ..."). So does Ayato's chapter asking for three ghoul's pies.

**Checked and fine:** the other quests of the journal (each step is counted by something real, every form they name exists), the gang
missions (each starts only when it can run, says so when it can't, and says how in the quest menu), the companions' lessons, every other
daily and every other romance chapter.

**So it can't come back:** a new test reads every place the mod counts quest progress and fails the build if any quest, favour or hunt
ever asks for something nothing counts; it found the favour bug by itself when it was put back on purpose.
