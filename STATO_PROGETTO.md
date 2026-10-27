# Stato del Progetto - Closet Antigravity

Questo documento riassume lo stato attuale dell'applicazione, come Ã¨ organizzata, le modifiche effettuate, le decisioni tecniche prese e le cose importanti da non toccare.

---

## 1. Cosa fa l'app oggi

L'app Ã¨ un assistente personale per la gestione dell'armadio e dei vestiti in stile **Samsung One UI**:
- **Gestione Armadi e Capi**: puoi creare piÃ¹ armadi (es. Principale, Invernale, Estivo), organizzare i capi in cartelle con emoji o copertine, aggiungere foto scattate dalla fotocamera o scelte dalla galleria, impostare tag e conteggiare le quantitÃ .
- **Popup Dettaglio Capo**: toccando un capo nell'armadio si apre una scheda a schermo intero con foto/emoji grande, nome, posizione, tutti i tag associati, contatore interattivo (se presente) e pulsante rapido per la modifica.
- **Home personalizzabile**: una schermata principale con widget a griglia 2D (orologi digitali/analogici, meteo/calendario, scorciatoie rapide, contatore capi, torcia e bussola con sensori reali del telefono).
- **Funzioni Smart**:
  - **Panni / Lavanderia**: gestione dei capi da lavare con timer di lavaggio.
  - **Viaggio / Valigia**: creazione di valigie per i viaggi con promemoria di ritorno capi.
  - **Spostamento Capi**: trasferimento rapido di vestiti tra armadi diversi.
  - **Registro Trasferimenti**: storico cronologico di tutti gli spostamenti effettuati.
  - **Foto Armadio**: panoramica fotografica degli armadi reali per ricordarsi come sono organizzati.
  - **Calendario & Cestino**: pianificazione degli outfit per giorno ed eventi con recupero dal cestino.
  - **Strumenti**: scorciatoie per torcia LED e bussola magnetica.
- **Personalizzazione One UI**: scelta del tema (Scuro, Chiaro, AMOLED nero puro), colore accento personalizzato, barra inferiore ("Isola") e funzioni preferite completamente riordinabili.

---

## 2. Le schermate e i file principali

Tutto il codice si trova nella cartella `app/src/main/java/com/example/closet/`:

### Logica e Dati:
- `ui/viewmodel/ClosetViewModel.kt`: Ã¨ il "cervello" dell'applicazione. Contiene tutte le liste (capi, armadi, cartelle, calendario), le funzioni per aggiungere/modificare elementi e le logiche di salvataggio su memoria.
- `ui/screens/HomeModels.kt`: definisce i modelli di dati condivisi (colori tema One UI, schede di navigazione, tipi di widget e motore di disposizione della griglia 2D).

### Schermate e Componenti:
- **Home**:
  - `HomeScreen.kt`: la schermata Home con la griglia widget e la barra inferiore ("Isola").
  - `HomeClockComponents.kt`: componenti per gli orologi digitali e analogici.
  - `HomeFunctionWidgets.kt`: widget di torcia, bussola e scorciatoie funzioni.
  - `HomeWardrobeWidgets.kt`: widget per contatori capi e anteprime armadio.
  - `HomeWidgetPickerDialog.kt`: finestra per scegliere e aggiungere nuovi widget alla Home.
- **Armadio (Wardrobe)**:
  - `WardrobeScreen.kt`: schermata principale dell'armadio con filtri, cartelle e capi.
  - `WardrobeCards.kt`: schede visive dei vestiti e delle cartelle (griglia ed elenco) con tocco rapido per aprire il dettaglio.
  - `WardrobeItemDialogs.kt`: finestre per aggiungere/modificare capi e cartelle, picker emoji e il nuovo popup di dettaglio singolo capo (`ClothingDetailDialog`).
  - `WardrobeManagementDialogs.kt`: finestre per creare e gestire armadi e tag.
- **Funzioni (Functions)**:
  - `FunctionsScreen.kt`: menu principale delle funzioni avanzate.
  - `ToolsScreen.kt`: torcia e bussola a schermo intero.
  - `TripScreen.kt`: preparazione valigia e gestione viaggio.
  - `MoveClothingScreen.kt`: spostamento capi tra armadi.
  - `TransfersActivityScreen.kt`: storico dei trasferimenti.
  - `ClosetPhotosScreen.kt`: scatto e visualizzazione foto panoramiche degli armadi.
  - `CameraCaptureDialog.kt`: fotocamera integrata con ritaglio e anteprima.
  - `CalendarScreen.kt`: calendario per associare vestiti alle giornate.
- **Impostazioni (Settings)**:
  - `SettingsScreen.kt`: impostazioni tema, backup/ripristino e colori.
  - `SettingsCustomizationDialogs.kt`: finestre per riordinare le schede dell'Isola e i preferiti.

---

## 3. Correzioni e miglioramenti fatti in questa sessione

1. **Risolti gli errori di compilazione iniziali (Problemi 1, 2, 3 e 4)**:
   - Rimossa la dipendenza duplicata della libreria `coil` che bloccava il build.
   - Ripristinato `compileSdk` e `targetSdk` alla versione 35 (compatibile con l'ambiente).
   - Eliminati file inutilizzati e tracce di Firebase/ClosetRepository che creavano conflitti.
2. **Modularizzazione dei file giganti (Problema 5)**:
   - `HomeScreen.kt` Ã¨ passato da 6.675 a circa 2.960 righe, estraendo orologi, widget e dialog in file separati.
   - `WardrobeScreen.kt` Ã¨ passato da 3.705 a 1.060 righe, estraendo card e finestre di dialogo.
   - `FunctionsScreen.kt` Ã¨ passato da 4.029 a 616 righe, separando ciascuna funzione in un file dedicato.
   - `SettingsScreen.kt` Ã¨ passato da 2.705 a 1.250 righe, separando i dialog di personalizzazione.
   - **Risultato**: Android Studio ora compila molto piÃ¹ velocemente e il codice Ã¨ facile da leggere.
3. **Ottimizzazione Prestazioni e Salvataggio Dati (Problema 6 - Opzione B)**:
   - **Foto leggere**: prima le immagini venivano caricate a grandezza originale bloccando lo schermo. Ora vengono scalate in modo intelligente consumando pochissima RAM e senza rallentamenti.
   - **Salvataggi asincroni**: la scrittura su disco avviene in background (`Dispatchers.IO`) senza bloccare l'interfaccia a 120 fps.
   - **Coda sequenziale sicura (`persistScope`)**: i salvataggi vengono eseguiti uno dopo l'altro in ordine, evitando che due modifiche rapide possano accavallarsi o corrompere i dati.
4. **Rinnovamento Gestione Funzioni e Drag-and-Drop Fluido (Problema 7)**:
   - **Risolto il freeze/blocco (ANR) e i salti grafici**: la schermata "Modifica funzioni" ora usa una lista temporanea locale (`workingList`), isolata dal pager principale e dall'Isola, eliminando i ricalcoli continui dell'intera app durante il trascinamento.
   - **Touch fluido e intuitivo**: trascinamento immediato afferrando l'icona con le tre linee (`â‰¡`), oppure tenendo premuta a lungo qualsiasi scheda (long-press). Uno swipe rapido consente invece il normale scorrimento della pagina.
   - **Animazioni fluide a molla (Spring 60 fps)**: le schede adiacenti si spostano dolcemente per fare spazio a quella trascinata, e al rilascio la scheda si adagia con un rimbalzo morbido naturale senza scatti.
   - **Salvataggio atomico**: introdotto `updateFunctionOrder()` in `ClosetViewModel` per registrare l'ordine definitivo solo al termine del riordino o alla pressione di "Salva", integrando anche i moduli suggerimenti outfit e statistiche.
5. **Risoluzione Bug e Riscrittura Drag-and-Drop in Armadio (Wardrobe)**:
   - **Eliminata l'oscillazione e gli scatti (60 fps jump)**: il calcolo degli spostamenti ora usa una formula matematica continua e stabile basata sulla distanza totale percorsa dal tocco, senza sottrarre ripetutamente l'altezza dello slot (che causava un rimbalzo infinito tra due posizioni).
   - **Confinamento e protezione dell'header**: la card trascinata rimane confinata all'area dei capi, e l'intestazione (titolo 'Armadio' e barra 'Ordina: Personalizzato') ha livello prioritario `zIndex(500f)`, impedendo che la card si sovrapponga visivamente ai comandi fissi.
   - **Apertura fluida dello spazio vuoto (gap)**: l'animazione di riposizionamento `Modifier.animateItem()` Ã¨ applicata unicamente agli elementi a riposo. I capi si aprono con animazione a molla fluida e la card trascinata resta ancorata con precisione 1:1 sotto al tocco.
   - **Supporto identico per Lista e Griglia**: la logica Ã¨ pienamente armonizzata sia nella visualizzazione a lista verticale sia nella griglia a 2 colonne.
   - **Salvataggio atomico ordine capi**: introdotto `updateClothingOrder(orderedIds)` in `ClosetViewModel` per memorizzare l'ordine personalizzato in modo immediato e coerente nel database.
6. **Riscrittura Totale Popup Gestione Armadi e Gestione Tag (v1.1.10)**:
   - **Risolto il problema dello scorrimento ai bordi**: sostituito il vecchio motore `LazyColumn` (che provocava salti di posizionamento e sganciava il tocco quando il primo elemento veniva spostato) con un contenitore a scorrimento continuo (`verticalScroll`) e posizionamento assoluto tramite coordinate e molle fluide.
   - **Auto-scroll automatico a 60 fps**: avvicinando un armadio o un tag al bordo superiore o inferiore, l'elenco scorre fluidamente in automatico con velocitÃ  proporzionale alla vicinanza al bordo, mantenendo la riga trascinata perfettamente agganciata al dito.
   - **Doppia presa intuitiva**: trascinamento istantaneo afferrando l'icona con le tre linee (`â‰¡`), oppure tenendo premuta a lungo qualsiasi parte della riga.
   - **Rilascio morbido a molla (Zero-Jump Settling)**: al rilascio, la card scivola dolcemente nel suo nuovo slot evidenziato senza alcun rimbalzo o scatto visivo.
   - **Stile One UI unificato**: aggiunti badge circolare con icona nell'intestazione, barra di aggiunta con focus e scorrimento automatico, contatore capi associati (`"X capi"`) per ciascun armadio/tag, sfumature di dissolvenza ai bordi e transizioni animate durante la rinomina.
7. **Popup Dettaglio Singolo Capo con Animazioni Fluide (v1.1.11)**:
   - **Nuovo Dialog dedicato (`ClothingDetailDialog`)**: cliccando su una card capo (sia in griglia che in elenco, in modalitÃ  normale), si apre un popup One UI dedicato.
   - **Visualizzazione completa ed elegante**: mostra la foto o emoji ad alta definizione in un grande riquadro arrotondato, il nome del capo, i badge di posizione (armadio e cartella) e l'elenco completo di tutti i tag associati a pillola (`FlowRow`).
   - **Supporto contatore interattivo**: per i capi contatore, mostra un controller dedicato con tasti `+` e `-` per regolare la quantitÃ  in tempo reale direttamente dal popup.
   - **Animazione fluida a due vie (Morphing)**: animazione a molla all'apertura e animazione morbida inversa alla chiusura (tramite pulsante "Chiudi", tocco esterno o apertura della modifica) per evitare qualsiasi scatto visivo.
   - **Pulsante Modifica rapida**: permette di passare direttamente alla schermata di modifica del capo.
8. **Didascalia Versione App e Autore Dinamica (v1.2.00)**:
   - **Footer in Impostazioni**: inserita in fondo alla schermata `SettingsScreen.kt` una sezione elegante e discreta con il nome dell'app, la versione e il nome dell'autore (**Riccardo Bettini**).
   - **Lettura automatica**: la versione non Ã¨ scritta "a mano" nel codice Kotlin, ma viene letta dinamicamente a runtime da Android (`packageManager.getPackageInfo`), aggiornandosi da sola ogni volta che viene modificato `versionName` in `build.gradle.kts`.
   - **Regola di progetto 8 in `AGENTS.md`**: aggiunta la regola per incrementare `versionCode` e `versionName` a ogni ciclo di modifica o correzione.

9. **Rimozione Funzioni Placeholder, Morphing Geometrico Isola e Ottimizzazione Totale Prestazioni (v1.2.02)**:
   - **Eliminazione funzioni inutilizzate**: rimosse definitivamente le schede "Statistiche di utilizzo" e "Suggerimenti outfit" da `FunctionsScreen.kt` e dal ciclo di ordinamento/preferenze di `ClosetViewModel.kt`.
   - **Vero morphing geometrico dell'Isola One UI**:
     - Eliminato il doppio contenitore `Surface` annidato all'interno di `OneUIFloatingIsland`, unificando il guscio della pillola in un unico contenitore fisico ad altezza costante 54.dp.
     - Sostituito il vecchio `animateContentSize` con un `SizeTransform` a molla fluida e `contentAlignment = Alignment.Center` su `AnimatedContent`. L'isola ora si espande e contrae geometricamente in modo perfettamente simmetrico e armonioso da capsula a capsula senza scatti, doppi bordi o disallineamenti di ombre.
     - Centrato il morphing del pulsante destro di azione rapida (Stella / Aggiungi / Tavolozza) in modo speculare alla pillola.
   - **Massimizzazione fluidità e prestazioni per dispositivi di ogni fascia**:
     - *Pager swipe zero-overhead*: rimosso il loop `LaunchedEffect` che lanciava decine di coroutine per frame a ogni tocco/scorrimento, sostituito con un calcolo reattivo istantaneo tramite `derivedStateOf`.
     - *Memoizzazione cartelle Wardrobe*: eliminati i filtri iterativi O(N) eseguiti a ogni frame di scorrimento delle griglie e delle liste dell'armadio, precomputando i conteggi e le anteprime una sola volta.
     - *Widget Home ultra-leggeri*: disattivati i listener e le animazioni primaverili continue dei widget quando la Home non è in modalità modifica (`!isEditMode`).
     - *Orologi digitali smart*: aggiornamento al minuto per gli orologi digitali senza secondi per azzerare i risvegli continui della CPU.
     - *Caching immagini Coil ad alte prestazioni*: configurata cache di memoria al 25% della RAM, cache su disco a 64MB e bitmap hardware in `MainActivity.kt`.
     - *Accelerazione hardware*: abilitati `hardwareAccelerated="true"` e `largeHeap="true"` in `AndroidManifest.xml`.

10. **Rinnovamento Schermata Impostazioni One UI (v1.2.03)**:
    - **Pulizia visiva ed eliminazione descrizioni ridondanti**: rimosse le descrizioni superflue sotto i titoli, i toggle e i pulsanti (es. "Personalizza tema, schede...", "Tonalità personalizzata", "Colori distinti nelle Funzioni", "Gestione delle schede", "Modifica funzioni preferite", ripristino e cancellazione dati), rendendo l'interfaccia molto più pulita, ordinata e immediata in pieno stile One UI.
    - **Slider orizzontale a 4 posizioni per il tema (`Theme4PositionSlider`)**:
      - Sostituita la vecchia griglia 2x2 ingombrante con uno slider a segmento orizzontale compatto ad altezza 54.dp con 4 opzioni: *Sistema*, *Chiaro*, *Scuro*, *AMOLED*.
      - Cursore/pillola interna animata a molla (`spring`) reattiva sia al tap diretto sui quadranti sia al trascinamento orizzontale fluido (`detectHorizontalDragGestures`).
    - **Sfumatura in uscita in alto nella schermata**:
      - Aggiunta in cima all'area scrollabile una sfumatura graduale sfumata su 32.dp con gradiente multi-stop che adatta il colore di fondo dinamicamente (tema Chiaro, Scuro o AMOLED puro #000000), permettendo ai contenuti di dissolversi dolcemente sotto l'header fisso durante lo scorrimento.

11. **Rifiniture Modalità Gestione Elementi Armadio (v1.2.04)**:
    - **Titolo dinamico animato della schermata**: quando si entra nella modalità di gestione degli elementi nell'armadio, il titolo in alto transiziona fluidamente da "Armadio" (o nome cartella) a **"Gestione elementi"** con un'animazione verticale sfumata (`AnimatedContent`), tornando al nome originale all'uscita.
    - **Morphing geometrico barra armadio / ordinamento**:
      - La capsula del nome dell'armadio si trasforma nella capsula di gestione ordinamento ("Ordina: ...") attraverso un unico contenitore con animazione geometrica a molla (`SizeTransform`) e interpolazione del colore del bordo (da grigio a colore d'accento). I contenuti interni (icona porta/ordina, testo e freccia) passano da uno stato all'altro senza alcuno scatto visivo.
    - **Tasto Cerca nell'Isola di Gestione**:
      - Inserito il pulsante **"Cerca"** nell'isola di gestione tra "Annulla" e "Fine". Quando viene premuto, fa apparire la consueta barra di ricerca degli elementi dell'armadio, permettendo di filtrare i capi e le cartelle direttamente durante la modalità di gestione. Toccando "Fine" o premendo Indietro si chiude la ricerca e si torna all'isola di gestione.
      - Il tasto "Cerca" supporta il morphing compatto a icona singola quando ci sono elementi selezionati per non sovraffollare l'isola su schermi compatti.

12. **Rinnovamento Sistema Widget e Correzione Aggiunta (v1.2.05)**:
    - **Risoluzione bug aggiunta widget**: corretto il disallineamento reattivo in `FlowWidgetLayout` in cui la lista interna di lavoro `workingWidgets` non riceveva i nuovi elementi aggiunti dal ViewModel a causa di un `LaunchedEffect` che confrontava l'istanza della lista anziché i suoi elementi. Ora qualsiasi nuovo widget aggiunto tramite il selettore compare immediatamente sulla schermata Home.
    - **Nuovo menu selettore widget con sezioni chiudibili**:
      - Suddiviso il catalogo widget in sezioni a fisarmonica ("Armadio & Capi", "Calendario & Impegni", "Funzioni Smart", "Orologio & Strumenti") con icona, conteggio elementi e freccia animata, che si aprono e chiudono con animazione fluida One UI.
    - **Anteprima visiva per ciascun widget**:
      - Ogni elemento nel selettore mostra ora una vera anteprima in miniatura scalata (`WidgetScaledPreview`) che riproduce fedelmente l'aspetto grafico, le proporzioni, i colori e i dati del widget reale prima di aggiungerlo.
    - **Correzione testo dimensione nel ridimensionamento**:
      - Sostituito il carattere corrotto con il moltiplicatore Unicode pulito (`\u00D7`), mostrando correttamente badge come "2 × 2", "1 × 2", "2 × 4" durante il ridimensionamento.
    - **Pulsante di rimozione widget rosso**:
      - Il pulsante circolare con segno meno (-) per rimuovere i widget in modalità modifica è ora di colore rosso brillante (`#E55353`) con icona bianca e bordo a contrasto per una visibilità immediata.

13. **Rifinitura Icone Isola Gestione Armadio (v1.2.06)**:
    - **Tasto Cerca evidenziato solo icona**: il pulsante di ricerca nell'isola di gestione degli elementi dell'armadio è stato reso a icona singola circolare (38.dp) con sfondo colorato d'accento soft e bordo abbinato, eliminando l'etichetta testuale per un layout più minimale e ordinato.
    - **Pulsanti Sposta ed Elimina solo icona**: quando si selezionano uno o più elementi, i pulsanti contestuali "Sposta" ed "Elimina" ora compaiono come comode icone circolari (38.dp) senza scritte testuali ingombranti, creando una fila perfettamente bilanciata e simmetrica di icone a cerchio nell'isola.

14. **Ricostruzione da Zero del Menu Aggiunta Widget One UI 9.0 (v1.2.07)**:
    - **Rinnovamento estetico e strutturale completo**:
      - *Header fisso One UI*: grande titolo "Aggiungi widget" a contrasto con sottotitolo esplicativo e pulsante di chiusura circolare.
      - *Barra di ricerca rapida istantanea*: permette di digitare per filtrare immediatamente i widget in tempo reale con conteggio dei risultati e tasto per cancellare il testo.
      - *Filtro a pillole orizzontali*: chip a capsula scorrevoli ("Tutti", "Armadio", "Calendario", "Funzioni", "Orologio & Strumenti") per selezionare la categoria desiderata.
      - *Sezioni chiudibili animate*: schede a fisarmonica con badge del conteggio widget, icone tematiche in cerchi colorati e freccia animata a molla.
      - *Card con anteprime grafiche reali*: ogni widget è racchiuso in una card arrotondata con box di anteprima dedicato che renderizza in scala l'esatto aspetto visivo e funzionale del widget prima di aggiungerlo.
      - *Sfumature superiore e inferiore in uscita*: i contenuti scivolano con dissolvenza graduale sotto la barra superiore fissa e in fondo alla schermata, in perfetta armonia con il resto dell'app.

15. **Ricostruzione del Menu Popup dei Preferiti (v1.2.08)**:
    - **Geometria e raggio di curvatura unificati**:
      - Il contenitore del menu popup ha ora lo stesso raggio di curvatura del tasto stella e dell'isola generale (`27.dp`).
      - Ciascuna colonna ha una larghezza fissa e perfetta di `54.dp` (con celle quadrate 54x54 dp e pulsanti circolari centrati da 42.dp).
      - Nel caso di colonna singola, il popup ha una larghezza totale di esattamente `54.dp`, risultando in una pillola stadium (o cerchio per singolo elemento) di dimensione e curvatura identiche al tasto stella sottostante.
    - **Allineamento millimetrico permanente della colonna più a destra**:
      - Eliminati offset o scostamenti empirici: grazie alla larghezza colonna a 54.dp e all'allineamento all'estremità destra (`Alignment.BottomEnd`), la colonna più a destra del menu dei preferiti si trova sempre in asse perfetto con il tasto stella sottostante, sia a 1 colonna sia a 2, 3 o 4 colonne (le colonne aggiuntive si espandono ordinatamente verso sinistra).
    - **Nuova animazione fluida organica dal tasto stella**:
      - Il centro orizzontale dell'animazione (`TransformOrigin`) viene calcolato dinamicamente sulla colonna di destra in corrispondenza del centro esatto del tasto stella (`favoritePivotX`).
      - L'animazione a molla con `scaleIn` e `scaleOut` fa nascere ed espandere il menu direttamente dal tasto stella, accompagnata da una rotazione a molla (72°) e un leggero pop di scala sull'icona della stella.
      - Quando si tocca "Configura funzioni preferite", si apre ora direttamente il dialog editor delle funzioni preferite.

16. **Semplificazione Menu Aggiunta Widget (v1.2.09 - v1.2.10)**:
    - **Rimozione barra di ricerca e filtri categorie**: rimossa la casella di ricerca e la riga orizzontale delle chip di categoria dal menu di aggiunta dei widget.
    - **Rimozione descrizione intestazione**: eliminato il sottotitolo esplicativo sotto "Aggiungi widget" per dare massimo respiro e immediatezza al titolo e ai contenuti.
    - **Interfaccia pulita e minimale**: la schermata presenta direttamente l'elenco ordinato e scorrevole delle 4 sezioni tematiche chiudibili ("Armadio & Capi", "Calendario & Impegni", "Funzioni Smart", "Orologio & Strumenti") con le rispettive card e anteprime grafiche in scala.

17. **Restyling Card Widget e Ricreazione Tasto "Aggiungi" (v1.2.11)**:
    - **Tasto "Aggiungi" a pillola One UI 9.0**: sostituito il vistoso cerchio blu pieno con un elegante pulsante a pillola (stadium) con bordo sottile, sfondo soft colorato d'accento e testo "+ Aggiungi", armonizzato con lo stile One UI.
    - **Risoluzione troncamento titoli**: riorganizzato il layout interno della card disponendo il titolo a larghezza intera in alto, così che nessun titolo ("Conteggio Mini", "Conteggio Capi", ecc.) venga più troncato con puntini di sospensione.
    - **Dimensioni anteprima ottimizzate**: riquadro di anteprima proporzionato (86x58 dp) per garantire massima leggibilità sia al widget in miniatura sia al testo e alle dimensioni.
    - **Correzione grammaticale badge**: corretto il testo delle dimensioni in "1 cella" (singolare) e "N celle" (plurale).

18. **Titolo Scorrevole e Tasto Indietro Fluttuante in Impostazioni (v1.2.12)**:
    - **Titolo "Impostazioni" scorrevole**: il titolo principale non è più bloccato in un header rigido, ma scorre naturalmente insieme a tutte le sezioni e impostazioni della schermata, dissolvendosi dolcemente sotto la sfumatura superiore al raggiungimento della cima.
    - **Tasto Indietro fluttuante e fisso**: il pulsante indietro è posizionato come elemento fluttuante (`Surface` circolare elevata con `zIndex = 20f`, bordo soft e ombra leggera) fisso nell'angolo superiore sinistro dello schermo (`start = 20.dp, top = 18.dp`). Rimane sempre comodamente visibile e cliccabile da qualsiasi livello di scorrimento, senza mai essere nascosto o muoversi con la pagina.
    - **Allineamento armonico**: a scorrimento zero, il titolo è perfettamente allineato al centro verticale del tasto indietro con spaziatura precisa, integrandosi fedelmente con il design One UI.

19. **Mini Bussola Grafica Reale nei Widget 1x1 e 1x2 (v1.2.13)**:
    - **Widget 1x1 trasformato in vera mini bussola (`CompassDialMini`)**: sostituito il semplice blocco di testo statico con un vero quadrante grafico circolare analogico/digitale ad alta precisione. Include ghiera concentrica, traccia graduata interna, tacche perimetrali ogni 30° con punti cardinali evidenziati, indicatore Nord triangolare a 0°, tacca fissa a ore 12 in colore d'accento che mostra la direzione frontale del telefono, e un ago magnetico sfaccettato 3D a doppia punta (rosso vivido/scuro a Nord, argento/ardesia a Sud) che ruota fluidamente per puntare al Nord fisico in tempo reale.
    - **Hub centrale digitale nel widget 1x1**: al centro della mini bussola, un mozzo circolare fisso e sempre perfettamente orientato mostra la direzione cardinale (es. "N", "NE", "O") e l'orientamento in gradi precisi ("0°", "142°").
    - **Aggiornamento armonizzato del widget 1x2**: sostituita l'icona generica con il medesimo quadrante grafico di precisione da 54.dp (con ago a vista intera e perno a rubino), affiancato sulla destra dalla lettura in caratteri grandi dei gradi, badge cardinale e nome esteso della direzione in italiano ("Sud-Est", "Nord-Ovest").

20. **Aggiornamento Reattivo Colori Widget Scorciatoie Funzioni (v1.2.14)**:
    - **Reattività immediata al toggle Impostazioni**: risolto il problema di mancato aggiornamento visivo nei widget scorciatoie della Home quando si attiva o disattiva l'opzione "Colori distinti nelle Funzioni".
    - **Propagazione esplicita dello stato**: passato lo stato `useDistinctColors` attraverso `HomeDashboardGrid`, `FlowWidgetLayout` e `WidgetCardContainer`, agganciando `AnimatedContent` alla coppia `Pair(widget.type, useDistinctColors)`. Questo assicura la ricomposizione reattiva immediata con transizione fluida su tutti i widget scorciatoia presenti nella Home.
    - **Sincronizzazione di tutti i formati**: allineati tutti i widget funzione (scorciatoie 1x1 di Calendario, Strumenti, Foto, Spostamento, Panni, Viaggio, Registro; pannelli scorciatoie rapide 1x4 e 2x2; widget 1x2, 2x2 e 2x4 di Foto, Spostamento, Panni, Viaggio e Trasferimenti) affinché usino istantaneamente i rispettivi colori distinti quando l'opzione è attiva, oppure il colore d'accento dell'app quando è disattivata.
    - **Allineamento anteprime e schermata Funzioni**: sincronizzate le anteprime nel selettore widget (`HomeWidgetPickerDialog`) e armonizzato il colore della voce "Foto armadio" (`Color(0xFF53B5A2)`) in `FunctionsScreen`.

21. **Sezioni Chiuse come Preimpostazione nel Menu Aggiungi Widget (v1.2.15)**:
    - **Stato iniziale compatto**: impostato `emptySet()` come valore predefinito per le categorie espanse nel menu di selezione widget (`HomeWidgetPickerDialog`).
    - **Vista d'insieme pulita**: all'apertura del dialog tutte le 4 sezioni tematiche ("Armadio & Capi", "Calendario & Impegni", "Funzioni Smart", "Orologio & Strumenti") si presentano chiuse e ordinate, permettendo all'utente di avere un colpo d'occhio immediato e aprire comodamente solo la sezione d'interesse toccandola.

22. **Design Minimale e Pulito per il Widget Bussola 1x1 (v1.2.16)**:
    - **Rimozione dei gradi numerici al centro**: eliminata la seconda riga numerica con i gradi ("0°", "142°") nel mozzo centrale del widget 1x1 della bussola.
    - **Mozzo centrale proporzionato e direzione cardinale in risalto**: ridotte le dimensioni del disco centrale per dare massimo respiro e visibilità alle tacche del quadrante e all'ago 3D in rotazione, con la sola sigla cardinale ("N", "NE", "E", ecc.) mostrata a caratteri nitidi in grassetto al centro.

23. **Armonizzazione e Uniformità di tutti i Widget 1x1 (Base Orologio Analogico) (v1.2.17)**:
    - **Geometria condivisa unificata**: standardizzati tutti i widget in formato 1x1 adottando come modello di riferimento l'orologio analogico (`ClockAnalogWidget1x1`).
    - **Margine e disco interno uniforme**: ogni widget 1x1 presenta ora lo stesso padding perimetrale di `10.dp` e lo stesso disco circolare interno con sfondo `colors.surfaceSecondary`, garantendo identico diametro ed elevazione visiva per tutti.
    - **Uniformazione dei contenuti**:
      - *Bussola 1x1*: allineato il padding a `10.dp` e rimosso il doppio bordo esterno per combaciare perfettamente con il disco dell'orologio.
      - *Torcia 1x1*: sostituito il cerchietto statico da 44.dp con il disco standard `surfaceSecondary` che occupa l'intero spazio e si illumina di giallo ambra solo quando accesa.
      - *Aggiungi Capo 1x1*: sostituito il cerchio blu pieno con il disco circolare sobrio ed elegante `surfaceSecondary` e icona `+` in colore d'accento.
      - *Cerca, Nuovo Evento e Scorciatoie Funzioni 1x1*: uniforme disco interno `surfaceSecondary` con icone centrate da 24.dp.
      - *Conteggio Capi e Calendario Oggi 1x1*: integrati all'interno del disco circolare `surfaceSecondary` con tipografia bilanciata e leggibile.

24. **Pulsante Indietro Elevato e Fluttuante Universale One UI (v1.2.18)**:
    - **Uniformazione di tutti i tasti Indietro dell'app**: esteso il design moderno, fluttuante ed elevato introdotto nelle Impostazioni a tutte le schermate secondarie dell'app.
    - **Geometria ed Elevazione One UI 9.0**:
      - Pulsante circolare da `40.dp` in `Surface(shape = CircleShape, color = colors.surfaceSecondary, border = BorderStroke(0.5.dp, colors.cardBorder), tonalElevation = 4.dp, shadowElevation = 6.dp)`.
      - Posizionato in alto a sinistra in modo fisso e fluttuante (`Alignment.TopStart`, `start = 20.dp, top = 18.dp`, `zIndex = 20f`), rimanendo sempre visibile e accessibile anche durante lo scorrimento dei contenuti sottostanti.
      - Icona con freccia `ArrowBack` da `20.dp`, con colorazione coerente basata sul colore d'accento o sull'opzione "Colori distinti delle funzioni".
    - **Schermate aggiornate**:
      - *Impostazioni (`SettingsScreen.kt`)*: titolo che scorre con la pagina e tasto indietro fisso fluttuante.
      - *Strumenti (`ToolsScreen.kt`)*: tasto indietro fluttuante elevato con accento blu/strumenti.
      - *Attività e Storico Trasferimenti (`TransfersActivityScreen.kt`)*: tasto indietro fluttuante elevato con accento arancione/trasferimenti.
      - *Viaggio / Valigia (`TripScreen.kt`)*: tasto indietro fluttuante elevato con accento verde smeraldo/viaggio e salvaguardia della conferma di uscita.
      - *Spostamento Capi e Panni (`MoveClothingScreen.kt`)*: tasto indietro fluttuante elevato con gestione chiusura cartelle e dialogo di uscita.
      - *Foto Armadio (`ClosetPhotosScreen.kt`)*: tasto indietro fluttuante elevato con accento teal/foto.
      - *Calendario (`CalendarScreen.kt`)*: pulsante indietro aggiornato con Surface elevata da `40.dp` e tonal elevation.
      - *Cartelle Armadio (`WardrobeScreen.kt`)*: tasto di uscita dalle cartelle (sia in visualizzazione a griglia sia a elenco) elevato con Surface circolare rifinita.

25. **Ottimizzazione e Compattezza Scheda Contatore nel Dettaglio Capo (v1.2.19)**:
    - **Rimozione della descrizione superflua**: eliminato il testo "Elemento con contatore" all'interno della scheda del contatore nel popup `ClothingDetailDialog`.
    - **Riduzione dell'ingombro verticale**:
      - Layout a riga singola pulita per "Quantità disponibile" (`13.5.sp`, grassetto) affiancato direttamente ai tasti stepper.
      - Pulsanti `+` e `-` ridimensionati da `36.dp` a `28.dp` con spaziatura ridotta a `8.dp`, e valore numerico da `16.sp`.
      - Spaziatura e margini interni ottimizzati (`vertical = 7.dp`, raggio curvatura `14.dp`), riducendo l'altezza complessiva del blocco da oltre 64.dp a meno di 40.dp.
      - Ottimizzazione complessiva dei margini e dell'altezza massima del dialog (`700.dp`) per impedire che i pulsanti inferiori "Chiudi" e "Modifica" vengano tagliati o finiscano oltre il bordo dello schermo.

26. **Posizionamento Contatore in Basso a Destra nell'Anteprima Capo (v1.2.20)**:
    - **Spostamento pillola contatore**: nella scheda capo a griglia dell'armadio (`ClothingGridCard`), il badge interattivo del contatore (`[- quantità +]`) è stato spostato dall'angolo in basso a sinistra (`Alignment.BottomStart`) all'angolo in basso a destra (`Alignment.BottomEnd`) dell'immagine di anteprima.
    - **Armonia visiva**: l'angolo in basso a destra offre una posizione più naturale e comoda per il tocco con una sola mano, lasciando pulito il lato sinistro.

27. **Badge Quantità in Modalità Gestione Elementi (v1.2.21)**:
    - **Pallino quantità contatore**: entrando in modalità di gestione elementi nell'Armadio (`isEditMode`), per tutti i capi contatore compare un badge circolare animato nell'angolo in alto a destra (`Alignment.TopEnd`) della foto di anteprima.
    - **Design One UI**:
      - Sfondo circolare nero traslucido (`Color.Black.copy(alpha = 0.72f)`), bordo sottile bianco sfumato (`0.8.dp`/`1.dp`) e numero in evidenza con il colore d'accento del tema.
      - Animazione a molla fluida sia in comparsa che in scomparsa (`scaleIn`/`scaleOut` con rimbalzo morbido One UI).
      - Integrato sia nella visualizzazione a griglia (`ClothingGridCard`) sia nella visualizzazione a elenco (`ClothingListCard`).

28. **Selezione Quantità Esatta al Posto di "Modifica" per Capi Contatore (v1.2.22)**:
    - **Sostituzione dinamica del pulsante Modifica**: in modalità gestione elementi, quando un capo contatore viene selezionato (checkbox attivo), al posto del pulsante "Modifica" compare la pillola stepper interattiva `[- quantità +]`.
    - **Selezione pezzi parziali o totali**: l'utente può scegliere il numero esatto di elementi da spostare (da 1 fino alla disponibilità massima dell'elemento, es. "1/3", "2/3" o "3"). Se deselezionato, il pulsante ritorna fluidamente a essere "Modifica".
    - **Spostamento intelligente parziale**:
      - Aggiornato `moveItemsToTarget` in `ClosetViewModel.kt` per supportare il dizionario delle quantità selezionate (`itemQuantities`).
      - Quando viene spostata una quantità parziale, i pezzi rimanenti rimangono nell'armadio/cartella di origine, mentre la quota selezionata viene trasferita nella destinazione e consolidata con eventuali capi omologhi già presenti.
      - La finestra di dialogo di spostamento mostra il conteggio totale reale dei pezzi selezionati.

29. **Fusione Automatica Capi Omonimi con Stessi Tag in Stile Contatore (v1.2.23)**:
    - **Consolidamento universale tra armadi e cartelle**: quando un capo (contatore o singolo) viene spostato in un armadio o cartella dove è già presente un altro capo (contatore o singolo) con lo stesso nome e gli stessi tag, i due elementi si fondono automaticamente in un unico elemento in stile contatore (`isCountable = true`).
    - **Somma delle quantità**: la quantità finale del capo unificato è pari alla somma delle quantità dei singoli capi spostati o preesistenti (es. 1 singolo + 1 singolo = contatore da 2; 1 singolo + 3 contatore = contatore da 4).
    - **Salvaguardia anteprime e dettagli**: preservata l'immagine fotografica o l'emoji del capo e rimossi i duplicati ridondanti.

---

## 4. Problemi ancora aperti o cose da fare in futuro

Al momento l'app è alla versione **1.2.23** (build **71**), non ha errori bloccanti, compila ed è stata testata con successo:
- **Gestione Funzioni, Armadio, Gestione Armadi e Gestione Tag**: tutti i sistemi di trascinamento, riordino e scorrimento sono completati, fluidi e coerenti in tutte le parti dell'app.
- **Test su dispositivo reale**: verificare l'uso effettivo della torcia e della bussola su diversi modelli di telefoni Android (alcuni dispositivi non hanno il sensore magnetico o il flash).
- **Backup esterno automatico**: attualmente il backup Ã¨ manuale tramite file JSON/ZIP esportabile dalle impostazioni. In futuro si potrebbe integrare il backup su Google Drive o archiviazione cloud se richiesto.
- **Passaggio a Room Database (eventuale)**: se la collezione dovesse superare le svariate migliaia di capi, si potrÃ  valutare il passaggio a Room (SQLite), anche se con la soluzione attuale l'app regge senza problemi centinaia di capi in modo istantaneo.

---

## 5. Scelte di design decise

- **Come sono salvati i dati**:
  - I dati testuali (capi, armadi, tag, cartelle, calendario, widget) sono salvati come JSON formattato all'interno delle `SharedPreferences` protette dell'app.
  - Le foto scattate o scelte per i capi sono salvate compresse in formato JPEG nella cartella privata `clothing_images` dentro la memoria interna dell'applicazione.
- **Interfaccia Grafica**:
  - Costruita interamente con **Jetpack Compose** e **Material 3**.
  - Segue i canoni estetici di **Samsung One UI 9.0**: angoli molto arrotondati (forme a pillola/squircle), dialog con effetto morphing, testo grande nelle intestazioni e controlli posizionati comodamente verso il basso dello schermo.
  - **Animazioni a due vie**: i popup moderni dell'app (come il dettaglio capo) gestiscono animazioni a molla sia in apertura sia in chiusura graduale (senza sparire di scatto alla pressione di Annulla, Chiudi o tocco esterno).

---

## 6. Cosa NON va toccato (Regole d'oro per evitare rotture)

1. **Non cambiare versioni in `gradle/libs.versions.toml` o `build.gradle.kts`**:
   - `agp` (Android Gradle Plugin) e `kotlin` sono attualmente allineati. Modificare versioni di Kotlin, KSP o Gradle senza necessitÃ  puÃ² rompere la compilazione.
   - Non alzare `compileSdk` oltre il 35 a meno che l'ambiente di Android Studio non sia aggiornato.
2. **Non spostare o rinominare le cartelle dei pacchetti**:
   - I file fanno tutti parte di `package com.example.closet.ui.screens` e `com.example.closet.ui.viewmodel`. Spostarli creerebbe errori di importazione.
3. **Non toccare i file con chiavi o firme di rilascio** (keystore o simili).
4. **Non modificare la cartella `clothing_images`**: contiene i file fisici delle foto associate ai vestiti. Se rimossi manualmente da codice, i capi perderebbero le loro immagini.
