package com.example.closet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class ReleaseCategory(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val items: List<String>
)

@Composable
fun ReleaseNotesDialog(
    versionName: String,
    onDismiss: () -> Unit
) {
    val colors = LocalOneUIColors.current

    val releaseCategories = listOf(
        ReleaseCategory(
            title = "Motore Widget Home Rinnovato",
            icon = Icons.Outlined.Widgets,
            iconColor = Color(0xFF29B6F6),
            items = listOf(
                "Movimento istantaneo al tocco, senza ritardi né blocchi.",
                "Posizionamento libero e stabile sulla griglia: nessun salto verso l'alto o compattamento indesiderato.",
                "Maniglia di ridimensionamento ergonomica con area tattile estesa (40dp).",
                "Scorrimento veloce tra le pagine con swipe sull'indicatore e isteresi anti-sfarfallio.",
                "Trasferimento facilitato dei widget da una pagina all'altra."
            )
        ),
        ReleaseCategory(
            title = "Promemoria & Notifiche Intelligenti",
            icon = Icons.Outlined.NotificationsActive,
            iconColor = Color(0xFFFFB74D),
            items = listOf(
                "Nuova gestione promemoria con data, ora e notifiche di sistema puntuali.",
                "Badge numerico e contatore integrati nell'icona campana sull'isola Home.",
                "Visualizzazione integrata dei promemoria nella schermata Calendario (non modificabili come eventi)."
            )
        ),
        ReleaseCategory(
            title = "Calendario & Nuova Gestione Eventi",
            icon = Icons.Outlined.CalendarMonth,
            iconColor = Color(0xFF66BB6A),
            items = listOf(
                "Nuovo menu di aggiunta eventi in formato popup elegante e centrato anziché dal basso.",
                "Visualizzazione rapida di eventi e promemoria con filtri data precisi."
            )
        ),
        ReleaseCategory(
            title = "Strumenti: Timer, Orologio & Livella",
            icon = Icons.Outlined.Construction,
            iconColor = Color(0xFFAB47BC),
            items = listOf(
                "Separazione completa tra Cronometro e Orologio.",
                "Nuovo Timer con selettore del tempo a scorrimento (ore, minuti, secondi) One UI 9.",
                "Riordinamento degli strumenti con animazione fluida identica alla gestione capi.",
                "Nuovo Widget Livella a bolla per la Home (1x1, 1x2, 2x2, 2x4).",
                "Sfumature graduate di dissolvenza in alto e in basso nella schermata strumenti."
            )
        ),
        ReleaseCategory(
            title = "Cambio Rapido Armadio via Swipe",
            icon = Icons.Outlined.SwapHoriz,
            iconColor = Color(0xFF42A5F5),
            items = listOf(
                "Cambio armadio rapido tramite swipe direttamente sul pulsante selettore.",
                "Direzione dello swipe configurabile nelle impostazioni (Verticale o Orizzontale)."
            )
        ),
        ReleaseCategory(
            title = "Rifiniture One UI 9 & Ottimizzazioni",
            icon = Icons.Outlined.AutoAwesome,
            iconColor = Color(0xFFFF7043),
            items = listOf(
                "Interfaccia Impostazioni ripulita senza descrizioni ridondanti.",
                "Nuova schermata Spostamento Capi modernizzata con selezione intuitiva.",
                "Ottimizzazioni prestazionali per fluidità garantita a 60/120 FPS."
            )
        )
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = colors.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Header: Icona + Titolo + Versione
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NewReleases,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Release Notes",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.accent.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.accent.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "v$versionName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "Tutte le novità del main update",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = colors.cardBorder.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Lista delle novità con scroll
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    releaseCategories.forEach { category ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surfaceSecondary.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.cardBorder.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(category.iconColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            tint = category.iconColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = category.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    category.items.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "•",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = category.iconColor,
                                                modifier = Modifier.padding(top = 1.dp)
                                            )
                                            Text(
                                                text = item,
                                                fontSize = 13.sp,
                                                color = colors.textSecondary,
                                                lineHeight = 18.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pulsante di chiusura
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Ho capito, inizia!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
