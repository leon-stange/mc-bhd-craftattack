# BHD-CraftAttack

**BHD-CraftAttack** ist ein eigenes Minecraft-Server-Plugin für unseren
CraftAttack-Server.

Die erste Plugin-Version ist **v0.0.1**.

## Zielplattform

- Minecraft Java Edition 26.3
- PaperMC 26.3 (Stable)
- Java 25

## Willkommensnachricht

Bei jedem Join erhält der beitretende Spieler folgende Nachricht im Chat:

```text
BHD - Craft Attack: Willkommen auf dem Server!
```

Die Nachricht erscheint bei jedem erneuten Beitritt, nicht nur beim ersten Join.

## Persönliche Elytra

Jeder Spieler erhält beim ersten Join genau eine persönliche Elytra direkt in
sein Inventar.

- Die Elytra ist unzerstörbar (`Unbreakable`) und kann durch Benutzung nicht
  kaputtgehen.
- Ihr Name lautet `Elytra - <Spielername>`, beispielsweise `Elytra - Leon`.
  Der gesamte Name wird dunkelrot dargestellt.
- Als Gegenstandsbeschreibung (Lore) wird `Persönliche Elytra von <Spielername>`
  angezeigt, beispielsweise `Persönliche Elytra von Leon`.
- Die Vergabe wird pro Spieler dauerhaft gespeichert. Erneutes Beitreten oder
  ein Serverneustart führt nicht zur Vergabe einer weiteren Elytra.

## Schutz vor Verarbeitung

Die persönliche Elytra darf weder im Schleifstein verarbeitet noch als
Reparaturmaterial verbraucht werden. Entsprechende Verarbeitungsvorgänge werden
verhindert, ohne die Elytra zu verbrauchen oder zu verändern. Besitzer-Zuordnung,
Name, Beschreibung und Schutz-Eigenschaften bleiben erhalten.

Gewöhnliche Elytren und andere Gegenstände sind von diesem Verarbeitungsschutz
nicht betroffen.

## Ablegen und Drop-Schutz

Ein Spieler darf seine persönliche Elytra ausrüsten, ablegen, innerhalb seines
Inventars verschieben und in eine Kiste legen oder daraus entnehmen.

Das manuelle Droppen auf den Boden ist dagegen gesperrt. Das gilt sowohl für
die Drop-Taste (standardmäßig `Q` beziehungsweise `Strg+Q`) als auch für das
Hinauswerfen über die Inventaroberfläche. Bei einem verhinderten Drop bleibt
die Elytra beim Spieler erhalten; es wird kein Gegenstand auf dem Boden erzeugt.
Gewöhnliche Elytren und andere Gegenstände sind von dieser Sperre nicht betroffen.

Bei einem verhinderten Drop erhält der Spieler exakt diesen Hinweis im Chat:

```text
BHD - Craft Attack: Du kannst deine Elytra in einer Kiste ablegen, aber nicht droppen.
```

## Verhalten beim Tod

Das übrige Inventar unterliegt weiterhin dem normalen Todesverhalten des Servers.
Die eigene persönliche Elytra wird davon ausgenommen, wenn der Spieler sie beim
Tod bei sich trägt:

| Situation beim Tod | Verhalten |
| --- | --- |
| Die eigene Elytra liegt im Spielerinventar. | Sie wird nicht als Todesbeute gedroppt und nach dem Respawn automatisch im Brustslot ausgerüstet. |
| Die eigene Elytra ist im Brustslot ausgerüstet. | Sie bleibt vor dem Verlust durch den Tod geschützt und wird nach dem Respawn wieder im Brustslot ausgerüstet. |
| Die eigene Elytra liegt in einer Kiste. | Es passiert nichts. Sie bleibt in der Kiste und es wird keine Ersatz-Elytra vergeben. |
| Der Spieler trägt keine eigene Elytra bei sich. | Es passiert nichts; das Plugin erzeugt beim Tod oder Respawn keine Elytra. |

Das automatische Ausrüsten gilt nur für die beim Tod tatsächlich mitgeführte
eigene Elytra. Eine in einer Kiste gelagerte Elytra wird nicht zurückgeholt.
Ist der Brustslot beim Respawn bereits belegt, darf der vorhandene Gegenstand
nicht überschrieben oder gelöscht werden; er wird sicher ins Inventar verschoben.
Falls dort kein Platz vorhanden ist, bleibt die Elytra erhalten und wird erst
ausgerüstet, sobald sie ohne Gegenstandsverlust angelegt werden kann.

Der Todesschutz darf die Elytra weder duplizieren noch zusätzlich als Gegenstand
am Todesort hinterlassen. Er betrifft die eigene, vom Plugin vergebene Elytra;
gewöhnliche Elytren erhalten diesen Schutz nicht.

Die persönliche Elytra wird über eine interne Item-Kennzeichnung mit der UUID
ihres ursprünglichen Besitzers erkannt. Der sichtbare Name allein dient nicht
als Erkennungsmerkmal.

## Befehle

| Befehl | Funktion | Berechtigung |
| --- | --- | --- |
| `/bhd-craftattack elytra <Spielername>` | Dem angegebenen Spieler eine neue persönliche Elytra als Ersatz direkt ins Inventar geben. | Nur Operatoren |
| `/bhd-craftattack version` | Die aktuell geladene Plugin-Version anzeigen; die erste Version ist `v0.0.1`. | Nur Operatoren |
| `/bhd-craftattack regeln` | Die Serverregeln im eigenen Chat anzeigen. | Alle Spieler |

## Serverregeln im Chat

Mit `/bhd-craftattack regeln` kann jeder Spieler die Regeln jederzeit abrufen.
Die Ausgabe wird nur dem ausführenden Spieler angezeigt. Die Regeltexte sollen
über die `config.yml` anpassbar sein.

Folgender konkreter Regelvorschlag soll als Ausgangspunkt für die Chat-Ausgabe
dienen:

```text
BHD - Craft Attack: Serverregeln
1. Geht respektvoll miteinander um. Keine Beleidigungen, Diskriminierung oder Belästigung.
2. Kein Griefing: Verändert oder zerstört fremde Bauwerke und Grundstücke nur mit Erlaubnis.
3. Kein Diebstahl: Fremde Kisten, Gegenstände, Tiere und Farmen sind ohne Erlaubnis tabu. Ausnahmen sind ausdrücklich freigegebene Gemeinschaftsbereiche.
4. PvP und Fallen nur mit Zustimmung aller Beteiligten. Streiche dürfen keinen dauerhaften Schaden oder Itemverlust verursachen.
5. Keine Cheats, X-Ray oder Duplizierungs-Exploits. Meldet ausnutzbare Fehler einem Operator.
6. Haltet Spawn und Gemeinschaftsbereiche ordentlich. Pflanzt gemeinsam genutzte Felder nach und repariert selbst verursachte Schäden.
7. Verursacht keine absichtlichen Lags. Sprecht besonders große Farmen und Redstone-Anlagen vorher mit einem Operator ab.
8. Meldet verschwundene persönliche Elytren einem Operator. Nutzt die Ersatzvergabe nicht, um zusätzliche Elytren zu sammeln.
```

Die Texte sind Vorschläge und können vor dem Einsatz an eure Spielweise angepasst
werden.

## Manuelle Elytra-Vergabe durch Operatoren

Der Elytra-Befehl dient als Notfall-Ersatz, falls die ursprüngliche Elytra verloren
gegangen oder verschwunden ist.

- Nur Server-Operatoren dürfen den Befehl ausführen. Andere Spieler erhalten
  eine Fehlermeldung; es wird keine Elytra vergeben.
- Die manuelle Vergabe funktioniert auch dann, wenn der Spieler seine einmalige
  Elytra beim ersten Join bereits erhalten hat.
- Die Ersatz-Elytra erhält den Namen `Elytra - <Spielername>` in Dunkelrot,
  die Beschreibung `Persönliche Elytra von <Spielername>`, ist unzerstörbar und
  bekommt dieselbe interne Besitzer-Kennzeichnung sowie denselben Drop-,
  Todes- und Verarbeitungsschutz wie die ursprünglich vergebene Elytra.
- Die Zuordnung erfolgt zum angegebenen Empfänger, nicht zum ausführenden
  Operator.
- Der Empfänger muss online sein. Ein unbekannter oder nicht verbundener Spieler
  führt zu einer verständlichen Fehlermeldung.
- Ist kein Platz im Inventar, wird die Vergabe mit einer Fehlermeldung
  abgebrochen. Die Elytra wird nicht als Ausweichlösung auf den Boden gedroppt.
- Der ausführende Operator erhält nach erfolgreicher Vergabe eine Bestätigung.
- Hat der Empfänger bereits seine persönliche Elytra im Inventar oder
  ausgerüstet, erhält der Operator einen Hinweis auf die mögliche doppelte
  Vergabe. Die Prüfung bezieht sich auf die eigene Plugin-Elytra des Empfängers;
  gewöhnliche Elytren zählen dabei nicht. Kisten werden nicht durchsucht.
- Die manuelle Ersatzvergabe löst bei späteren Joins keine erneute automatische
  Vergabe aus und entfernt keine bereits in Kisten liegenden Elytren.

## Optionaler Todeszähler

Die Anzahl der Tode pro Spieler kann optional in der Tab-Liste angezeigt werden.
Diese Anzeige soll über die Plugin-Konfiguration ein- und ausschaltbar sein.
Die Zählerstände bleiben über Spieler-Rejoins und Serverneustarts hinweg erhalten.
Der Elytra-Todesschutz beeinflusst die Zählung nicht: Ein Tod wird auch dann
gezählt, wenn die persönliche Elytra erhalten bleibt.

Weitere Funktionen, Befehle und Berechtigungen werden noch ergänzt.
