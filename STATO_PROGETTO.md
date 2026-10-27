# Stato del Progetto - Closet Antigravity

Questo documento riassume lo stato attuale dell'applicazione, come è organizzata, le modifiche effettuate, le decisioni tecniche prese e le cose importanti da non toccare.

---

## 1. Cosa fa l'app oggi

L'app è un assistente personale per la gestione dell'armadio e dei vestiti in pieno stile **Samsung One UI 9.0**:
- **Gestione Armadi, Cartelle e Capi**:
  - Creazione di più armadi (es. Principale, Invernale, Estivo) e organizzazione interna in cartelle con emoji o immagini di copertina personalizzate.
  - Aggiunta di capi con foto da fotocamera/galleria o emoji, nome, associazione a cartella e tag multipli.
  - **Supporto Capi Contatore**: elementi che rappresentano più pezzi identici (es. 5 paia di calzini, 3 t-shirt identiche), con badge quantità, stepper interattivo rapido `[- quantità +]` e selettore delle quantità da spostare.
  - **Fusione Automatica nello Spostamento**: spostando uno o più capi tra armadi o cartelle, se nella destinazione esiste già un capo con lo stesso nome e gli stessi tag, i due elementi si fondono automaticamente in un unico elemento in stile contatore sommando le rispettive quantità.
- **Popup Dettaglio Capo**:
  - Toccando un capo nell'armadio si apre una scheda a schermo intero con foto/emoji grande, nome, posizione, tutti i tag associati, contatore interattivo compatto (se presente) e pulsante rapido per la modifica.
- **Home personalizzabile**:
  - Schermata principale a griglia 2D con widget ridimensionabili e riordinabili (orologi digitali/analogici, meteo/calendario, scorciatoie rapide, contatore capi, torcia e bussola con sensori reali del telefono).
  - Tutti i widget in formato 1x1 sono perfettamente uniformati sulla base dell'orologio analogico.
  - Menu di aggiunta widget One UI minimale con sezioni chiuse come impostazione predefinita e anteprime grafiche in scala.
- **Funzioni Smart**:
  - **Panni / Lavanderia**: gestione dei capi da lavare con timer di lavaggio e ritorno automatico.
  - **Viaggio / Valigia**: creazione di valigie per i viaggi con promemoria di ritorno capi.
  - **Spostamento Capi**: trasferimento rapido o parziale di vestiti tra armadi diversi con storico dei trasferimenti.
  - **Foto Armadio**: scatto e visualizzazione di foto panoramiche degli armadi reali.
  - **Calendario & Cestino**: pianificazione degli outfit per giorno ed eventi, con recupero dei capi eliminati dal cestino.
  - **Strumenti**: scorciatoie e schermata dedicata per torcia LED e bussola magnetica reale.
- **Personalizzazione e Navigazione One UI**:
  - Scelta del tema (Scuro, Chiaro, AMOLED nero puro o Segui Sistema), colore accento personalizzato, barra inferiore fluttuante ("Isola") e funzioni preferite espandibili dal tasto stella.
  - Pulsante Indietro fluttuante ed elevato (`Surface` circolare One UI a 40 dp con ombra e bordo soft) standardizzato e identico in tutte le schermate dell'app.
- **Controllo di Versione Git**:
  - Repository Git locale inizializzato e tracciato sul branch `main`.

---

## 2. Le schermate e i file principali

Tutto il codice si trova nella cartella `app/src/main/java/com/example/closet/`:

### Logica e Dati:
- `ui/viewmodel/ClosetViewModel.kt`: è il motore dell'applicazione. Contiene le liste reattive (capi, armadi, cartelle, calendario, cronologia trasferimenti), le funzioni per aggiungere/modificare/spostare elementi e le logiche di salvataggio atomico e asincrono su memoria interna.
- `ui/screens/HomeModels.kt`: definisce i modelli di dati condivisi (colori tema One UI, schede di navigazione, tipi di widget e motore di disposizione della griglia 2D).

### Schermate e Componenti:
- **Home**:
  - `HomeScreen.kt`: la schermata Home con la griglia widget e la barra inferiore fluttuante ("Isola").
  - `HomeClockComponents.kt`: componenti per gli orologi digitali e analogici.
  - `HomeFunctionWidgets.kt`: widget di torcia, bussola, scorciatoie funzioni e contatori capi.
  - `HomeWardrobeWidgets.kt`: widget per contatori capi e anteprime armadio.
  - `HomeWidgetPickerDialog.kt`: finestra One UI a fisarmonica per scegliere e aggiungere nuovi widget alla Home con anteprime in miniatura.
- **Armadio (Wardrobe)**:
  - `WardrobeScreen.kt`: schermata principale dell'armadio con filtri, cartelle, capi e modalità di gestione con drag-and-drop.
  - `WardrobeCards.kt`: schede visive dei vestiti e delle cartelle (griglia ed elenco), badge contatore in basso a destra, badge quantità animato in alto a destra e apertura del dettaglio capo.
  - `WardrobeItemDialogs.kt`: finestre per aggiungere/modificare capi e cartelle, picker emoji e popup di dettaglio singolo capo (`ClothingDetailDialog`).
  - `WardrobeManagementDialogs.kt`: finestre con auto-scroll fluido a 60 fps per creare, rinominare e riordinare armadi e tag.
- **Funzioni (Functions)**:
  - `FunctionsScreen.kt`: menu principale delle funzioni avanzate con tasto stella preferiti.
  - `ToolsScreen.kt`: torcia e bussola a schermo intero con tasto indietro fluttuante.
  - `TripScreen.kt`: preparazione valigia e gestione viaggio con tasto indietro fluttuante.
  - `MoveClothingScreen.kt`: spostamento capi tra armadi con tasto indietro fluttuante.
  - `TransfersActivityScreen.kt`: storico cronologico dei trasferimenti con tasto indietro fluttuante.
  - `ClosetPhotosScreen.kt`: scatto e visualizzazione foto panoramiche degli armadi con tasto indietro fluttuante.
  - `CameraCaptureDialog.kt`: fotocamera integrata con ritaglio e anteprima.
  - `CalendarScreen.kt`: calendario per associare vestiti alle giornate con tasto indietro fluttuante.
- **Impostazioni (Settings)**:
  - `SettingsScreen.kt`: slider tema a 4 posizioni, sfumature di dissolvenza, tasto indietro fluttuante, backup/ripristino dati e crediti versione.
  - `SettingsCustomizationDialogs.kt`: finestre per riordinare le schede dell'Isola e i preferiti.

---

## 3. Riepilogo cronologico delle modifiche e novità

### Fondamenta e Ottimizzazioni di Sistema (v1.1.00 - v1.2.02)
1. **Risoluzione errori di compilazione e dipendenze**:
   - Rimossa la dipendenza duplicata di `coil` e mantenuto `compileSdk`/`targetSdk` alla versione 35.
   - Eliminati file inutilizzati e tracce legacy di Firebase.
2. **Modularizzazione del codice**:
   - Suddivisi i file monolitici (`HomeScreen`, `WardrobeScreen`, `FunctionsScreen`, `SettingsScreen`) in moduli specializzati e riutilizzabili, riducendo drasticamente i tempi di build di Android Studio.
3. **Ottimizzazione Prestazioni e Salvataggio Dati**:
   - Ridimensionamento intelligente delle immagini su disco con caching Coil avanzato in memoria RAM e memoria interna.
   - Salvataggi asincroni su `Dispatchers.IO` con coda atomica sequenziale per impedire la corruzione dei dati.
4. **Drag-and-Drop fluido a 60 fps**:
   - Riordino funzioni, armadi, tag e capi d'abbigliamento completamente riscritti con molle fluide (`spring`), auto-scroll automatico avvicinandosi ai bordi e formule basate su coordinate assolute (zero oscillazioni o salti).

### Rifiniture One UI 9.0 e Interfaccia (v1.2.03 - v1.2.13)
5. **Rinnovamento Schermata Impostazioni**:
   - Eliminazione di descrizioni superflue, introduzione dello slider orizzontale a 4 posizioni per il tema (*Sistema, Chiaro, Scuro, AMOLED*) e sfumatura fluida di dissolvenza sotto l'header.
6. **Morphing dell'Isola One UI e Menu Preferiti**:
   - Morphing geometrico a molla dell'isola fluttuante e menu popup dei preferiti che nasce ed espande organicamente dal tasto stella con allineamento millimetrico.
7. **Menu Aggiunta Widget a Fisarmonica**:
   - Catalogo widget organizzato in sezioni chiudibili con anteprime visive in scala reale e pulsante "+ Aggiungi" a pillola One UI.
8. **Mini Bussola Grafica Reale nei Widget 1x1 e 1x2**:
   - Quadrante analogico con ghiera graduata, indicatore Nord triangolare e ago magnetico 3D bicolore collegato ai sensori reali del dispositivo.

### Novità e Perfezionamenti Recenti (v1.2.14 - v1.2.24)
9. **Reattività Colori nei Widget Scorciatoie Funzioni (v1.2.14)**:
   - I widget scorciatoia della Home aggiornano istantaneamente il loro colore non appena l'utente attiva o disattiva l'impostazione "Colori distinti delle funzioni" nelle Impostazioni.
10. **Sezioni Chiuse di Default nel Menu Widget (v1.2.15)**:
    - All'apertura della finestra per aggiungere widget, tutte le sezioni tematiche si presentano chiuse e ordinate, offrendo un colpo d'occhio pulito e immediato.
11. **Bussola 1x1 Minimale e Pulita (v1.2.16)**:
    - Rimossi i gradi numerici dal centro del widget 1x1 per dare massimo respiro al quadrante e all'ago 3D rotante, mantenendo in risalto la sola direzione cardinale.
12. **Uniformazione Totale di Tutti i Widget 1x1 (v1.2.17)**:
    - Standardizzati tutti i widget 1x1 (Bussola, Torcia, Aggiungi Capo, Cerca, Nuovo Evento, Conteggio Capi, Calendario Oggi, Scorciatoie Funzioni) adottando la geometria condivisa dell'orologio analogico: padding perimetrale fisso a `10.dp` e disco interno circolare `surfaceSecondary`.
13. **Pulsante Indietro Fluttuante Elevato Universale (v1.2.18)**:
    - Esteso il pulsante Indietro circolare fluttuante da 40.dp (`Surface` con ombra, bordo soft ed elevazione fissa a `Alignment.TopStart`) a tutte le schermate dell'app: Impostazioni, Strumenti, Storico Trasferimenti, Viaggio/Valigia, Spostamento Capi/Panni, Foto Armadio, Calendario e navigazione interna delle cartelle dell'Armadio.
14. **Compattezza Scheda Contatore nel Dettaglio Capo (v1.2.19)**:
    - Rimosso il testo descrittivo ridondante "Elemento con contatore" dal popup `ClothingDetailDialog`. Ridotta l'altezza dello stepper a meno di 40.dp e impostato un limite verticale protettivo, garantendo che i tasti "Chiudi" e "Modifica" non vengano mai tagliati fuori schermo.
15. **Posizionamento Contatore in Basso a Destra nell'Anteprima Capo (v1.2.20)**:
    - Nella scheda a griglia dell'armadio (`ClothingGridCard`), la pillola del contatore `[- quantità +]` è stata spostata dall'angolo in basso a sinistra all'angolo in basso a destra dell'anteprima, migliorando ergonomia e pulizia visiva.
16. **Badge Quantità in Modalità Gestione Elementi (v1.2.21)**:
    - Entrando in modalità di gestione elementi nell'Armadio (`isEditMode`), su tutti i capi contatore compare un badge circolare animato nell'angolo in alto a destra della foto, con sfondo scuro traslucido e numero colorato d'accento.
17. **Selezione Quantità Esatta al Posto di "Modifica" (v1.2.22)**:
    - In modalità gestione elementi, quando viene selezionato un capo contatore, al posto del pulsante "Modifica" compare la pillola stepper interattiva per scegliere il numero esatto di pezzi da spostare (da 1 fino alla quantità massima). Lo spostamento gestisce correttamente le quote parziali lasciando il resto nell'armadio d'origine.
18. **Fusione Automatica Capi Omonimi con Stessi Tag (v1.2.23)**:
    - Quando un capo (contatore o singolo) viene spostato in un armadio o cartella dove è già presente un altro capo (contatore o singolo) con lo stesso nome e gli stessi tag, i due elementi si fondono automaticamente in un unico elemento in stile contatore (`isCountable = true`).
    - La quantità totale risultante è pari alla somma delle quantità dei singoli capi (i capi singoli valgono 1), preservando anteprima fotografica/emoji e dettagli.
19. **Inizializzazione Controllo di Versione Git (v1.2.24)**:
    - Inizializzato il repository Git locale sul branch `main`, configurato `.gitignore` per escludere file di cache e build e creato il commit iniziale tracciando l'intero stato del progetto.

---

## 4. Problemi ancora aperti o cose da fare in futuro

Al momento l'app è alla versione **1.2.24** (build **72**), compila con successo e non presenta errori bloccanti:
- **Test su dispositivo reale**: verificare l'uso continuativo della bussola e della fotocamera su vari dispositivi fisici con diverse densità di schermo.
- **Backup esterno automatico**: il backup è attualmente manuale (esportazione/importazione file JSON/ZIP dalle Impostazioni). Valutare in futuro l'integrazione opzionale con Google Drive se richiesto.
- **Passaggio a Room Database (opzionale)**: l'attuale architettura JSON asincrona è reattiva, leggera e stabile per centinaia di capi. In caso di collezioni enormi (molte migliaia di capi con decine di megabyte di dati), si potrà considerare una migrazione trasparente a Room SQLite.

---

## 5. Scelte di design decise

- **Salvataggio Dati**:
  - Dati testuali e configurazioni salvati in modo atomico e asincrono su `SharedPreferences` protette in formato JSON.
  - Immagini fotografiche compresse in JPEG salvate nella directory privata `clothing_images` della memoria interna, con caricamento scalato ad alta efficienza di RAM.
- **Interfaccia Grafica**:
  - Costruita interamente in **Jetpack Compose** con token di design **Samsung One UI 9.0**:
    - Forme a pillola (squircle/stadium) con angoli morbidi e raggi proporzionati.
    - Controlli principali posizionati in basso per l'uso naturale con una sola mano.
    - Animazioni a due vie con molle fluide (`spring`) sia in apertura sia in chiusura di dialog e popup.
    - Pulsanti Indietro standardizzati come cerchi fluttuanti elevati (40.dp) in alto a sinistra.
- **Logica Contatori e Movimenti**:
  - Un capo è contatore quando `isCountable == true` e ha un valore `quantity >= 1`.
  - Negli spostamenti tra armadi/cartelle, elementi con stesso nome e stessi tag collidono fondendosi automaticamente in un unico capo contatore sommando le rispettive quantità.

---

## 6. Cosa NON va toccato (Regole d'oro per evitare rotture)

1. **Non cambiare versioni in `gradle/libs.versions.toml` o `build.gradle.kts`**:
   - `agp` (Android Gradle Plugin), Kotlin e le librerie Compose sono allineate e stabili. Modifiche arbitrarie possono causare incompatibilità di compilazione.
   - Non alzare `compileSdk` oltre il 35.
2. **Non spostare o rinominare le cartelle dei pacchetti**:
   - I file del progetto risiedono in `package com.example.closet.ui.screens`, `com.example.closet.ui.theme` e `com.example.closet.ui.viewmodel`. Spostarli comprometterebbe le importazioni.
3. **Non toccare file di sicurezza o firme** (file keystore o credenziali).
4. **Non toccare o cancellare manualmente la cartella `clothing_images`**: contiene le foto reali salvate dei capi d'abbigliamento.
