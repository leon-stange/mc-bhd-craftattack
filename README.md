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
- Bei vollem Inventar wird die Erstvergabe verschoben. Sobald ein Platz frei ist,
  erhält der Spieler seine Elytra; sie wird nicht auf den Boden geworfen.

## Schutz vor Verarbeitung

Die persönliche Elytra darf weder im Schleifstein verarbeitet noch als
Reparaturmaterial verbraucht werden. Entsprechende Verarbeitungsvorgänge werden
verhindert, ohne die Elytra zu verbrauchen oder zu verändern. Besitzer-Zuordnung,
Name, Beschreibung und Schutz-Eigenschaften bleiben erhalten.

Gewöhnliche Elytren und andere Gegenstände sind von diesem Verarbeitungsschutz
nicht betroffen.
Auch das Verbrauchen in Reparatur-Rezepten am Craftingfeld, an der Werkbank oder
im automatischen Crafter wird verhindert.

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
Ist der Brustslot beim Respawn bereits belegt, wird der vorhandene Gegenstand
in den bisherigen Inventarslot der Elytra getauscht. Dadurch geht auch bei
vollem Inventar kein Gegenstand verloren.

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
| `/bhd-craftattack death reset <Spielername>` | Den Todeszähler des angegebenen Spielers auf `0` zurücksetzen. | Nur Operatoren |
| `/bhd-craftattack team <Teamname>` | Den eigenen Teamnamen setzen oder ändern. | Alle Spieler |
| `/bhd-craftattack team <Spielername> <Teamname>` | Den Teamnamen eines anderen Spielers setzen oder ändern. | Nur Operatoren |
| `/bhd-craftattack team <Spielername> reset` | Den Teamnamen des angegebenen Spielers löschen. | Nur Operatoren |

Alle Befehle verwenden einheitlich den Hauptbefehl `/bhd-craftattack`.

## Teamnamen in Chat und Tab-Liste

Jeder Spieler kann seinen eigenen Teamnamen frei wählen. Ein Teamname ist ein
sichtbarer Zusatz zum Spielernamen und verleiht keine zusätzlichen Rechte.

```text
/bhd-craftattack team Baumeister
```

Damit erscheint der Spieler beispielsweise als `[Baumeister] Leon` in der
Tab-Liste und mit demselben Präfix im Chat. Die eigentliche Chatnachricht bleibt
erhalten; vorhandene Chatformatierung wird weiterverwendet.

Eigene Teamnamen mit Leerzeichen werden in Anführungszeichen angegeben:

```text
/bhd-craftattack team "Die Baumeister"
```

Teamnamen werden als normaler Text angezeigt. Sonderzeichen und Umlaute sind
möglich; Farb- oder Formatierungscodes werden nicht interpretiert. Leere Namen
und Steuerzeichen wie Zeilenumbrüche werden abgelehnt.

Operatoren können den Teamnamen eines online verbundenen Spielers verwalten:

```text
/bhd-craftattack team Leon Baumeister
/bhd-craftattack team Leon Die Baumeister
/bhd-craftattack team Leon reset
```

Der letzte Befehl entfernt den Teamnamen und damit den Präfix in Chat und
Tab-Liste. Andere Spieler dürfen fremde Teamnamen weder setzen noch zurücksetzen.
Teamnamen werden dauerhaft in den Spielerdaten gespeichert und nach Rejoin,
Respawn oder Serverneustart wieder angezeigt.

## Serverregeln im Chat

Mit `/bhd-craftattack regeln` kann jeder Spieler die Regeln jederzeit abrufen.
Die Ausgabe wird nur dem ausführenden Spieler angezeigt. Die Regeltexte sind
über die `config.yml` anpassbar.

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
  Die Warnung verhindert die ausdrücklich angeforderte Ersatzvergabe nicht.
- Die manuelle Ersatzvergabe löst bei späteren Joins keine erneute automatische
  Vergabe aus und entfernt keine bereits in Kisten liegenden Elytren.

## Todeszähler

Die Anzahl der Tode pro Spieler wird in der Tab-Liste angezeigt.
Die Anzeige lässt sich über `death-counter.enabled` in der Plugin-Konfiguration
ein- und ausschalten; in einer neu erzeugten Konfiguration ist sie eingeschaltet.
Eine vorhandene Einstellung `enabled: false` wird nicht überschrieben. In diesem
Fall `death-counter.enabled` auf `true` setzen und den Server neu starten.
Die Zählerstände bleiben über Spieler-Rejoins und Serverneustarts hinweg erhalten.
Der Elytra-Todesschutz beeinflusst die Zählung nicht: Ein Tod wird auch dann
gezählt, wenn die persönliche Elytra erhalten bleibt.

Verwendet wird die dauerhaft gespeicherte Minecraft-Statistik `DEATHS`,
einschließlich bereits vor der Plugin-Installation gezählter Tode. Die Anzeige
wird einmal pro Sekunde aktualisiert.

Mit `/bhd-craftattack death reset <Spielername>` kann ein Operator den Zähler
eines einzelnen Spielers auf `0` zurücksetzen. Andere Spieler bleiben unverändert.
Der Befehl funktioniert für online verbundene und dem Server bereits bekannte
Offline-Spieler. Unbekannte Spieler werden mit einer Fehlermeldung abgelehnt.
Die Änderung wird dauerhaft gespeichert und betrifft die Minecraft-Statistik
`DEATHS` selbst. Auch nach einem Neustart gilt damit der zurückgesetzte Wert;
zukünftige Tode werden wieder ab `0` weitergezählt.

## Speicherung und Konfiguration

Die `config.yml` wird beim ersten Start unter `plugins/BHD-CraftAttack/` erstellt.
Nach Änderungen an den Regeln oder am Todeszähler den Server vollständig neu
starten.

Erstvergabe, Teamname und die Markierung zum Anziehen nach dem Tod liegen in den
persistenten Spielerdaten von Minecraft. Die Besitzer-UUID liegt direkt auf der
Elytra. Dafür sind keine Datenbank und keine zusätzliche Spielerdatei des Plugins
nötig. Bei Sicherungen auch die Spieler- und Weltdaten des Servers sichern.

## Bauen und installieren

Voraussetzung ist ein **JDK 25**. Der Gradle-Wrapper lädt die benötigten
Build-Abhängigkeiten beim ersten Aufruf herunter.

```bash
./gradlew build
```

Unter Windows: `gradlew.bat build`.

Die fertige Datei liegt unter `build/libs/BHD-CraftAttack-0.0.1.jar`. Diese JAR
in den `plugins`-Ordner des Paper-26.3-Servers kopieren und den Server vollständig
neu starten. Zusätzliche Plugins, Laufzeitbibliotheken oder Client-Mods sind
nicht erforderlich.

## Tests

`./gradlew build` prüft auch Plugin-Registrierung, Version und Standardkonfiguration.
Die Verhaltensprüfungen laufen separat auf einem lokalen Paper-26.3-Testserver:

```bash
./gradlew integrationTest
```

Dabei werden Paper Stable Build 190 und dessen Abhängigkeiten heruntergeladen.
Für diesen isolierten Testserver wird die Minecraft-EULA angenommen. Er lauscht
nur auf `127.0.0.1` mit einem automatisch vergebenen Port und beendet sich nach
den Tests selbst. Sämtliche Testdaten liegen unter `build/integration/`, die
letzte Ausgabe in `build/integration/latest.log`.

Die Tests verwenden simulierte Spieler und Inventaransichten mit der tatsächlichen
Paper-Item-, Metadaten-, Event- und Scoreboard-API. Geprüft werden unter anderem
Erstvergabe, persistente Kennzeichnungen, Inventar/Brustslot/Nebenhand beim Tod,
Respawn, volle Inventare, doppelte Ersatz-Elytren, Drop- und Verarbeitungsschutz,
Berechtigungen, konfigurierbare Regeln und die Tab-Anzeige. Zusätzliche Prüfungen
decken den Todeszähler-Reset einschließlich echter Offline-Statistikdateien,
Team-Befehle, fremde Team-Verwaltung, persistente Teamnamen und die
Chat-Darstellung ab. Der Test-Plugin-Code
wird nicht in die auszuliefernde JAR gepackt. Eine visuelle Prüfung mit einem
Minecraft-Client bleibt ein eigener Spieltest.

Grundlagen: [Paper-Projektaufbau](https://docs.papermc.io/paper/dev/project-setup/),
[persistente Daten](https://docs.papermc.io/paper/dev/pdc/) und
[einzelne Gegenstände beim Tod behalten](https://jd.papermc.io/paper/26.3/org/bukkit/event/entity/PlayerDeathEvent.html#getItemsToKeep()).
Die Chat-Anpassung verwendet [Papers Chat-Renderer](https://docs.papermc.io/paper/dev/chat-events/).
