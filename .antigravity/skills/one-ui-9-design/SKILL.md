---
name: one-ui-9-design
description: Linee guida ingegneristiche e token di design per l'implementazione dell'interfaccia Samsung One UI 9.0 su stack React, Tailwind CSS e Capacitor.
---

# Samsung One UI 9.0 Frontend Specification

## 1. Principi Architetturali e Layout
- Viewing vs Interaction Area:
  - Viewing Area (top 35-40% del viewport): altezza iniziale 280px con titolo principale in `text-[34px] font-bold tracking-tight`. Al verificarsi dello scroll, il contenitore collassa dinamicamente in una toolbar compatta alta 56px con titolo ridotto a `text-[19px] font-semibold` e fino a 3 pulsanti di azione allineati a destra.
  - Interaction Area (bottom 60-65% del viewport): tutti i componenti interattivi devono posizionarsi preferenzialmente nella zona inferiore del display per massimizzare l'accessibilità a una mano.
- Margini e Griglie:
  - Padding orizzontale fisso perimetrale dello schermo: 16px.
  - Nessuna lista continua edge-to-edge per elementi di configurazione: utilizzare esclusivamente blocchi raggruppati ("Focus Blocks") separati dal canvas.

## 2. Token Geometrici (Squircle e Concentricità)
- Focus Blocks (Card principali): `border-radius: 28px` (Tailwind: `rounded-[28px]`).
- Modali, Dialoghi e Bottom Sheet: `border-radius: 34px` (Tailwind: `rounded-[34px]` o `rounded-t-[34px]`).
- Elementi di Input, Search Bar e Bottoni Primari: Pillola completa (`rounded-full`, `border-radius: 9999px`).
- Regola di Concentricità Formale:
  Per qualsiasi elemento figlio annidato con padding `P` rispetto al genitore con raggio `R_out`, il raggio interno deve rispettare:
  R_in = max(0, R_out - P)
  È vietato utilizzare lo stesso raggio di curvatura tra contenitore e contenuto interno.

## 3. Ottica dei Materiali e Frosted Glass (Pipeline One UI 9)
Il vetro traslucido richiede la simultanea applicazione di blur gaussiano, boost di saturazione e bordo speculare:
- Filtro composito: `backdrop-filter: blur(32px) saturate(190%) contrast(102%)`
- Parametri Light Mode:
  - Canvas di sfondo: `#F2F2F7`
  - Focus Block (solido): `#FFFFFF`, ombra `0 1px 3px rgba(0, 0, 0, 0.02)`
  - Superficie Vetro: `rgba(247, 247, 250, 0.72)`
  - Bordo di rifrazione: `1px solid rgba(255, 255, 255, 0.65)`
  - Specular Highlight: `inset 0 1px 0 0 rgba(255, 255, 255, 0.85)`
  - Ombra esterna: `0 8px 32px -4px rgba(0, 0, 0, 0.06)`
- Parametri Dark Mode (AMOLED):
  - Canvas di sfondo: `#000000` (nero assoluto)
  - Focus Block (solido): `#17171A`, nessuna ombra
  - Superficie Vetro: `rgba(28, 28, 32, 0.68)`
  - Bordo di rifrazione: `1px solid rgba(255, 255, 255, 0.09)`
  - Specular Highlight: `inset 0 1px 0 0 rgba(255, 255, 255, 0.12)`
  - Ombra esterna: `0 12px 36px -4px rgba(0, 0, 0, 0.55)`
- Colore d'accento: `#2B66FF` (One UI Blue), attivo `#1A52E6`.

## 4. Dinamica e Micro-interazioni
- Nessun effetto ripple circolare stile Material Design.
- Al tocco (`:active`): compressione geometrica isotropa con `transform: scale(0.975)`.
- Funzione di temporizzazione standard per ogni transizione di stato: `cubic-bezier(0.22, 1, 0.36, 1)`.
- Durata: 150ms per feedback tattile e toggle; 380ms per transizioni di navigazione, espansione header e modali.

## 5. Fonti di Riferimento Tecnico per l'Agente
Se necessiti di estrarre token specifici non coperti dalle regole precedenti (es. token cromatici secondari, icone di sistema, specifiche per tablet/foldable), consulta prioritariamente le seguenti risorse:
1. Portale Ufficiale Samsung Developer:
   - Linee guida e visual hierarchy: https://developer.samsung.com/one-ui/design-guidelines
   - Risorse per form factor ampi: https://developer.samsung.com/one-ui/large-screens
2. Repository AOSP / Android Graphics Core (Fisica del Blur e Shader):
   - Documentazione RenderEffect (deviazione standard sigma = 0.57735 * r + 0.5): https://developer.android.com/reference/android/graphics/RenderEffect
   - Android AGSL Shader Pipeline: https://developer.android.com/develop/ui/views/graphics/agsl
3. Sorgenti Framework Samsung (Reverse Engineering & Token Estratti):
   - Samsung Open Source Release Center: https://opensource.samsung.com (per il recupero dei file XML `framework-res` e `SecSettings` da cui estrarre `dimens.xml` e `colors.xml`).
   - XDA Developers Android Teardown & One UI Analysis: thread tecnici dedicati all'estrazione degli APK di sistema (`SystemUI.apk`, `SecSettings.apk`).
4. Figma Community One UI Kits:
   - Kit vettoriali e continuous curvature references: ricerche con tag "One UI Design System", "Samsung One UI", "One UI 9 UI Kit".
