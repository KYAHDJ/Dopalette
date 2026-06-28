package com.dopalette.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val CreativeGreen = Color(0xFF2E6BFF)
private val CreativeBlue = Color(0xFF2E6BFF)
private val DeepInk = Color(0xFF001233)
private val ModalTop = Color(0xFFFFFFFF)
private val ModalMid = Color(0xFFFFFFFF)
private val ModalBottom = Color(0xFFE9EEF6)
private val ModalBorder = Color(0xFFC4CEDD)
private val TextPrimary = Color(0xFF001233)
private val TextMuted = Color(0xFF53657C)
private val DangerRed = Color(0xFFF17878)

@Composable
private fun PremiumActionDialog(
    eyebrow: String,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    danger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(34.dp),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .background(ModalMid, RoundedCornerShape(34.dp))
                    .border(BorderStroke(1.dp, ModalBorder), RoundedCornerShape(34.dp))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = eyebrow.uppercase(),
                    color = if (danger) DangerRed else CreativeGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = message,
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(2.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (danger) DangerRed else CreativeGreen,
                            contentColor = Color(0xFF001233)
                        )
                    ) {
                        Text(confirmText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ModalBottom,
                            contentColor = TextPrimary
                        )
                    ) {
                        Text(dismissText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FinishedArtworkPreviewDialog(
    artwork: ImageBitmap,
    title: String = "Finished artwork",
    onDismiss: () -> Unit,
    onContinueEditing: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val spin = remember { Animatable(0f) }
    val shine = remember { Animatable(-1.25f) }

    LaunchedEffect(Unit) {
        spin.snapTo(0f)
        shine.snapTo(-1.25f)

        spin.animateTo(
            targetValue = 360f,
            animationSpec = tween(
                durationMillis = 850,
                easing = FastOutSlowInEasing
            )
        )

        shine.animateTo(
            targetValue = 1.25f,
            animationSpec = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(34.dp),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .background(ModalMid, RoundedCornerShape(34.dp))
                    .border(BorderStroke(1.dp, ModalBorder), RoundedCornerShape(34.dp))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ARTWORK DONE",
                    color = CreativeGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, ModalBorder), RoundedCornerShape(26.dp))
                ) {
                    Image(
                        bitmap = artwork,
                        contentDescription = title,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Fit,
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E6BFF),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Download", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onContinueEditing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE3C94A),
                            contentColor = Color(0xFF18212F)
                        )
                    ) {
                        Text("Keep Coloring", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onDelete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE96C6C),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Delete", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ModalBottom,
                            contentColor = TextPrimary
                        )
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ClearCanvasDialog(
    onCancel: () -> Unit,
    onClear: () -> Unit
) {
    PremiumActionDialog(
        eyebrow = "Coloring page",
        title = "Clear your coloring?",
        message = "This removes the colors on this page. Your finished artwork stays safe.",
        confirmText = "Clear",
        dismissText = "Keep Coloring",
        danger = true,
        onDismiss = onCancel,
        onConfirm = onClear
    )
}

@Composable
fun FinishedArtworkDialog(
    onContinueEditing: () -> Unit,
    onFinishArtwork: () -> Unit
) {
    PremiumActionDialog(
        eyebrow = "Artwork done",
        title = "Finish artwork?",
        message = "Save this finished artwork. You can still color more later.",
        confirmText = "Finish",
        dismissText = "Keep Coloring",
        danger = false,
        onDismiss = onContinueEditing,
        onConfirm = onFinishArtwork
    )
}

@Composable
fun MakeNewDraftDialog(
    onCancel: () -> Unit,
    onMakeNew: () -> Unit
) {
    PremiumActionDialog(
        eyebrow = "New coloring",
        title = "Start fresh?",
        message = "This starts a clean coloring page. Your finished artwork stays saved.",
        confirmText = "Start New",
        dismissText = "Cancel",
        danger = false,
        onDismiss = onCancel,
        onConfirm = onMakeNew
    )
}
