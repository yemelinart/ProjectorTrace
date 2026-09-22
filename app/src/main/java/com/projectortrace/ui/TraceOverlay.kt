package com.projectortrace.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectortrace.R
import com.projectortrace.model.CanvasFramePreset
import com.projectortrace.model.CropCorner
import com.projectortrace.model.GuideCornerColor
import com.projectortrace.model.GuideShapeColor
import com.projectortrace.model.GridColorMode
import com.projectortrace.model.ImageBlendMode
import com.projectortrace.model.OverlayBlendMode
import com.projectortrace.model.PalettePosition
import com.projectortrace.model.ProjectionBlankMode
import com.projectortrace.model.ProjectorOrientation
import com.projectortrace.model.TraceMode
import com.projectortrace.model.TransformState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class TraceMenuPanel {
    Main,
    Image,
    Transform,
    TransformRotate,
    Flip,
    TransformCornerPin,
    Guides,
    GuideColorPalette,
    GuideGrid,
    GuideRulers,
    GuideCanvasFrame,
    GuideCrop,
    GuideShapes,
    GuideCorners,
    GuideOverlays,
    Filter,
    FilterPaint,
    FilterThreshold,
    FilterChannelMixer,
    FilterPosterize,
    FilterBlending,
    FilterBrightness,
    FilterContrast,
    FilterDetailisation,
    FilterLines,
    FilterInvert,
    FilterVolume,
    FilterMagicOutline,
    FilterEdgeOutline,
    SettingsBlink,
    Profiles,
    Info,
    Settings,
    About,
    Privacy,
}

internal enum class TraceMenuSize {
    Slim,
    Normal,
    Large;

    fun next(): TraceMenuSize = when (this) {
        Slim -> Normal
        Normal -> Large
        Large -> Slim
    }
}

internal enum class TraceHudDuration(val millis: Long, private val displayName: String) {
    TwoSeconds(2_000L, "2 sec"),
    FiveSeconds(5_000L, "5 sec"),
    TenSeconds(10_000L, "10 sec");

    fun next(): TraceHudDuration = when (this) {
        TwoSeconds -> FiveSeconds
        FiveSeconds -> TenSeconds
        TenSeconds -> TwoSeconds
    }

    fun label(): String = displayName
}

internal enum class TraceMenuAutoHide(val millis: Long?, private val displayName: String) {
    TwoSeconds(2_000L, "2 sec"),
    FiveSeconds(5_000L, "5 sec"),
    TenSeconds(10_000L, "10 sec"),
    Always(null, "Always");

    fun next(): TraceMenuAutoHide = when (this) {
        TwoSeconds -> FiveSeconds
        FiveSeconds -> TenSeconds
        TenSeconds -> Always
        Always -> TwoSeconds
    }

    fun label(): String = displayName
}

internal enum class TraceMenuAction {
    OpenImage,
    OpenLibrary,
    SaveProfile,
    LoadProfile,
    ClearProfile,
    CloseImage,
    ResetImage,
    ResetTransform,
    ResetGuides,
    ToggleLock,
    QuitApp,
    ToggleFlipHorizontal,
    ToggleFlipVertical,
    ResetCornerPin,
    ResetFilters,
    ResetAll,
    ToggleGrid,
    ToggleRulers,
    ToggleCenterCross,
    ToggleCanvasFrame,
    ToggleCrop,
    ResetCrop,
    ToggleCorners,
    ToggleShapes,
    ToggleColorPalette,
    CycleGridColor,
    OpenOverlay,
    ToggleOverlay,
    ToggleThreshold,
    TogglePaint,
    TogglePosterize,
    ToggleLines,
    ToggleInvert,
    ToggleVolume,
    ToggleMagicOutline,
    ToggleEdgeOutline,
    ToggleBrightness,
    ToggleProjectionBlank,
    ToggleKeepScreenAwake,
    CycleMenuSize,
    CycleHudDuration,
    CycleMenuAutoHide,
    CycleRotateStep,
    CycleProjectionBlankMode,
    CycleProjectorOrientation,
    Back,
    Placeholder,
}

internal data class TraceMenuEntry(
    val title: String,
    val icon: String,
    val detail: String? = null,
    val opensPanel: TraceMenuPanel? = null,
    val mode: TraceMode? = null,
    val action: TraceMenuAction? = null,
)

internal fun traceMenuEntries(
    panel: TraceMenuPanel,
    transform: TransformState,
    hasImage: Boolean,
    imageLabel: String?,
    imageSizeLabel: String?,
    hasSavedProfile: Boolean,
    menuSize: TraceMenuSize,
    hudDuration: TraceHudDuration,
    menuAutoHide: TraceMenuAutoHide,
    projectionBlankEnabled: Boolean,
    projectionBlankMode: ProjectionBlankMode,
    keepScreenAwake: Boolean,
    projectorOrientation: ProjectorOrientation,
): List<TraceMenuEntry> = when (panel) {
    TraceMenuPanel.Main -> listOf(
        TraceMenuEntry("Image", "IMG", opensPanel = TraceMenuPanel.Image),
        TraceMenuEntry("Transform", "TRN", opensPanel = TraceMenuPanel.Transform),
        TraceMenuEntry("Guides", "GDE", opensPanel = TraceMenuPanel.Guides),
        TraceMenuEntry("Filter", "FLT", opensPanel = TraceMenuPanel.Filter),
        TraceMenuEntry("Info", "INF", opensPanel = TraceMenuPanel.Info),
        TraceMenuEntry("Settings", "SET", opensPanel = TraceMenuPanel.Settings),
        TraceMenuEntry("Quit", "PWR", action = TraceMenuAction.QuitApp),
    )
    TraceMenuPanel.Image -> listOf(
        TraceMenuEntry("Gallery", "GAL", action = TraceMenuAction.OpenLibrary),
        TraceMenuEntry("Browse Files", "FIL", action = TraceMenuAction.OpenImage),
        TraceMenuEntry("Profiles", "PRE", detail = if (hasSavedProfile) "Saved" else null, opensPanel = TraceMenuPanel.Profiles),
        TraceMenuEntry("Close", "CLS", action = TraceMenuAction.CloseImage),
        TraceMenuEntry("Reset", "RST", action = TraceMenuAction.ResetImage),
        TraceMenuEntry("Lock", "LCK", detail = if (transform.isLocked) "On" else null, action = TraceMenuAction.ToggleLock),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.Profiles -> listOf(
        TraceMenuEntry("Save Profile", "PRE", detail = "Current setup", action = TraceMenuAction.SaveProfile),
        TraceMenuEntry("Load Profile", "PRE", detail = if (hasSavedProfile) "Apply saved setup" else "No saved profile", action = TraceMenuAction.LoadProfile),
        TraceMenuEntry("Clear Profile", "RST", detail = if (hasSavedProfile) "Remove saved setup" else "Nothing saved", action = TraceMenuAction.ClearProfile),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.Transform -> listOf(
        TraceMenuEntry("Move", "MOV", detail = transform.moveDetailOrNull(), mode = TraceMode.Move),
        TraceMenuEntry("Zoom", "ZOM", detail = transform.zoomDetailOrNull(), mode = TraceMode.Zoom),
        TraceMenuEntry("Rotate", "ROT", detail = transform.rotateMenuDetail(), opensPanel = TraceMenuPanel.TransformRotate),
        TraceMenuEntry("Fit", "FIT", detail = transform.fitDetailOrNull(), mode = TraceMode.Fit),
        TraceMenuEntry("Distort", "DST", detail = transform.cornerPinDetailOrNull(), opensPanel = TraceMenuPanel.TransformCornerPin),
        TraceMenuEntry("Flip", "FLP", detail = transform.flipDetailOrNull(), opensPanel = TraceMenuPanel.Flip),
        TraceMenuEntry("Reset Transform", "RST", action = TraceMenuAction.ResetTransform),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.TransformCornerPin -> listOf(
        TraceMenuEntry("Adjust Distort", "DST", detail = "${transform.cornerPinSelectedCornerLabel()} ${transform.cornerPinSelectedOffsetLabel()}", mode = TraceMode.Distort),
        TraceMenuEntry("Reset Distort", "RST", detail = transform.cornerPinDetailOrNull(), action = TraceMenuAction.ResetCornerPin),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.TransformRotate -> listOf(
        TraceMenuEntry("Rotate", "ROT", detail = transform.rotationLabel(), mode = TraceMode.Rotate),
        TraceMenuEntry("Rotate Step", "ROT", detail = transform.rotationStepLabel(), action = TraceMenuAction.CycleRotateStep),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.Flip -> listOf(
        TraceMenuEntry("Horizontal", "FLH", detail = if (transform.isFlippedHorizontal) "On" else "Off", action = TraceMenuAction.ToggleFlipHorizontal),
        TraceMenuEntry("Vertical", "FLV", detail = if (transform.isFlippedVertical) "On" else "Off", action = TraceMenuAction.ToggleFlipVertical),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.Guides -> listOf(
        TraceMenuEntry("Color Palette", "PAL", detail = transform.paletteDetailOrNull(), opensPanel = TraceMenuPanel.GuideColorPalette),
        TraceMenuEntry("Grid", "GRD", detail = transform.gridDetailOrNull(), opensPanel = TraceMenuPanel.GuideGrid),
        TraceMenuEntry("Center Cross / Rulers", "CRS", detail = transform.rulersDetailOrNull(), opensPanel = TraceMenuPanel.GuideRulers),
        TraceMenuEntry("Canvas Frame", "FRM", detail = transform.canvasFrameDetailOrNull(), opensPanel = TraceMenuPanel.GuideCanvasFrame),
        TraceMenuEntry("Crop Mode", "CRP", detail = transform.cropDetailOrNull(), opensPanel = TraceMenuPanel.GuideCrop),
        TraceMenuEntry("Shapes", "SHP", detail = transform.shapeDetailOrNull(), opensPanel = TraceMenuPanel.GuideShapes),
        TraceMenuEntry("Corners", "CRN", detail = if (transform.isGuideFrameVisible) "On" else "Off", opensPanel = TraceMenuPanel.GuideCorners),
        TraceMenuEntry("Reset Guides", "RST", action = TraceMenuAction.ResetGuides),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.GuideColorPalette -> listOf(
        TraceMenuEntry("Color Palette", "PAL", detail = if (transform.colorPaletteCount > 0) "On" else "Off", action = TraceMenuAction.ToggleColorPalette),
        TraceMenuEntry("Palette Colors", "PAL", detail = transform.colorPaletteLabel(), mode = TraceMode.ColorPalette),
        TraceMenuEntry("Palette Mode", "PAL", detail = transform.colorPaletteModeLabel(), mode = TraceMode.PaletteMode),
        TraceMenuEntry("Move Palette", "PAL", detail = transform.colorPaletteOffsetLabel(), mode = TraceMode.PaletteMove),
        TraceMenuEntry("Palette Scale", "PAL", detail = transform.colorPaletteScaleLabel(), mode = TraceMode.PaletteScale),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.GuideGrid -> listOf(
        TraceMenuEntry("Grid", "GRD", detail = if (transform.isGridVisible) "On" else "Off", action = TraceMenuAction.ToggleGrid),
        TraceMenuEntry("Grid Size", "GRD", detail = "${transform.gridSpacingPx.roundToInt()} px", mode = TraceMode.Frame),
        TraceMenuEntry("Grid Color", "GRD", detail = transform.gridColorLabel(), action = TraceMenuAction.CycleGridColor),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.GuideRulers -> listOf(
        TraceMenuEntry("Rulers", "CRS", detail = if (transform.isRulersVisible) "On" else "Off", action = TraceMenuAction.ToggleRulers),
        TraceMenuEntry("Center Cross", "CRS", detail = if (transform.isCenterCrossVisible) "On" else "Off", action = TraceMenuAction.ToggleCenterCross),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.GuideCanvasFrame -> listOf(
        TraceMenuEntry("Canvas Frame", "FRM", detail = if (transform.isCanvasFrameVisible) "On" else "Off", action = TraceMenuAction.ToggleCanvasFrame),
        TraceMenuEntry("Frame Preset", "FRM", detail = transform.canvasFramePresetLabel(), mode = TraceMode.CanvasFrame),
        TraceMenuEntry("Custom Ratio", "FRM", detail = transform.customCanvasFrameRatioLabel(), mode = TraceMode.CanvasFrameRatio),
        TraceMenuEntry("Frame Color", "CLR", detail = transform.canvasFrameColorLabel(), mode = TraceMode.CanvasFrameColor),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.GuideCrop -> listOf(
        TraceMenuEntry("Crop", "CRP", detail = if (transform.isCropEnabled) "On" else "Off", action = TraceMenuAction.ToggleCrop),
        TraceMenuEntry("Adjust Crop", "CRP", detail = "${transform.cropEdgeLabel()} ${transform.cropAmountLabel()}", mode = TraceMode.CropAdjust),
        TraceMenuEntry("Reset Crop", "RST", detail = transform.cropTotalLabel(), action = TraceMenuAction.ResetCrop),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.GuideShapes -> listOf(
        TraceMenuEntry("Shapes", "SHP", detail = if (transform.guideShape != com.projectortrace.model.GuideShape.Off) "On" else "Off", action = TraceMenuAction.ToggleShapes),
        TraceMenuEntry("Shape Type", "SHP", detail = transform.guideShapeLabel(), mode = TraceMode.Shape),
        TraceMenuEntry("Color", "CLR", detail = transform.guideShapeColorLabel(), mode = TraceMode.ShapeColor),
        TraceMenuEntry("Rotate Shape", "ROT", detail = transform.guideShapeRotationLabel(), mode = TraceMode.ShapeRotation),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.GuideCorners -> listOf(
        TraceMenuEntry("Corners", "CRN", detail = if (transform.isGuideFrameVisible) "On" else "Off", action = TraceMenuAction.ToggleCorners),
        TraceMenuEntry("Adjust Corners", "CRN", detail = "${transform.guideSelectedCornerLabel()} ${transform.guideSelectedCornerOffsetLabel()}", mode = TraceMode.GuideCorner),
        TraceMenuEntry("Color", "CLR", detail = transform.guideCornerColorLabel(), mode = TraceMode.GuideCornerColor),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.GuideOverlays -> listOf(
        TraceMenuEntry("Overlay", "OVR", detail = if (transform.isOverlayVisible) "On" else "Off", action = TraceMenuAction.ToggleOverlay),
        TraceMenuEntry("Open Overlay", "OVR", action = TraceMenuAction.OpenOverlay),
        TraceMenuEntry("Overlay Opacity", "OPA", detail = "${transform.overlayOpacityPercent()}%", mode = TraceMode.OverlayOpacity),
        TraceMenuEntry("Overlay Mode", "MOD", detail = transform.overlayBlendModeLabel(), mode = TraceMode.OverlayBlend),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.Filter -> listOf(
        TraceMenuEntry("Paint", "PNT", detail = transform.paintDetailLabel(), opensPanel = TraceMenuPanel.FilterPaint),
        TraceMenuEntry("Magic Outline", "MAG", detail = transform.magicOutlineDetailOrNull(), opensPanel = TraceMenuPanel.FilterMagicOutline),
        TraceMenuEntry("Threshold", "THR", detail = transform.thresholdLabel(), opensPanel = TraceMenuPanel.FilterThreshold),
        TraceMenuEntry("Channel Mixer", "CHN", detail = transform.channelMixerDetailOrNull(), opensPanel = TraceMenuPanel.FilterChannelMixer),
        TraceMenuEntry("Posterize", "PST", detail = transform.posterizeLabel(), opensPanel = TraceMenuPanel.FilterPosterize),
        TraceMenuEntry("Blending", "OPA", detail = transform.blendingDetailOrNull(), opensPanel = TraceMenuPanel.FilterBlending),
        TraceMenuEntry("Brightness", "BRT", detail = transform.brightnessDetailOrNull() ?: "Off", opensPanel = TraceMenuPanel.FilterBrightness),
        TraceMenuEntry("Edge / Outline", "EDG", detail = transform.edgeOutlineDetailOrNull(), opensPanel = TraceMenuPanel.FilterEdgeOutline),
        TraceMenuEntry("Contrast", "CON", detail = transform.contrastDetailOrNull(), opensPanel = TraceMenuPanel.FilterContrast),
        TraceMenuEntry("Detailisation", "CLR", detail = transform.detailisationDetailOrNull(), opensPanel = TraceMenuPanel.FilterDetailisation),
        TraceMenuEntry("Lines", "LIN", detail = if (transform.lineArt > 0f) "${transform.lineArtPercent()}%" else "Off", opensPanel = TraceMenuPanel.FilterLines),
        TraceMenuEntry("Invert", "INV", detail = if (transform.isInverted) "On" else "Off", opensPanel = TraceMenuPanel.FilterInvert),
        TraceMenuEntry("Blur", "BLR", detail = transform.volumeDetailOrNull() ?: "Off", opensPanel = TraceMenuPanel.FilterVolume),
        TraceMenuEntry("Reset Filter", "RST", action = TraceMenuAction.ResetFilters),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterPaint -> listOf(
        TraceMenuEntry("Paint", "PNT", detail = if (transform.paintDetail > 0f) "On" else "Off", action = TraceMenuAction.TogglePaint),
        TraceMenuEntry("Paint Detail", "PNT", detail = "${transform.paintDetailPercent()}%", mode = TraceMode.Paint),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterThreshold -> listOf(
        TraceMenuEntry("Threshold", "THR", detail = if (transform.isThresholdEnabled) "On" else "Off", action = TraceMenuAction.ToggleThreshold),
        TraceMenuEntry("Threshold Level", "THR", detail = "${(transform.threshold * 100f).roundToInt()}%", mode = TraceMode.Threshold),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterChannelMixer -> listOf(
        TraceMenuEntry("Channel Mixer", "CHN", detail = transform.channelMixerDetailOrNull(), mode = TraceMode.ChannelMixer),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterPosterize -> listOf(
        TraceMenuEntry("Posterize", "PST", detail = if (transform.posterize > 0f) "On" else "Off", action = TraceMenuAction.TogglePosterize),
        TraceMenuEntry("Posterize Level", "PST", detail = "${transform.posterizePercent()}%", mode = TraceMode.Posterize),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterBlending -> listOf(
        TraceMenuEntry("Opacity", "OPA", detail = "${transform.opacityPercent()}%", mode = TraceMode.Opacity),
        TraceMenuEntry("Mode", "MOD", detail = transform.imageBlendModeLabel(), mode = TraceMode.BlendMode),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterBrightness -> listOf(
        TraceMenuEntry("Brightness", "BRT", detail = if (transform.isBrightnessEnabled) "On" else "Off", action = TraceMenuAction.ToggleBrightness),
        TraceMenuEntry("Brightness Level", "BRT", detail = "${transform.brightnessPercent()}%", mode = TraceMode.Brightness),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterContrast -> listOf(
        TraceMenuEntry("Contrast", "CON", detail = "${transform.contrastPercent()}%", mode = TraceMode.Contrast),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterDetailisation -> listOf(
        TraceMenuEntry("Sharpness", "CLR", detail = "${transform.clarityPercent()}%", mode = TraceMode.Clarity),
        TraceMenuEntry("Noise Reduction", "CLR", detail = "${transform.noiseReductionPercent()}%", mode = TraceMode.NoiseReduction),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterLines -> listOf(
        TraceMenuEntry("Lines", "LIN", detail = if (transform.lineArt > 0f) "On" else "Off", action = TraceMenuAction.ToggleLines),
        TraceMenuEntry("Lines Strength", "LIN", detail = "${transform.lineArtPercent()}%", mode = TraceMode.LineArt),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterInvert -> listOf(
        TraceMenuEntry("Invert", "INV", detail = if (transform.isInverted) "On" else "Off", action = TraceMenuAction.ToggleInvert),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterVolume -> listOf(
        TraceMenuEntry("Blur", "BLR", detail = if (transform.volume > 0f) "On" else "Off", action = TraceMenuAction.ToggleVolume),
        TraceMenuEntry("Blur Strength", "BLR", detail = "${transform.volumePercent()}%", mode = TraceMode.Volume),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterMagicOutline -> listOf(
        TraceMenuEntry("Magic Outline", "MAG", detail = if (transform.magicOutlineStrength > 0f) "On" else "Off", action = TraceMenuAction.ToggleMagicOutline),
        TraceMenuEntry("More Detail", "MAG", detail = "${transform.magicOutlineDetailPercent()}%", mode = TraceMode.MagicOutlineDetail),
        TraceMenuEntry("Line Weight", "MAG", detail = "${transform.magicOutlineThicknessPercent()}%", mode = TraceMode.MagicOutlineThickness),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.FilterEdgeOutline -> listOf(
        TraceMenuEntry("Edge / Outline", "EDG", detail = if (transform.edgeOutlineStrength > 0f) "On" else "Off", action = TraceMenuAction.ToggleEdgeOutline),
        TraceMenuEntry("Strength", "EDG", detail = "${transform.edgeOutlineStrengthPercent()}%", mode = TraceMode.EdgeStrength),
        TraceMenuEntry("Line Thickness", "EDG", detail = "${transform.edgeOutlineThicknessPercent()}%", mode = TraceMode.EdgeThickness),
        TraceMenuEntry("Detail Level", "EDG", detail = "${transform.edgeOutlineDetailPercent()}%", mode = TraceMode.EdgeDetail),
        TraceMenuEntry("Smoothing", "EDG", detail = "${transform.edgeOutlineSmoothingPercent()}%", mode = TraceMode.EdgeSmoothing),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.Info -> listOf(
        TraceMenuEntry("Projector Trace", "APP", detail = "V${com.projectortrace.BuildConfig.VERSION_NAME}"),
        TraceMenuEntry("File", "IMG", detail = imageStatusLabel(hasImage = hasImage, imageLabel = imageLabel)),
        TraceMenuEntry("Image Size", "INF", detail = imageSizeLabel ?: if (hasImage) "Reading..." else "No image"),
        TraceMenuEntry("Scale", "ZOM", detail = "${transform.zoomPercent()}%"),
        TraceMenuEntry("Filters", "FLT", detail = transform.activeFiltersLabel()),
        TraceMenuEntry("Mode", "MOD", detail = transform.currentMode.name),
        TraceMenuEntry("Lock", "LCK", detail = if (transform.isLocked) "On" else "Off"),
        TraceMenuEntry("Vibecoder", "DEV", detail = "Sergey Yemelin"),
        TraceMenuEntry("Codex", "AI", detail = "Model 5.5"),
        TraceMenuEntry("Tested on", "TV", detail = "Google TV"),
        TraceMenuEntry("Remote only", "TV", detail = "D-pad, OK, Back/Menu"),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.Settings -> listOf(
        TraceMenuEntry("Menu Size", "SIZ", detail = menuSize.label(), action = TraceMenuAction.CycleMenuSize),
        TraceMenuEntry("Menu Position", "MOV", detail = transform.menuOffsetLabel(), mode = TraceMode.MenuMove),
        TraceMenuEntry("Menu Display", "HUD", detail = menuAutoHide.label(), action = TraceMenuAction.CycleMenuAutoHide),
        TraceMenuEntry("Blink", "BLK", detail = if (projectionBlankEnabled) projectionBlankMode.label() else "Off", opensPanel = TraceMenuPanel.SettingsBlink),
        TraceMenuEntry("Keep Screen Awake", "AWK", detail = if (keepScreenAwake) "On" else "Off", action = TraceMenuAction.ToggleKeepScreenAwake),
        TraceMenuEntry("Projector Mode", "ROT", detail = projectorOrientation.label(), action = TraceMenuAction.CycleProjectorOrientation),
        TraceMenuEntry("About", "INF", opensPanel = TraceMenuPanel.About),
        TraceMenuEntry("Privacy", "INF", opensPanel = TraceMenuPanel.Privacy),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.SettingsBlink -> listOf(
        TraceMenuEntry("Blink", "BLK", detail = if (projectionBlankEnabled) "On" else "Off", action = TraceMenuAction.ToggleProjectionBlank),
        TraceMenuEntry("Blink Mode", "OK", detail = projectionBlankMode.label(), action = TraceMenuAction.CycleProjectionBlankMode),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.Privacy -> listOf(
        TraceMenuEntry("Privacy policy", "INF", detail = "Updated 22 Sep 2026"),
        TraceMenuEntry("Developer", "DEV", detail = "Sergey Yemelin"),
        TraceMenuEntry("Processing", "IMG", detail = "Images processed on this device"),
        TraceMenuEntry("Collection", "INF", detail = "No uploads, tracking or analytics"),
        TraceMenuEntry("Access", "IMG", detail = "Only files you allow Android to share"),
        TraceMenuEntry("Storage", "INF", detail = "Settings and file references saved locally"),
        TraceMenuEntry("Backup", "INF", detail = "Android may back up app settings"),
        TraceMenuEntry("Delete", "INF", detail = "Clear app storage in Android settings"),
        TraceMenuEntry("Contact", "INF", detail = "github.com/yemelinart/ProjectorTrace/issues"),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
    TraceMenuPanel.About -> listOf(
        TraceMenuEntry("Projector Trace", "APP", detail = "V${com.projectortrace.BuildConfig.VERSION_NAME}"),
        TraceMenuEntry("Author", "DEV", detail = "S.Yemelin"),
        TraceMenuEntry("Built", "AI", detail = "VibeCoded in 1 night"),
        TraceMenuEntry("Back", "BCK", action = TraceMenuAction.Back),
    )
}

@Composable
internal fun TraceOverlay(
    transform: TransformState,
    hasImage: Boolean,
    imageLabel: String?,
    imageSizeLabel: String?,
    imageNotice: String?,
    menuPanel: TraceMenuPanel,
    hasSavedProfile: Boolean,
    menuSize: TraceMenuSize,
    hudDuration: TraceHudDuration,
    menuAutoHide: TraceMenuAutoHide,
    projectionBlankEnabled: Boolean,
    projectionBlankMode: ProjectionBlankMode,
    keepScreenAwake: Boolean,
    projectorOrientation: ProjectorOrientation,
    selectedIndex: Int,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val entries = traceMenuEntries(
        panel = menuPanel,
        transform = transform,
        hasImage = hasImage,
        imageLabel = imageLabel,
        imageSizeLabel = imageSizeLabel,
        hasSavedProfile = hasSavedProfile,
        menuSize = menuSize,
        hudDuration = hudDuration,
        menuAutoHide = menuAutoHide,
        projectionBlankEnabled = projectionBlankEnabled,
        projectionBlankMode = projectionBlankMode,
        keepScreenAwake = keepScreenAwake,
        projectorOrientation = projectorOrientation,
    )
    val selected = selectedIndex.coerceIn(0, entries.lastIndex.coerceAtLeast(0))
    val panelShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    val menuWidth = menuSize.panelWidth(isCompact)
    val listState = rememberLazyListState()

    LaunchedEffect(menuPanel, selected) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(selected)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(menuWidth)
            .clip(panelShape)
            .background(Color(0xE6F8F9FB))
            .border(width = 1.dp, color = Color(0x44FFFFFF), shape = panelShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isCompact) 12.dp else 16.dp,
                    top = if (isCompact) 12.dp else 16.dp,
                    end = if (isCompact) 10.dp else 12.dp,
                    bottom = if (isCompact) 12.dp else 14.dp,
                ),
        ) {
            Text(
                text = if (menuPanel == TraceMenuPanel.Main) "Projector Trace" else menuPanel.title(),
                color = Color(0xFF111318),
                fontSize = if (isCompact) 20.sp else 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (menuPanel == TraceMenuPanel.Main) {
                    "Professional Art Projector Utility\nby S.Yemelin  |  V${com.projectortrace.BuildConfig.VERSION_NAME}"
                } else {
                    "OK select  |  Left/Right adjust  |  Back"
                },
                color = Color(0xFF6B7280),
                fontSize = if (isCompact) 12.sp else 14.sp,
                fontWeight = FontWeight.Light,
            )

            LazyColumn(
                modifier = Modifier
                    .padding(top = if (isCompact) 12.dp else 16.dp)
                    .fillMaxWidth()
                    .weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 3.dp else 4.dp),
            ) {
                itemsIndexed(entries) { index, entry ->
                    SideMenuRow(
                        entry = entry,
                        isSelected = index == selected,
                        isCurrentMode = entry.mode == transform.currentMode,
                        isCompact = isCompact,
                    )
                }
            }

            if (menuPanel == TraceMenuPanel.Main) {
                MainMenuStatus(
                    transform = transform,
                    hasImage = hasImage,
                    imageLabel = imageLabel,
                    imageNotice = imageNotice,
                    isCompact = isCompact,
                    modifier = Modifier.padding(top = if (isCompact) 8.dp else 10.dp),
                )
            }
        }
    }
}

internal fun TraceMenuSize.panelWidth(isCompact: Boolean): Dp = when (this) {
    TraceMenuSize.Slim -> if (isCompact) 260.dp else 252.dp
    TraceMenuSize.Normal -> if (isCompact) 300.dp else 300.dp
    TraceMenuSize.Large -> if (isCompact) 340.dp else 356.dp
}

internal fun TraceMenuSize.sceneInset(isCompact: Boolean): Dp =
    panelWidth(isCompact) + if (isCompact) 8.dp else 12.dp

internal fun TraceMenuSize.label(): String = when (this) {
    TraceMenuSize.Slim -> "Slim"
    TraceMenuSize.Normal -> "Normal"
    TraceMenuSize.Large -> "Large"
}

internal fun ProjectionBlankMode.label(): String = when (this) {
    ProjectionBlankMode.Hold -> "Image / Black"
    ProjectionBlankMode.Toggle -> "Image / Black"
    ProjectionBlankMode.White -> "Image / White"
    ProjectionBlankMode.Compare -> "Blink Compare"
}

@Composable
private fun MainMenuStatus(
    transform: TransformState,
    hasImage: Boolean,
    imageLabel: String?,
    imageNotice: String?,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val image = imageStatusLabel(hasImage = hasImage, imageLabel = imageLabel)
    val lock = if (transform.isLocked) "Lock on" else "Lock off"
    val value = transform.currentModeValueLabel()
    val firstLine = "${transform.currentMode.name}  |  Z ${transform.zoomPercent()}%  |  R ${transform.rotationDegrees.roundToInt()} deg"
    val secondLine = "X ${transform.offsetX.roundToInt()}  Y ${transform.offsetY.roundToInt()}  |  $lock"
    val thirdLine = when {
        imageNotice != null -> imageNotice
        value != null -> "$image  |  $value"
        else -> image
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x66FFFFFF))
            .border(width = 1.dp, color = Color(0x55E5E7EB), shape = RoundedCornerShape(14.dp))
            .padding(horizontal = if (isCompact) 9.dp else 10.dp, vertical = if (isCompact) 7.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = firstLine,
            color = Color(0xFF374151),
            fontSize = if (isCompact) 10.sp else 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = secondLine,
            color = Color(0xFF6B7280),
            fontSize = if (isCompact) 9.sp else 10.sp,
            fontWeight = FontWeight.Light,
        )
        Text(
            text = thirdLine,
            color = if (imageNotice != null) Color(0xFFB45309) else Color(0xFF6B7280),
            fontSize = if (isCompact) 9.sp else 10.sp,
            fontWeight = if (imageNotice != null) FontWeight.Medium else FontWeight.Light,
            maxLines = 1,
        )
    }
}

@Composable
private fun SideMenuRow(
    entry: TraceMenuEntry,
    isSelected: Boolean,
    isCurrentMode: Boolean,
    isCompact: Boolean,
) {
    val rowShape = RoundedCornerShape(14.dp)
    val iconRes = entry.iconResourceId()
    val toggleValue = entry.toggleValueOrNull()
    val background = when {
        isSelected -> Color(0xFFFFFFFF)
        isCurrentMode -> Color(0xFFE8F0FF)
        else -> Color(0x00FFFFFF)
    }
    val borderColor = if (isSelected) Color(0xFFE5E7EB) else Color.Transparent
    val textColor = if (isSelected || isCurrentMode) Color(0xFF111318) else Color(0xFF1F2937)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isCompact) 34.dp else 38.dp)
            .clip(rowShape)
            .background(background)
            .border(width = 1.dp, color = borderColor, shape = rowShape)
            .padding(horizontal = if (isCompact) 7.dp else 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 28.dp else 31.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) Color(0xFF111318) else Color(0xFFE5E7EB)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = entry.icon,
                        color = if (isSelected) Color.White else Color(0xFF374151),
                        fontSize = if (isCompact) 8.sp else 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = entry.title,
                color = textColor,
                fontSize = if (isCompact) 13.sp else 14.sp,
                fontWeight = if (isSelected || isCurrentMode) FontWeight.SemiBold else FontWeight.Medium,
            )
            if (entry.detail != null && toggleValue == null) {
                Text(
                    text = entry.detail,
                    color = Color(0xFF6B7280),
                    fontSize = if (isCompact) 8.sp else 9.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        if (toggleValue != null) {
            TogglePill(
                isOn = toggleValue,
                isCompact = isCompact,
            )
        } else if (entry.opensPanel != null || entry.mode != null) {
            Text(
                text = ">",
                color = Color(0xFF9CA3AF),
                fontSize = if (isCompact) 16.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TogglePill(
    isOn: Boolean,
    isCompact: Boolean,
) {
    Box(
        modifier = Modifier
            .width(if (isCompact) 36.dp else 40.dp)
            .height(if (isCompact) 20.dp else 22.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (isOn) Color(0xFF34C759) else Color(0xFFE5E7EB))
            .padding(3.dp),
        contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 14.dp else 16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = if (isOn) Color(0x2234C759) else Color(0x22000000),
                    shape = RoundedCornerShape(999.dp),
                ),
        )
    }
}

@Composable
private fun MenuValueStrip(
    transform: TransformState,
    hasImage: Boolean,
    imageLabel: String?,
    isCompact: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x99FFFFFF))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Now: ${transform.currentMode.name}",
            color = Color(0xFF111318),
            fontSize = if (isCompact) 13.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = transform.currentModeHint(),
            color = Color(0xFF4B5563),
            fontSize = if (isCompact) 10.sp else 11.sp,
        )
        OverlayValues(
            "Zoom" to "${transform.zoomPercent()}%",
            "Rot" to transform.rotationLabel(),
            "Fit" to transform.imageFitLabel(),
            "Image" to imageStatusLabel(hasImage = hasImage, imageLabel = imageLabel),
            isCompact = true,
        )
    }
}

private fun TraceMenuPanel.title(): String = when (this) {
    TraceMenuPanel.Main -> "Projector Trace"
    TraceMenuPanel.Image -> "Image"
    TraceMenuPanel.Transform -> "Transform"
    TraceMenuPanel.TransformRotate -> "Rotate"
    TraceMenuPanel.Flip -> "Flip"
    TraceMenuPanel.TransformCornerPin -> "Distort"
    TraceMenuPanel.Guides -> "Guides"
    TraceMenuPanel.GuideColorPalette -> "Color Palette"
    TraceMenuPanel.GuideGrid -> "Grid"
    TraceMenuPanel.GuideRulers -> "Rulers"
    TraceMenuPanel.GuideCanvasFrame -> "Canvas Frame"
    TraceMenuPanel.GuideCrop -> "Crop Mode"
    TraceMenuPanel.GuideShapes -> "Shapes"
    TraceMenuPanel.GuideCorners -> "Corners"
    TraceMenuPanel.GuideOverlays -> "Overlays"
    TraceMenuPanel.Filter -> "Filter"
    TraceMenuPanel.FilterPaint -> "Paint"
    TraceMenuPanel.FilterThreshold -> "Threshold"
    TraceMenuPanel.FilterChannelMixer -> "Channel Mixer"
    TraceMenuPanel.FilterPosterize -> "Posterize"
    TraceMenuPanel.FilterBlending -> "Blending"
    TraceMenuPanel.FilterBrightness -> "Brightness"
    TraceMenuPanel.FilterContrast -> "Contrast"
    TraceMenuPanel.FilterDetailisation -> "Detailisation"
    TraceMenuPanel.FilterLines -> "Lines"
    TraceMenuPanel.FilterInvert -> "Invert"
    TraceMenuPanel.FilterVolume -> "Blur"
    TraceMenuPanel.FilterMagicOutline -> "Magic Outline"
    TraceMenuPanel.FilterEdgeOutline -> "Edge / Outline"
    TraceMenuPanel.Profiles -> "Profiles"
    TraceMenuPanel.Info -> "Info"
    TraceMenuPanel.Settings -> "Settings"
    TraceMenuPanel.SettingsBlink -> "Blink"
    TraceMenuPanel.About -> "About"
    TraceMenuPanel.Privacy -> "Privacy"
}

internal fun TraceMenuPanel.parentPanel(): TraceMenuPanel = when (this) {
    TraceMenuPanel.Flip -> TraceMenuPanel.Transform
    TraceMenuPanel.TransformRotate -> TraceMenuPanel.Transform
    TraceMenuPanel.TransformCornerPin -> TraceMenuPanel.Transform
    TraceMenuPanel.Profiles -> TraceMenuPanel.Image
    TraceMenuPanel.GuideColorPalette,
    TraceMenuPanel.GuideGrid,
    TraceMenuPanel.GuideRulers,
    TraceMenuPanel.GuideCanvasFrame,
    TraceMenuPanel.GuideCrop,
    TraceMenuPanel.GuideShapes,
    TraceMenuPanel.GuideCorners,
    TraceMenuPanel.GuideOverlays -> TraceMenuPanel.Guides
    TraceMenuPanel.FilterPaint,
    TraceMenuPanel.FilterThreshold,
    TraceMenuPanel.FilterChannelMixer,
    TraceMenuPanel.FilterPosterize,
    TraceMenuPanel.FilterBlending,
    TraceMenuPanel.FilterBrightness,
    TraceMenuPanel.FilterContrast,
    TraceMenuPanel.FilterDetailisation,
    TraceMenuPanel.FilterLines,
    TraceMenuPanel.FilterInvert,
    TraceMenuPanel.FilterVolume,
    TraceMenuPanel.FilterMagicOutline,
    TraceMenuPanel.FilterEdgeOutline -> TraceMenuPanel.Filter
    TraceMenuPanel.SettingsBlink,
    TraceMenuPanel.About,
    TraceMenuPanel.Privacy -> TraceMenuPanel.Settings
    TraceMenuPanel.Main -> TraceMenuPanel.Main
    else -> TraceMenuPanel.Main
}

private fun TraceMenuPanel.depth(): Int = when (this) {
    TraceMenuPanel.Main -> 0
    TraceMenuPanel.Image,
    TraceMenuPanel.Transform,
    TraceMenuPanel.Guides,
    TraceMenuPanel.Filter,
    TraceMenuPanel.Info,
    TraceMenuPanel.Settings -> 1
    TraceMenuPanel.Flip,
    TraceMenuPanel.TransformRotate,
    TraceMenuPanel.TransformCornerPin,
    TraceMenuPanel.Profiles,
    TraceMenuPanel.GuideColorPalette,
    TraceMenuPanel.GuideGrid,
    TraceMenuPanel.GuideRulers,
    TraceMenuPanel.GuideCanvasFrame,
    TraceMenuPanel.GuideCrop,
    TraceMenuPanel.GuideShapes,
    TraceMenuPanel.GuideCorners,
    TraceMenuPanel.GuideOverlays,
    TraceMenuPanel.FilterPaint,
    TraceMenuPanel.FilterThreshold,
    TraceMenuPanel.FilterChannelMixer,
    TraceMenuPanel.FilterPosterize,
    TraceMenuPanel.FilterBlending,
    TraceMenuPanel.FilterBrightness,
    TraceMenuPanel.FilterContrast,
    TraceMenuPanel.FilterDetailisation,
    TraceMenuPanel.FilterLines,
    TraceMenuPanel.FilterInvert,
    TraceMenuPanel.FilterVolume,
    TraceMenuPanel.FilterMagicOutline,
    TraceMenuPanel.FilterEdgeOutline,
    TraceMenuPanel.SettingsBlink,
    TraceMenuPanel.About,
    TraceMenuPanel.Privacy -> 2
}

private val TraceModePickerDisplayModes = listOf(
    TraceMode.Move,
    TraceMode.Zoom,
    TraceMode.Rotate,
    TraceMode.Fit,
    TraceMode.Distort,
    TraceMode.MenuMove,
    TraceMode.Frame,
    TraceMode.CanvasFrame,
    TraceMode.CanvasFrameRatio,
    TraceMode.CanvasFrameColor,
    TraceMode.CropAdjust,
    TraceMode.Shape,
    TraceMode.ShapeColor,
    TraceMode.ShapeRotation,
    TraceMode.GuideCorner,
    TraceMode.GuideCornerColor,
    TraceMode.ColorPalette,
    TraceMode.PaletteMode,
    TraceMode.PaletteMove,
    TraceMode.PaletteScale,
    TraceMode.Paint,
    TraceMode.MagicOutlineDetail,
    TraceMode.MagicOutlineThickness,
    TraceMode.Threshold,
    TraceMode.ChannelMixer,
    TraceMode.Posterize,
    TraceMode.Opacity,
    TraceMode.BlendMode,
    TraceMode.Brightness,
    TraceMode.EdgeStrength,
    TraceMode.EdgeThickness,
    TraceMode.EdgeDetail,
    TraceMode.EdgeSmoothing,
    TraceMode.Contrast,
    TraceMode.Clarity,
    TraceMode.NoiseReduction,
    TraceMode.LineArt,
    TraceMode.Volume,
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TraceModePicker(
    selectedMode: TraceMode,
    currentMode: TraceMode,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val panelWidth = if (isCompact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.width(560.dp)
    }

    Column(
        modifier = modifier
            .padding(if (isCompact) 12.dp else 24.dp)
            .then(panelWidth)
            .background(Color(0xF20D0F12))
            .border(width = 1.dp, color = Color(0xFF374151))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Choose Mode",
            color = Color(0xFFFFD166),
            fontSize = if (isCompact) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = selectedMode.name,
            color = Color(0xFFE8EEF7),
            fontSize = if (isCompact) 14.sp else 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TraceModePickerDisplayModes.forEach { mode ->
                PickerModeBadge(
                    label = mode.shortLabel(),
                    isSelected = mode == selectedMode,
                    isCurrent = mode == currentMode,
                    isCompact = isCompact,
                )
            }
        }
        Text(
            text = "Arrows Choose  |  OK Select  |  Back/Menu Close",
            color = Color(0xFFB6C2D2),
            fontSize = if (isCompact) 10.sp else 12.sp,
        )
    }
}

@Composable
private fun PickerModeBadge(
    label: String,
    isSelected: Boolean,
    isCurrent: Boolean,
    isCompact: Boolean,
) {
    val background = when {
        isSelected -> Color(0xFFFFD166)
        isCurrent -> Color(0xFF1F2937)
        else -> Color(0xFF111827)
    }
    val border = when {
        isSelected -> Color(0xFFFFD166)
        isCurrent -> Color(0xFF9DECF9)
        else -> Color(0xFF374151)
    }

    Box(
        modifier = Modifier
            .widthIn(min = if (isCompact) 66.dp else 78.dp)
            .background(background)
            .border(width = if (isCurrent && !isSelected) 2.dp else 1.dp, color = border)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF111827) else Color(0xFFE8EEF7),
            fontSize = if (isCompact) 10.sp else 11.sp,
            fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
internal fun TraceLockBadge(
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(if (isCompact) 12.dp else 24.dp)
            .background(Color(0xDD0D0F12))
            .border(width = 1.dp, color = Color(0xFFD97706))
            .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = "LOCKED",
            color = Color(0xFFFFD166),
            fontSize = if (isCompact) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun imageStatusLabel(
    hasImage: Boolean,
    imageLabel: String?,
): String {
    if (!hasImage) return "Placeholder"

    val label = imageLabel?.trim().orEmpty()
    if (label.isBlank()) return "Selected"

    val maxLength = 24
    if (label.length <= maxLength) return label

    val extension = label.substringAfterLast('.', missingDelimiterValue = "")
    return if (extension.isNotBlank() && extension.length <= 5) {
        val suffix = ".$extension"
        val prefixLength = (maxLength - suffix.length - 3).coerceAtLeast(8)
        "${label.take(prefixLength)}...$suffix"
    } else {
        "${label.take(maxLength - 3)}..."
    }
}

private fun TraceMenuEntry.iconResourceId(): Int? = when {
    title == "Select Corner" && icon == "CRN" -> R.drawable.pt_icon_corners
    title == "Adjust Corners" && icon == "CRN" -> R.drawable.pt_icon_corners
    title == "Select Corner" && icon == "CRP" -> R.drawable.pt_icon_fit
    title == "Adjust Crop" && icon == "CRP" -> R.drawable.pt_icon_fit
    else -> when (title) {
    "Image" -> R.drawable.pt_icon_image
    "Transform" -> R.drawable.pt_icon_transform
    "Guides" -> R.drawable.pt_icon_guides
    "Filter" -> R.drawable.pt_icon_filter
    "Info" -> R.drawable.pt_icon_info
    "Settings" -> R.drawable.pt_icon_settings
    "Quit" -> R.drawable.pt_icon_quit
    "Gallery" -> R.drawable.pt_icon_image
    "Open" -> R.drawable.pt_icon_open
    "Browse Files" -> R.drawable.pt_icon_browse_files
    "Profiles" -> R.drawable.pt_icon_preset
    "Save Profile" -> R.drawable.pt_icon_preset
    "Load Profile" -> R.drawable.pt_icon_preset
    "Clear Profile" -> R.drawable.pt_icon_reset
    "USB Folder" -> R.drawable.pt_icon_usb_folder
    "Close" -> R.drawable.pt_icon_close
    "Reset" -> R.drawable.pt_icon_reset
    "Reset Transform" -> R.drawable.pt_icon_reset
    "Reset Guides" -> R.drawable.pt_icon_reset
    "Lock" -> R.drawable.pt_icon_lock
    "Back" -> R.drawable.pt_icon_back
    "Move" -> R.drawable.pt_icon_move
    "Zoom" -> R.drawable.pt_icon_zoom
    "Rotate" -> R.drawable.pt_icon_rotate
    "Rotate Step" -> R.drawable.pt_icon_rotate
    "Fit" -> R.drawable.pt_icon_fit
    "Distort" -> R.drawable.pt_icon_distort
    "Select Corner" -> R.drawable.pt_icon_distort
    "Adjust Distort" -> R.drawable.pt_icon_distort
    "Reset Distort" -> R.drawable.pt_icon_reset
    "Flip" -> R.drawable.pt_icon_flip_horizontal
    "Horizontal" -> R.drawable.pt_icon_flip_horizontal
    "Vertical" -> R.drawable.pt_icon_flip_vertical
    "Grid" -> R.drawable.pt_icon_grid
    "Grid Size" -> R.drawable.pt_icon_grid
    "Grid Color" -> R.drawable.pt_icon_grid
    "Center Cross / Rulers" -> R.drawable.pt_icon_guides
    "Rulers" -> R.drawable.pt_icon_guides
    "Center Cross" -> R.drawable.pt_icon_guides
    "Canvas Frame" -> R.drawable.pt_icon_fit
    "Frame Preset" -> R.drawable.pt_icon_fit
    "Frame Color" -> R.drawable.pt_icon_color_palette
    "Custom Ratio" -> R.drawable.pt_icon_fit
    "Crop Mode" -> R.drawable.pt_icon_fit
    "Crop" -> R.drawable.pt_icon_fit
    "Select Edge" -> R.drawable.pt_icon_fit
    "Adjust Edge" -> R.drawable.pt_icon_fit
    "Reset Crop" -> R.drawable.pt_icon_reset
    "Corners" -> R.drawable.pt_icon_corners
    "Shapes" -> R.drawable.pt_icon_shapes
    "Shape Type" -> R.drawable.pt_icon_shapes
    "Color" -> R.drawable.pt_icon_color_palette
    "Rotate Shape" -> R.drawable.pt_icon_rotate
    "Color Palette" -> R.drawable.pt_icon_color_palette
    "Palette Colors" -> R.drawable.pt_icon_color_palette
    "Palette Mode" -> R.drawable.pt_icon_color_palette
    "Move Palette" -> R.drawable.pt_icon_color_palette
    "Palette Scale" -> R.drawable.pt_icon_color_palette
    "Palette Corner" -> R.drawable.pt_icon_color_palette
    "Overlays" -> R.drawable.pt_icon_overlays
    "Overlay" -> R.drawable.pt_icon_overlays
    "Open Overlay" -> R.drawable.pt_icon_overlays
    "Overlay Opacity" -> R.drawable.pt_icon_opacity
    "Overlay Mode" -> R.drawable.pt_icon_overlays
    "Preset" -> R.drawable.pt_icon_preset
    "Blending" -> R.drawable.pt_icon_opacity
    "Opacity" -> R.drawable.pt_icon_opacity
    "Mode" -> R.drawable.pt_icon_transform
    "Brightness" -> R.drawable.pt_icon_appearance
    "Brightness Level" -> R.drawable.pt_icon_appearance
    "Threshold" -> R.drawable.pt_icon_threshold
    "Threshold Level" -> R.drawable.pt_icon_threshold
    "Channel Mixer" -> R.drawable.pt_icon_channel_mixer
    "Paint" -> R.drawable.pt_icon_paint
    "Paint Detail" -> R.drawable.pt_icon_paint
    "Posterize" -> R.drawable.pt_icon_posterize
    "Posterize Level" -> R.drawable.pt_icon_posterize
    "Lines" -> R.drawable.pt_icon_lines
    "Lines Strength" -> R.drawable.pt_icon_lines
    "Clarity" -> R.drawable.pt_icon_clarity
    "Contrast" -> R.drawable.pt_icon_contrast
    "Detailisation" -> R.drawable.pt_icon_clarity
    "Sharpness" -> R.drawable.pt_icon_clarity
    "Noise Reduction" -> R.drawable.pt_icon_clarity
    "Invert" -> R.drawable.pt_icon_invert
    "Blur" -> R.drawable.pt_icon_volume
    "Blur Strength" -> R.drawable.pt_icon_volume
    "Reset Filter" -> R.drawable.pt_icon_reset
    "Magic Outline" -> R.drawable.pt_icon_lines
    "More Detail" -> R.drawable.pt_icon_lines
    "Line Weight" -> R.drawable.pt_icon_lines
    "Edge / Outline" -> R.drawable.pt_icon_lines
    "Strength" -> R.drawable.pt_icon_lines
    "Line Thickness" -> R.drawable.pt_icon_lines
    "Detail Level" -> R.drawable.pt_icon_lines
    "Smoothing" -> R.drawable.pt_icon_lines
    "Projector Trace" -> R.drawable.pt_icon_image
    "File" -> R.drawable.pt_icon_image_info
    "Image Size" -> R.drawable.pt_icon_info
    "Scale" -> R.drawable.pt_icon_zoom
    "Filters" -> R.drawable.pt_icon_filter
    "Vibecoder" -> R.drawable.pt_icon_info
    "Codex" -> R.drawable.pt_icon_info
    "Tested on" -> R.drawable.pt_icon_info
    "Remote only" -> R.drawable.pt_icon_info
    "Current mode" -> R.drawable.pt_icon_transform
    "Menu Size" -> R.drawable.pt_icon_menu_size
    "Menu Position" -> R.drawable.pt_icon_move
    "Menu Display" -> R.drawable.pt_icon_info_popup
    "Blink" -> R.drawable.pt_icon_opacity
    "Projection Blank" -> R.drawable.pt_icon_opacity
    "Blink Mode" -> R.drawable.pt_icon_lock
    "Blank OK Mode" -> R.drawable.pt_icon_lock
    "Keep Screen Awake" -> R.drawable.pt_icon_lock
    "Appearance" -> R.drawable.pt_icon_appearance
    "Projector Mode" -> R.drawable.pt_icon_rotate
    "Reset All" -> R.drawable.pt_icon_reset_all
    "About" -> R.drawable.pt_icon_info
    "Author" -> R.drawable.pt_icon_info
    "Built" -> R.drawable.pt_icon_info
    else -> null
    }
}

private fun TraceMenuEntry.toggleValueOrNull(): Boolean? {
    if (detail != "On" && detail != "Off") return null

    return when (action) {
        TraceMenuAction.ToggleLock,
        TraceMenuAction.ToggleGrid,
        TraceMenuAction.ToggleRulers,
        TraceMenuAction.ToggleCenterCross,
        TraceMenuAction.ToggleCanvasFrame,
        TraceMenuAction.ToggleCrop,
        TraceMenuAction.ToggleCorners,
        TraceMenuAction.ToggleShapes,
        TraceMenuAction.ToggleColorPalette,
        TraceMenuAction.ToggleOverlay,
        TraceMenuAction.ToggleFlipHorizontal,
        TraceMenuAction.ToggleFlipVertical,
        TraceMenuAction.ToggleThreshold,
        TraceMenuAction.TogglePaint,
        TraceMenuAction.TogglePosterize,
        TraceMenuAction.ToggleLines,
        TraceMenuAction.ToggleVolume,
        TraceMenuAction.ToggleMagicOutline,
        TraceMenuAction.ToggleEdgeOutline,
        TraceMenuAction.ToggleBrightness,
        TraceMenuAction.ToggleProjectionBlank,
        TraceMenuAction.ToggleKeepScreenAwake,
        TraceMenuAction.ToggleInvert -> detail == "On"
        else -> null
    }
}

@Composable
internal fun TraceImageLibrary(
    items: List<MediaImageItem>,
    selectedIndex: Int,
    message: String?,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentItem = items.getOrNull(selectedIndex)
    val thumbnail by produceState<Bitmap?>(initialValue = null, key1 = currentItem?.uri) {
        value = null
        value = currentItem?.let { loadLibraryThumbnail(context, it.uri) }
    }
    val panelWidth = if (isCompact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.widthIn(min = 420.dp, max = 680.dp)
    }

    Column(
        modifier = modifier
            .padding(if (isCompact) 12.dp else 24.dp)
            .then(panelWidth)
            .background(Color(0xEE0D0F12))
            .border(width = 1.dp, color = Color(0xFF374151))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Image Library",
            color = Color(0xFFFFD166),
            fontSize = if (isCompact) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
        )

        if (message != null) {
            Text(
                text = message,
                color = Color(0xFFE8EEF7),
                fontSize = if (isCompact) 13.sp else 15.sp,
            )
        } else if (currentItem != null) {
            Text(
                text = "${selectedIndex + 1} / ${items.size}",
                color = Color(0xFF9CA3AF),
                fontSize = if (isCompact) 11.sp else 12.sp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 132.dp else 180.dp)
                    .background(Color(0xFF111827))
                    .border(width = 1.dp, color = Color(0xFF374151)),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = "Preview unavailable",
                        color = Color(0xFF9CA3AF),
                        fontSize = if (isCompact) 11.sp else 12.sp,
                    )
                }
            }
            Text(
                text = currentItem.displayName,
                color = Color(0xFFE8EEF7),
                fontSize = if (isCompact) 15.sp else 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = currentItem.volumeName,
                color = Color(0xFFB6C2D2),
                fontSize = if (isCompact) 11.sp else 12.sp,
            )
        }

        Text(
            text = "Left/Right Browse  |  Up/Down CH/Page RW/FF Jump 10  |  OK Open",
            color = Color(0xFFB6C2D2),
            fontSize = if (isCompact) 10.sp else 12.sp,
        )
        Text(
            text = "Hold OK Refresh  |  Back Close",
            color = Color(0xFFB6C2D2),
            fontSize = if (isCompact) 10.sp else 12.sp,
        )
    }
}

private suspend fun loadLibraryThumbnail(
    context: Context,
    uri: Uri,
): Bitmap? = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            context.contentResolver.loadThumbnail(uri, Size(360, 240), null)
        }.getOrNull()?.let { return@withContext it }
    }

    runCatching {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val sampleSize = thumbnailSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            targetWidth = 360,
            targetHeight = 240,
        )
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }.getOrNull()
}

private fun thumbnailSampleSize(
    width: Int,
    height: Int,
    targetWidth: Int,
    targetHeight: Int,
): Int {
    if (width <= 0 || height <= 0) return 1
    return com.projectortrace.imaging.calculateSampleSize(width, height, targetWidth, targetHeight)
}

@Composable
private fun ModePanel(
    currentMode: TraceMode,
    isCompact: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ModeRow(
            modes = listOf(
                TraceMode.Move,
                TraceMode.Zoom,
                TraceMode.Rotate,
                TraceMode.Corner,
                TraceMode.Distort,
                TraceMode.Frame,
                TraceMode.View,
                TraceMode.Fit,
                TraceMode.Guide,
                TraceMode.Step,
                TraceMode.Preset,
            ),
            currentMode = currentMode,
            isCompact = isCompact,
        )
        ModeRow(
            modes = listOf(
                TraceMode.Opacity,
                TraceMode.Contrast,
                TraceMode.Clarity,
                TraceMode.LineArt,
                TraceMode.Invert,
                TraceMode.Lock,
                TraceMode.Setup,
                TraceMode.Open,
            ),
            currentMode = currentMode,
            isCompact = isCompact,
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ModeRow(
    modes: List<TraceMode>,
    currentMode: TraceMode,
    isCompact: Boolean,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        modes.forEach { mode ->
            ModeBadge(
                label = mode.shortLabel(),
                isActive = mode == currentMode,
                isCompact = isCompact,
            )
        }
    }
}

@Composable
private fun ModeBadge(
    label: String,
    isActive: Boolean,
    isCompact: Boolean,
) {
    Box(
        modifier = Modifier
            .widthIn(min = if (isCompact) 46.dp else 54.dp)
            .background(if (isActive) Color(0xFFFFD166) else Color(0xFF111827))
            .border(width = 1.dp, color = if (isActive) Color(0xFFFFD166) else Color(0xFF374151))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            color = if (isActive) Color(0xFF111827) else Color(0xFFE8EEF7),
            fontSize = if (isCompact) 10.sp else 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private fun TransformState.currentModeHint(): String = when (currentMode) {
    TraceMode.Move -> "Arrows move the image"
    TraceMode.Zoom -> "Up/right zoom in, down/left zoom out"
    TraceMode.Rotate -> "Left/right rotate by ${rotationStepLabel()}"
    TraceMode.Corner -> "Arrows select corner, OK moves it"
    TraceMode.Distort -> "Arrows move selected corner, OK selects next corner"
    TraceMode.Frame -> "Left/right width, up/down height"
    TraceMode.View -> "Arrows choose Auto, Landscape, or Portrait"
    TraceMode.Fit -> "Arrows choose Fit, Fill, or Stretch"
    TraceMode.MenuMove -> "D-pad moves the menu overlay"
    TraceMode.Guide -> "Any arrow shows or hides the corner frame"
    TraceMode.Rulers -> "Any arrow shows or hides center rulers"
    TraceMode.CenterCross -> "Any arrow shows or hides the center cross"
    TraceMode.CanvasFrame -> "Arrows choose frame ratio preset"
    TraceMode.CanvasFrameRatio -> "Arrows adjust the custom canvas ratio"
    TraceMode.CanvasFrameColor -> "Arrows choose frame color"
    TraceMode.CropEdge -> "OK selects side, arrows crop selected side"
    TraceMode.CropAdjust -> "OK selects side, arrows crop selected side"
    TraceMode.Shape -> "Arrows choose thirds, golden ratio, diagonals"
    TraceMode.ShapeColor -> "Arrows choose black, white, magenta, yellow, or green"
    TraceMode.ColorPalette -> "Right/up more colors, left/down fewer or off"
    TraceMode.PaletteMode -> "Arrows choose Balanced, Dominant, or Paint"
    TraceMode.PaletteMove -> "D-pad moves palette freely around the screen"
    TraceMode.PaletteScale -> "Right/up larger palette, left/down smaller"
    TraceMode.PalettePosition -> "Arrows move palette around the screen"
    TraceMode.Step -> "Arrows choose fine, normal, or coarse control"
    TraceMode.Preset -> "Arrows choose a tracing filter preset"
    TraceMode.Opacity -> "Up/right more visible, down/left more transparent"
    TraceMode.BlendMode -> "Arrows choose Normal, Color, or Overlay"
    TraceMode.Brightness -> "Right/up brighter, left/down darker"
    TraceMode.Threshold -> "Right/up darker threshold, left/down lighter"
    TraceMode.ChannelMixer -> "Arrows choose normal, black-white, or two-color"
    TraceMode.Paint -> "Right/up more paint detail, left/down broader color"
    TraceMode.Posterize -> "Right/up fewer colors, left/down softer posterize"
    TraceMode.Volume -> "Right/up stronger blur, left/down sharper image"
    TraceMode.MagicOutlineDetail -> "Right/up more artistic detail, left/down cleaner line-art"
    TraceMode.MagicOutlineThickness -> "Right/up bolder lines, left/down thinner lines"
    TraceMode.EdgeStrength -> "Right/up stronger outline, left/down weaker"
    TraceMode.EdgeThickness -> "Right/up thicker outline, left/down thinner"
    TraceMode.EdgeDetail -> "Right/up more edge detail, left/down cleaner"
    TraceMode.EdgeSmoothing -> "Right/up smoother outline, left/down sharper"
    TraceMode.Contrast -> "Up/right more contrast, down/left less contrast"
    TraceMode.Clarity -> "Up/right sharper, down/left softer"
    TraceMode.NoiseReduction -> "Up/right smoother, down/left less reduction"
    TraceMode.LineArt -> "Up/right stronger contours, down/left weaker contours"
    TraceMode.GuideCorner -> "OK selects corner, D-pad moves selected corner"
    TraceMode.GuideCornerColor -> "Arrows choose white, black, magenta, red, yellow, green, or cyan"
    TraceMode.GuideCornerMove -> "OK selects corner, D-pad moves selected corner"
    TraceMode.ShapeRotation -> "Left/right rotate composition guide"
    TraceMode.OverlayOpacity -> "Up/right stronger overlay, down/left weaker"
    TraceMode.OverlayBlend -> "Arrows choose overlay blend mode"
    TraceMode.Invert -> "Any arrow toggles inverted image"
    TraceMode.Lock -> "Any arrow locks or unlocks transform controls"
    TraceMode.Setup -> "Up Reset, Right Filters, Left Image, Down Folder"
    TraceMode.Open -> "Up file or library, left folder, right library"
}

private fun TransformState.currentModeValueLabel(): String? = when (currentMode) {
    TraceMode.Move -> "X ${offsetX.roundToInt()}  Y ${offsetY.roundToInt()}"
    TraceMode.Zoom -> "${zoomPercent()}%"
    TraceMode.Rotate -> rotationLabel()
    TraceMode.Corner -> cornerPinSelectedCornerLabel()
    TraceMode.Distort -> cornerPinSelectedOffsetLabel()
    TraceMode.Frame -> "${gridSpacingPx.roundToInt()} px"
    TraceMode.View -> canvasOrientationLabel()
    TraceMode.Fit -> imageFitLabel()
    TraceMode.MenuMove -> menuOffsetLabel()
    TraceMode.Guide -> if (isGuideFrameVisible) "On" else "Off"
    TraceMode.Rulers -> if (isRulersVisible) "On" else "Off"
    TraceMode.CenterCross -> if (isCenterCrossVisible) "On" else "Off"
    TraceMode.CanvasFrame -> canvasFramePresetLabel()
    TraceMode.CanvasFrameRatio -> customCanvasFrameRatioLabel()
    TraceMode.CanvasFrameColor -> canvasFrameColorLabel()
    TraceMode.CropEdge -> "${cropEdgeLabel()} ${cropAmountLabel()}"
    TraceMode.CropAdjust -> "${cropEdgeLabel()} ${cropAmountLabel()}"
    TraceMode.Shape -> guideShapeLabel()
    TraceMode.ShapeColor -> guideShapeColorLabel()
    TraceMode.ColorPalette -> colorPaletteLabel()
    TraceMode.PaletteMode -> colorPaletteModeLabel()
    TraceMode.PaletteMove -> colorPaletteOffsetLabel()
    TraceMode.PaletteScale -> colorPaletteScaleLabel()
    TraceMode.PalettePosition -> colorPalettePositionLabel()
    TraceMode.Step -> controlStepLabel()
    TraceMode.Preset -> tracingPresetLabel()
    TraceMode.Opacity -> "${opacityPercent()}%"
    TraceMode.BlendMode -> imageBlendModeLabel()
    TraceMode.Brightness -> if (isBrightnessEnabled) "${brightnessPercent()}%" else "Off"
    TraceMode.Threshold -> if (isThresholdEnabled) "${(threshold * 100f).roundToInt()}%" else "Off"
    TraceMode.ChannelMixer -> channelMixerLabel()
    TraceMode.Paint -> if (paintDetail > 0f) "${paintDetailPercent()}%" else "Off"
    TraceMode.Posterize -> if (posterize > 0f) "${posterizePercent()}%" else "Off"
    TraceMode.Volume -> if (volume > 0f) "${volumePercent()}%" else "Off"
    TraceMode.MagicOutlineDetail -> if (magicOutlineStrength > 0f) "${magicOutlineDetailPercent()}%" else "Off"
    TraceMode.MagicOutlineThickness -> if (magicOutlineStrength > 0f) "${magicOutlineThicknessPercent()}%" else "Off"
    TraceMode.EdgeStrength -> if (edgeOutlineStrength > 0f) "${edgeOutlineStrengthPercent()}%" else "Off"
    TraceMode.EdgeThickness -> "${edgeOutlineThicknessPercent()}%"
    TraceMode.EdgeDetail -> "${edgeOutlineDetailPercent()}%"
    TraceMode.EdgeSmoothing -> "${edgeOutlineSmoothingPercent()}%"
    TraceMode.Contrast -> "${contrastPercent()}%"
    TraceMode.Clarity -> "${clarityPercent()}%"
    TraceMode.NoiseReduction -> "${noiseReductionPercent()}%"
    TraceMode.LineArt -> if (lineArt > 0f) "${lineArtPercent()}%" else "Off"
    TraceMode.GuideCorner -> "${guideSelectedCornerLabel()} ${guideSelectedCornerOffsetLabel()}"
    TraceMode.GuideCornerColor -> guideCornerColorLabel()
    TraceMode.GuideCornerMove -> guideSelectedCornerOffsetLabel()
    TraceMode.ShapeRotation -> guideShapeRotationLabel()
    TraceMode.OverlayOpacity -> "${overlayOpacityPercent()}%"
    TraceMode.OverlayBlend -> overlayBlendModeLabel()
    TraceMode.Invert -> if (isInverted) "On" else "Off"
    TraceMode.Lock -> if (isLocked) "On" else "Off"
    TraceMode.Setup -> null
    TraceMode.Open -> null
}

private fun TransformState.moveDetailOrNull(): String? =
    if (abs(offsetX) > 0.5f || abs(offsetY) > 0.5f) {
        "X ${offsetX.roundToInt()}  Y ${offsetY.roundToInt()}"
    } else {
        null
    }

private fun TransformState.zoomDetailOrNull(): String? =
    if (abs(scale - 1f) > 0.005f) "${zoomPercent()}%" else null

private fun TransformState.rotateDetailOrNull(): String? =
    if (abs(rotationDegrees) > 0.5f) rotationLabel() else null

private fun TransformState.rotateMenuDetail(): String =
    rotateDetailOrNull()?.let { "$it / ${rotationStepLabel()}" } ?: rotationStepLabel()

internal fun TransformState.rotationStepLabel(): String = "${rotationStepDegrees.roundToInt()} deg"

private fun TransformState.fitDetailOrNull(): String? =
    if (imageFitLabel() != "Fit") imageFitLabel() else null

private fun TransformState.distortDetailOrNull(): String? =
    if (
        abs(topLeft.x) > 0.5f || abs(topLeft.y) > 0.5f ||
        abs(topRight.x) > 0.5f || abs(topRight.y) > 0.5f ||
        abs(bottomRight.x) > 0.5f || abs(bottomRight.y) > 0.5f ||
        abs(bottomLeft.x) > 0.5f || abs(bottomLeft.y) > 0.5f
    ) {
        "Edited"
    } else {
        null
    }

private fun TransformState.cornerPinDetailOrNull(): String? =
    distortDetailOrNull() ?: cornerPinSelectedCornerLabel()

private fun TransformState.cornerPinSelectedCornerLabel(): String = when (selectedCorner) {
    com.projectortrace.model.DistortCorner.TopLeft -> "Top Left"
    com.projectortrace.model.DistortCorner.TopRight -> "Top Right"
    com.projectortrace.model.DistortCorner.BottomRight -> "Bottom Right"
    com.projectortrace.model.DistortCorner.BottomLeft -> "Bottom Left"
}

private fun TransformState.cornerPinSelectedOffsetLabel(): String {
    val offset = when (selectedCorner) {
        com.projectortrace.model.DistortCorner.TopLeft -> topLeft
        com.projectortrace.model.DistortCorner.TopRight -> topRight
        com.projectortrace.model.DistortCorner.BottomRight -> bottomRight
        com.projectortrace.model.DistortCorner.BottomLeft -> bottomLeft
    }
    return "${cornerPinSelectedCornerLabel()}  ${offset.x.roundToInt()}, ${offset.y.roundToInt()}"
}

private fun TransformState.flipDetailOrNull(): String? = when {
    isFlippedHorizontal && isFlippedVertical -> "Horizontal + Vertical"
    isFlippedHorizontal -> "Horizontal"
    isFlippedVertical -> "Vertical"
    else -> null
}

private fun TransformState.gridDetailOrNull(): String? =
    if (!isGridVisible) {
        "Off"
    } else if (abs(gridSpacingPx - 120f) > 0.5f) {
        "${gridSpacingPx.roundToInt()} px"
    } else {
        gridColorLabel()
    }

private fun TransformState.rulersDetailOrNull(): String? = when {
    !isRulersVisible -> "Off"
    isCenterCrossVisible -> "Rulers + Cross"
    else -> "Rulers"
}

private fun TransformState.canvasFrameDetailOrNull(): String? =
    if (isCanvasFrameVisible) canvasFramePresetLabel() else "Off"

private fun TransformState.cropDetailOrNull(): String? =
    if (isCropEnabled) cropTotalLabel() else "Off"

internal fun TransformState.gridColorLabel(): String = when (gridColorMode) {
    GridColorMode.White -> "White"
    GridColorMode.Black -> "Black"
}

internal fun TransformState.canvasFramePresetLabel(): String = when (canvasFramePreset) {
    CanvasFramePreset.Ratio1x1 -> "1:1"
    CanvasFramePreset.Ratio2x3 -> "2:3"
    CanvasFramePreset.Ratio3x2 -> "3:2"
    CanvasFramePreset.Ratio4x5 -> "4:5"
    CanvasFramePreset.Ratio5x4 -> "5:4"
    CanvasFramePreset.Ratio9x16 -> "9:16"
    CanvasFramePreset.Ratio16x9 -> "16:9"
    CanvasFramePreset.Ratio16x20 -> "16:20"
    CanvasFramePreset.Ratio18x24 -> "18:24"
    CanvasFramePreset.Ratio24x36 -> "24:36"
    CanvasFramePreset.Custom -> "Custom"
}

internal fun TransformState.customCanvasFrameRatioLabel(): String =
    String.format(Locale.US, "%.2f:1", customCanvasFrameRatio)

internal fun TransformState.canvasFrameColorLabel(): String = when (canvasFrameColor) {
    GuideCornerColor.White -> "White"
    GuideCornerColor.Black -> "Black"
    GuideCornerColor.Magenta -> "Magenta"
    GuideCornerColor.Red -> "Red"
    GuideCornerColor.Yellow -> "Yellow"
    GuideCornerColor.Green -> "Green"
    GuideCornerColor.Cyan -> "Cyan"
}

internal fun TransformState.cropCornerLabel(): String = when (selectedCropCorner) {
    CropCorner.TopLeft -> "Top Left"
    CropCorner.TopRight -> "Top Right"
    CropCorner.BottomRight -> "Bottom Right"
    CropCorner.BottomLeft -> "Bottom Left"
}

internal fun TransformState.cropEdgeLabel(): String = when (selectedCropEdge) {
    com.projectortrace.model.CropEdge.Left -> "Left"
    com.projectortrace.model.CropEdge.Top -> "Top"
    com.projectortrace.model.CropEdge.Right -> "Right"
    com.projectortrace.model.CropEdge.Bottom -> "Bottom"
}

internal fun TransformState.cropAmountLabel(): String = when (selectedCropEdge) {
    com.projectortrace.model.CropEdge.Left -> "${(cropLeft * 100f).roundToInt()}%"
    com.projectortrace.model.CropEdge.Top -> "${(cropTop * 100f).roundToInt()}%"
    com.projectortrace.model.CropEdge.Right -> "${(cropRight * 100f).roundToInt()}%"
    com.projectortrace.model.CropEdge.Bottom -> "${(cropBottom * 100f).roundToInt()}%"
}

internal fun TransformState.cropTotalLabel(): String =
    "${((cropLeft + cropTop + cropRight + cropBottom) * 100f).roundToInt()}% total"

internal fun ProjectorOrientation.label(): String = when (this) {
    ProjectorOrientation.Normal -> "Normal"
    ProjectorOrientation.UpsideDown -> "Upside Down"
    ProjectorOrientation.RotateLeft -> "Rotate Left"
    ProjectorOrientation.RotateRight -> "Rotate Right"
}

private fun PalettePosition.label(): String = when (this) {
    PalettePosition.TopStart -> "Top Left"
    PalettePosition.TopCenter -> "Top"
    PalettePosition.TopEnd -> "Top Right"
    PalettePosition.CenterEnd -> "Right"
    PalettePosition.BottomEnd -> "Bottom Right"
    PalettePosition.BottomCenter -> "Bottom"
    PalettePosition.BottomStart -> "Bottom Left"
    PalettePosition.CenterStart -> "Left"
}

private fun ImageBlendMode.label(): String = when (this) {
    ImageBlendMode.Normal -> "Normal"
    ImageBlendMode.Color -> "Color"
    ImageBlendMode.Overlay -> "Overlay"
}

private fun OverlayBlendMode.label(): String = when (this) {
    OverlayBlendMode.Normal -> "Normal"
    OverlayBlendMode.Multiply -> "Multiply"
    OverlayBlendMode.Screen -> "Screen"
    OverlayBlendMode.Overlay -> "Overlay"
}

private fun TransformState.shapeDetailOrNull(): String? =
    if (guideShapeLabel() == "Off") null else "${guideShapeLabel()} ${guideShapeColorLabel()}"

private fun TransformState.paletteDetailOrNull(): String? =
    colorPaletteLabel().takeUnless { it == "Off" }

private fun TransformState.presetDetailOrNull(): String? =
    tracingPresetLabel().takeUnless { it == "Custom" }

private fun TransformState.opacityDetailOrNull(): String? =
    if (opacityPercent() != 100) "${opacityPercent()}%" else null

private fun TransformState.blendingDetailOrNull(): String? = when {
    opacityPercent() != 100 && imageBlendMode != ImageBlendMode.Normal -> "${opacityPercent()}% ${imageBlendModeLabel()}"
    opacityPercent() != 100 -> "${opacityPercent()}%"
    imageBlendMode != ImageBlendMode.Normal -> imageBlendModeLabel()
    else -> null
}

internal fun TransformState.brightnessPercent(): Int = (brightness * 100f).roundToInt()

private fun TransformState.brightnessDetailOrNull(): String? =
    if (isBrightnessEnabled) "${brightnessPercent()}%" else null

private fun TransformState.channelMixerDetailOrNull(): String? =
    channelMixerLabel().takeUnless { it == "Normal" }

private fun TransformState.paintDetailPercent(): Int = (paintDetail * 100f).roundToInt()

private fun TransformState.posterizePercent(): Int = (posterize * 100f).roundToInt()

private fun TransformState.volumePercent(): Int = (volume * 100f).roundToInt()

private fun TransformState.volumeDetailOrNull(): String? =
    if (volumePercent() > 0) "${volumePercent()}%" else null

internal fun TransformState.magicOutlineDetailPercent(): Int = (magicOutlineDetail * 100f).roundToInt()

internal fun TransformState.magicOutlineThicknessPercent(): Int = (magicOutlineThickness * 100f).roundToInt()

private fun TransformState.magicOutlineDetailOrNull(): String? =
    if (magicOutlineStrength > 0f) "Detail ${magicOutlineDetailPercent()}% / ${magicOutlineThicknessPercent()}%" else "Off"

internal fun TransformState.edgeOutlineStrengthPercent(): Int = (edgeOutlineStrength * 100f).roundToInt()

internal fun TransformState.edgeOutlineThicknessPercent(): Int = (edgeOutlineThickness * 100f).roundToInt()

internal fun TransformState.edgeOutlineDetailPercent(): Int = (edgeOutlineDetail * 100f).roundToInt()

internal fun TransformState.edgeOutlineSmoothingPercent(): Int = (edgeOutlineSmoothing * 100f).roundToInt()

private fun TransformState.edgeOutlineDetailOrNull(): String? =
    if (edgeOutlineStrength > 0f) "Strength ${edgeOutlineStrengthPercent()}%" else "Off"

private fun TransformState.clarityDetailOrNull(): String? =
    if (clarityPercent() > 0) "${clarityPercent()}%" else null

internal fun TransformState.noiseReductionPercent(): Int = (noiseReduction * 100f).roundToInt()

private fun TransformState.detailisationDetailOrNull(): String? = when {
    clarityPercent() > 0 && noiseReductionPercent() > 0 -> "S ${clarityPercent()}% / NR ${noiseReductionPercent()}%"
    clarityPercent() > 0 -> "Sharp ${clarityPercent()}%"
    noiseReductionPercent() > 0 -> "Noise ${noiseReductionPercent()}%"
    else -> null
}

private fun TransformState.contrastDetailOrNull(): String? =
    if (contrastPercent() != 100) "${contrastPercent()}%" else null

private fun TransformState.activeFiltersLabel(): String {
    val filters = buildList {
        if (opacityPercent() != 100) add("Opacity ${opacityPercent()}%")
        if (imageBlendMode != ImageBlendMode.Normal) add("Blend ${imageBlendModeLabel()}")
        if (isBrightnessEnabled) add("Brightness ${brightnessPercent()}%")
        if (isThresholdEnabled) add("Threshold ${(threshold * 100f).roundToInt()}%")
        if (channelMixerLabel() != "Normal") add(channelMixerLabel())
        if (paintDetail > 0f) add("Paint ${paintDetailPercent()}%")
        if (posterize > 0f) add("Posterize ${posterizePercent()}%")
        if (volume > 0f) add("Blur ${volumePercent()}%")
        if (magicOutlineStrength > 0f) add("Magic ${magicOutlineDetailPercent()}%")
        if (edgeOutlineStrength > 0f) add("Edge ${edgeOutlineStrengthPercent()}%")
        if (lineArt > 0f) add("Lines ${lineArtPercent()}%")
        if (clarityPercent() > 0) add("Clarity ${clarityPercent()}%")
        if (noiseReductionPercent() > 0) add("Noise ${noiseReductionPercent()}%")
        if (contrastPercent() != 100) add("Contrast ${contrastPercent()}%")
        if (isInverted) add("Invert")
    }
    if (filters.isEmpty()) return "None"

    val text = filters.joinToString(", ")
    return if (text.length <= 42) text else "${text.take(39)}..."
}

internal fun TransformState.colorPalettePositionLabel(): String = colorPalettePosition.label()

internal fun TransformState.menuOffsetLabel(): String =
    "X ${menuOffsetX.roundToInt()} / Y ${menuOffsetY.roundToInt()}"

internal fun TransformState.colorPaletteOffsetLabel(): String =
    "X ${colorPaletteOffsetX.roundToInt()} / Y ${colorPaletteOffsetY.roundToInt()}"

internal fun TransformState.colorPaletteScaleLabel(): String =
    "${(colorPaletteScale * 100f).roundToInt()}%"

internal fun TransformState.guideShapeRotationLabel(): String = "${guideShapeRotationDegrees.roundToInt()} deg"

internal fun TransformState.guideShapeColorLabel(): String = when (guideShapeColor) {
    GuideShapeColor.Black -> "Black"
    GuideShapeColor.White -> "White"
    GuideShapeColor.Magenta -> "Magenta"
    GuideShapeColor.Yellow -> "Yellow"
    GuideShapeColor.Green -> "Green"
}

internal fun TransformState.guideCornerColorLabel(): String = when (guideCornerColor) {
    GuideCornerColor.White -> "White"
    GuideCornerColor.Black -> "Black"
    GuideCornerColor.Magenta -> "Magenta"
    GuideCornerColor.Red -> "Red"
    GuideCornerColor.Yellow -> "Yellow"
    GuideCornerColor.Green -> "Green"
    GuideCornerColor.Cyan -> "Cyan"
}

internal fun TransformState.overlayOpacityPercent(): Int = (overlayOpacity * 100f).roundToInt()

internal fun TransformState.overlayBlendModeLabel(): String = overlayBlendMode.label()

internal fun TransformState.imageBlendModeLabel(): String = imageBlendMode.label()

internal fun TransformState.guideSelectedCornerLabel(): String = when (guideSelectedCorner) {
    com.projectortrace.model.DistortCorner.TopLeft -> "TL"
    com.projectortrace.model.DistortCorner.TopRight -> "TR"
    com.projectortrace.model.DistortCorner.BottomRight -> "BR"
    com.projectortrace.model.DistortCorner.BottomLeft -> "BL"
}

internal fun TransformState.guideSelectedCornerOffsetLabel(): String {
    val offset = when (guideSelectedCorner) {
        com.projectortrace.model.DistortCorner.TopLeft -> guideTopLeft
        com.projectortrace.model.DistortCorner.TopRight -> guideTopRight
        com.projectortrace.model.DistortCorner.BottomRight -> guideBottomRight
        com.projectortrace.model.DistortCorner.BottomLeft -> guideBottomLeft
    }
    return "${offset.x.roundToInt()},${offset.y.roundToInt()}"
}

private fun TransformState.resetHintLabel(): String = when (currentMode) {
    TraceMode.View,
    TraceMode.Fit -> "Hold OK geometry"
    TraceMode.Open -> "Hold OK image"
    TraceMode.Setup -> "Use arrows"
    else -> "Hold OK current"
}

private fun TraceMode.shortLabel(): String = when (this) {
    TraceMode.Move -> "Move"
    TraceMode.Zoom -> "Zoom"
    TraceMode.Rotate -> "Rot"
    TraceMode.Corner -> "Corner"
    TraceMode.Distort -> "Warp"
    TraceMode.Frame -> "Frame"
    TraceMode.View -> "View"
    TraceMode.Fit -> "Fit"
    TraceMode.MenuMove -> "Menu"
    TraceMode.Guide -> "Guide"
    TraceMode.Rulers -> "Rulers"
    TraceMode.CenterCross -> "Cross"
    TraceMode.CanvasFrame -> "Frame"
    TraceMode.CanvasFrameRatio -> "Ratio"
    TraceMode.CanvasFrameColor -> "F Color"
    TraceMode.CropEdge -> "Crop Side"
    TraceMode.CropAdjust -> "Crop"
    TraceMode.Shape -> "Shape"
    TraceMode.ShapeColor -> "Color"
    TraceMode.ColorPalette -> "Palette"
    TraceMode.PaletteMode -> "Pal Mode"
    TraceMode.PaletteMove -> "Pal Move"
    TraceMode.PaletteScale -> "Pal Size"
    TraceMode.PalettePosition -> "Pal Pos"
    TraceMode.Step -> "Step"
    TraceMode.Preset -> "Preset"
    TraceMode.Opacity -> "Alpha"
    TraceMode.BlendMode -> "Blend"
    TraceMode.Brightness -> "Bright"
    TraceMode.Threshold -> "Thresh"
    TraceMode.ChannelMixer -> "Mixer"
    TraceMode.Paint -> "Paint"
    TraceMode.Posterize -> "Post"
    TraceMode.Volume -> "Blur"
    TraceMode.MagicOutlineDetail -> "Magic D"
    TraceMode.MagicOutlineThickness -> "Magic W"
    TraceMode.EdgeStrength -> "Edge"
    TraceMode.EdgeThickness -> "Thick"
    TraceMode.EdgeDetail -> "Detail"
    TraceMode.EdgeSmoothing -> "Smooth"
    TraceMode.Contrast -> "Contr"
    TraceMode.Clarity -> "Clear"
    TraceMode.NoiseReduction -> "Noise"
    TraceMode.LineArt -> "Lines"
    TraceMode.GuideCorner -> "Corners"
    TraceMode.GuideCornerColor -> "Color"
    TraceMode.GuideCornerMove -> "Corners"
    TraceMode.ShapeRotation -> "Shape Rot"
    TraceMode.OverlayOpacity -> "O Alpha"
    TraceMode.OverlayBlend -> "O Blend"
    TraceMode.Invert -> "Invert"
    TraceMode.Lock -> "Lock"
    TraceMode.Setup -> "Setup"
    TraceMode.Open -> "Open"
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun OverlayValues(
    vararg values: Pair<String, String>,
    isCompact: Boolean,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
    ) {
        values.forEach { (label, value) ->
            OverlayValue(label = label, value = value, isCompact = isCompact)
        }
    }
}

@Composable
private fun OverlayValue(
    label: String,
    value: String,
    isCompact: Boolean,
) {
    Column {
        Text(
            text = label,
            color = Color(0xFF6B7280),
            fontSize = if (isCompact) 10.sp else 11.sp,
        )
        Text(
            text = value,
            color = Color(0xFF111318),
            fontSize = if (isCompact) 12.sp else 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
