# 🧭 Projekt-Roadmap – SKFlipper (Fabric 1.21.5)

Ein strukturierter Entwicklungsplan für den Hypixel SkyBlock Auction/Bazaar Flipper Mod.  
Diese Roadmap ist priorisiert, modular aufgebaut und auf die aktuelle Projektstruktur abgestimmt.  
**⚠️ Wichtig:** Es dürfen keine API-, Dependency- oder Version-Änderungen vorgenommen werden.  
Alle Arbeiten erfolgen strikt innerhalb der bestehenden Fabric-1.21.5-Konfiguration.

---

## 🥇 Phase 1 – Grundpfeiler festigen (Prio A – essentiell)

Ziel: Stabiler, fehlerfreier Unterbau für API, Cache und UI.

- [ ] **1.1 Service-API Validierung**  
  Überprüfung und Absicherung aller Service-Klassen (`BazaarService`, `AuctionHouseService`, `BitsShopService` etc.)  
  gegen fehlerhafte Responses mit `DataIntegrity`.

- [ ] **1.2 CacheManager Optimierung**  
  Einführung definierter Cache-Intervalle und Ablaufzeiten (z. B. 60 s / 300 s).  
  Optional LRU-Cache zur Performance-Optimierung.

- [ ] **1.3 ServiceMonitor Ausbau**  
  Status-Logging und Heartbeat-System für API-Zustände, Ratelimits und Key-Cooldowns.

- [ ] **1.4 UI Base-Screen Cleanup**  
  Vereinheitlichung der Owo-UI-Struktur in `SKFlipperScreen` und FilterBars.

---

## 🧮 Phase 2 – Analyse & Flipping-Logik (Prio A)

Ziel: Zuverlässige Berechnung profitabler Bazaar- und Auktionsflips.

- [ ] **2.1 BazaarAnalyzer V2**  
  Erweiterung um Steuerberechnung, Mindestvolumen, ROI-Formel (Profit / Investition).

- [ ] **2.2 AuctionAnalyzer (NEU)**  
  Neue Analysekategorie für Auction-House-Flips (BIN- und Sofortkäufe).  
  Implementierung analog zum `BazaarAnalyzer`.

- [ ] **2.3 Unified AnalyzerCore**  
  Gemeinsame Basisklasse für Analysetools (z. B. Margin, Volumen, Invest Cap).  
  Ziel: weniger redundanter Code.

---

## 💻 Phase 3 – Benutzeroberfläche & UX (Prio B)

Ziel: Interaktive, verständliche und modulare Benutzeroberfläche mit Owo-UI.

- [ ] **3.1 Tab-System**  
  Tabs für *Bazaar / Auction / Bits Shop / Analytics* im Hauptbildschirm.

- [ ] **3.2 Analyzer Results Panel**  
  Dynamische Ergebnisanzeige mit Tabellen (Item, Profit %, Volumen, Trend).

- [ ] **3.3 FilterBar Upgrade**  
  Erweiterte Filteroptionen: Kategorie, Mindestprofit, Sortierung, Volumen.

- [ ] **3.4 Log/Status HUD**  
  Anzeige von Cache-Status, API-Key-Zustand und Online-Status in der UI.

---

## 🌐 Phase 4 – Netzwerk & Resilienz (Prio B/C)

Ziel: Fehlerresistenz und Offline-Stabilität bei API-Problemen.

- [ ] **4.1 Error Recovery System**  
  Retry-Mechanismus mit Exponential Backoff und Key-Failover bei 403/429.

- [ ] **4.2 OfflineCache**  
  Speichern von letzten API-Ergebnissen zur Nutzung bei Ausfällen.

- [ ] **4.3 Data Sanitizer**  
  Plausibilitätsprüfungen gegen `EconomyConstants` zur Sicherung konsistenter Daten.

---

## 📈 Phase 5 – Quality of Life & Refactoring (Prio C)

Ziel: Codequalität, Erweiterbarkeit und Benutzerfreundlichkeit verbessern.

- [ ] **5.1 Logging Overhaul**  
  Log-Level-System (`INFO`, `WARN`, `ERROR`) + GUI-Umschaltung.

- [ ] **5.2 ModConfig Erweiterung**  
  Benutzeroptionen für Refresh-Intervalle, ROI-Grenze, Sprache etc.

- [ ] **5.3 Dependency Review**  
  Sicherstellen, dass alle Imports konsistent und Versionen stabil bleiben.

- [ ] **5.4 Dokumentation (KDoc)**  
  Kommentare und Funktionsbeschreibungen für Analyzer, Services und UI.

---

## 🔒 Phase 6 – Testing & Release (Prio C)

Ziel: Funktionale Tests, Fehlerfreiheit und Release-Build.

- [ ] **6.1 Unit-Tests für Analyzer & Services**  
  Validierung der Profit- und Preisberechnung mit Testdaten.

- [ ] **6.2 Integrationstest (Mock API)**  
  Simulierte Hypixel-API zur vollständigen Build-Validierung.

- [ ] **6.3 Finaler Release-Build**  
  Kompilierung, Remapping und Obfuscation für veröffentlichungsfertige JAR.

---

## 🗓 Umsetzungsreihenfolge

1. Phase 1 – Core-Stabilität  
2. Phase 2 – Analyzer-Systeme  
3. Phase 3 – Benutzeroberfläche  
4. Phase 4 + 5 – Netzwerk & Qualität  
5. Phase 6 – Testing / Release

---

## 🧱 Technische Eckdaten (nicht ändern!)

| Komponente | Version |
|-------------|----------|
| Minecraft | 1.21.5 |
| Fabric Loader | 0.17.0 |
| Loom | 1.11-SNAPSHOT |
| Fabric API | 0.128.2+1.21.5 |
| Kotlin | 2.1.20 |
| Fabric Language Kotlin | 1.13.2+kotlin.2.1.20 |
| owo-lib | 0.12.21+1.21.5 |
| Yet Another Config Lib v3 | 3.7.1+1.21.5-fabric |

---

💡 *Hinweis:*  
Diese Roadmap wird fortlaufend aktualisiert, sobald Features abgeschlossen oder priorisiert werden.  
Für Änderungen an Phasen oder Zielen immer vorher technische Konsistenz prüfen (Fabric 1.21.5!).

