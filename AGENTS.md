Sono un principiante e non conosco la programmazione. Il progetto è un'app Android scritta in Kotlin e compilata con Android Studio.

Modalità di lavoro (Orientata all'azione e alla velocità):

1. **Azione diretta e autonoma**: Non fermarti a chiedere conferme prima di iniziare e non aspettare l'approvazione del piano. Procedi direttamente con le modifiche necessarie per completare la richiesta.
2. **Modifiche complete**: Implementa l'intera modifica o funzionalità richiesta in un unico passaggio, senza spezzettarla inutilmente in micro-passaggi.
3. **Stabilità delle dipendenze**: Non aggiornare né modificare le versioni di Gradle, Kotlin, SDK o librerie esterne, a meno che non sia strettamente necessario o richiesto.
4. **Sicurezza dei file**: Non toccare file contenenti chiavi, password o certificati (es. keystore, file di credenziali). Non cancellare file esistenti a meno che non siano stati creati per sbaglio o non più necessari per la modifica.
5. **Verifica ed errori**: Se serve verificare errori di compilazione, prova ad eseguire il check da terminale in autonomia. Se riscontri problemi di compilazione che non puoi risolvere subito da terminale, prosegui e lascia compilare a me da Android Studio.
6. **Incremento versione**: Al termine della modifica, incrementa nel file `build.gradle.kts` sia il `versionCode` (+1) sia il `versionName` (es. da "1.1.01" a "1.1.02").
7. **Main Upgrade / Rilascio Release**: Quando viene richiesto un "main update" o rilascio di versione, compila l'APK release (`.\gradlew.bat assembleRelease --no-daemon`), carica il codice (`git push origin main`) e pubblica in automatico la Release su GitHub con il comando `gh release create v<VERSIONE> app\build\outputs\apk\release\app-release.apk --title "Closet v<VERSIONE>" --notes "..."`.
8. **Riepilogo finale semplice**: A lavoro completato, riassumi cosa hai fatto in italiano semplice, elencando:
   - I file modificati o creati;
   - Cosa devo fare o controllare esattamente nell'app per testare la novità.
