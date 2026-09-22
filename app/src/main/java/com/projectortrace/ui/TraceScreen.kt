package com.projectortrace.ui

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlendMode
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.projectortrace.model.CanvasFramePreset
import com.projectortrace.model.CanvasOrientation
import com.projectortrace.model.ChannelMixerMode
import com.projectortrace.model.ColorPaletteMode
import com.projectortrace.model.ControlStep
import com.projectortrace.model.CornerOffset
import com.projectortrace.model.CropCorner
import com.projectortrace.model.CropEdge
import com.projectortrace.model.DistortCorner
import com.projectortrace.model.GuideCornerColor
import com.projectortrace.model.GuideShape
import com.projectortrace.model.GuideShapeColor
import com.projectortrace.model.GridColorMode
import com.projectortrace.model.ImageFit
import com.projectortrace.model.ImageBlendMode
import com.projectortrace.model.OverlayBlendMode
import com.projectortrace.model.PalettePosition
import com.projectortrace.model.ProjectionBlankMode
import com.projectortrace.model.ProjectorOrientation
import com.projectortrace.model.TraceMode
import com.projectortrace.model.TracingPreset
import com.projectortrace.model.TransformState
import com.projectortrace.R
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.projectortrace.imaging.calculateSampleSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val MoveStepPx = 12f
private const val DistortStepPx = 10f
private const val FrameStepPx = 10f
private const val ZoomStep = 0.05f
private const val OpacityStep = 0.05f
private const val BrightnessStep = 0.05f
private const val ContrastStep = 0.05f
private const val ClarityStep = 0.05f
private const val NoiseReductionStep = 0.05f
private const val LineArtStep = 0.05f
private const val EdgeOutlineStep = 0.05f
private const val ThresholdStep = 0.03f
private const val PaintDetailStep = 0.05f
private const val PosterizeStep = 0.05f
private const val VolumeStep = 0.05f
private const val MagicOutlineStep = 0.05f
private const val CropStep = 0.01f
private const val CanvasFrameRatioStep = 0.03f
private const val MinScale = 0.2f
private const val MaxScale = 5f
private const val MaxSavedOffsetPx = 4000f
private const val MaxSavedRotationDegrees = 3600f
private const val MaxCornerOffsetPx = 700f
private const val MaxGuideInsetPx = 700f
private const val MinGridSpacingPx = 24f
private const val MaxGridSpacingPx = 320f
private const val GridSpacingStepPx = 8f
private const val MinPaletteColors = 2
private const val MaxPaletteColors = 16
private const val MaxPaletteOffsetPx = 2200f
private const val MinPaletteScale = 0.55f
private const val MaxPaletteScale = 1.85f
private const val PaletteScaleStep = 0.05f
private const val MaxMenuOffsetPx = 900f
private const val MaxDecodedImageDimensionPx = 3072
private const val MaxLineArtDimensionPx = 1600
private const val MaxEdgeOutlineDimensionPx = 1500
private const val MaxClarityDimensionPx = 1400
private const val MaxThresholdDimensionPx = 1800
private const val MaxPaintDimensionPx = 1280
private const val MaxPosterizeDimensionPx = 1800
private const val MaxVolumeDimensionPx = 1200
private const val MaxMagicOutlineDimensionPx = 1500
private const val MaxPaletteDimensionPx = 640
private const val MaxLibraryImages = 300
private const val MaxFolderScanDepth = 6
private const val LibraryPageJump = 10
private const val PreferencesName = "projector_trace_settings"
private const val KeyScale = "scale"
private const val KeyOffsetX = "offset_x"
private const val KeyOffsetY = "offset_y"
private const val KeyRotation = "rotation"
private const val KeyFlipHorizontal = "flip_horizontal"
private const val KeyFlipVertical = "flip_vertical"
private const val KeyOpacity = "opacity"
private const val KeyImageBlendMode = "image_blend_mode"
private const val KeyBrightnessEnabled = "brightness_enabled"
private const val KeyBrightness = "brightness"
private const val KeyContrast = "contrast"
private const val KeyClarity = "clarity"
private const val KeyNoiseReduction = "noise_reduction"
private const val KeyLineArt = "line_art"
private const val KeyThresholdEnabled = "threshold_enabled"
private const val KeyThreshold = "threshold"
private const val KeyChannelMixer = "channel_mixer"
private const val KeyPaintDetail = "paint_detail"
private const val KeyPosterize = "posterize"
private const val KeyVolume = "volume"
private const val KeyMagicOutlineStrength = "magic_outline_strength"
private const val KeyMagicOutlineDetail = "magic_outline_detail"
private const val KeyMagicOutlineThickness = "magic_outline_thickness"
private const val KeyEdgeOutlineStrength = "edge_outline_strength"
private const val KeyEdgeOutlineThickness = "edge_outline_thickness"
private const val KeyEdgeOutlineDetail = "edge_outline_detail"
private const val KeyEdgeOutlineSmoothing = "edge_outline_smoothing"
private const val KeyInverted = "inverted"
private const val KeyTracingPreset = "tracing_preset"
private const val KeyLocked = "locked"
private const val KeyMode = "mode"
private const val KeyControlStep = "control_step"
private const val KeyRotationStepDegrees = "rotation_step_degrees"
private const val KeySelectedCorner = "selected_corner"
private const val KeyTopLeftX = "top_left_x"
private const val KeyTopLeftY = "top_left_y"
private const val KeyTopRightX = "top_right_x"
private const val KeyTopRightY = "top_right_y"
private const val KeyBottomRightX = "bottom_right_x"
private const val KeyBottomRightY = "bottom_right_y"
private const val KeyBottomLeftX = "bottom_left_x"
private const val KeyBottomLeftY = "bottom_left_y"
private const val KeyGuideInset = "guide_inset"
private const val KeyGuideInsetX = "guide_inset_x"
private const val KeyGuideInsetY = "guide_inset_y"
private const val KeyGuideSelectedCorner = "guide_selected_corner"
private const val KeyGuideCornerColor = "guide_corner_color"
private const val KeyGuideTopLeftX = "guide_top_left_x"
private const val KeyGuideTopLeftY = "guide_top_left_y"
private const val KeyGuideTopRightX = "guide_top_right_x"
private const val KeyGuideTopRightY = "guide_top_right_y"
private const val KeyGuideBottomRightX = "guide_bottom_right_x"
private const val KeyGuideBottomRightY = "guide_bottom_right_y"
private const val KeyGuideBottomLeftX = "guide_bottom_left_x"
private const val KeyGuideBottomLeftY = "guide_bottom_left_y"
private const val KeyGridVisible = "grid_visible"
private const val KeyGridSpacing = "grid_spacing"
private const val KeyGridColorMode = "grid_color_mode"
private const val KeyRulersVisible = "rulers_visible"
private const val KeyCenterCrossVisible = "center_cross_visible"
private const val KeyCanvasFrameVisible = "canvas_frame_visible"
private const val KeyCanvasFramePreset = "canvas_frame_preset"
private const val KeyCustomCanvasFrameRatio = "custom_canvas_frame_ratio"
private const val KeyCanvasFrameColor = "canvas_frame_color"
private const val KeyCropEnabled = "crop_enabled"
private const val KeySelectedCropEdge = "selected_crop_edge"
private const val KeySelectedCropCorner = "selected_crop_corner"
private const val KeyCropLeft = "crop_left"
private const val KeyCropTop = "crop_top"
private const val KeyCropRight = "crop_right"
private const val KeyCropBottom = "crop_bottom"
private const val KeyGuideFrameVisible = "guide_frame_visible"
private const val KeyGuideShape = "guide_shape"
private const val KeyGuideShapeColor = "guide_shape_color"
private const val KeyGuideShapeRotation = "guide_shape_rotation"
private const val KeyColorPaletteCount = "color_palette_count"
private const val KeyColorPaletteMode = "color_palette_mode"
private const val KeyColorPalettePosition = "color_palette_position"
private const val KeyColorPaletteOffsetX = "color_palette_offset_x"
private const val KeyColorPaletteOffsetY = "color_palette_offset_y"
private const val KeyColorPaletteScale = "color_palette_scale"
private const val KeyOverlayVisible = "overlay_visible"
private const val KeyOverlayOpacity = "overlay_opacity"
private const val KeyOverlayBlendMode = "overlay_blend_mode"
private const val KeyCanvasOrientation = "canvas_orientation"
private const val KeyImageFit = "image_fit"
private const val KeyMenuOffsetX = "menu_offset_x"
private const val KeyMenuOffsetY = "menu_offset_y"
private const val KeyMenuSize = "menu_size"
private const val KeyHudDuration = "hud_duration"
private const val KeyMenuAutoHide = "menu_auto_hide"
private const val KeyProjectionBlankEnabled = "projection_blank_enabled"
private const val KeyProjectionBlankMode = "projection_blank_mode"
private const val KeyKeepScreenAwake = "keep_screen_awake"
private const val KeyProjectorOrientation = "projector_orientation"
private const val KeyLastImageUri = "last_image_uri"
private const val KeyLastOverlayUri = "last_overlay_uri"
private const val KeyLastFolderUri = "last_folder_uri"
private const val KeySavedProfile = "saved_profile"
private const val ModePickerColumns = 6
private val ModePickerModes = listOf(
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
private val InvertColorMatrix = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    )
)
private val BlackWhiteColorFilter = ColorMatrixColorFilter(
    ColorMatrix().apply {
        setSaturation(0f)
    }
)

private enum class BlinkCompareStep {
    Filtered,
    Original,
    Outline,
    BlackWhite;

    fun next(): BlinkCompareStep = when (this) {
        Filtered -> Original
        Original -> Outline
        Outline -> BlackWhite
        BlackWhite -> Filtered
    }
}

internal data class MediaImageItem(
    val uri: Uri,
    val displayName: String,
    val volumeName: String,
    val dateModifiedSeconds: Long,
)

@Composable
fun TraceScreen(
    externalImageUri: Uri? = null,
    onExternalImageConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    var transform by remember { mutableStateOf(context.loadTransformState().copy(currentMode = TraceMode.Move)) }
    var selectedImageUri by remember { mutableStateOf(context.loadLastImageUri()) }
    var selectedOverlayUri by remember { mutableStateOf(context.loadLastOverlayUri()) }
    var selectedFolderUri by remember { mutableStateOf(context.loadLastFolderUri()) }
    var isLaunchSplashVisible by remember { mutableStateOf(true) }
    var isOverlayVisible by remember { mutableStateOf(false) }
    var menuPanel by remember { mutableStateOf(TraceMenuPanel.Main) }
    var menuPanelHistory by remember { mutableStateOf<List<TraceMenuPanel>>(emptyList()) }
    var menuSize by remember { mutableStateOf(context.loadMenuSize()) }
    var hudDuration by remember { mutableStateOf(context.loadHudDuration()) }
    var menuAutoHide by remember { mutableStateOf(context.loadMenuAutoHide()) }
    var projectionBlankEnabled by remember { mutableStateOf(context.loadProjectionBlankEnabled()) }
    var projectionBlankMode by remember { mutableStateOf(context.loadProjectionBlankMode()) }
    var isProjectionBlanked by remember { mutableStateOf(false) }
    var blinkCompareStep by remember { mutableStateOf(BlinkCompareStep.Filtered) }
    var keepScreenAwake by remember { mutableStateOf(context.loadKeepScreenAwake()) }
    var projectorOrientation by remember { mutableStateOf(context.loadProjectorOrientation()) }
    var menuSelectedIndex by remember { mutableStateOf(0) }
    var isDirectControlActive by remember { mutableStateOf(false) }
    var miniHudPulse by remember { mutableStateOf(0) }
    var valueOverlayText by remember { mutableStateOf<String?>(null) }
    var valueOverlayPulse by remember { mutableStateOf(0) }
    var isModePickerVisible by remember { mutableStateOf(false) }
    var modePickerIndex by remember { mutableStateOf(transform.currentMode.indexInPicker()) }
    var isImageLibraryVisible by remember { mutableStateOf(false) }
    var shouldRememberSelectedImage by remember { mutableStateOf(selectedImageUri != null) }
    var imageLibraryItems by remember { mutableStateOf(emptyList<MediaImageItem>()) }
    var imageLibraryIndex by remember { mutableStateOf(0) }
    var imageLibraryMessage by remember { mutableStateOf<String?>(null) }
    var imageNotice by remember { mutableStateOf<String?>(null) }
    var imageLibraryRefreshToken by remember { mutableStateOf(0) }
    var hasSavedProfile by remember { mutableStateOf(context.hasSavedProfile()) }
    var okLongPressHandled by remember { mutableStateOf(false) }
    var menuLongPressHandled by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    fun chooseImage(
        uri: Uri,
        persistPermission: Boolean = true,
    ) {
        val persistedPermission = if (persistPermission) {
            context.persistReadPermission(uri)
        } else {
            false
        }
        shouldRememberSelectedImage = persistedPermission ||
            context.canRestoreImageUri(uri)
        imageNotice = null
        selectedImageUri = uri
        isImageLibraryVisible = false
        isModePickerVisible = false
        menuPanel = TraceMenuPanel.Main
        menuSelectedIndex = 0
        if (!shouldRememberSelectedImage) {
            imageNotice = "Image opened for this session"
        }
        miniHudPulse += 1
    }

    fun refocusTraceScreen() {
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(Unit) {
        delay(350L)
        isLaunchSplashVisible = false
        isOverlayVisible = true
        refocusTraceScreen()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            chooseImage(uri)
        }
    }
    val overlayPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.persistReadPermission(uri)
            selectedOverlayUri = uri
            transform = transform.copy(isOverlayVisible = true)
            imageNotice = "Overlay loaded"
            miniHudPulse += 1
        }
    }
    val fallbackImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            chooseImage(uri)
        }
    }
    val fileBrowserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            chooseImage(uri)
        } else {
            refocusTraceScreen()
        }
    }
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.persistReadPermission(uri)
            selectedFolderUri = uri
            imageNotice = "Folder selected. Opening image library..."
            isImageLibraryVisible = true
            imageLibraryMessage = "Scanning selected folder..."
            imageLibraryRefreshToken += 1
        }
    }
    val mediaPermissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (context.hasMediaImagePermission()) {
            isImageLibraryVisible = true
            imageLibraryMessage = "Scanning images..."
            imageLibraryRefreshToken += 1
        } else if (selectedFolderUri != null) {
            isImageLibraryVisible = true
            imageLibraryMessage = "Scanning selected folder..."
            imageLibraryRefreshToken += 1
        } else {
            isImageLibraryVisible = true
            imageLibraryItems = emptyList()
            imageLibraryIndex = 0
            imageLibraryMessage = "Image permission is required for local library"
        }
    }

    fun openImageLibrary() {
        isImageLibraryVisible = true
        isModePickerVisible = false
        imageLibraryMessage = "Scanning images..."

        if (context.hasMediaImagePermission() || selectedFolderUri != null) {
            imageLibraryRefreshToken += 1
        } else {
            mediaPermissionRequest.launch(mediaImagePermissions())
        }
    }

    fun openLibraryFallback(message: String) {
        imageNotice = message
        miniHudPulse += 1
        openImageLibrary()
        refocusTraceScreen()
    }

    fun openImagePickerOrLibrary() {
        when {
            context.canOpenDocumentImage() -> {
                runCatching {
                    imagePicker.launch(arrayOf("image/*"))
                }.onFailure {
                    openLibraryFallback("System file browser did not open. Showing local images...")
                }
            }
            context.canGetContentImage() -> {
                runCatching {
                    fallbackImagePicker.launch("image/*")
                }.onFailure {
                    openLibraryFallback("System file browser did not open. Showing local images...")
                }
            }
            else -> {
                openLibraryFallback("No Android TV file browser found. Showing local images...")
            }
        }
    }

    fun openFolderPickerOrLibrary() {
        if (context.canOpenDocumentTree()) {
            runCatching {
                folderPicker.launch(null)
            }.onFailure {
                openLibraryFallback("Folder picker did not open. Opening local library...")
            }
        } else {
            openLibraryFallback("No TV folder picker. Opening local library...")
        }
    }

    fun openOverlayPicker() {
        if (context.canOpenDocumentImage()) {
            runCatching {
                overlayPicker.launch(arrayOf("image/*"))
            }.onFailure {
                imageNotice = "System overlay picker did not open"
                miniHudPulse += 1
                refocusTraceScreen()
            }
        } else {
            imageNotice = "No Android TV file browser for overlay"
            miniHudPulse += 1
            refocusTraceScreen()
        }
    }

    fun handleProjectionBlankKey(event: ComposeKeyEvent): Boolean {
        if (
            !projectionBlankEnabled ||
            isDirectControlActive ||
            isOverlayVisible ||
            isImageLibraryVisible ||
            isModePickerVisible
        ) return false

        val nativeEvent = event.nativeKeyEvent
        val isOkKey = nativeEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            nativeEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
            nativeEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        if (!isOkKey) return false

        when (projectionBlankMode) {
            ProjectionBlankMode.Hold -> {
                if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                    isProjectionBlanked = !isProjectionBlanked
                }
            }
            ProjectionBlankMode.Toggle -> {
                if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                    isProjectionBlanked = !isProjectionBlanked
                }
            }
            ProjectionBlankMode.White -> {
                if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                    isProjectionBlanked = !isProjectionBlanked
                }
            }
            ProjectionBlankMode.Compare -> {
                if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                    isProjectionBlanked = false
                    blinkCompareStep = blinkCompareStep.next()
                    imageNotice = when (blinkCompareStep) {
                        BlinkCompareStep.Filtered -> "Compare: filtered"
                        BlinkCompareStep.Original -> "Compare: original"
                        BlinkCompareStep.Outline -> "Compare: outline"
                        BlinkCompareStep.BlackWhite -> "Compare: black-white"
                    }
                    miniHudPulse += 1
                }
            }
        }

        return true
    }

    // Coalesce remote key repeats; persist the latest state when leaving the app.
    val latestTransform by rememberUpdatedState(transform)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) context.saveTransformState(latestTransform)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            context.saveTransformState(latestTransform)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(transform) {
        delay(250L)
        context.saveTransformState(transform)
    }

    fun navigateBack() {
        when {
            isImageLibraryVisible -> isImageLibraryVisible = false
            isModePickerVisible -> isModePickerVisible = false
            isProjectionBlanked -> isProjectionBlanked = false
            isDirectControlActive -> {
                isDirectControlActive = false
                isOverlayVisible = true
            }
            isOverlayVisible && menuPanelHistory.isNotEmpty() -> {
                menuPanel = menuPanelHistory.last()
                menuPanelHistory = menuPanelHistory.dropLast(1)
                menuSelectedIndex = 0
            }
            isOverlayVisible -> isOverlayVisible = false
            else -> (context as? Activity)?.finish()
        }
    }
    BackHandler { navigateBack() }

    LaunchedEffect(menuSize) {
        context.saveMenuSize(menuSize)
    }

    LaunchedEffect(hudDuration) {
        context.saveHudDuration(hudDuration)
    }

    LaunchedEffect(menuAutoHide) {
        context.saveMenuAutoHide(menuAutoHide)
    }

    LaunchedEffect(projectionBlankEnabled) {
        context.saveProjectionBlankEnabled(projectionBlankEnabled)
        if (!projectionBlankEnabled) {
            isProjectionBlanked = false
            blinkCompareStep = BlinkCompareStep.Filtered
        }
    }

    LaunchedEffect(projectionBlankMode) {
        context.saveProjectionBlankMode(projectionBlankMode)
        isProjectionBlanked = false
        blinkCompareStep = BlinkCompareStep.Filtered
    }

    LaunchedEffect(keepScreenAwake) {
        context.saveKeepScreenAwake(keepScreenAwake)
    }

    DisposableEffect(keepScreenAwake) {
        val activity = context as? Activity
        if (keepScreenAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(projectorOrientation) {
        context.saveProjectorOrientation(projectorOrientation)
    }

    LaunchedEffect(selectedImageUri, shouldRememberSelectedImage) {
        when {
            selectedImageUri == null -> context.clearLastImageUri()
            shouldRememberSelectedImage -> context.saveLastImageUri(selectedImageUri!!)
            else -> context.clearLastImageUri()
        }
        refocusTraceScreen()
    }

    LaunchedEffect(selectedOverlayUri) {
        selectedOverlayUri?.let { context.saveLastOverlayUri(it) } ?: context.clearLastOverlayUri()
        refocusTraceScreen()
    }

    LaunchedEffect(externalImageUri) {
        externalImageUri?.let { uri ->
            chooseImage(uri)
            onExternalImageConsumed()
        }
    }

    LaunchedEffect(selectedFolderUri) {
        selectedFolderUri?.let { context.saveLastFolderUri(it) } ?: context.clearLastFolderUri()
    }

    LaunchedEffect(valueOverlayPulse) {
        if (valueOverlayPulse == 0) return@LaunchedEffect
        delay(1_250L)
        valueOverlayText = null
    }

    LaunchedEffect(isImageLibraryVisible) {
        if (!isImageLibraryVisible) {
            refocusTraceScreen()
        }
    }

    LaunchedEffect(imageLibraryRefreshToken) {
        if (imageLibraryRefreshToken == 0) return@LaunchedEffect

        val hasMediaPermission = context.hasMediaImagePermission()
        val items = loadImageLibrary(
            context = context,
            folderUri = selectedFolderUri,
            includeMediaStore = hasMediaPermission,
        )
        imageLibraryItems = items
        imageLibraryIndex = 0
        imageLibraryMessage = when {
            items.isNotEmpty() -> null
            selectedFolderUri != null && hasMediaPermission -> "No images found in library or selected folder"
            selectedFolderUri != null -> "No images found in selected folder"
            else -> "No indexed images found"
        }
    }

    LaunchedEffect(isOverlayVisible, isDirectControlActive, menuPanel, menuSelectedIndex, menuAutoHide, selectedImageUri) {
        val hideAfterMillis = menuAutoHide.millis ?: return@LaunchedEffect
        if (selectedImageUri == null) return@LaunchedEffect
        if (!isOverlayVisible || isDirectControlActive || isImageLibraryVisible || isModePickerVisible) return@LaunchedEffect

        delay(hideAfterMillis)
        if (isDirectControlActive) return@LaunchedEffect
        isOverlayVisible = false
        menuPanelHistory = emptyList()
        refocusTraceScreen()
    }

    val selectedImageLabel by produceState<String?>(
        initialValue = null,
        key1 = selectedImageUri,
    ) {
        value = selectedImageUri?.let { loadImageDisplayName(context, it) }
    }
    val selectedImageSizeLabel by produceState<String?>(
        initialValue = null,
        key1 = selectedImageUri,
    ) {
        value = selectedImageUri?.let { loadImageSizeLabel(context, it) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050608))
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP && !keyEvent.nativeKeyEvent.isCanceled) {
                        navigateBack()
                    }
                    return@onPreviewKeyEvent true
                }
                if (isLaunchSplashVisible) {
                    return@onPreviewKeyEvent true
                }

                if (isImageLibraryVisible) {
                    return@onPreviewKeyEvent handleImageLibraryKeyEvent(
                        event = keyEvent,
                        items = imageLibraryItems,
                        selectedIndex = imageLibraryIndex,
                        onSelectedIndexChange = { imageLibraryIndex = it },
                        onClose = {
                            isImageLibraryVisible = false
                            refocusTraceScreen()
                        },
                        onChooseImage = { uri ->
                            chooseImage(uri)
                        },
                        onRefresh = { openImageLibrary() },
                        okLongPressHandled = okLongPressHandled,
                        onOkLongPressHandledChange = { okLongPressHandled = it },
                    )
                }

                if (isModePickerVisible) {
                    return@onPreviewKeyEvent handleModePickerKeyEvent(
                        event = keyEvent,
                        selectedIndex = modePickerIndex,
                        onSelectedIndexChange = { modePickerIndex = it },
                        onChooseMode = { mode ->
                            transform = transform.copy(currentMode = mode)
                            isModePickerVisible = false
                            isOverlayVisible = true
                            miniHudPulse += 1
                        },
                        onClose = {
                            isModePickerVisible = false
                            refocusTraceScreen()
                        },
                    )
                }

                if (handleProjectionBlankKey(keyEvent)) {
                    return@onPreviewKeyEvent true
                }

                val nativeKeyCode = keyEvent.nativeKeyEvent.keyCode
                if (!isOverlayVisible && nativeKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    val nativeEvent = keyEvent.nativeKeyEvent
                    if (!isDirectControlActive && nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                        isOverlayVisible = true
                        isProjectionBlanked = false
                        menuPanel = TraceMenuPanel.Main
                        menuPanelHistory = emptyList()
                        menuSelectedIndex = 0
                        return@onPreviewKeyEvent true
                    }
                }

                if (isOverlayVisible && !isDirectControlActive) {
                    return@onPreviewKeyEvent handleTraceMenuKeyEvent(
                        event = keyEvent,
                        panel = menuPanel,
                        selectedIndex = menuSelectedIndex,
                        transform = transform,
                        hasImage = selectedImageUri != null,
                        imageLabel = selectedImageLabel,
                        imageSizeLabel = selectedImageSizeLabel,
                        hasSavedProfile = hasSavedProfile,
                        menuSize = menuSize,
                        hudDuration = hudDuration,
                        menuAutoHide = menuAutoHide,
                        projectionBlankEnabled = projectionBlankEnabled,
                        projectionBlankMode = projectionBlankMode,
                        keepScreenAwake = keepScreenAwake,
                        projectorOrientation = projectorOrientation,
                        onSelectedIndexChange = { menuSelectedIndex = it },
                        onPanelChange = {
                            if (menuPanelHistory.isNotEmpty()) {
                                menuPanel = menuPanelHistory.last()
                                menuPanelHistory = menuPanelHistory.dropLast(1)
                            } else {
                                menuPanel = it
                            }
                            menuSelectedIndex = 0
                        },
                        onCloseMenu = {
                            isOverlayVisible = false
                            menuPanelHistory = emptyList()
                            miniHudPulse += 1
                            refocusTraceScreen()
                        },
                        onSelectEntry = { entry ->
                            when {
                                entry.opensPanel != null -> {
                                    menuPanelHistory = menuPanelHistory + menuPanel
                                    menuPanel = entry.opensPanel
                                    menuSelectedIndex = 0
                                }

                                entry.mode != null -> {
                                    transform = transform.copy(currentMode = entry.mode)
                                    isDirectControlActive = true
                                    isOverlayVisible = entry.mode == TraceMode.MenuMove
                                    miniHudPulse += 1
                                    refocusTraceScreen()
                                }

                                entry.action != null -> {
                                    when (entry.action) {
                                        TraceMenuAction.OpenImage -> {
                                            isOverlayVisible = false
                                            menuPanelHistory = emptyList()
                                            openImagePickerOrLibrary()
                                        }

                                        TraceMenuAction.OpenLibrary -> {
                                            isOverlayVisible = false
                                            menuPanelHistory = emptyList()
                                            openImageLibrary()
                                        }

                                        TraceMenuAction.SaveProfile -> {
                                            context.saveProfile(transform)
                                            hasSavedProfile = true
                                            imageNotice = "Profile saved"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.LoadProfile -> {
                                            val savedProfile = context.loadProfile()
                                            if (savedProfile != null) {
                                                transform = savedProfile.copy(currentMode = transform.currentMode)
                                                imageNotice = "Profile loaded"
                                            } else {
                                                imageNotice = "No saved profile"
                                            }
                                            hasSavedProfile = savedProfile != null
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ClearProfile -> {
                                            context.clearProfile()
                                            hasSavedProfile = false
                                            imageNotice = "Profile cleared"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.CloseImage -> {
                                            selectedImageUri?.let { context.releaseReadPermission(it) }
                                            selectedImageUri = null
                                            shouldRememberSelectedImage = false
                                            imageNotice = "Image closed"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.QuitApp -> {
                                            (context as? Activity)?.finishAndRemoveTask()
                                                ?: (context as? Activity)?.finish()
                                        }

                                        TraceMenuAction.ToggleFlipHorizontal -> {
                                            transform = transform.copy(isFlippedHorizontal = !transform.isFlippedHorizontal)
                                            imageNotice = if (transform.isFlippedHorizontal) "Flip horizontal on" else "Flip horizontal off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleFlipVertical -> {
                                            transform = transform.copy(isFlippedVertical = !transform.isFlippedVertical)
                                            imageNotice = if (transform.isFlippedVertical) "Flip vertical on" else "Flip vertical off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ResetCornerPin -> {
                                            transform = transform.copy(
                                                selectedCorner = DistortCorner.TopLeft,
                                                topLeft = CornerOffset(),
                                                topRight = CornerOffset(),
                                                bottomRight = CornerOffset(),
                                                bottomLeft = CornerOffset(),
                                            )
                                            imageNotice = "Corner pin reset"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ResetImage -> {
                                            transform = transform.resetImageAdjustments()
                                            imageNotice = "Image reset"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ResetTransform -> {
                                            transform = transform.resetTransformOnly()
                                            imageNotice = "Transform reset"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleLock -> {
                                            transform = transform.copy(isLocked = !transform.isLocked)
                                            imageNotice = if (transform.isLocked) "Transform locked" else "Transform unlocked"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ResetGuides -> {
                                            transform = transform.resetGuides()
                                            imageNotice = "Guides reset"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ResetFilters -> {
                                            transform = transform.resetFilters()
                                            imageNotice = "Filter reset"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ResetAll -> {
                                            transform = transform.resetAllSettings()
                                            imageNotice = "Settings reset"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleGrid -> {
                                            transform = transform.copy(isGridVisible = !transform.isGridVisible)
                                            imageNotice = if (transform.isGridVisible) {
                                                "Grid on: ${transform.gridSpacingPx.roundToInt()} px"
                                            } else {
                                                "Grid off"
                                            }
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.CycleGridColor -> {
                                            val nextColorMode = transform.gridColorMode.next()
                                            transform = transform.copy(
                                                isGridVisible = true,
                                                gridColorMode = nextColorMode,
                                            )
                                            imageNotice = "Grid color: ${nextColorMode.label()}"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleRulers -> {
                                            transform = transform.copy(isRulersVisible = !transform.isRulersVisible)
                                            imageNotice = if (transform.isRulersVisible) "Rulers on" else "Rulers off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleCenterCross -> {
                                            transform = transform.copy(
                                                isRulersVisible = true,
                                                isCenterCrossVisible = !transform.isCenterCrossVisible,
                                            )
                                            imageNotice = if (transform.isCenterCrossVisible) "Center cross on" else "Center cross off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleCanvasFrame -> {
                                            transform = transform.copy(isCanvasFrameVisible = !transform.isCanvasFrameVisible)
                                            imageNotice = if (transform.isCanvasFrameVisible) "Canvas frame on" else "Canvas frame off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleCrop -> {
                                            transform = transform.copy(isCropEnabled = !transform.isCropEnabled)
                                            imageNotice = if (transform.isCropEnabled) "Crop on" else "Crop off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ResetCrop -> {
                                            transform = transform.copy(
                                                isCropEnabled = false,
                                                selectedCropEdge = CropEdge.Left,
                                                selectedCropCorner = CropCorner.TopLeft,
                                                cropLeft = 0f,
                                                cropTop = 0f,
                                                cropRight = 0f,
                                                cropBottom = 0f,
                                            )
                                            imageNotice = "Crop reset"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.OpenOverlay -> {
                                            isOverlayVisible = false
                                            menuPanelHistory = emptyList()
                                            openOverlayPicker()
                                        }

                                        TraceMenuAction.ToggleOverlay -> {
                                            transform = transform.copy(isOverlayVisible = !transform.isOverlayVisible)
                                            imageNotice = if (transform.isOverlayVisible) "Overlay on" else "Overlay off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleCorners -> {
                                            transform = transform.copy(isGuideFrameVisible = !transform.isGuideFrameVisible)
                                            imageNotice = if (transform.isGuideFrameVisible) "Corners on" else "Corners off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleShapes -> {
                                            val nextShape = if (transform.guideShape == GuideShape.Off) {
                                                GuideShape.Thirds
                                            } else {
                                                GuideShape.Off
                                            }
                                            transform = transform.copy(guideShape = nextShape)
                                            imageNotice = if (nextShape == GuideShape.Off) {
                                                "Shapes off"
                                            } else {
                                                "Shapes on: ${transform.copy(guideShape = nextShape).guideShapeLabel()}"
                                            }
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleColorPalette -> {
                                            val nextCount = if (transform.colorPaletteCount > 0) 0 else 8
                                            transform = transform.copy(colorPaletteCount = nextCount)
                                            imageNotice = if (nextCount > 0) "Palette on: $nextCount colors" else "Palette off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleInvert -> {
                                            transform = transform.copy(
                                                isInverted = !transform.isInverted,
                                                tracingPreset = TracingPreset.Custom,
                                            )
                                            imageNotice = if (transform.isInverted) "Invert on" else "Invert off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleThreshold -> {
                                            transform = transform.copy(
                                                isThresholdEnabled = !transform.isThresholdEnabled,
                                                tracingPreset = TracingPreset.Custom,
                                            )
                                            imageNotice = if (transform.isThresholdEnabled) {
                                                "Threshold on: ${(transform.threshold * 100f).roundToInt()}%"
                                            } else {
                                                "Threshold off"
                                            }
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.TogglePaint -> {
                                            val nextPaint = if (transform.paintDetail > 0f) 0f else 0.5f
                                            transform = transform.copy(
                                                isThresholdEnabled = false,
                                                paintDetail = nextPaint,
                                                tracingPreset = TracingPreset.Custom,
                                            )
                                            imageNotice = if (nextPaint > 0f) "Paint on: 50%" else "Paint off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.TogglePosterize -> {
                                            val nextPosterize = if (transform.posterize > 0f) 0f else 0.5f
                                            transform = transform.copy(
                                                isThresholdEnabled = false,
                                                posterize = nextPosterize,
                                                tracingPreset = TracingPreset.Custom,
                                            )
                                            imageNotice = if (nextPosterize > 0f) "Posterize on: 50%" else "Posterize off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleLines -> {
                                            val nextLines = if (transform.lineArt > 0f) 0f else 0.5f
                                            transform = transform.copy(
                                                lineArt = nextLines,
                                                tracingPreset = TracingPreset.Custom,
                                            )
                                            imageNotice = if (nextLines > 0f) "Lines on: 50%" else "Lines off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleVolume -> {
                                            val nextVolume = if (transform.volume > 0f) 0f else 0.5f
                                            transform = transform.copy(
                                                isThresholdEnabled = false,
                                                volume = nextVolume,
                                                tracingPreset = TracingPreset.Custom,
                                            )
                                            imageNotice = if (nextVolume > 0f) "Blur on: 50%" else "Blur off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleMagicOutline -> {
                                            val nextStrength = if (transform.magicOutlineStrength > 0f) 0f else 1f
                                            transform = transform.copy(
                                                magicOutlineStrength = nextStrength,
                                                tracingPreset = TracingPreset.Custom,
                                            )
                                            imageNotice = if (nextStrength > 0f) "Magic outline on" else "Magic outline off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleEdgeOutline -> {
                                            val nextStrength = if (transform.edgeOutlineStrength > 0f) 0f else 0.55f
                                            transform = transform.copy(
                                                edgeOutlineStrength = nextStrength,
                                                tracingPreset = TracingPreset.Custom,
                                            )
                                            imageNotice = if (nextStrength > 0f) "Edge outline on: 55%" else "Edge outline off"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleBrightness -> {
                                            transform = transform.copy(
                                                isBrightnessEnabled = !transform.isBrightnessEnabled,
                                                tracingPreset = TracingPreset.Custom,
                                            )
                                            imageNotice = if (transform.isBrightnessEnabled) {
                                                "Brightness on: ${transform.brightnessPercent()}%"
                                            } else {
                                                "Brightness off"
                                            }
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.CycleMenuSize -> {
                                            val nextSize = menuSize.next()
                                            menuSize = nextSize
                                            imageNotice = "Menu size: ${nextSize.label()}"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.CycleHudDuration -> {
                                            val nextDuration = hudDuration.next()
                                            hudDuration = nextDuration
                                            imageNotice = "Info popup: ${nextDuration.label()}"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.CycleMenuAutoHide -> {
                                            val nextAutoHide = menuAutoHide.next()
                                            menuAutoHide = nextAutoHide
                                            imageNotice = "Menu display: ${nextAutoHide.label()}"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.CycleRotateStep -> {
                                            val nextStep = transform.rotationStepDegrees.nextRotationStep()
                                            transform = transform.copy(rotationStepDegrees = nextStep)
                                            imageNotice = "Rotate step: ${nextStep.roundToInt()} deg"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleProjectionBlank -> {
                                            projectionBlankEnabled = !projectionBlankEnabled
                                            if (!projectionBlankEnabled) {
                                                isProjectionBlanked = false
                                            }
                                            imageNotice = if (projectionBlankEnabled) {
                                                "Projection blank on"
                                            } else {
                                                "Projection blank off"
                                            }
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.CycleProjectionBlankMode -> {
                                            projectionBlankMode = projectionBlankMode.next()
                                            isProjectionBlanked = false
                                            imageNotice = "Blank mode: ${projectionBlankMode.label()}"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.ToggleKeepScreenAwake -> {
                                            keepScreenAwake = !keepScreenAwake
                                            imageNotice = if (keepScreenAwake) {
                                                "Keep screen awake on"
                                            } else {
                                                "Keep screen awake off"
                                            }
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.CycleProjectorOrientation -> {
                                            val nextOrientation = projectorOrientation.next()
                                            projectorOrientation = nextOrientation
                                            imageNotice = "Projector mode: ${nextOrientation.label()}"
                                            miniHudPulse += 1
                                        }

                                        TraceMenuAction.Back -> {
                                            if (menuPanelHistory.isNotEmpty()) {
                                                menuPanel = menuPanelHistory.last()
                                                menuPanelHistory = menuPanelHistory.dropLast(1)
                                            } else {
                                                menuPanel = menuPanel.parentPanel()
                                            }
                                            menuSelectedIndex = 0
                                        }

                                        TraceMenuAction.Placeholder -> {
                                            imageNotice = "${entry.title} will be added next"
                                            miniHudPulse += 1
                                        }
                                    }
                                }
                            }
                        },
                    )
                }

                handleRemoteKeyEvent(
                    event = keyEvent,
                    transform = transform,
                    isOverlayVisible = !isDirectControlActive,
                    onTransformChange = {
                        it.liveValueOverlayText()
                            ?.takeIf { label -> isDirectControlActive && it != transform }
                            ?.let { label ->
                                valueOverlayText = label
                                valueOverlayPulse += 1
                            }
                        transform = it
                        miniHudPulse += 1
                    },
                    onConfirmDirectControl = {
                        if (isDirectControlActive) {
                            isDirectControlActive = false
                            isOverlayVisible = true
                            isProjectionBlanked = false
                            miniHudPulse += 1
                            refocusTraceScreen()
                        }
                    },
                    onToggleOverlay = {
                        val wasDirectControlActive = isDirectControlActive
                        isModePickerVisible = false
                        if (wasDirectControlActive) {
                            isDirectControlActive = false
                            isOverlayVisible = true
                            isProjectionBlanked = false
                        } else {
                            val nextOverlayVisible = !isOverlayVisible
                            isOverlayVisible = nextOverlayVisible
                            if (nextOverlayVisible) {
                                isProjectionBlanked = false
                                transform = transform.copy(currentMode = TraceMode.Move)
                                menuPanel = TraceMenuPanel.Main
                                menuPanelHistory = emptyList()
                                menuSelectedIndex = 0
                            }
                        }
                        miniHudPulse += 1
                    },
                    onOpenModePicker = {},
                    onOpenImage = { openImagePickerOrLibrary() },
                    onOpenFolder = { openFolderPickerOrLibrary() },
                    onOpenLibrary = { openImageLibrary() },
                    onSetupAction = { keyCode ->
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                transform = transform.resetAllSettings()
                                imageNotice = "Image settings reset"
                                miniHudPulse += 1
                            }

                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                transform = transform.resetFilters()
                                imageNotice = "Filters reset"
                                miniHudPulse += 1
                            }

                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                selectedImageUri?.let { context.releaseReadPermission(it) }
                                selectedImageUri = null
                                shouldRememberSelectedImage = false
                                imageNotice = "Image cleared"
                                miniHudPulse += 1
                            }

                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                selectedFolderUri?.let { context.releaseReadPermission(it) }
                                selectedFolderUri = null
                                imageLibraryItems = emptyList()
                                imageLibraryIndex = 0
                                imageNotice = "Folder cleared"
                                miniHudPulse += 1
                            }
                        }
                    },
                    okLongPressHandled = okLongPressHandled,
                    onOkLongPressHandledChange = { okLongPressHandled = it },
                    menuLongPressHandled = menuLongPressHandled,
                    onMenuLongPressHandledChange = { menuLongPressHandled = it },
                )
            }
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        val imageViewportModifier = Modifier.fillMaxSize()
        val density = LocalDensity.current
        val usesRotatedProjection = projectorOrientation.isSideways()
        val projectorSurfaceWidth = if (usesRotatedProjection) maxHeight else maxWidth
        val projectorSurfaceHeight = if (usesRotatedProjection) maxWidth else maxHeight
        val isProjectorPortrait = projectorSurfaceHeight > projectorSurfaceWidth
        val isCompactOverlay = isProjectorPortrait || projectorSurfaceWidth < 840.dp
        val overlayAlignment = Alignment.CenterStart
        val menuOffset = remember(
            transform.menuOffsetX,
            transform.menuOffsetY,
            menuSize,
            isCompactOverlay,
            projectorSurfaceWidth,
            density,
        ) {
            with(density) {
                val menuWidthPx = menuSize.panelWidth(isCompactOverlay).toPx()
                val projectionWidthPx = projectorSurfaceWidth.toPx()
                val maxX = (projectionWidthPx - menuWidthPx).coerceAtLeast(0f)
                Offset(
                    x = transform.menuOffsetX.coerceIn(0f, maxX),
                    y = 0f,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(projectorSurfaceWidth)
                .height(projectorSurfaceHeight)
                .graphicsLayer {
                    rotationZ = projectorOrientation.rotationDegrees()
                    transformOrigin = TransformOrigin.Center
                },
        ) {
            Box(modifier = imageViewportModifier) {
                if (selectedImageUri == null) {
                    EmptyImageField(
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SelectedImage(
                        uri = selectedImageUri,
                        overlayUri = selectedOverlayUri,
                        transform = transform,
                        blinkCompareStep = if (projectionBlankEnabled) blinkCompareStep else BlinkCompareStep.Filtered,
                        showGuides = !isOverlayVisible && (
                            transform.currentMode == TraceMode.Corner ||
                                transform.currentMode == TraceMode.Distort
                            ),
                        onImageLoadFailed = {
                            imageNotice = "Image unavailable. Reconnect USB or open another image."
                            selectedImageUri = null
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                CenterRulersOverlay(
                    transform = transform,
                    modifier = Modifier.fillMaxSize(),
                )

                if (selectedImageUri == null) {
                    CanvasFrameOverlay(
                        transform = transform,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (transform.isLocked) {
                    TraceLockBadge(
                        isCompact = isCompactOverlay,
                        modifier = Modifier.align(if (isProjectorPortrait) Alignment.TopCenter else Alignment.TopEnd),
                    )
                }
            }

            AnimatedVisibility(
                visible = isOverlayVisible,
                enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = 170),
                    initialOffsetX = { -it },
                ) + fadeIn(animationSpec = tween(durationMillis = 120)),
                exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = 140),
                    targetOffsetX = { -it },
                ) + fadeOut(animationSpec = tween(durationMillis = 100)),
                modifier = Modifier
                    .align(overlayAlignment)
                    .graphicsLayer {
                        translationX = menuOffset.x
                        translationY = menuOffset.y
                    },
            ) {
                TraceOverlay(
                    transform = transform,
                    hasImage = selectedImageUri != null,
                    imageLabel = selectedImageLabel,
                    imageSizeLabel = selectedImageSizeLabel,
                    imageNotice = imageNotice,
                    menuPanel = menuPanel,
                    hasSavedProfile = hasSavedProfile,
                    menuSize = menuSize,
                    hudDuration = hudDuration,
                    menuAutoHide = menuAutoHide,
                    projectionBlankEnabled = projectionBlankEnabled,
                    projectionBlankMode = projectionBlankMode,
                    keepScreenAwake = keepScreenAwake,
                    projectorOrientation = projectorOrientation,
                    selectedIndex = menuSelectedIndex,
                    isCompact = isCompactOverlay,
                    modifier = Modifier,
                )
            }

            if (isImageLibraryVisible) {
                TraceImageLibrary(
                    items = imageLibraryItems,
                    selectedIndex = imageLibraryIndex,
                    message = imageLibraryMessage,
                    isCompact = isCompactOverlay,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            ValueChangeOverlay(
                text = valueOverlayText,
                isCompact = isCompactOverlay,
                modifier = Modifier.align(Alignment.TopEnd),
            )

            if (isModePickerVisible) {
                TraceModePicker(
                    selectedMode = ModePickerModes[modePickerIndex.coerceIn(0, ModePickerModes.lastIndex)],
                    currentMode = transform.currentMode,
                    isCompact = isCompactOverlay,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            AnimatedVisibility(
                visible = isLaunchSplashVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 120)),
                exit = fadeOut(animationSpec = tween(durationMillis = 650)),
                modifier = Modifier.fillMaxSize(),
            ) {
                LaunchSplash(isCompact = isCompactOverlay)
            }
        }

        if (isProjectionBlanked) {
            ProjectionBlankOverlay(
                color = if (projectionBlankMode == ProjectionBlankMode.White) Color.White else Color.Black,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
@Composable
private fun LaunchSplash(
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_projector_trace),
                contentDescription = null,
                modifier = Modifier.size(if (isCompact) 132.dp else 172.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = "Projector Trace",
                color = Color(0xFF111318),
                fontSize = if (isCompact) 30.sp else 42.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = "Professional Art Projector Utility",
                color = Color(0xFF4B5563),
                fontSize = if (isCompact) 14.sp else 18.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "by S.Yemelin  |  V${com.projectortrace.BuildConfig.VERSION_NAME}",
                color = Color(0xFF6B7280),
                fontSize = if (isCompact) 12.sp else 14.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun GridGuideOverlay(
    transform: TransformState,
    modifier: Modifier = Modifier,
) {
    if (!transform.isGridVisible) return

    Canvas(modifier = modifier) {
        val spacing = transform.gridSpacingPx.coerceIn(MinGridSpacingPx, MaxGridSpacingPx)
        val color = when (transform.gridColorMode) {
            GridColorMode.White -> Color(0x70FFFFFF)
            GridColorMode.Black -> Color(0x99000000)
        }
        val stroke = 1.2f

        var x = spacing
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), stroke)
            x += spacing
        }

        var y = spacing
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
            y += spacing
        }
    }
}

@Composable
private fun CenterRulersOverlay(
    transform: TransformState,
    modifier: Modifier = Modifier,
) {
    if (!transform.isRulersVisible) return

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val lineColor = Color(0x99F4F7FB)
        val crossColor = Color(0xD8FFD166)

        drawLine(lineColor, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 1.6f)
        drawLine(lineColor, Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 1.6f)

        if (transform.isCenterCrossVisible) {
            val arm = min(size.width, size.height) * 0.045f
            drawLine(crossColor, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), strokeWidth = 3.2f)
            drawLine(crossColor, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), strokeWidth = 3.2f)
            drawCircle(crossColor, radius = 4.5f, center = center)
        }
    }
}

@Composable
private fun CanvasFrameOverlay(
    transform: TransformState,
    modifier: Modifier = Modifier,
) {
    if (!transform.isCanvasFrameVisible) return

    Canvas(modifier = modifier) {
        val targetAspect = transform.canvasFrameAspectRatio()
        val viewportAspect = size.width / size.height
        val frameWidth: Float
        val frameHeight: Float

        if (viewportAspect > targetAspect) {
            frameHeight = size.height * 0.88f
            frameWidth = frameHeight * targetAspect
        } else {
            frameWidth = size.width * 0.88f
            frameHeight = frameWidth / targetAspect
        }

        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f
        val active = transform.currentMode == TraceMode.CanvasFrame ||
            transform.currentMode == TraceMode.CanvasFrameRatio ||
            transform.currentMode == TraceMode.CanvasFrameColor
        val color = transform.canvasFrameColor.composeCornerColor(alpha = if (active) 0.92f else 0.72f)

        drawRect(
            color = color,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
            style = Stroke(width = 3f),
        )
    }
}

@Composable
private fun CropGuideOverlay(
    transform: TransformState,
    modifier: Modifier = Modifier,
) {
    val isCropControlActive = transform.currentMode == TraceMode.CropEdge || transform.currentMode == TraceMode.CropAdjust
    if (!transform.isCropEnabled && !isCropControlActive) return

    Canvas(modifier = modifier) {
        val left = size.width * transform.cropLeft.coerceIn(0f, 0.45f)
        val top = size.height * transform.cropTop.coerceIn(0f, 0.45f)
        val right = size.width * (1f - transform.cropRight.coerceIn(0f, 0.45f))
        val bottom = size.height * (1f - transform.cropBottom.coerceIn(0f, 0.45f))
        val edgeColor = Color(0xFFFFD166)
        val dimColor = Color(0x55000000)
        val inactiveColor = Color(0x99F4F7FB)

        if (top > 0f) {
            drawRect(dimColor, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, top))
        }
        if (bottom < size.height) {
            drawRect(dimColor, topLeft = Offset(0f, bottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - bottom))
        }
        if (left > 0f) {
            drawRect(dimColor, topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(left, bottom - top))
        }
        if (right < size.width) {
            drawRect(dimColor, topLeft = Offset(right, top), size = androidx.compose.ui.geometry.Size(size.width - right, bottom - top))
        }

        drawRect(
            color = if (transform.currentMode == TraceMode.CropAdjust) inactiveColor else edgeColor,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size((right - left).coerceAtLeast(1f), (bottom - top).coerceAtLeast(1f)),
            style = Stroke(width = if (transform.currentMode == TraceMode.CropAdjust) 4f else 2.4f),
        )

        if (transform.currentMode == TraceMode.CropEdge || transform.currentMode == TraceMode.CropAdjust) {
            val selectedStroke = if (transform.currentMode == TraceMode.CropAdjust) 6f else 4f
            when (transform.selectedCropEdge) {
                CropEdge.Left -> drawLine(edgeColor, Offset(left, top), Offset(left, bottom), strokeWidth = selectedStroke)
                CropEdge.Top -> drawLine(edgeColor, Offset(left, top), Offset(right, top), strokeWidth = selectedStroke)
                CropEdge.Right -> drawLine(edgeColor, Offset(right, top), Offset(right, bottom), strokeWidth = selectedStroke)
                CropEdge.Bottom -> drawLine(edgeColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = selectedStroke)
            }
        }
    }
}

@Composable
private fun AlignmentFrame(
    transform: TransformState,
    modifier: Modifier = Modifier,
) {
    if (!transform.isGuideFrameVisible) return

    Canvas(modifier = modifier) {
        val insetX = transform.guideInsetX
            .coerceIn(0f, (size.width / 2f - 24f).coerceAtLeast(0f))
        val insetY = transform.guideInsetY
            .coerceIn(0f, (size.height / 2f - 24f).coerceAtLeast(0f))
        val cornerLength = (min(size.width, size.height) * 0.08f).coerceIn(44f, 128f)
        val isFrameMode = transform.currentMode == TraceMode.Frame ||
            transform.currentMode == TraceMode.Guide ||
            transform.currentMode == TraceMode.GuideCorner ||
            transform.currentMode == TraceMode.GuideCornerColor ||
            transform.currentMode == TraceMode.GuideCornerMove
        val strokeWidth = if (isFrameMode) 5f else 3f
        val color = transform.guideCornerColor.composeCornerColor(alpha = if (isFrameMode) 0.92f else 0.72f)
        val inactiveColor = transform.guideCornerColor.composeCornerColor(alpha = if (isFrameMode) 0.46f else 0.72f)
        val topLeft = Offset(insetX, insetY) + Offset(transform.guideTopLeft.x, transform.guideTopLeft.y)
        val topRight = Offset(size.width - insetX, insetY) + Offset(transform.guideTopRight.x, transform.guideTopRight.y)
        val bottomRight = Offset(size.width - insetX, size.height - insetY) + Offset(transform.guideBottomRight.x, transform.guideBottomRight.y)
        val bottomLeft = Offset(insetX, size.height - insetY) + Offset(transform.guideBottomLeft.x, transform.guideBottomLeft.y)

        drawGuideCorner(
            corner = topLeft,
            horizontalEnd = topLeft + Offset(cornerLength, 0f),
            verticalEnd = topLeft + Offset(0f, cornerLength),
            color = if (transform.guideSelectedCorner == DistortCorner.TopLeft) color else inactiveColor,
            strokeWidth = if (transform.guideSelectedCorner == DistortCorner.TopLeft) strokeWidth * 1.25f else strokeWidth,
        )
        drawGuideCorner(
            corner = topRight,
            horizontalEnd = topRight + Offset(-cornerLength, 0f),
            verticalEnd = topRight + Offset(0f, cornerLength),
            color = if (transform.guideSelectedCorner == DistortCorner.TopRight) color else inactiveColor,
            strokeWidth = if (transform.guideSelectedCorner == DistortCorner.TopRight) strokeWidth * 1.25f else strokeWidth,
        )
        drawGuideCorner(
            corner = bottomRight,
            horizontalEnd = bottomRight + Offset(-cornerLength, 0f),
            verticalEnd = bottomRight + Offset(0f, -cornerLength),
            color = if (transform.guideSelectedCorner == DistortCorner.BottomRight) color else inactiveColor,
            strokeWidth = if (transform.guideSelectedCorner == DistortCorner.BottomRight) strokeWidth * 1.25f else strokeWidth,
        )
        drawGuideCorner(
            corner = bottomLeft,
            horizontalEnd = bottomLeft + Offset(cornerLength, 0f),
            verticalEnd = bottomLeft + Offset(0f, -cornerLength),
            color = if (transform.guideSelectedCorner == DistortCorner.BottomLeft) color else inactiveColor,
            strokeWidth = if (transform.guideSelectedCorner == DistortCorner.BottomLeft) strokeWidth * 1.25f else strokeWidth,
        )
    }
}

@Composable
private fun ShapeGuideOverlay(
    transform: TransformState,
    modifier: Modifier = Modifier,
) {
    if (transform.guideShape == GuideShape.Off) return

    Canvas(modifier = modifier) {
        val active = transform.currentMode == TraceMode.Shape ||
            transform.currentMode == TraceMode.ShapeColor ||
            transform.currentMode == TraceMode.ShapeRotation
        val lineColor = transform.guideShapeColor.composeShapeColor(alpha = if (active) 0.92f else 0.72f)
        val accentColor = transform.guideShapeColor.composeShapeColor(alpha = if (active) 0.72f else 0.48f)
        val stroke = if (active) 3f else 2f

        rotate(degrees = transform.guideShapeRotationDegrees, pivot = Offset(size.width / 2f, size.height / 2f)) {
            when (transform.guideShape) {
                GuideShape.Off -> Unit
                GuideShape.Thirds -> {
                    drawLine(lineColor, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), stroke)
                    drawLine(lineColor, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), stroke)
                    drawLine(lineColor, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), stroke)
                    drawLine(lineColor, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), stroke)
                }
                GuideShape.ThirdsDense -> {
                    for (i in 1..5) {
                        val x = size.width * i / 6f
                        val y = size.height * i / 6f
                        drawLine(if (i == 2 || i == 4) lineColor else accentColor, Offset(x, 0f), Offset(x, size.height), stroke)
                        drawLine(if (i == 2 || i == 4) lineColor else accentColor, Offset(0f, y), Offset(size.width, y), stroke)
                    }
                }
                GuideShape.GoldenRatio -> {
                    val a = 0.382f
                    val b = 0.618f
                    drawLine(lineColor, Offset(size.width * a, 0f), Offset(size.width * a, size.height), stroke)
                    drawLine(lineColor, Offset(size.width * b, 0f), Offset(size.width * b, size.height), stroke)
                    drawLine(lineColor, Offset(0f, size.height * a), Offset(size.width, size.height * a), stroke)
                    drawLine(lineColor, Offset(0f, size.height * b), Offset(size.width, size.height * b), stroke)
                    drawCircle(accentColor, radius = min(size.width, size.height) * 0.19f, center = Offset(size.width * a, size.height * a), style = Stroke(width = stroke))
                    drawCircle(accentColor, radius = min(size.width, size.height) * 0.31f, center = Offset(size.width * b, size.height * b), style = Stroke(width = stroke))
                }
                GuideShape.GoldenSpiral -> {
                    val a = 0.382f
                    val b = 0.618f
                    drawLine(lineColor, Offset(size.width * b, 0f), Offset(size.width * b, size.height), stroke)
                    drawLine(lineColor, Offset(size.width * b, size.height * b), Offset(size.width, size.height * b), stroke)
                    drawLine(lineColor, Offset(size.width * a, 0f), Offset(size.width * a, size.height * a), stroke)
                    drawLine(lineColor, Offset(0f, size.height * a), Offset(size.width * a, size.height * a), stroke)
                    val base = min(size.width, size.height)
                    listOf(0.34f, 0.22f, 0.14f, 0.09f).forEachIndexed { index, radius ->
                        drawCircle(accentColor, radius = base * radius, center = Offset(size.width * (0.62f - index * 0.08f), size.height * (0.62f - index * 0.07f)), style = Stroke(width = stroke))
                    }
                }
                GuideShape.Diagonal -> {
                    drawLine(lineColor, Offset.Zero, Offset(size.width, size.height), stroke)
                    drawLine(lineColor, Offset(size.width, 0f), Offset(0f, size.height), stroke)
                    drawLine(accentColor, Offset(size.width / 2f, 0f), Offset(size.width, size.height / 2f), stroke)
                    drawLine(accentColor, Offset(0f, size.height / 2f), Offset(size.width / 2f, size.height), stroke)
                }
                GuideShape.DynamicSymmetry -> {
                    drawLine(lineColor, Offset.Zero, Offset(size.width, size.height), stroke)
                    drawLine(lineColor, Offset(size.width, 0f), Offset(0f, size.height), stroke)
                    drawLine(accentColor, Offset(size.width * 0.25f, 0f), Offset(size.width, size.height * 0.75f), stroke)
                    drawLine(accentColor, Offset(0f, size.height * 0.25f), Offset(size.width * 0.75f, size.height), stroke)
                    drawLine(accentColor, Offset(size.width * 0.75f, 0f), Offset(0f, size.height * 0.75f), stroke)
                    drawLine(accentColor, Offset(size.width, size.height * 0.25f), Offset(size.width * 0.25f, size.height), stroke)
                }
                GuideShape.Triangle -> {
                    drawLine(lineColor, Offset.Zero, Offset(size.width, size.height), stroke)
                    drawLine(lineColor, Offset(size.width, 0f), Offset(0f, size.height), stroke)
                    drawLine(accentColor, Offset(size.width / 2f, 0f), Offset(0f, size.height), stroke)
                    drawLine(accentColor, Offset(size.width / 2f, 0f), Offset(size.width, size.height), stroke)
                }
                GuideShape.CenterCross -> {
                    drawLine(lineColor, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), stroke)
                    drawLine(lineColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), stroke)
                    drawCircle(accentColor, radius = min(size.width, size.height) * 0.18f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(width = stroke))
                }
            }
        }
    }
}

@Composable
private fun EmptyImageField(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color(0xFF050608)))
}

@Composable
private fun ProjectionBlankOverlay(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(color))
}

@Composable
private fun ValueChangeOverlay(
    text: String?,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn(animationSpec = tween(durationMillis = 80)),
        exit = fadeOut(animationSpec = tween(durationMillis = 180)),
        modifier = modifier.padding(top = if (isCompact) 18.dp else 28.dp, end = if (isCompact) 20.dp else 34.dp),
    ) {
        Text(
            text = text.orEmpty(),
            color = Color.White,
            fontSize = if (isCompact) 24.sp else 30.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

private sealed class ImageLoadResult {
    object Loading : ImageLoadResult()
    data class Loaded(val bitmap: Bitmap) : ImageLoadResult()
    object Failed : ImageLoadResult()
}

@Composable
private fun SelectedImage(
    uri: Uri?,
    overlayUri: Uri?,
    transform: TransformState,
    blinkCompareStep: BlinkCompareStep,
    showGuides: Boolean,
    onImageLoadFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val targetWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val targetHeightPx = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
        key(uri, targetWidthPx, targetHeightPx) {
            val imageLoadResult by produceState<ImageLoadResult>(
                initialValue = ImageLoadResult.Loading,
                key1 = uri,
                key2 = targetWidthPx,
                key3 = targetHeightPx,
            ) {
                value = ImageLoadResult.Loading
                val loadedBitmap = uri?.let {
                    loadScaledBitmap(
                        context = context,
                        uri = it,
                        targetWidthPx = targetWidthPx,
                        targetHeightPx = targetHeightPx,
                    )
                }
                value = if (loadedBitmap != null) {
                    ImageLoadResult.Loaded(loadedBitmap)
                } else {
                    ImageLoadResult.Failed
                }
            }
            val bitmap = (imageLoadResult as? ImageLoadResult.Loaded)?.bitmap
            val overlayBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = overlayUri,
                key2 = targetWidthPx,
                key3 = targetHeightPx,
            ) {
                value = null
                val loadedBitmap = overlayUri?.let {
                    loadScaledBitmap(
                        context = context,
                        uri = it,
                        targetWidthPx = targetWidthPx,
                        targetHeightPx = targetHeightPx,
                    )
                }
                value = loadedBitmap
            }

            LaunchedEffect(imageLoadResult) {
                if (imageLoadResult == ImageLoadResult.Failed) {
                    onImageLoadFailed()
                }
            }

            val lineArtBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = bitmap,
                key2 = transform.lineArt > 0f,
            ) {
                value = if (bitmap != null && transform.lineArt > 0f) {
                    createLineArtBitmap(bitmap)
                } else {
                    null
                }
            }
            val edgeOutlineBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = bitmap,
                key2 = EdgeOutlineRenderKey(
                    strength = if (transform.edgeOutlineStrength > 0f) 1f else 0f,
                    thickness = transform.edgeOutlineThickness,
                    detail = transform.edgeOutlineDetail,
                    smoothing = transform.edgeOutlineSmoothing,
                ),
            ) {
                value = if (bitmap != null && transform.edgeOutlineStrength > 0f) {
                    createEdgeOutlineBitmap(
                        bitmap = bitmap,
                        thickness = transform.edgeOutlineThickness,
                        detail = transform.edgeOutlineDetail,
                        smoothing = transform.edgeOutlineSmoothing,
                    )
                } else {
                    null
                }
            }
            val magicOutlineBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = bitmap,
                key2 = MagicOutlineRenderKey(
                    strength = if (transform.magicOutlineStrength > 0f) 1f else 0f,
                    detail = transform.magicOutlineDetail,
                    thickness = transform.magicOutlineThickness,
                    compareStep = blinkCompareStep,
                ),
            ) {
                value = if (
                    bitmap != null &&
                    (transform.magicOutlineStrength > 0f || blinkCompareStep == BlinkCompareStep.Outline)
                ) {
                    createMagicOutlineBitmap(
                        bitmap = bitmap,
                        detail = if (blinkCompareStep == BlinkCompareStep.Outline) 0.68f else transform.magicOutlineDetail,
                        thickness = if (blinkCompareStep == BlinkCompareStep.Outline) 0.42f else transform.magicOutlineThickness,
                    )
                } else {
                    null
                }
            }
            val clarityBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = bitmap,
                key2 = transform.clarity > 0f,
            ) {
                value = if (bitmap != null && transform.clarity > 0f) {
                    createClarityBitmap(bitmap)
                } else {
                    null
                }
            }
            val thresholdBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = bitmap,
                key2 = ThresholdRenderKey(
                    isEnabled = transform.isThresholdEnabled,
                    threshold = transform.threshold,
                    channelMixer = transform.channelMixer,
                ),
            ) {
                value = if (bitmap != null && transform.isThresholdEnabled) {
                    createThresholdBitmap(
                        bitmap = bitmap,
                        threshold = transform.threshold,
                        channelMixer = transform.channelMixer,
                    )
                } else {
                    null
                }
            }
            val paintBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = bitmap,
                key2 = PaintRenderKey(
                    isThresholdEnabled = transform.isThresholdEnabled,
                    detail = transform.paintDetail,
                ),
            ) {
                value = if (bitmap != null && !transform.isThresholdEnabled && transform.paintDetail > 0f) {
                    createPaintBitmap(
                        bitmap = bitmap,
                        detail = transform.paintDetail,
                    )
                } else {
                    null
                }
            }
            val posterizeBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = bitmap,
                key2 = PosterizeRenderKey(
                    isThresholdEnabled = transform.isThresholdEnabled,
                    paintDetail = transform.paintDetail,
                    volume = transform.volume,
                    strength = transform.posterize,
                ),
            ) {
                value = if (
                    bitmap != null &&
                    !transform.isThresholdEnabled &&
                    transform.paintDetail <= 0f &&
                    transform.volume <= 0f &&
                    transform.posterize > 0f
                ) {
                    createPosterizeBitmap(
                        bitmap = bitmap,
                        strength = transform.posterize,
                    )
                } else {
                    null
                }
            }
            val volumeBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = bitmap,
                key2 = VolumeRenderKey(
                    isThresholdEnabled = transform.isThresholdEnabled,
                    paintDetail = transform.paintDetail,
                    strength = transform.volume,
                ),
            ) {
                value = if (
                    bitmap != null &&
                    !transform.isThresholdEnabled &&
                    transform.paintDetail <= 0f &&
                    transform.volume > 0f
                ) {
                    createVolumeBitmap(
                        bitmap = bitmap,
                        strength = transform.volume,
                    )
                } else {
                    null
                }
            }
            val noiseReductionBitmap by produceState<Bitmap?>(
                initialValue = null,
                key1 = bitmap,
                key2 = NoiseReductionRenderKey(
                    isThresholdEnabled = transform.isThresholdEnabled,
                    paintDetail = transform.paintDetail,
                    volume = transform.volume,
                    posterize = transform.posterize,
                    strength = transform.noiseReduction,
                ),
            ) {
                value = if (
                    bitmap != null &&
                    !transform.isThresholdEnabled &&
                    transform.paintDetail <= 0f &&
                    transform.volume <= 0f &&
                    transform.posterize <= 0f &&
                    transform.noiseReduction > 0f
                ) {
                    createNoiseReductionBitmap(
                        bitmap = bitmap,
                        strength = transform.noiseReduction,
                    )
                } else {
                    null
                }
            }
            val paletteColors by produceState<List<Int>>(
                initialValue = emptyList(),
                key1 = bitmap,
                key2 = transform.colorPaletteCount,
                key3 = transform.colorPaletteMode,
            ) {
                value = if (bitmap != null && transform.colorPaletteCount > 0) {
                    extractColorPalette(
                        bitmap = bitmap,
                        colorCount = transform.colorPaletteCount,
                        mode = transform.colorPaletteMode,
                    )
                } else {
                    emptyList()
                }
            }
            if (imageLoadResult == ImageLoadResult.Loading) {
                ImageLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (bitmap != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = transform.scale * if (transform.isFlippedHorizontal) -1f else 1f
                            scaleY = transform.scale * if (transform.isFlippedVertical) -1f else 1f
                            translationX = transform.offsetX
                            translationY = transform.offsetY
                            rotationZ = transform.rotationDegrees
                            alpha = transform.opacity
                        },
                ) {
                    drawDistortedBitmap(
                        bitmap = bitmap,
                        transform = transform,
                        blinkCompareStep = blinkCompareStep,
                        thresholdBitmap = thresholdBitmap,
                        paintBitmap = paintBitmap,
                        volumeBitmap = volumeBitmap,
                        posterizeBitmap = posterizeBitmap,
                        noiseReductionBitmap = noiseReductionBitmap,
                        clarityBitmap = clarityBitmap,
                        lineArtBitmap = lineArtBitmap,
                        magicOutlineBitmap = magicOutlineBitmap,
                        edgeOutlineBitmap = edgeOutlineBitmap,
                        overlayBitmap = overlayBitmap,
                    )

                    if (showGuides) {
                        drawDistortGuides(
                            bitmapSize = IntSize(bitmap.width, bitmap.height),
                            transform = transform,
                        )
                    }
                }

                if (transform.hasAttachedCanvasGuides()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = transform.scale * if (transform.isFlippedHorizontal) -1f else 1f
                                scaleY = transform.scale * if (transform.isFlippedVertical) -1f else 1f
                                translationX = transform.offsetX
                                translationY = transform.offsetY
                                rotationZ = transform.rotationDegrees
                            },
                    ) {
                        drawAttachedCanvasGuides(
                            bitmapSize = IntSize(bitmap.width, bitmap.height),
                            transform = transform,
                        )
                    }
                }

                if (paletteColors.isNotEmpty()) {
                    ColorPaletteOverlay(
                        colors = paletteColors,
                        colorCount = transform.colorPaletteCount,
                        mode = transform.colorPaletteMode,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .graphicsLayer {
                                translationX = transform.colorPaletteOffsetX
                                translationY = transform.colorPaletteOffsetY
                                scaleX = transform.colorPaletteScale.coerceIn(MinPaletteScale, MaxPaletteScale)
                                scaleY = transform.colorPaletteScale.coerceIn(MinPaletteScale, MaxPaletteScale)
                                transformOrigin = TransformOrigin(1f, 1f)
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color(0xDD0D0F12))
            .border(width = 1.dp, color = Color(0xFF374151))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Loading image...",
            color = Color(0xFFE8EEF7),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ColorPaletteOverlay(
    colors: List<Int>,
    colorCount: Int,
    mode: ColorPaletteMode,
    modifier: Modifier = Modifier,
) {
    val columns = when {
        colorCount <= 4 -> colorCount.coerceAtLeast(2)
        colorCount <= 9 -> 3
        else -> 4
    }
    val rows = ((colors.size + columns - 1) / columns).coerceAtLeast(1)
    val swatchSize = 34.dp
    val gapPx = 8f
    val canvasWidth = swatchSize * columns + 8.dp * (columns - 1)
    val canvasHeight = swatchSize * rows + 8.dp * (rows - 1)

    Column(
        modifier = modifier
            .background(Color(0xD80B0C10))
            .border(width = 1.dp, color = Color(0x55FFFFFF))
            .padding(10.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "Paint Palette $colorCount",
            color = Color(0xFFE8EEF7),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = mode.label(),
            color = Color(0xFFB6C2D2),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
        Canvas(
            modifier = Modifier
                .width(canvasWidth)
                .height(canvasHeight),
        ) {
            val swatchPx = swatchSize.toPx()
            colors.forEachIndexed { index, colorInt ->
                val column = index % columns
                val row = index / columns
                val left = column * (swatchPx + gapPx)
                val top = row * (swatchPx + gapPx)
                drawRect(
                    color = Color(colorInt),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(swatchPx, swatchPx),
                )
                drawRect(
                    color = Color(0xAAFFFFFF),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(swatchPx, swatchPx),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
                )
            }
        }
    }
}

private fun handleRemoteKeyEvent(
    event: ComposeKeyEvent,
    transform: TransformState,
    isOverlayVisible: Boolean,
    onTransformChange: (TransformState) -> Unit,
    onConfirmDirectControl: () -> Unit,
    onToggleOverlay: () -> Unit,
    onOpenModePicker: () -> Unit,
    onOpenImage: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenLibrary: () -> Unit,
    onSetupAction: (Int) -> Unit,
    okLongPressHandled: Boolean,
    onOkLongPressHandledChange: (Boolean) -> Unit,
    menuLongPressHandled: Boolean,
    onMenuLongPressHandledChange: (Boolean) -> Unit,
): Boolean {
    val nativeEvent = event.nativeKeyEvent

    return when (nativeEvent.keyCode) {
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_SETTINGS -> {
            handleBackOrMenuKey(
                nativeEvent = nativeEvent,
                onToggleOverlay = onToggleOverlay,
                onOpenImage = onOpenImage,
                menuLongPressHandled = menuLongPressHandled,
                onMenuLongPressHandledChange = onMenuLongPressHandledChange,
            )
        }

        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
            handleOkKey(
                nativeEvent = nativeEvent,
                transform = transform,
                isOverlayVisible = isOverlayVisible,
                onTransformChange = onTransformChange,
                onConfirmDirectControl = onConfirmDirectControl,
                onOpenModePicker = onOpenModePicker,
                okLongPressHandled = okLongPressHandled,
                onOkLongPressHandledChange = onOkLongPressHandledChange,
            )
        }

        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        KeyEvent.KEYCODE_BUTTON_R2 -> {
            true
        }

        KeyEvent.KEYCODE_MEDIA_REWIND,
        KeyEvent.KEYCODE_BUTTON_L2 -> {
            true
        }

        KeyEvent.KEYCODE_CHANNEL_UP,
        KeyEvent.KEYCODE_PAGE_DOWN,
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_BUTTON_R1 -> {
            true
        }

        KeyEvent.KEYCODE_CHANNEL_DOWN,
        KeyEvent.KEYCODE_PAGE_UP,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_BUTTON_L1 -> {
            true
        }

        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN -> {
            if (isOverlayVisible) return true

            if (nativeEvent.action == KeyEvent.ACTION_DOWN) {
                if (transform.currentMode == TraceMode.Open) {
                    if (nativeEvent.repeatCount == 0) {
                        when (nativeEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP -> onOpenImage()
                            KeyEvent.KEYCODE_DPAD_LEFT -> onOpenFolder()
                            KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_DPAD_DOWN -> onOpenLibrary()
                        }
                    }
                } else if (transform.currentMode == TraceMode.Setup) {
                    if (nativeEvent.repeatCount == 0) {
                        onSetupAction(nativeEvent.keyCode)
                    }
                } else if (
                    !transform.shouldIgnoreRepeatedToggle(
                        keyCode = nativeEvent.keyCode,
                        repeatCount = nativeEvent.repeatCount,
                    )
                ) {
                    onTransformChange(transform.updateForDirection(nativeEvent.keyCode))
                }
            }
            true
        }

        else -> false
    }
}

private fun handleTraceMenuKeyEvent(
    event: ComposeKeyEvent,
    panel: TraceMenuPanel,
    selectedIndex: Int,
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
    onSelectedIndexChange: (Int) -> Unit,
    onPanelChange: (TraceMenuPanel) -> Unit,
    onCloseMenu: () -> Unit,
    onSelectEntry: (TraceMenuEntry) -> Unit,
): Boolean {
    val nativeEvent = event.nativeKeyEvent
    // Menu navigation follows the physical remote: right opens, left backs out, up/down scroll.
    val keyCode = nativeEvent.keyCode
    val entries = traceMenuEntries(
        panel = panel,
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

    fun goBack() {
        if (panel == TraceMenuPanel.Main) {
            onCloseMenu()
        } else {
            onPanelChange(panel.parentPanel())
        }
    }

    return when (keyCode) {
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_SETTINGS -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                goBack()
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_UP -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0 && entries.isNotEmpty()) {
                onSelectedIndexChange((selected - 1).floorMod(entries.size))
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_DOWN -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0 && entries.isNotEmpty()) {
                onSelectedIndexChange((selected + 1).floorMod(entries.size))
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_LEFT -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                goBack()
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                entries.getOrNull(selected)?.let(onSelectEntry)
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
            if (nativeEvent.action == KeyEvent.ACTION_UP) {
                entries.getOrNull(selected)?.let(onSelectEntry)
            }
            true
        }

        else -> true
    }
}

private fun handleModePickerKeyEvent(
    event: ComposeKeyEvent,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onChooseMode: (TraceMode) -> Unit,
    onClose: () -> Unit,
): Boolean {
    val nativeEvent = event.nativeKeyEvent
    // Picker navigation is UI navigation, not canvas movement, so it stays physical.
    val keyCode = nativeEvent.keyCode
    val lastIndex = ModePickerModes.lastIndex

    return when (keyCode) {
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_SETTINGS -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                onClose()
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
            if (nativeEvent.action == KeyEvent.ACTION_UP) {
                onChooseMode(ModePickerModes[selectedIndex.coerceIn(0, lastIndex)])
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_LEFT -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                onSelectedIndexChange((selectedIndex - 1).floorMod(ModePickerModes.size))
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                onSelectedIndexChange((selectedIndex + 1).floorMod(ModePickerModes.size))
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_UP -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                onSelectedIndexChange((selectedIndex - ModePickerColumns).floorMod(ModePickerModes.size))
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_DOWN -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                onSelectedIndexChange((selectedIndex + ModePickerColumns).floorMod(ModePickerModes.size))
            }
            true
        }

        else -> true
    }
}

private fun handleImageLibraryKeyEvent(
    event: ComposeKeyEvent,
    items: List<MediaImageItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onClose: () -> Unit,
    onChooseImage: (Uri) -> Unit,
    onRefresh: () -> Unit,
    okLongPressHandled: Boolean,
    onOkLongPressHandledChange: (Boolean) -> Unit,
): Boolean {
    val nativeEvent = event.nativeKeyEvent
    // Library navigation stays physical for predictable list/grid browsing.
    val keyCode = nativeEvent.keyCode

    return when (keyCode) {
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_SETTINGS -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
                onClose()
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
            when {
                nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount > 0 -> {
                    if (!okLongPressHandled) {
                        onRefresh()
                        onOkLongPressHandledChange(true)
                    }
                }

                nativeEvent.action == KeyEvent.ACTION_UP -> {
                    if (okLongPressHandled) {
                        onOkLongPressHandledChange(false)
                    } else {
                        items.getOrNull(selectedIndex)?.let { onChooseImage(it.uri) }
                    }
                }
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_LEFT -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0 && items.isNotEmpty()) {
                onSelectedIndexChange((selectedIndex - 1).floorMod(items.size))
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_RIGHT -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0 && items.isNotEmpty()) {
                onSelectedIndexChange((selectedIndex + 1).floorMod(items.size))
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_UP -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0 && items.isNotEmpty()) {
                onSelectedIndexChange((selectedIndex - LibraryPageJump).coerceAtLeast(0))
            }
            true
        }

        KeyEvent.KEYCODE_DPAD_DOWN -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0 && items.isNotEmpty()) {
                onSelectedIndexChange((selectedIndex + LibraryPageJump).coerceAtMost(items.lastIndex))
            }
            true
        }

        KeyEvent.KEYCODE_CHANNEL_UP,
        KeyEvent.KEYCODE_PAGE_DOWN,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_R1 -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0 && items.isNotEmpty()) {
                onSelectedIndexChange((selectedIndex + LibraryPageJump).coerceAtMost(items.lastIndex))
            }
            true
        }

        KeyEvent.KEYCODE_CHANNEL_DOWN,
        KeyEvent.KEYCODE_PAGE_UP,
        KeyEvent.KEYCODE_MEDIA_REWIND,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_BUTTON_L2,
        KeyEvent.KEYCODE_BUTTON_L1 -> {
            if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0 && items.isNotEmpty()) {
                onSelectedIndexChange((selectedIndex - LibraryPageJump).coerceAtLeast(0))
            }
            true
        }

        else -> true
    }
}

private fun TransformState.shouldIgnoreRepeatedToggle(
    keyCode: Int,
    repeatCount: Int,
): Boolean {
    if (repeatCount == 0) return false

    return when (currentMode) {
        TraceMode.Guide,
        TraceMode.Rulers,
        TraceMode.CenterCross,
        TraceMode.CanvasFrame,
        TraceMode.CanvasFrameColor,
        TraceMode.CropEdge,
        TraceMode.Shape,
        TraceMode.ShapeColor,
        TraceMode.PaletteMode,
        TraceMode.PaletteScale,
        TraceMode.PalettePosition,
        TraceMode.ChannelMixer,
        TraceMode.BlendMode,
        TraceMode.GuideCorner,
        TraceMode.GuideCornerColor,
        TraceMode.OverlayBlend,
        TraceMode.Invert,
        TraceMode.Lock -> true
        else -> false
    }
}

private fun handleBackOrMenuKey(
    nativeEvent: KeyEvent,
    onToggleOverlay: () -> Unit,
    onOpenImage: () -> Unit,
    menuLongPressHandled: Boolean,
    onMenuLongPressHandledChange: (Boolean) -> Unit,
): Boolean {
    if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount > 0) {
        if (!menuLongPressHandled) {
            onOpenImage()
            onMenuLongPressHandledChange(true)
        }
        return true
    }

    if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount == 0) {
        onToggleOverlay()
        return true
    }

    if (nativeEvent.action == KeyEvent.ACTION_UP) {
        if (menuLongPressHandled) {
            onMenuLongPressHandledChange(false)
        }
    }

    return true
}

private fun handleOkKey(
    nativeEvent: KeyEvent,
    transform: TransformState,
    isOverlayVisible: Boolean,
    onTransformChange: (TransformState) -> Unit,
    onConfirmDirectControl: () -> Unit,
    onOpenModePicker: () -> Unit,
    okLongPressHandled: Boolean,
    onOkLongPressHandledChange: (Boolean) -> Unit,
): Boolean {
    if (nativeEvent.action == KeyEvent.ACTION_DOWN && nativeEvent.repeatCount > 0) {
        if (!okLongPressHandled) {
            onTransformChange(transform.resetCurrentMode())
            onOkLongPressHandledChange(true)
        }
        return true
    }

    if (nativeEvent.action == KeyEvent.ACTION_UP) {
        if (okLongPressHandled) {
            onOkLongPressHandledChange(false)
        } else if (!isOverlayVisible && transform.currentMode == TraceMode.Corner) {
            onTransformChange(transform.copy(currentMode = TraceMode.Distort))
        } else if (!isOverlayVisible && transform.currentMode == TraceMode.Distort) {
            onTransformChange(transform.copy(selectedCorner = transform.selectedCorner.next()))
        } else if (!isOverlayVisible && transform.currentMode == TraceMode.CropEdge) {
            onTransformChange(transform.copy(isCropEnabled = true, selectedCropEdge = transform.selectedCropEdge.next()))
        } else if (!isOverlayVisible && transform.currentMode == TraceMode.CropAdjust) {
            onTransformChange(transform.copy(isCropEnabled = true, selectedCropEdge = transform.selectedCropEdge.next()))
        } else if (!isOverlayVisible && transform.currentMode == TraceMode.GuideCorner) {
            onTransformChange(transform.copy(isGuideFrameVisible = true, guideSelectedCorner = transform.guideSelectedCorner.next()))
        } else if (!isOverlayVisible && transform.currentMode == TraceMode.GuideCornerMove) {
            onTransformChange(transform.copy(isGuideFrameVisible = true, guideSelectedCorner = transform.guideSelectedCorner.next()))
        } else if (!isOverlayVisible) {
            onConfirmDirectControl()
        }
    }

    return true
}

private fun TraceMode.indexInPicker(): Int =
    ModePickerModes.indexOf(this).takeIf { it >= 0 } ?: 0

internal fun TransformState.updateForDirection(
    keyCode: Int,
): TransformState {
    if (isLocked && currentMode != TraceMode.Lock) return this

    return when (currentMode) {
        TraceMode.Move -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> copy(offsetX = (offsetX - moveStepPx()).coerceIn(-MaxSavedOffsetPx, MaxSavedOffsetPx))
            KeyEvent.KEYCODE_DPAD_RIGHT -> copy(offsetX = (offsetX + moveStepPx()).coerceIn(-MaxSavedOffsetPx, MaxSavedOffsetPx))
            KeyEvent.KEYCODE_DPAD_UP -> copy(offsetY = (offsetY - moveStepPx()).coerceIn(-MaxSavedOffsetPx, MaxSavedOffsetPx))
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(offsetY = (offsetY + moveStepPx()).coerceIn(-MaxSavedOffsetPx, MaxSavedOffsetPx))
            else -> this
        }

        TraceMode.Zoom -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> copy(scale = (scale + zoomStep()).coerceIn(MinScale, MaxScale))
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(scale = (scale - zoomStep()).coerceIn(MinScale, MaxScale))
            else -> this
        }

        TraceMode.Rotate -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> copy(rotationDegrees = (rotationDegrees - rotationStepDegrees()) % 360f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> copy(rotationDegrees = (rotationDegrees + rotationStepDegrees()) % 360f)
            else -> this
        }

        TraceMode.Corner -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(selectedCorner = selectedCorner.previous())
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(selectedCorner = selectedCorner.next())
            else -> this
        }

        TraceMode.Distort -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> moveSelectedCorner(dx = -distortStepPx(), dy = 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveSelectedCorner(dx = distortStepPx(), dy = 0f)
            KeyEvent.KEYCODE_DPAD_UP -> moveSelectedCorner(dx = 0f, dy = -distortStepPx())
            KeyEvent.KEYCODE_DPAD_DOWN -> moveSelectedCorner(dx = 0f, dy = distortStepPx())
            else -> this
        }

        TraceMode.Frame -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isGridVisible = true,
                gridSpacingPx = (gridSpacingPx - GridSpacingStepPx).coerceIn(MinGridSpacingPx, MaxGridSpacingPx),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isGridVisible = true,
                gridSpacingPx = (gridSpacingPx + GridSpacingStepPx).coerceIn(MinGridSpacingPx, MaxGridSpacingPx),
            )
            else -> this
        }

        TraceMode.View -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(canvasOrientation = canvasOrientation.previous())
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(canvasOrientation = canvasOrientation.next())
            else -> this
        }

        TraceMode.Fit -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(imageFit = imageFit.previous())
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(imageFit = imageFit.next())
            else -> this
        }

        TraceMode.MenuMove -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> copy(menuOffsetX = (menuOffsetX - menuMoveStepPx()).coerceIn(-MaxMenuOffsetPx, MaxMenuOffsetPx))
            KeyEvent.KEYCODE_DPAD_RIGHT -> copy(menuOffsetX = (menuOffsetX + menuMoveStepPx()).coerceIn(-MaxMenuOffsetPx, MaxMenuOffsetPx))
            KeyEvent.KEYCODE_DPAD_UP -> copy(menuOffsetY = (menuOffsetY - menuMoveStepPx()).coerceIn(-MaxMenuOffsetPx, MaxMenuOffsetPx))
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(menuOffsetY = (menuOffsetY + menuMoveStepPx()).coerceIn(-MaxMenuOffsetPx, MaxMenuOffsetPx))
            else -> this
        }

        TraceMode.Guide -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(isGuideFrameVisible = !isGuideFrameVisible)
            else -> this
        }

        TraceMode.Rulers -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(isRulersVisible = !isRulersVisible)
            else -> this
        }

        TraceMode.CenterCross -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(isRulersVisible = true, isCenterCrossVisible = !isCenterCrossVisible)
            else -> this
        }

        TraceMode.CanvasFrame -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isCanvasFrameVisible = true,
                canvasFramePreset = canvasFramePreset.previous(),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isCanvasFrameVisible = true,
                canvasFramePreset = canvasFramePreset.next(),
            )
            else -> this
        }

        TraceMode.CanvasFrameRatio -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isCanvasFrameVisible = true,
                canvasFramePreset = CanvasFramePreset.Custom,
                customCanvasFrameRatio = (customCanvasFrameRatio - canvasFrameRatioStep()).coerceIn(0.35f, 1.7f),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isCanvasFrameVisible = true,
                canvasFramePreset = CanvasFramePreset.Custom,
                customCanvasFrameRatio = (customCanvasFrameRatio + canvasFrameRatioStep()).coerceIn(0.35f, 1.7f),
            )
            else -> this
        }

        TraceMode.CanvasFrameColor -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isCanvasFrameVisible = true,
                canvasFrameColor = canvasFrameColor.previous(),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isCanvasFrameVisible = true,
                canvasFrameColor = canvasFrameColor.next(),
            )
            else -> this
        }

        TraceMode.CropEdge -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(isCropEnabled = true, selectedCropEdge = selectedCropEdge.previous())
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(isCropEnabled = true, selectedCropEdge = selectedCropEdge.next())
            else -> this
        }

        TraceMode.CropAdjust -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> adjustCropSelectedEdgeForKey(keyCode)
            else -> this
        }

        TraceMode.Shape -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(guideShape = guideShape.previous())
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(guideShape = guideShape.next())
            else -> this
        }

        TraceMode.ShapeColor -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                guideShape = if (guideShape == GuideShape.Off) GuideShape.Thirds else guideShape,
                guideShapeColor = guideShapeColor.previous(),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                guideShape = if (guideShape == GuideShape.Off) GuideShape.Thirds else guideShape,
                guideShapeColor = guideShapeColor.next(),
            )
            else -> this
        }

        TraceMode.ColorPalette -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                colorPaletteCount = when {
                    colorPaletteCount <= 0 -> MinPaletteColors
                    colorPaletteCount <= MinPaletteColors -> 0
                    else -> colorPaletteCount - 1
                }
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) {
                    8
                } else {
                    (colorPaletteCount + 1).coerceAtMost(MaxPaletteColors)
                }
            )
            else -> this
        }

        TraceMode.PaletteMode -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPaletteMode = colorPaletteMode.previous(),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPaletteMode = colorPaletteMode.next(),
            )
            else -> this
        }

        TraceMode.PaletteMove -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPaletteOffsetX = (colorPaletteOffsetX - paletteMoveStepPx()).coerceIn(-MaxPaletteOffsetPx, MaxPaletteOffsetPx),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPaletteOffsetX = (colorPaletteOffsetX + paletteMoveStepPx()).coerceIn(-MaxPaletteOffsetPx, MaxPaletteOffsetPx),
            )
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPaletteOffsetY = (colorPaletteOffsetY - paletteMoveStepPx()).coerceIn(-MaxPaletteOffsetPx, MaxPaletteOffsetPx),
            )
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPaletteOffsetY = (colorPaletteOffsetY + paletteMoveStepPx()).coerceIn(-MaxPaletteOffsetPx, MaxPaletteOffsetPx),
            )
            else -> this
        }

        TraceMode.PaletteScale -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPaletteScale = (colorPaletteScale - PaletteScaleStep).coerceIn(MinPaletteScale, MaxPaletteScale),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPaletteScale = (colorPaletteScale + PaletteScaleStep).coerceIn(MinPaletteScale, MaxPaletteScale),
            )
            else -> this
        }

        TraceMode.PalettePosition -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPalettePosition = colorPalettePosition.previous(),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                colorPaletteCount = if (colorPaletteCount <= 0) 8 else colorPaletteCount,
                colorPalettePosition = colorPalettePosition.next(),
            )
            else -> this
        }

        TraceMode.Step -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(controlStep = controlStep.previous())
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(controlStep = controlStep.next())
            else -> this
        }

        TraceMode.Preset -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> applyTracingPreset(tracingPreset.previous())
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> applyTracingPreset(tracingPreset.next())
            else -> this
        }

        TraceMode.Opacity -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(opacity = (opacity - OpacityStep).coerceIn(0.1f, 1f))
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(opacity = (opacity + OpacityStep).coerceIn(0.1f, 1f))
            else -> this
        }

        TraceMode.BlendMode -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                imageBlendMode = imageBlendMode.previous(),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                imageBlendMode = imageBlendMode.next(),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.Brightness -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isBrightnessEnabled = true,
                brightness = (brightness - BrightnessStep).coerceIn(0f, 2f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isBrightnessEnabled = true,
                brightness = (brightness + BrightnessStep).coerceIn(0f, 2f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.Threshold -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isThresholdEnabled = true,
                threshold = (threshold - ThresholdStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isThresholdEnabled = true,
                threshold = (threshold + ThresholdStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.ChannelMixer -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                channelMixer = channelMixer.previous(),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                channelMixer = channelMixer.next(),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.Paint -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isThresholdEnabled = false,
                paintDetail = (paintDetail - PaintDetailStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isThresholdEnabled = false,
                paintDetail = (paintDetail + PaintDetailStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.Posterize -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isThresholdEnabled = false,
                posterize = (posterize - PosterizeStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isThresholdEnabled = false,
                posterize = (posterize + PosterizeStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.Volume -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isThresholdEnabled = false,
                volume = (volume - VolumeStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isThresholdEnabled = false,
                volume = (volume + VolumeStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.MagicOutlineDetail -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                magicOutlineStrength = if (magicOutlineStrength <= 0f) 1f else magicOutlineStrength,
                magicOutlineDetail = (magicOutlineDetail - MagicOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                magicOutlineStrength = if (magicOutlineStrength <= 0f) 1f else magicOutlineStrength,
                magicOutlineDetail = (magicOutlineDetail + MagicOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.MagicOutlineThickness -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                magicOutlineStrength = if (magicOutlineStrength <= 0f) 1f else magicOutlineStrength,
                magicOutlineThickness = (magicOutlineThickness - MagicOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                magicOutlineStrength = if (magicOutlineStrength <= 0f) 1f else magicOutlineStrength,
                magicOutlineThickness = (magicOutlineThickness + MagicOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.EdgeStrength -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                edgeOutlineStrength = (edgeOutlineStrength - EdgeOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                edgeOutlineStrength = (edgeOutlineStrength + EdgeOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.EdgeThickness -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                edgeOutlineStrength = if (edgeOutlineStrength <= 0f) 0.55f else edgeOutlineStrength,
                edgeOutlineThickness = (edgeOutlineThickness - EdgeOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                edgeOutlineStrength = if (edgeOutlineStrength <= 0f) 0.55f else edgeOutlineStrength,
                edgeOutlineThickness = (edgeOutlineThickness + EdgeOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.EdgeDetail -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                edgeOutlineStrength = if (edgeOutlineStrength <= 0f) 0.55f else edgeOutlineStrength,
                edgeOutlineDetail = (edgeOutlineDetail - EdgeOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                edgeOutlineStrength = if (edgeOutlineStrength <= 0f) 0.55f else edgeOutlineStrength,
                edgeOutlineDetail = (edgeOutlineDetail + EdgeOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.EdgeSmoothing -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                edgeOutlineStrength = if (edgeOutlineStrength <= 0f) 0.55f else edgeOutlineStrength,
                edgeOutlineSmoothing = (edgeOutlineSmoothing - EdgeOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                edgeOutlineStrength = if (edgeOutlineStrength <= 0f) 0.55f else edgeOutlineStrength,
                edgeOutlineSmoothing = (edgeOutlineSmoothing + EdgeOutlineStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.Contrast -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                contrast = (contrast - ContrastStep).coerceIn(0.5f, 2f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                contrast = (contrast + ContrastStep).coerceIn(0.5f, 2f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.Clarity -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                clarity = (clarity - ClarityStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                clarity = (clarity + ClarityStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.NoiseReduction -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                noiseReduction = (noiseReduction - NoiseReductionStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                noiseReduction = (noiseReduction + NoiseReductionStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.LineArt -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                lineArt = (lineArt - LineArtStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                lineArt = (lineArt + LineArtStep).coerceIn(0f, 1f),
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.GuideCorner -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> moveSelectedGuideCorner(dx = -distortStepPx(), dy = 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveSelectedGuideCorner(dx = distortStepPx(), dy = 0f)
            KeyEvent.KEYCODE_DPAD_UP -> moveSelectedGuideCorner(dx = 0f, dy = -distortStepPx())
            KeyEvent.KEYCODE_DPAD_DOWN -> moveSelectedGuideCorner(dx = 0f, dy = distortStepPx())
            else -> this
        }

        TraceMode.GuideCornerColor -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isGuideFrameVisible = true,
                guideCornerColor = guideCornerColor.previous(),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isGuideFrameVisible = true,
                guideCornerColor = guideCornerColor.next(),
            )
            else -> this
        }

        TraceMode.GuideCornerMove -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> moveSelectedGuideCorner(dx = -distortStepPx(), dy = 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveSelectedGuideCorner(dx = distortStepPx(), dy = 0f)
            KeyEvent.KEYCODE_DPAD_UP -> moveSelectedGuideCorner(dx = 0f, dy = -distortStepPx())
            KeyEvent.KEYCODE_DPAD_DOWN -> moveSelectedGuideCorner(dx = 0f, dy = distortStepPx())
            else -> this
        }

        TraceMode.ShapeRotation -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                guideShape = if (guideShape == GuideShape.Off) GuideShape.Thirds else guideShape,
                guideShapeRotationDegrees = guideShapeRotationDegrees - 5f,
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                guideShape = if (guideShape == GuideShape.Off) GuideShape.Thirds else guideShape,
                guideShapeRotationDegrees = guideShapeRotationDegrees + 5f,
            )
            else -> this
        }

        TraceMode.OverlayOpacity -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isOverlayVisible = true,
                overlayOpacity = (overlayOpacity - OpacityStep).coerceIn(0f, 1f),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isOverlayVisible = true,
                overlayOpacity = (overlayOpacity + OpacityStep).coerceIn(0f, 1f),
            )
            else -> this
        }

        TraceMode.OverlayBlend -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP -> copy(
                isOverlayVisible = true,
                overlayBlendMode = overlayBlendMode.previous(),
            )
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isOverlayVisible = true,
                overlayBlendMode = overlayBlendMode.next(),
            )
            else -> this
        }

        TraceMode.Invert -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(
                isInverted = !isInverted,
                tracingPreset = TracingPreset.Custom,
            )
            else -> this
        }

        TraceMode.Lock -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> copy(isLocked = !isLocked)
            else -> this
        }

        TraceMode.Setup -> this

        TraceMode.Open -> this
    }
}

private fun TransformState.resetCurrentMode(): TransformState = when (currentMode) {
    TraceMode.Move -> copy(offsetX = 0f, offsetY = 0f)
    TraceMode.Zoom -> copy(scale = 1f)
    TraceMode.Rotate -> copy(rotationDegrees = 0f)
    TraceMode.Corner -> copy(selectedCorner = DistortCorner.TopLeft)
    TraceMode.Distort -> copy(
        topLeft = CornerOffset(),
        topRight = CornerOffset(),
        bottomRight = CornerOffset(),
        bottomLeft = CornerOffset(),
    )
    TraceMode.Frame -> copy(
        isGridVisible = false,
        gridSpacingPx = TransformState().gridSpacingPx,
        gridColorMode = TransformState().gridColorMode,
    )
    TraceMode.View -> resetGeometryForCurrentImage()
    TraceMode.Fit -> resetGeometryForCurrentImage()
    TraceMode.MenuMove -> copy(menuOffsetX = 0f, menuOffsetY = 0f)
    TraceMode.Guide -> copy(isGuideFrameVisible = true)
    TraceMode.Rulers -> copy(isRulersVisible = false)
    TraceMode.CenterCross -> copy(isCenterCrossVisible = true)
    TraceMode.CanvasFrame -> copy(
        isCanvasFrameVisible = false,
        canvasFramePreset = CanvasFramePreset.Ratio16x20,
        canvasFrameColor = GuideCornerColor.White,
    )
    TraceMode.CanvasFrameRatio -> copy(customCanvasFrameRatio = 0.8f, canvasFramePreset = CanvasFramePreset.Custom)
    TraceMode.CanvasFrameColor -> copy(canvasFrameColor = GuideCornerColor.White)
    TraceMode.CropEdge -> copy(selectedCropEdge = CropEdge.Left)
    TraceMode.CropAdjust -> copy(
        isCropEnabled = false,
        cropLeft = 0f,
        cropTop = 0f,
        cropRight = 0f,
        cropBottom = 0f,
    )
    TraceMode.Shape -> copy(guideShape = GuideShape.Off)
    TraceMode.ShapeColor -> copy(guideShapeColor = GuideShapeColor.White)
    TraceMode.ColorPalette -> copy(colorPaletteCount = 0)
    TraceMode.PaletteMode -> copy(colorPaletteMode = ColorPaletteMode.Balanced)
    TraceMode.PaletteMove -> copy(colorPaletteOffsetX = 0f, colorPaletteOffsetY = 0f)
    TraceMode.PaletteScale -> copy(colorPaletteScale = 1f)
    TraceMode.PalettePosition -> copy(colorPalettePosition = PalettePosition.BottomEnd)
    TraceMode.Step -> copy(controlStep = ControlStep.Normal)
    TraceMode.Preset -> resetFilters()
    TraceMode.Opacity -> copy(opacity = 1f)
    TraceMode.BlendMode -> copy(imageBlendMode = ImageBlendMode.Normal, tracingPreset = TracingPreset.Custom)
    TraceMode.Brightness -> copy(
        isBrightnessEnabled = false,
        brightness = 1f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.Threshold -> copy(
        isThresholdEnabled = false,
        threshold = 0.5f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.ChannelMixer -> copy(
        channelMixer = ChannelMixerMode.Normal,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.Paint -> copy(
        paintDetail = 0f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.Posterize -> copy(
        posterize = 0f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.Volume -> copy(
        volume = 0f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.MagicOutlineDetail -> copy(
        magicOutlineStrength = 0f,
        magicOutlineDetail = 0.55f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.MagicOutlineThickness -> copy(
        magicOutlineThickness = 0.35f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.EdgeStrength -> copy(
        edgeOutlineStrength = 0f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.EdgeThickness -> copy(edgeOutlineThickness = 0.35f, tracingPreset = TracingPreset.Custom)
    TraceMode.EdgeDetail -> copy(edgeOutlineDetail = 0.55f, tracingPreset = TracingPreset.Custom)
    TraceMode.EdgeSmoothing -> copy(edgeOutlineSmoothing = 0.25f, tracingPreset = TracingPreset.Custom)
    TraceMode.Contrast -> copy(
        contrast = 1f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.Clarity -> copy(
        clarity = 0f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.NoiseReduction -> copy(
        noiseReduction = 0f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.LineArt -> copy(
        lineArt = 0f,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.GuideCorner -> copy(guideSelectedCorner = DistortCorner.TopLeft)
    TraceMode.GuideCornerColor -> copy(guideCornerColor = GuideCornerColor.White)
    TraceMode.GuideCornerMove -> copy(
        guideTopLeft = CornerOffset(),
        guideTopRight = CornerOffset(),
        guideBottomRight = CornerOffset(),
        guideBottomLeft = CornerOffset(),
    )
    TraceMode.ShapeRotation -> copy(guideShapeRotationDegrees = 0f)
    TraceMode.OverlayOpacity -> copy(overlayOpacity = 0.5f)
    TraceMode.OverlayBlend -> copy(overlayBlendMode = OverlayBlendMode.Normal)
    TraceMode.Invert -> copy(
        isInverted = false,
        tracingPreset = TracingPreset.Custom,
    )
    TraceMode.Lock -> copy(isLocked = false)
    TraceMode.Setup -> resetAllSettings()
    TraceMode.Open -> resetImageAdjustments()
}

private fun TransformState.resetAllSettings(): TransformState =
    TransformState(currentMode = currentMode)

private fun TransformState.resetTransformOnly(): TransformState = copy(
    scale = 1f,
    offsetX = 0f,
    offsetY = 0f,
    rotationDegrees = 0f,
    isFlippedHorizontal = false,
    isFlippedVertical = false,
    selectedCorner = DistortCorner.TopLeft,
    topLeft = CornerOffset(),
    topRight = CornerOffset(),
    bottomRight = CornerOffset(),
    bottomLeft = CornerOffset(),
    rotationStepDegrees = 1f,
    canvasOrientation = CanvasOrientation.Auto,
    imageFit = ImageFit.Fit,
    isLocked = false,
)

private fun TransformState.resetGeometryForCurrentImage(): TransformState = copy(
    scale = 1f,
    offsetX = 0f,
    offsetY = 0f,
    rotationDegrees = 0f,
    isFlippedHorizontal = false,
    isFlippedVertical = false,
    selectedCorner = DistortCorner.TopLeft,
    topLeft = CornerOffset(),
    topRight = CornerOffset(),
    bottomRight = CornerOffset(),
    bottomLeft = CornerOffset(),
    guideShape = GuideShape.Off,
    guideShapeColor = GuideShapeColor.White,
    guideShapeRotationDegrees = 0f,
    guideSelectedCorner = DistortCorner.TopLeft,
    guideCornerColor = GuideCornerColor.White,
    guideTopLeft = CornerOffset(),
    guideTopRight = CornerOffset(),
    guideBottomRight = CornerOffset(),
    guideBottomLeft = CornerOffset(),
    isGridVisible = false,
    gridSpacingPx = TransformState().gridSpacingPx,
    gridColorMode = TransformState().gridColorMode,
    isRulersVisible = false,
    isCenterCrossVisible = true,
    isCanvasFrameVisible = false,
    canvasFramePreset = CanvasFramePreset.Ratio16x20,
    customCanvasFrameRatio = 0.8f,
    canvasFrameColor = GuideCornerColor.White,
    isCropEnabled = false,
    selectedCropEdge = CropEdge.Left,
    selectedCropCorner = CropCorner.TopLeft,
    cropLeft = 0f,
    cropTop = 0f,
    cropRight = 0f,
    cropBottom = 0f,
    colorPaletteCount = 0,
    colorPaletteMode = ColorPaletteMode.Balanced,
    colorPalettePosition = PalettePosition.BottomEnd,
    colorPaletteOffsetX = 0f,
    colorPaletteOffsetY = 0f,
    colorPaletteScale = 1f,
    isOverlayVisible = false,
    overlayOpacity = 0.5f,
    overlayBlendMode = OverlayBlendMode.Normal,
    canvasOrientation = CanvasOrientation.Auto,
    imageFit = ImageFit.Fit,
    menuOffsetX = 0f,
    menuOffsetY = 0f,
)

private fun TransformState.resetGuides(): TransformState = copy(
    guideShape = GuideShape.Off,
    guideShapeColor = GuideShapeColor.White,
    guideShapeRotationDegrees = 0f,
    guideSelectedCorner = DistortCorner.TopLeft,
    guideCornerColor = GuideCornerColor.White,
    guideTopLeft = CornerOffset(),
    guideTopRight = CornerOffset(),
    guideBottomRight = CornerOffset(),
    guideBottomLeft = CornerOffset(),
    isGridVisible = false,
    gridSpacingPx = TransformState().gridSpacingPx,
    gridColorMode = TransformState().gridColorMode,
    isRulersVisible = false,
    isCenterCrossVisible = true,
    isCanvasFrameVisible = false,
    canvasFramePreset = CanvasFramePreset.Ratio16x20,
    customCanvasFrameRatio = 0.8f,
    canvasFrameColor = GuideCornerColor.White,
    isCropEnabled = false,
    selectedCropEdge = CropEdge.Left,
    selectedCropCorner = CropCorner.TopLeft,
    cropLeft = 0f,
    cropTop = 0f,
    cropRight = 0f,
    cropBottom = 0f,
    isGuideFrameVisible = true,
    guideInsetX = 80f,
    guideInsetY = 80f,
    colorPaletteCount = 0,
    colorPaletteMode = ColorPaletteMode.Balanced,
    colorPalettePosition = PalettePosition.BottomEnd,
    colorPaletteOffsetX = 0f,
    colorPaletteOffsetY = 0f,
    colorPaletteScale = 1f,
    isOverlayVisible = false,
    overlayOpacity = 0.5f,
    overlayBlendMode = OverlayBlendMode.Normal,
)

private fun TransformState.resetFilters(): TransformState = copy(
    opacity = 1f,
    imageBlendMode = ImageBlendMode.Normal,
    isBrightnessEnabled = false,
    brightness = 1f,
    contrast = 1f,
    clarity = 0f,
    noiseReduction = 0f,
    lineArt = 0f,
    isThresholdEnabled = false,
    threshold = 0.5f,
    channelMixer = ChannelMixerMode.Normal,
    paintDetail = 0f,
    posterize = 0f,
    volume = 0f,
    magicOutlineStrength = 0f,
    magicOutlineDetail = 0.55f,
    magicOutlineThickness = 0.35f,
    edgeOutlineStrength = 0f,
    edgeOutlineThickness = 0.35f,
    edgeOutlineDetail = 0.55f,
    edgeOutlineSmoothing = 0.25f,
    isInverted = false,
    tracingPreset = TracingPreset.Custom,
)

private fun TransformState.resetImageAdjustments(): TransformState =
    resetFilters()
        .resetGeometryForCurrentImage()
        .copy(isLocked = false)

internal fun TransformState.zoomPercent(): Int = (scale * 100f).roundToInt()

internal fun TransformState.rotationLabel(): String {
    val rounded = rotationDegrees.roundToInt()
    return if (abs(rotationDegrees - rounded) < 0.05f) {
        "$rounded deg"
    } else {
        String.format(Locale.US, "%.1f deg", rotationDegrees)
    }
}

internal fun TransformState.opacityPercent(): Int = (opacity * 100f).roundToInt()

internal fun TransformState.contrastPercent(): Int = (contrast * 100f).roundToInt()

internal fun TransformState.clarityPercent(): Int = (clarity * 100f).roundToInt()

internal fun TransformState.lineArtPercent(): Int = (lineArt * 100f).roundToInt()

internal fun TransformState.thresholdLabel(): String = if (isThresholdEnabled) {
    "${(threshold * 100f).roundToInt()}%"
} else {
    "Off"
}

internal fun TransformState.channelMixerLabel(): String = when (channelMixer) {
    ChannelMixerMode.Normal -> "Normal"
    ChannelMixerMode.BlackWhite -> "Black White"
    ChannelMixerMode.RedGreen -> "Red Green"
    ChannelMixerMode.YellowBlue -> "Yellow Blue"
    ChannelMixerMode.MagentaCyan -> "Magenta Cyan"
}

internal fun TransformState.paintDetailLabel(): String = if (paintDetail <= 0f) {
    "Off"
} else {
    "Detail ${(paintDetail * 100f).roundToInt()}%"
}

internal fun TransformState.posterizeLabel(): String = if (posterize <= 0f) {
    "Off"
} else {
    "Strength ${(posterize * 100f).roundToInt()}%"
}

internal fun TransformState.guideShapeLabel(): String = when (guideShape) {
    GuideShape.Off -> "Off"
    GuideShape.Thirds -> "Rule of Thirds"
    GuideShape.ThirdsDense -> "Dense Thirds"
    GuideShape.GoldenRatio -> "Golden Ratio"
    GuideShape.GoldenSpiral -> "Golden Spiral"
    GuideShape.Diagonal -> "Diagonals"
    GuideShape.DynamicSymmetry -> "Dynamic Symmetry"
    GuideShape.Triangle -> "Golden Triangle"
    GuideShape.CenterCross -> "Center Cross"
}

private fun GuideShapeColor.composeShapeColor(alpha: Float): Color = when (this) {
    GuideShapeColor.Black -> Color(0xFF000000)
    GuideShapeColor.White -> Color(0xFFFFFFFF)
    GuideShapeColor.Magenta -> Color(0xFFFF2BD6)
    GuideShapeColor.Yellow -> Color(0xFFFFD60A)
    GuideShapeColor.Green -> Color(0xFF30D158)
}.copy(alpha = alpha.coerceIn(0f, 1f))

private fun GuideCornerColor.composeCornerColor(alpha: Float): Color = when (this) {
    GuideCornerColor.White -> Color(0xFFFFFFFF)
    GuideCornerColor.Black -> Color(0xFF000000)
    GuideCornerColor.Magenta -> Color(0xFFFF2BD6)
    GuideCornerColor.Red -> Color(0xFFFF453A)
    GuideCornerColor.Yellow -> Color(0xFFFFD60A)
    GuideCornerColor.Green -> Color(0xFF30D158)
    GuideCornerColor.Cyan -> Color(0xFF64D2FF)
}.copy(alpha = alpha.coerceIn(0f, 1f))

internal fun TransformState.colorPaletteLabel(): String = if (colorPaletteCount <= 0) {
    "Off"
} else {
    "$colorPaletteCount colors"
}

internal fun TransformState.colorPaletteModeLabel(): String = colorPaletteMode.label()

private fun ColorPaletteMode.label(): String = when (this) {
    ColorPaletteMode.Balanced -> "Balanced"
    ColorPaletteMode.Dominant -> "Dominant"
    ColorPaletteMode.Paint -> "Paint"
}

internal fun GridColorMode.label(): String = when (this) {
    GridColorMode.White -> "White"
    GridColorMode.Black -> "Black"
}

private fun ProjectorOrientation.rotationDegrees(): Float = when (this) {
    ProjectorOrientation.Normal -> 0f
    ProjectorOrientation.UpsideDown -> 180f
    ProjectorOrientation.RotateLeft -> -90f
    ProjectorOrientation.RotateRight -> 90f
}

private fun ProjectorOrientation.isSideways(): Boolean =
    this == ProjectorOrientation.RotateLeft || this == ProjectorOrientation.RotateRight

private fun PalettePosition.alignment(): Alignment = when (this) {
    PalettePosition.TopStart -> Alignment.TopStart
    PalettePosition.TopCenter -> Alignment.TopCenter
    PalettePosition.TopEnd -> Alignment.TopEnd
    PalettePosition.CenterEnd -> Alignment.CenterEnd
    PalettePosition.BottomEnd -> Alignment.BottomEnd
    PalettePosition.BottomCenter -> Alignment.BottomCenter
    PalettePosition.BottomStart -> Alignment.BottomStart
    PalettePosition.CenterStart -> Alignment.CenterStart
}

internal fun TransformState.controlStepLabel(): String = when (controlStep) {
    ControlStep.Fine -> "Fine"
    ControlStep.Normal -> "Normal"
    ControlStep.Coarse -> "Coarse"
}

internal fun TransformState.selectedCornerLabel(): String = when (selectedCorner) {
    DistortCorner.TopLeft -> "TL"
    DistortCorner.TopRight -> "TR"
    DistortCorner.BottomRight -> "BR"
    DistortCorner.BottomLeft -> "BL"
}

internal fun TransformState.selectedCornerOffsetLabel(): String {
    val offset = selectedCornerOffset()
    return "${offset.x.roundToInt()},${offset.y.roundToInt()}"
}

internal fun TransformState.guideFrameLabel(): String = if (isGuideFrameVisible) {
    "${guideInsetX.roundToInt()}x${guideInsetY.roundToInt()} px"
} else {
    "Off"
}

internal fun TransformState.canvasOrientationLabel(): String = when (canvasOrientation) {
    CanvasOrientation.Auto -> "Auto"
    CanvasOrientation.Landscape -> "Landscape"
    CanvasOrientation.Portrait -> "Portrait"
}

internal fun TransformState.imageFitLabel(): String = when (imageFit) {
    ImageFit.Fit -> "Fit"
    ImageFit.Fill -> "Fill"
    ImageFit.Stretch -> "Stretch"
}

internal fun TransformState.tracingPresetLabel(): String = when (tracingPreset) {
    TracingPreset.Custom -> "Custom"
    TracingPreset.Photo -> "Photo"
    TracingPreset.Sketch -> "Sketch"
    TracingPreset.Lines -> "Lines"
    TracingPreset.InvertedLines -> "Inverted"
}

private fun TransformState.applyTracingPreset(preset: TracingPreset): TransformState = when (preset) {
    TracingPreset.Custom -> copy(tracingPreset = TracingPreset.Custom)
    TracingPreset.Photo -> copy(
        imageBlendMode = ImageBlendMode.Normal,
        isBrightnessEnabled = false,
        brightness = 1f,
        contrast = 1f,
        clarity = 0f,
        noiseReduction = 0f,
        lineArt = 0f,
        isThresholdEnabled = false,
        channelMixer = ChannelMixerMode.Normal,
        paintDetail = 0f,
        posterize = 0f,
        volume = 0f,
        isInverted = false,
        tracingPreset = preset,
    )
    TracingPreset.Sketch -> copy(
        imageBlendMode = ImageBlendMode.Normal,
        isBrightnessEnabled = false,
        brightness = 1f,
        contrast = 1.25f,
        clarity = 0.35f,
        noiseReduction = 0f,
        lineArt = 0.3f,
        isThresholdEnabled = false,
        channelMixer = ChannelMixerMode.BlackWhite,
        paintDetail = 0f,
        posterize = 0f,
        volume = 0f,
        isInverted = false,
        tracingPreset = preset,
    )
    TracingPreset.Lines -> copy(
        imageBlendMode = ImageBlendMode.Normal,
        isBrightnessEnabled = false,
        brightness = 1f,
        contrast = 1.55f,
        clarity = 0.45f,
        noiseReduction = 0f,
        lineArt = 0.85f,
        isThresholdEnabled = false,
        channelMixer = ChannelMixerMode.BlackWhite,
        paintDetail = 0f,
        posterize = 0f,
        volume = 0f,
        isInverted = false,
        tracingPreset = preset,
    )
    TracingPreset.InvertedLines -> copy(
        imageBlendMode = ImageBlendMode.Normal,
        isBrightnessEnabled = false,
        brightness = 1f,
        contrast = 1.6f,
        clarity = 0.4f,
        noiseReduction = 0f,
        lineArt = 0.9f,
        isThresholdEnabled = false,
        channelMixer = ChannelMixerMode.BlackWhite,
        paintDetail = 0f,
        posterize = 0f,
        volume = 0f,
        isInverted = true,
        tracingPreset = preset,
    )
}

private fun TransformState.moveStepPx(): Float = when (controlStep) {
    ControlStep.Fine -> 2f
    ControlStep.Normal -> MoveStepPx
    ControlStep.Coarse -> 40f
}

private fun TransformState.distortStepPx(): Float = when (controlStep) {
    ControlStep.Fine -> 2f
    ControlStep.Normal -> DistortStepPx
    ControlStep.Coarse -> 32f
}

private fun TransformState.frameStepPx(): Float = when (controlStep) {
    ControlStep.Fine -> 2f
    ControlStep.Normal -> FrameStepPx
    ControlStep.Coarse -> 40f
}

private fun TransformState.zoomStep(): Float = when (controlStep) {
    ControlStep.Fine -> 0.01f
    ControlStep.Normal -> ZoomStep
    ControlStep.Coarse -> 0.15f
}

private fun TransformState.rotationStepDegrees(): Float = when (controlStep) {
    ControlStep.Fine,
    ControlStep.Normal,
    ControlStep.Coarse -> rotationStepDegrees.coerceIn(1f, 90f)
}

private fun Float.nextRotationStep(): Float = when (roundToInt()) {
    1 -> 5f
    5 -> 90f
    else -> 1f
}

private fun Float.nearestRotationStep(): Float {
    val value = roundToInt()
    return when {
        value < 3 -> 1f
        value < 45 -> 5f
        else -> 90f
    }
}

private fun TransformState.cropStep(): Float = when (controlStep) {
    ControlStep.Fine -> 0.003f
    ControlStep.Normal -> CropStep
    ControlStep.Coarse -> 0.03f
}

private fun TransformState.paletteMoveStepPx(): Float = when (controlStep) {
    ControlStep.Fine -> 8f
    ControlStep.Normal -> 24f
    ControlStep.Coarse -> 64f
}

private fun TransformState.menuMoveStepPx(): Float = when (controlStep) {
    ControlStep.Fine -> 6f
    ControlStep.Normal -> 18f
    ControlStep.Coarse -> 48f
}

private fun TransformState.canvasFrameRatioStep(): Float = when (controlStep) {
    ControlStep.Fine -> 0.01f
    ControlStep.Normal -> CanvasFrameRatioStep
    ControlStep.Coarse -> 0.08f
}

private fun TransformState.canvasFrameAspectRatio(): Float = when (canvasFramePreset) {
    CanvasFramePreset.Ratio1x1 -> 1f
    CanvasFramePreset.Ratio2x3 -> 2f / 3f
    CanvasFramePreset.Ratio3x2 -> 3f / 2f
    CanvasFramePreset.Ratio4x5 -> 4f / 5f
    CanvasFramePreset.Ratio5x4 -> 5f / 4f
    CanvasFramePreset.Ratio9x16 -> 9f / 16f
    CanvasFramePreset.Ratio16x9 -> 16f / 9f
    CanvasFramePreset.Ratio16x20 -> 16f / 20f
    CanvasFramePreset.Ratio18x24 -> 18f / 24f
    CanvasFramePreset.Ratio24x36 -> 24f / 36f
    CanvasFramePreset.Custom -> customCanvasFrameRatio.coerceIn(0.35f, 1.7f)
}

private fun CanvasOrientation.isPortraitForScreen(screenIsPortrait: Boolean): Boolean = when (this) {
    CanvasOrientation.Auto -> screenIsPortrait
    CanvasOrientation.Landscape -> false
    CanvasOrientation.Portrait -> true
}

private fun TransformState.moveSelectedCorner(
    dx: Float,
    dy: Float,
): TransformState = when (selectedCorner) {
    DistortCorner.TopLeft -> copy(topLeft = topLeft.moved(dx, dy))
    DistortCorner.TopRight -> copy(topRight = topRight.moved(dx, dy))
    DistortCorner.BottomRight -> copy(bottomRight = bottomRight.moved(dx, dy))
    DistortCorner.BottomLeft -> copy(bottomLeft = bottomLeft.moved(dx, dy))
}

private fun TransformState.selectedCornerOffset(): CornerOffset = when (selectedCorner) {
    DistortCorner.TopLeft -> topLeft
    DistortCorner.TopRight -> topRight
    DistortCorner.BottomRight -> bottomRight
    DistortCorner.BottomLeft -> bottomLeft
}

private fun TransformState.moveSelectedGuideCorner(
    dx: Float,
    dy: Float,
): TransformState = when (guideSelectedCorner) {
    DistortCorner.TopLeft -> copy(isGuideFrameVisible = true, guideTopLeft = guideTopLeft.moved(dx, dy))
    DistortCorner.TopRight -> copy(isGuideFrameVisible = true, guideTopRight = guideTopRight.moved(dx, dy))
    DistortCorner.BottomRight -> copy(isGuideFrameVisible = true, guideBottomRight = guideBottomRight.moved(dx, dy))
    DistortCorner.BottomLeft -> copy(isGuideFrameVisible = true, guideBottomLeft = guideBottomLeft.moved(dx, dy))
}

private fun TransformState.adjustCropSelectedEdge(delta: Float): TransformState {
    val maxInset = 0.45f
    return when (selectedCropEdge) {
        CropEdge.Left -> copy(isCropEnabled = true, cropLeft = (cropLeft + delta).coerceIn(0f, maxInset))
        CropEdge.Top -> copy(isCropEnabled = true, cropTop = (cropTop + delta).coerceIn(0f, maxInset))
        CropEdge.Right -> copy(isCropEnabled = true, cropRight = (cropRight + delta).coerceIn(0f, maxInset))
        CropEdge.Bottom -> copy(isCropEnabled = true, cropBottom = (cropBottom + delta).coerceIn(0f, maxInset))
    }
}

private fun TransformState.adjustCropSelectedEdgeForKey(keyCode: Int): TransformState {
    val step = cropStep()
    return when (selectedCropEdge) {
        CropEdge.Left -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> adjustCropSelectedEdge(step)
            KeyEvent.KEYCODE_DPAD_LEFT -> adjustCropSelectedEdge(-step)
            else -> this
        }
        CropEdge.Top -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> adjustCropSelectedEdge(step)
            KeyEvent.KEYCODE_DPAD_UP -> adjustCropSelectedEdge(-step)
            else -> this
        }
        CropEdge.Right -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> adjustCropSelectedEdge(step)
            KeyEvent.KEYCODE_DPAD_RIGHT -> adjustCropSelectedEdge(-step)
            else -> this
        }
        CropEdge.Bottom -> when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> adjustCropSelectedEdge(step)
            KeyEvent.KEYCODE_DPAD_DOWN -> adjustCropSelectedEdge(-step)
            else -> this
        }
    }
}

private fun TransformState.adjustCropSelectedCorner(
    dx: Float,
    dy: Float,
): TransformState {
    val maxInset = 0.45f
    return when (selectedCropCorner) {
        CropCorner.TopLeft -> copy(
            isCropEnabled = true,
            cropLeft = (cropLeft + dx).coerceIn(0f, maxInset),
            cropTop = (cropTop + dy).coerceIn(0f, maxInset),
        )
        CropCorner.TopRight -> copy(
            isCropEnabled = true,
            cropRight = (cropRight - dx).coerceIn(0f, maxInset),
            cropTop = (cropTop + dy).coerceIn(0f, maxInset),
        )
        CropCorner.BottomRight -> copy(
            isCropEnabled = true,
            cropRight = (cropRight - dx).coerceIn(0f, maxInset),
            cropBottom = (cropBottom - dy).coerceIn(0f, maxInset),
        )
        CropCorner.BottomLeft -> copy(
            isCropEnabled = true,
            cropLeft = (cropLeft + dx).coerceIn(0f, maxInset),
            cropBottom = (cropBottom - dy).coerceIn(0f, maxInset),
        )
    }
}

private fun TransformState.liveValueOverlayText(): String? = when (currentMode) {
    TraceMode.Move -> "Move  X ${offsetX.roundToInt()}  Y ${offsetY.roundToInt()}"
    TraceMode.Zoom -> "Zoom ${zoomPercent()}%"
    TraceMode.Rotate -> "Rotate ${rotationDegrees.roundToInt()} deg"
    TraceMode.Corner -> "Corner ${selectedCorner.shortLabel()}"
    TraceMode.Distort -> {
        val offset = selectedCornerOffset()
        "Distort ${selectedCorner.shortLabel()} ${offset.x.roundToInt()}, ${offset.y.roundToInt()}"
    }
    TraceMode.Frame -> "Grid ${gridSpacingPx.roundToInt()} px"
    TraceMode.View -> "Canvas ${canvasOrientationLabel()}"
    TraceMode.Fit -> "Fit ${imageFitLabel()}"
    TraceMode.MenuMove -> "Menu X ${menuOffsetX.roundToInt()}  Y ${menuOffsetY.roundToInt()}"
    TraceMode.Guide -> "Corners ${if (isGuideFrameVisible) "On" else "Off"}"
    TraceMode.Rulers -> "Rulers ${if (isRulersVisible) "On" else "Off"}"
    TraceMode.CenterCross -> "Center Cross ${if (isCenterCrossVisible) "On" else "Off"}"
    TraceMode.CanvasFrame -> "Frame ${canvasFramePresetLabel()}"
    TraceMode.CanvasFrameRatio -> "Frame ${customCanvasFrameRatioLabel()}"
    TraceMode.CanvasFrameColor -> "Frame ${canvasFrameColorLabel()}"
    TraceMode.CropEdge -> "Crop ${cropEdgeLabel()} ${cropAmountLabel()}"
    TraceMode.CropAdjust -> "Crop ${cropEdgeLabel()} ${cropAmountLabel()}"
    TraceMode.Shape -> "Shape ${guideShapeLabel()}"
    TraceMode.ShapeColor -> "Shape ${guideShapeColorLabel()}"
    TraceMode.ColorPalette -> "Palette ${colorPaletteCount.coerceAtLeast(0)}"
    TraceMode.PaletteMode -> "Palette ${colorPaletteModeLabel()}"
    TraceMode.PaletteMove -> "Palette X ${colorPaletteOffsetX.roundToInt()}  Y ${colorPaletteOffsetY.roundToInt()}"
    TraceMode.PaletteScale -> "Palette ${(colorPaletteScale * 100f).roundToInt()}%"
    TraceMode.PalettePosition -> "Palette ${colorPalettePositionLabel()}"
    TraceMode.Step -> "Step ${controlStepLabel()}"
    TraceMode.Preset -> "Preset ${tracingPresetLabel()}"
    TraceMode.Opacity -> "Opacity ${(opacity * 100f).roundToInt()}%"
    TraceMode.BlendMode -> "Mode ${imageBlendModeLabel()}"
    TraceMode.Brightness -> "Brightness ${(brightness * 100f).roundToInt()}%"
    TraceMode.Threshold -> "Threshold ${(threshold * 100f).roundToInt()}%"
    TraceMode.ChannelMixer -> "Channel ${channelMixerLabel()}"
    TraceMode.Paint -> "Paint ${(paintDetail * 100f).roundToInt()}%"
    TraceMode.Posterize -> "Posterize ${(posterize * 100f).roundToInt()}%"
    TraceMode.Volume -> "Blur ${(volume * 100f).roundToInt()}%"
    TraceMode.MagicOutlineDetail -> "Magic Detail ${(magicOutlineDetail * 100f).roundToInt()}%"
    TraceMode.MagicOutlineThickness -> "Magic Line ${(magicOutlineThickness * 100f).roundToInt()}%"
    TraceMode.EdgeStrength -> "Edge ${(edgeOutlineStrength * 100f).roundToInt()}%"
    TraceMode.EdgeThickness -> "Edge Thick ${(edgeOutlineThickness * 100f).roundToInt()}%"
    TraceMode.EdgeDetail -> "Edge Detail ${(edgeOutlineDetail * 100f).roundToInt()}%"
    TraceMode.EdgeSmoothing -> "Smoothing ${(edgeOutlineSmoothing * 100f).roundToInt()}%"
    TraceMode.Contrast -> "Contrast ${(contrast * 100f).roundToInt()}%"
    TraceMode.Clarity -> "Clarity ${(clarity * 100f).roundToInt()}%"
    TraceMode.NoiseReduction -> "Noise ${(noiseReduction * 100f).roundToInt()}%"
    TraceMode.LineArt -> "Lines ${(lineArt * 100f).roundToInt()}%"
    TraceMode.GuideCorner -> {
        val offset = when (guideSelectedCorner) {
            DistortCorner.TopLeft -> guideTopLeft
            DistortCorner.TopRight -> guideTopRight
            DistortCorner.BottomRight -> guideBottomRight
            DistortCorner.BottomLeft -> guideBottomLeft
        }
        "Corners ${guideSelectedCorner.shortLabel()} ${offset.x.roundToInt()}, ${offset.y.roundToInt()}"
    }
    TraceMode.GuideCornerColor -> "Corners ${guideCornerColorLabel()}"
    TraceMode.GuideCornerMove -> {
        val offset = when (guideSelectedCorner) {
            DistortCorner.TopLeft -> guideTopLeft
            DistortCorner.TopRight -> guideTopRight
            DistortCorner.BottomRight -> guideBottomRight
            DistortCorner.BottomLeft -> guideBottomLeft
        }
        "Corner ${guideSelectedCorner.shortLabel()} ${offset.x.roundToInt()}, ${offset.y.roundToInt()}"
    }
    TraceMode.ShapeRotation -> "Shape ${guideShapeRotationDegrees.roundToInt()} deg"
    TraceMode.OverlayOpacity -> "Overlay ${(overlayOpacity * 100f).roundToInt()}%"
    TraceMode.OverlayBlend -> "Overlay ${overlayBlendModeLabel()}"
    TraceMode.Invert -> "Invert ${if (isInverted) "On" else "Off"}"
    TraceMode.Lock -> "Lock ${if (isLocked) "On" else "Off"}"
    TraceMode.Setup,
    TraceMode.Open -> null
}

private fun CornerOffset.moved(
    dx: Float,
    dy: Float,
): CornerOffset = copy(
    x = (x + dx).coerceIn(-MaxCornerOffsetPx, MaxCornerOffsetPx),
    y = (y + dy).coerceIn(-MaxCornerOffsetPx, MaxCornerOffsetPx),
)

private fun DrawScope.drawGuideCorner(
    corner: Offset,
    horizontalEnd: Offset,
    verticalEnd: Offset,
    color: Color,
    strokeWidth: Float,
) {
    drawLine(color, corner, horizontalEnd, strokeWidth = strokeWidth)
    drawLine(color, corner, verticalEnd, strokeWidth = strokeWidth)
    drawCircle(color, radius = strokeWidth * 1.35f, center = corner)
}

private fun DrawScope.drawDistortedBitmap(
    bitmap: Bitmap,
    transform: TransformState,
    blinkCompareStep: BlinkCompareStep,
    thresholdBitmap: Bitmap?,
    paintBitmap: Bitmap?,
    volumeBitmap: Bitmap?,
    posterizeBitmap: Bitmap?,
    noiseReductionBitmap: Bitmap?,
    clarityBitmap: Bitmap?,
    lineArtBitmap: Bitmap?,
    magicOutlineBitmap: Bitmap?,
    edgeOutlineBitmap: Bitmap?,
    overlayBitmap: Bitmap?,
) {
    val corners = fittedImageCorners(
        bitmapSize = IntSize(bitmap.width, bitmap.height),
        viewportSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        transform = transform,
    )

    drawIntoCanvas { canvas ->
        val baseBitmap = when (blinkCompareStep) {
            BlinkCompareStep.Original,
            BlinkCompareStep.Outline,
            BlinkCompareStep.BlackWhite -> bitmap
            BlinkCompareStep.Filtered -> when {
                transform.isThresholdEnabled -> thresholdBitmap ?: bitmap
                transform.paintDetail > 0f -> paintBitmap ?: bitmap
                transform.volume > 0f -> volumeBitmap ?: bitmap
                transform.posterize > 0f -> posterizeBitmap ?: bitmap
                transform.noiseReduction > 0f -> noiseReductionBitmap ?: bitmap
                else -> bitmap
            }
        }
        val basePaint = when (blinkCompareStep) {
            BlinkCompareStep.BlackWhite -> blackWhitePaint()
            BlinkCompareStep.Original,
            BlinkCompareStep.Outline -> bitmapPaintForFilters(TransformState(), false)
            BlinkCompareStep.Filtered -> bitmapPaintForFilters(transform, transform.isThresholdEnabled && thresholdBitmap != null)
        }
        val nativeCanvas = canvas.nativeCanvas
        val clipPath = cropPathFor(
            bitmapWidth = baseBitmap.width,
            bitmapHeight = baseBitmap.height,
            corners = corners,
            transform = transform,
        )
        nativeCanvas.save()
        nativeCanvas.clipPath(clipPath)
        if (blinkCompareStep == BlinkCompareStep.Outline) {
            nativeCanvas.drawColor(android.graphics.Color.WHITE)
        } else {
            canvas.nativeCanvas.drawBitmap(
                baseBitmap,
                perspectiveMatrix(
                    bitmapWidth = baseBitmap.width,
                    bitmapHeight = baseBitmap.height,
                    corners = corners,
                    transform = transform,
                ),
                basePaint,
            )
        }

        if (overlayBitmap != null && transform.isOverlayVisible && blinkCompareStep == BlinkCompareStep.Filtered) {
            canvas.nativeCanvas.drawBitmap(
                overlayBitmap,
                perspectiveMatrix(
                    bitmapWidth = overlayBitmap.width,
                    bitmapHeight = overlayBitmap.height,
                    corners = corners,
                    transform = transform,
                ),
                overlayPaint(transform),
            )
        }

        if (clarityBitmap != null && transform.clarity > 0f && blinkCompareStep == BlinkCompareStep.Filtered) {
            canvas.nativeCanvas.drawBitmap(
                clarityBitmap,
                perspectiveMatrix(
                    bitmapWidth = clarityBitmap.width,
                    bitmapHeight = clarityBitmap.height,
                    corners = corners,
                    transform = transform,
                ),
                clarityPaint(strength = transform.clarity, transform = transform),
            )
        }

        if (
            magicOutlineBitmap != null &&
            (transform.magicOutlineStrength > 0f || blinkCompareStep == BlinkCompareStep.Outline) &&
            blinkCompareStep != BlinkCompareStep.Original &&
            blinkCompareStep != BlinkCompareStep.BlackWhite
        ) {
            canvas.nativeCanvas.drawBitmap(
                magicOutlineBitmap,
                perspectiveMatrix(
                    bitmapWidth = magicOutlineBitmap.width,
                    bitmapHeight = magicOutlineBitmap.height,
                    corners = corners,
                    transform = transform,
                ),
                magicOutlinePaint(
                    strength = if (blinkCompareStep == BlinkCompareStep.Outline) 1f else transform.magicOutlineStrength,
                    transform = if (blinkCompareStep == BlinkCompareStep.Outline) TransformState() else transform,
                ),
            )
        }

        if (edgeOutlineBitmap != null && transform.edgeOutlineStrength > 0f && blinkCompareStep == BlinkCompareStep.Filtered) {
            canvas.nativeCanvas.drawBitmap(
                edgeOutlineBitmap,
                perspectiveMatrix(
                    bitmapWidth = edgeOutlineBitmap.width,
                    bitmapHeight = edgeOutlineBitmap.height,
                    corners = corners,
                    transform = transform,
                ),
                edgeOutlinePaint(strength = transform.edgeOutlineStrength, transform = transform),
            )
        }

        if (lineArtBitmap != null && transform.lineArt > 0f && blinkCompareStep == BlinkCompareStep.Filtered) {
            canvas.nativeCanvas.drawBitmap(
                lineArtBitmap,
                perspectiveMatrix(
                    bitmapWidth = lineArtBitmap.width,
                    bitmapHeight = lineArtBitmap.height,
                    corners = corners,
                    transform = transform,
                ),
                lineArtPaint(strength = transform.lineArt, transform = transform),
            )
        }
        nativeCanvas.restore()
    }
}

private fun TransformState.hasAttachedCanvasGuides(): Boolean =
    isGridVisible ||
        isCanvasFrameVisible ||
        isCropEnabled ||
        currentMode == TraceMode.CropEdge ||
        currentMode == TraceMode.CropAdjust ||
        isGuideFrameVisible ||
        guideShape != GuideShape.Off

private fun DrawScope.drawAttachedCanvasGuides(
    bitmapSize: IntSize,
    transform: TransformState,
) {
    if (transform.isGridVisible) {
        drawAttachedGridGuide(bitmapSize = bitmapSize, transform = transform)
    }

    drawAttachedCanvasFrame(bitmapSize = bitmapSize, transform = transform)
    drawAttachedCropGuide(bitmapSize = bitmapSize, transform = transform)
    drawAttachedAlignmentCorners(bitmapSize = bitmapSize, transform = transform)
    drawAttachedShapeGuide(bitmapSize = bitmapSize, transform = transform)
}

private fun DrawScope.drawAttachedGridGuide(
    bitmapSize: IntSize,
    transform: TransformState,
) {
    if (!transform.isGridVisible) return

    val corners = fittedImageCorners(
        bitmapSize = bitmapSize,
        viewportSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        transform = transform,
    )
    val crop = transform.sourceCropFor(
        bitmapWidth = bitmapSize.width,
        bitmapHeight = bitmapSize.height,
    )
    val cropWidth = crop.right - crop.left
    val cropHeight = crop.bottom - crop.top
    if (cropWidth <= 1f || cropHeight <= 1f) return

    val gridMatrix = perspectiveMatrix(
        bitmapWidth = bitmapSize.width,
        bitmapHeight = bitmapSize.height,
        corners = corners,
        transform = transform,
    )
    val spacing = transform.gridSpacingPx.coerceIn(MinGridSpacingPx, MaxGridSpacingPx)
    val averageGridWidth = (
        corners.topLeft.distanceTo(corners.topRight) +
            corners.bottomLeft.distanceTo(corners.bottomRight)
        ) / 2f
    val averageGridHeight = (
        corners.topLeft.distanceTo(corners.bottomLeft) +
            corners.topRight.distanceTo(corners.bottomRight)
        ) / 2f
    if (averageGridWidth <= 1f || averageGridHeight <= 1f) return

    val color = when (transform.gridColorMode) {
        GridColorMode.White -> Color(0x70FFFFFF)
        GridColorMode.Black -> Color(0x99000000)
    }
    val stroke = 1.2f

    var localX = spacing
    while (localX < averageGridWidth) {
        val u = (localX / averageGridWidth).coerceIn(0f, 1f)
        val sourceX = crop.left + cropWidth * u
        val mapped = floatArrayOf(sourceX, crop.top, sourceX, crop.bottom)
        gridMatrix.mapPoints(mapped)
        drawLine(
            color = color,
            start = Offset(mapped[0], mapped[1]),
            end = Offset(mapped[2], mapped[3]),
            strokeWidth = stroke,
        )
        localX += spacing
    }

    var localY = spacing
    while (localY < averageGridHeight) {
        val v = (localY / averageGridHeight).coerceIn(0f, 1f)
        val sourceY = crop.top + cropHeight * v
        val mapped = floatArrayOf(crop.left, sourceY, crop.right, sourceY)
        gridMatrix.mapPoints(mapped)
        drawLine(
            color = color,
            start = Offset(mapped[0], mapped[1]),
            end = Offset(mapped[2], mapped[3]),
            strokeWidth = stroke,
        )
        localY += spacing
    }
}

private fun DrawScope.drawAttachedCanvasFrame(
    bitmapSize: IntSize,
    transform: TransformState,
) {
    if (!transform.isCanvasFrameVisible) return

    val corners = fittedImageCorners(
        bitmapSize = bitmapSize,
        viewportSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        transform = transform,
    )
    val crop = transform.sourceCropFor(bitmapWidth = bitmapSize.width, bitmapHeight = bitmapSize.height)
    val cropWidth = crop.right - crop.left
    val cropHeight = crop.bottom - crop.top
    if (cropWidth <= 1f || cropHeight <= 1f) return

    val frameAspect = transform.canvasFrameAspectRatio()
    val cropAspect = cropWidth / cropHeight
    val frameWidth: Float
    val frameHeight: Float
    if (cropAspect > frameAspect) {
        frameHeight = cropHeight * 0.92f
        frameWidth = frameHeight * frameAspect
    } else {
        frameWidth = cropWidth * 0.92f
        frameHeight = frameWidth / frameAspect
    }

    val left = crop.left + (cropWidth - frameWidth) / 2f
    val top = crop.top + (cropHeight - frameHeight) / 2f
    val right = left + frameWidth
    val bottom = top + frameHeight
    val matrix = perspectiveMatrix(
        bitmapWidth = bitmapSize.width,
        bitmapHeight = bitmapSize.height,
        corners = corners,
        transform = transform,
    )
    val points = matrix.mapSourcePoints(
        left, top,
        right, top,
        right, bottom,
        left, bottom,
    )
    val active = transform.currentMode == TraceMode.CanvasFrame ||
        transform.currentMode == TraceMode.CanvasFrameRatio ||
        transform.currentMode == TraceMode.CanvasFrameColor
    val color = transform.canvasFrameColor.composeCornerColor(alpha = if (active) 0.92f else 0.72f)
    drawClosedPolyline(points, color = color, strokeWidth = if (active) 3.4f else 2.4f)
}

private fun DrawScope.drawAttachedCropGuide(
    bitmapSize: IntSize,
    transform: TransformState,
) {
    val isCropControlActive = transform.currentMode == TraceMode.CropEdge || transform.currentMode == TraceMode.CropAdjust
    if (!transform.isCropEnabled && !isCropControlActive) return

    val corners = fittedImageCorners(
        bitmapSize = bitmapSize,
        viewportSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        transform = transform,
    )
    val matrix = perspectiveMatrix(
        bitmapWidth = bitmapSize.width,
        bitmapHeight = bitmapSize.height,
        corners = corners,
        transform = transform.copy(isCropEnabled = false),
    )
    val left = bitmapSize.width * transform.cropLeft.coerceIn(0f, 0.45f)
    val top = bitmapSize.height * transform.cropTop.coerceIn(0f, 0.45f)
    val right = bitmapSize.width * (1f - transform.cropRight.coerceIn(0f, 0.45f))
    val bottom = bitmapSize.height * (1f - transform.cropBottom.coerceIn(0f, 0.45f))
    if (right <= left + 1f || bottom <= top + 1f) return

    val points = matrix.mapSourcePoints(
        left, top,
        right, top,
        right, bottom,
        left, bottom,
    )
    val frameColor = if (transform.currentMode == TraceMode.CropAdjust) Color(0x99F4F7FB) else Color(0xFFFFD166)
    drawClosedPolyline(points, color = frameColor, strokeWidth = if (isCropControlActive) 3.2f else 2.2f)

    if (isCropControlActive) {
        val selectedStroke = 5.2f
        when (transform.selectedCropEdge) {
            CropEdge.Left -> drawLine(Color(0xFFFFD166), points[0], points[3], strokeWidth = selectedStroke)
            CropEdge.Top -> drawLine(Color(0xFFFFD166), points[0], points[1], strokeWidth = selectedStroke)
            CropEdge.Right -> drawLine(Color(0xFFFFD166), points[1], points[2], strokeWidth = selectedStroke)
            CropEdge.Bottom -> drawLine(Color(0xFFFFD166), points[3], points[2], strokeWidth = selectedStroke)
        }
    }
}

private fun DrawScope.drawAttachedAlignmentCorners(
    bitmapSize: IntSize,
    transform: TransformState,
) {
    if (!transform.isGuideFrameVisible) return

    val corners = fittedImageCorners(
        bitmapSize = bitmapSize,
        viewportSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        transform = transform,
    )
    val width = (
        corners.topLeft.distanceTo(corners.topRight) +
            corners.bottomLeft.distanceTo(corners.bottomRight)
        ) / 2f
    val height = (
        corners.topLeft.distanceTo(corners.bottomLeft) +
            corners.topRight.distanceTo(corners.bottomRight)
        ) / 2f
    if (width <= 1f || height <= 1f) return

    val insetU = (transform.guideInsetX / width).coerceIn(0f, 0.45f)
    val insetV = (transform.guideInsetY / height).coerceIn(0f, 0.45f)
    val cornerLengthU = (70f / width).coerceIn(0.04f, 0.14f)
    val cornerLengthV = (70f / height).coerceIn(0.04f, 0.14f)
    val baseColor = if (
        transform.currentMode == TraceMode.GuideCorner ||
        transform.currentMode == TraceMode.GuideCornerColor ||
        transform.currentMode == TraceMode.GuideCornerMove
    ) {
        transform.guideCornerColor.composeCornerColor(alpha = 0.92f)
    } else {
        transform.guideCornerColor.composeCornerColor(alpha = 0.72f)
    }
    val inactiveColor = transform.guideCornerColor.composeCornerColor(
        alpha = if (
            transform.currentMode == TraceMode.GuideCorner ||
            transform.currentMode == TraceMode.GuideCornerColor ||
            transform.currentMode == TraceMode.GuideCornerMove
        ) {
            0.46f
        } else {
            0.72f
        },
    )
    val stroke = if (
        transform.currentMode == TraceMode.GuideCorner ||
        transform.currentMode == TraceMode.GuideCornerColor ||
        transform.currentMode == TraceMode.GuideCornerMove
    ) {
        4f
    } else {
        2.6f
    }

    fun point(u: Float, v: Float): Offset = corners.interpolate(u, v)
    fun cornerOffset(corner: DistortCorner): Offset = when (corner) {
        DistortCorner.TopLeft -> transform.guideTopLeft.asOffset()
        DistortCorner.TopRight -> transform.guideTopRight.asOffset()
        DistortCorner.BottomRight -> transform.guideBottomRight.asOffset()
        DistortCorner.BottomLeft -> transform.guideBottomLeft.asOffset()
    }
    fun drawCorner(corner: DistortCorner, u: Float, v: Float, hEndU: Float, hEndV: Float, vEndU: Float, vEndV: Float) {
        val offset = cornerOffset(corner)
        val color = if (transform.guideSelectedCorner == corner) baseColor else inactiveColor
        val strokeWidth = if (transform.guideSelectedCorner == corner) stroke * 1.25f else stroke
        drawGuideCorner(
            corner = point(u, v) + offset,
            horizontalEnd = point(hEndU, hEndV) + offset,
            verticalEnd = point(vEndU, vEndV) + offset,
            color = color,
            strokeWidth = strokeWidth,
        )
    }

    drawCorner(DistortCorner.TopLeft, insetU, insetV, insetU + cornerLengthU, insetV, insetU, insetV + cornerLengthV)
    drawCorner(DistortCorner.TopRight, 1f - insetU, insetV, 1f - insetU - cornerLengthU, insetV, 1f - insetU, insetV + cornerLengthV)
    drawCorner(DistortCorner.BottomRight, 1f - insetU, 1f - insetV, 1f - insetU - cornerLengthU, 1f - insetV, 1f - insetU, 1f - insetV - cornerLengthV)
    drawCorner(DistortCorner.BottomLeft, insetU, 1f - insetV, insetU + cornerLengthU, 1f - insetV, insetU, 1f - insetV - cornerLengthV)
}

private fun DrawScope.drawAttachedShapeGuide(
    bitmapSize: IntSize,
    transform: TransformState,
) {
    if (transform.guideShape == GuideShape.Off) return

    val corners = fittedImageCorners(
        bitmapSize = bitmapSize,
        viewportSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        transform = transform,
    )
    val crop = transform.sourceCropFor(bitmapWidth = bitmapSize.width, bitmapHeight = bitmapSize.height)
    val matrix = perspectiveMatrix(
        bitmapWidth = bitmapSize.width,
        bitmapHeight = bitmapSize.height,
        corners = corners,
        transform = transform,
    )
    val active = transform.currentMode == TraceMode.Shape ||
        transform.currentMode == TraceMode.ShapeColor ||
        transform.currentMode == TraceMode.ShapeRotation
    val lineColor = transform.guideShapeColor.composeShapeColor(alpha = if (active) 0.92f else 0.72f)
    val accentColor = transform.guideShapeColor.composeShapeColor(alpha = if (active) 0.72f else 0.48f)
    val stroke = if (active) 3f else 2f
    val cx = (crop.left + crop.right) / 2f
    val cy = (crop.top + crop.bottom) / 2f

    fun p(u: Float, v: Float): Pair<Float, Float> {
        val x = crop.left + (crop.right - crop.left) * u
        val y = crop.top + (crop.bottom - crop.top) * v
        return rotateSourcePoint(x, y, cx, cy, transform.guideShapeRotationDegrees)
    }
    fun line(u1: Float, v1: Float, u2: Float, v2: Float, color: Color = lineColor) {
        val a = p(u1, v1)
        val b = p(u2, v2)
        val mapped = matrix.mapSourcePoints(a.first, a.second, b.first, b.second)
        drawLine(color, mapped[0], mapped[1], strokeWidth = stroke)
    }

    when (transform.guideShape) {
        GuideShape.Off -> Unit
        GuideShape.Thirds -> {
            line(1f / 3f, 0f, 1f / 3f, 1f)
            line(2f / 3f, 0f, 2f / 3f, 1f)
            line(0f, 1f / 3f, 1f, 1f / 3f)
            line(0f, 2f / 3f, 1f, 2f / 3f)
        }
        GuideShape.ThirdsDense -> {
            for (i in 1..5) {
                val t = i / 6f
                val c = if (i == 2 || i == 4) lineColor else accentColor
                line(t, 0f, t, 1f, c)
                line(0f, t, 1f, t, c)
            }
        }
        GuideShape.GoldenRatio -> {
            val a = 0.382f
            val b = 0.618f
            line(a, 0f, a, 1f)
            line(b, 0f, b, 1f)
            line(0f, a, 1f, a)
            line(0f, b, 1f, b)
        }
        GuideShape.GoldenSpiral -> {
            val a = 0.382f
            val b = 0.618f
            line(b, 0f, b, 1f)
            line(b, b, 1f, b)
            line(a, 0f, a, a)
            line(0f, a, a, a)
            line(a, a, b, b, accentColor)
            line(b, b, 1f, 1f, accentColor)
        }
        GuideShape.Diagonal -> {
            line(0f, 0f, 1f, 1f)
            line(1f, 0f, 0f, 1f)
            line(0.5f, 0f, 1f, 0.5f, accentColor)
            line(0f, 0.5f, 0.5f, 1f, accentColor)
        }
        GuideShape.DynamicSymmetry -> {
            line(0f, 0f, 1f, 1f)
            line(1f, 0f, 0f, 1f)
            line(0.25f, 0f, 1f, 0.75f, accentColor)
            line(0f, 0.25f, 0.75f, 1f, accentColor)
            line(0.75f, 0f, 0f, 0.75f, accentColor)
            line(1f, 0.25f, 0.25f, 1f, accentColor)
        }
        GuideShape.Triangle -> {
            line(0f, 0f, 1f, 1f)
            line(1f, 0f, 0f, 1f)
            line(0.5f, 0f, 0f, 1f, accentColor)
            line(0.5f, 0f, 1f, 1f, accentColor)
        }
        GuideShape.CenterCross -> {
            line(0.5f, 0f, 0.5f, 1f)
            line(0f, 0.5f, 1f, 0.5f)
        }
    }
}

private fun perspectiveMatrix(
    bitmapWidth: Int,
    bitmapHeight: Int,
    corners: ImageCorners,
    transform: TransformState,
): Matrix {
    val source = floatArrayOf(
        0f,
        0f,
        bitmapWidth.toFloat(),
        0f,
        bitmapWidth.toFloat(),
        bitmapHeight.toFloat(),
        0f,
        bitmapHeight.toFloat(),
    )
    val target = floatArrayOf(
        corners.topLeft.x,
        corners.topLeft.y,
        corners.topRight.x,
        corners.topRight.y,
        corners.bottomRight.x,
        corners.bottomRight.y,
        corners.bottomLeft.x,
        corners.bottomLeft.y,
    )

    return Matrix().apply {
        setPolyToPoly(source, 0, target, 0, 4)
    }
}

private fun cropPathFor(
    bitmapWidth: Int,
    bitmapHeight: Int,
    corners: ImageCorners,
    transform: TransformState,
): Path {
    val crop = transform.sourceCropFor(bitmapWidth = bitmapWidth, bitmapHeight = bitmapHeight)
    val matrix = perspectiveMatrix(
        bitmapWidth = bitmapWidth,
        bitmapHeight = bitmapHeight,
        corners = corners,
        transform = transform,
    )
    val points = matrix.mapSourcePoints(
        crop.left,
        crop.top,
        crop.right,
        crop.top,
        crop.right,
        crop.bottom,
        crop.left,
        crop.bottom,
    )

    return Path().apply {
        moveTo(points[0].x, points[0].y)
        lineTo(points[1].x, points[1].y)
        lineTo(points[2].x, points[2].y)
        lineTo(points[3].x, points[3].y)
        close()
    }
}

private data class ThresholdRenderKey(
    val isEnabled: Boolean,
    val threshold: Float,
    val channelMixer: ChannelMixerMode,
)

private data class PaintRenderKey(
    val isThresholdEnabled: Boolean,
    val detail: Float,
)

private data class PosterizeRenderKey(
    val isThresholdEnabled: Boolean,
    val paintDetail: Float,
    val volume: Float,
    val strength: Float,
)

private data class VolumeRenderKey(
    val isThresholdEnabled: Boolean,
    val paintDetail: Float,
    val strength: Float,
)

private data class NoiseReductionRenderKey(
    val isThresholdEnabled: Boolean,
    val paintDetail: Float,
    val volume: Float,
    val posterize: Float,
    val strength: Float,
)

private data class EdgeOutlineRenderKey(
    val strength: Float,
    val thickness: Float,
    val detail: Float,
    val smoothing: Float,
)

private data class MagicOutlineRenderKey(
    val strength: Float,
    val detail: Float,
    val thickness: Float,
    val compareStep: BlinkCompareStep,
)

private data class SourceCrop(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

private fun TransformState.sourceCropFor(
    bitmapWidth: Int,
    bitmapHeight: Int,
): SourceCrop {
    if (!isCropEnabled) {
        return SourceCrop(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())
    }

    val left = (bitmapWidth * cropLeft.coerceIn(0f, 0.45f))
    val top = (bitmapHeight * cropTop.coerceIn(0f, 0.45f))
    val right = (bitmapWidth * (1f - cropRight.coerceIn(0f, 0.45f))).coerceAtLeast(left + 8f)
    val bottom = (bitmapHeight * (1f - cropBottom.coerceIn(0f, 0.45f))).coerceAtLeast(top + 8f)

    return SourceCrop(
        left = left,
        top = top,
        right = right.coerceAtMost(bitmapWidth.toFloat()),
        bottom = bottom.coerceAtMost(bitmapHeight.toFloat()),
    )
}

private fun bitmapPaintForFilters(
    transform: TransformState,
    isThresholdBitmap: Boolean,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        colorFilter = if (isThresholdBitmap) {
            if (transform.isInverted) ColorMatrixColorFilter(InvertColorMatrix) else null
        } else {
            imageColorFilter(
                imageBlendMode = transform.imageBlendMode,
                isBrightnessEnabled = transform.isBrightnessEnabled,
                brightness = transform.brightness,
                contrast = transform.contrast,
                channelMixer = transform.channelMixer,
                isInverted = transform.isInverted,
            )
        }
    }

private fun overlayPaint(transform: TransformState): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        alpha = (transform.overlayOpacity * 255f).roundToInt().coerceIn(0, 255)
        colorFilter = transform.finalInvertColorFilter()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            blendMode = when (transform.overlayBlendMode) {
                OverlayBlendMode.Normal -> null
                OverlayBlendMode.Multiply -> BlendMode.MULTIPLY
                OverlayBlendMode.Screen -> BlendMode.SCREEN
                OverlayBlendMode.Overlay -> BlendMode.OVERLAY
            }
        } else {
            xfermode = when (transform.overlayBlendMode) {
                OverlayBlendMode.Normal -> null
                OverlayBlendMode.Multiply -> android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY)
                OverlayBlendMode.Screen -> android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN)
                OverlayBlendMode.Overlay -> android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.OVERLAY)
            }
        }
    }

private fun lineArtPaint(
    strength: Float,
    transform: TransformState,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        alpha = (strength * 255f).roundToInt().coerceIn(0, 255)
        colorFilter = transform.finalInvertColorFilter()
    }

private fun edgeOutlinePaint(
    strength: Float,
    transform: TransformState,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        alpha = (strength * 255f).roundToInt().coerceIn(0, 255)
        colorFilter = transform.finalInvertColorFilter()
    }

private fun magicOutlinePaint(
    strength: Float,
    transform: TransformState,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        alpha = (strength.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        colorFilter = transform.finalInvertColorFilter()
    }

private fun blackWhitePaint(): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        colorFilter = BlackWhiteColorFilter
    }

private fun clarityPaint(
    strength: Float,
    transform: TransformState,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
        alpha = (strength * 180f).roundToInt().coerceIn(0, 180)
        colorFilter = transform.finalInvertColorFilter()
    }

private fun TransformState.finalInvertColorFilter(): ColorMatrixColorFilter? =
    if (isInverted) ColorMatrixColorFilter(InvertColorMatrix) else null

private fun imageColorFilter(
    imageBlendMode: ImageBlendMode,
    isBrightnessEnabled: Boolean,
    brightness: Float,
    contrast: Float,
    channelMixer: ChannelMixerMode,
    isInverted: Boolean,
): ColorMatrixColorFilter? {
    val brightnessMatrix = brightnessMatrix(isBrightnessEnabled, brightness)
    val contrastMatrix = contrastMatrix(contrast)
    val blendMatrix = imageBlendMatrix(imageBlendMode)
    val channelMatrix = channelMixerMatrix(channelMixer)
    val invertMatrix = if (isInverted) InvertColorMatrix else null
    val matrices = listOfNotNull(brightnessMatrix, contrastMatrix, blendMatrix, channelMatrix, invertMatrix)
    if (matrices.isEmpty()) return null

    val matrix = ColorMatrix(matrices.first())
    matrices.drop(1).forEach { nextMatrix ->
        matrix.setConcat(nextMatrix, matrix)
    }

    return ColorMatrixColorFilter(matrix)
}

private fun imageBlendMatrix(imageBlendMode: ImageBlendMode): ColorMatrix? = when (imageBlendMode) {
    ImageBlendMode.Normal -> null
    ImageBlendMode.Color -> ColorMatrix().apply {
        setSaturation(1.9f)
    }
    ImageBlendMode.Overlay -> ColorMatrix(
        floatArrayOf(
            1.18f, 0f, 0f, 0f, -18f,
            0f, 1.18f, 0f, 0f, -18f,
            0f, 0f, 1.18f, 0f, -18f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}

private fun brightnessMatrix(
    isEnabled: Boolean,
    brightness: Float,
): ColorMatrix? {
    if (!isEnabled) return null
    val multiplier = brightness.coerceIn(0f, 2f)
    if (abs(multiplier - 1f) < 0.001f) return null
    return ColorMatrix(
        floatArrayOf(
            multiplier, 0f, 0f, 0f, 0f,
            0f, multiplier, 0f, 0f, 0f,
            0f, 0f, multiplier, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}

private fun contrastMatrix(contrast: Float): ColorMatrix? {
    if (contrast == 1f) return null
    val translate = 128f * (1f - contrast)
    return ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}

private fun channelMixerMatrix(channelMixer: ChannelMixerMode): ColorMatrix? {
    if (channelMixer == ChannelMixerMode.Normal) return null
    val (low, high) = channelMixerEndpoints(channelMixer)
    val redSlope = (high.red - low.red) / 255f
    val greenSlope = (high.green - low.green) / 255f
    val blueSlope = (high.blue - low.blue) / 255f

    fun row(slope: Float, translate: Float): FloatArray = floatArrayOf(
        0.299f * slope,
        0.587f * slope,
        0.114f * slope,
        0f,
        translate,
    )

    val redRow = row(redSlope, low.red.toFloat())
    val greenRow = row(greenSlope, low.green.toFloat())
    val blueRow = row(blueSlope, low.blue.toFloat())

    return ColorMatrix(
        floatArrayOf(
            redRow[0], redRow[1], redRow[2], redRow[3], redRow[4],
            greenRow[0], greenRow[1], greenRow[2], greenRow[3], greenRow[4],
            blueRow[0], blueRow[1], blueRow[2], blueRow[3], blueRow[4],
            0f, 0f, 0f, 1f, 0f,
        )
    )
}

private data class RgbColor(
    val red: Int,
    val green: Int,
    val blue: Int,
) {
    fun toArgb(): Int = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun channelMixerEndpoints(channelMixer: ChannelMixerMode): Pair<RgbColor, RgbColor> = when (channelMixer) {
    ChannelMixerMode.Normal,
    ChannelMixerMode.BlackWhite -> RgbColor(0, 0, 0) to RgbColor(255, 255, 255)
    ChannelMixerMode.RedGreen -> RgbColor(255, 0, 0) to RgbColor(0, 255, 0)
    ChannelMixerMode.YellowBlue -> RgbColor(255, 230, 0) to RgbColor(0, 70, 255)
    ChannelMixerMode.MagentaCyan -> RgbColor(255, 0, 255) to RgbColor(0, 255, 255)
}

private fun DrawScope.drawDistortGuides(
    bitmapSize: IntSize,
    transform: TransformState,
) {
    val corners = fittedImageCorners(
        bitmapSize = bitmapSize,
        viewportSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        transform = transform,
    )
    val markerRadius = min(size.width, size.height).coerceAtMost(1080f) * 0.012f
    val lineColor = Color(0xCCF4F7FB)
    val activeColor = Color(0xFFFFD166)

    drawLine(lineColor, corners.topLeft, corners.topRight, strokeWidth = 3f)
    drawLine(lineColor, corners.topRight, corners.bottomRight, strokeWidth = 3f)
    drawLine(lineColor, corners.bottomRight, corners.bottomLeft, strokeWidth = 3f)
    drawLine(lineColor, corners.bottomLeft, corners.topLeft, strokeWidth = 3f)

    DistortCorner.values().forEach { corner ->
        drawCircle(
            color = if (corner == transform.selectedCorner) activeColor else lineColor,
            radius = if (corner == transform.selectedCorner) markerRadius * 1.4f else markerRadius,
            center = corners.pointFor(corner),
        )
        drawCornerLabel(
            corner = corner,
            point = corners.pointFor(corner),
            isActive = corner == transform.selectedCorner,
        )
    }
}

private fun DrawScope.drawCornerLabel(
    corner: DistortCorner,
    point: Offset,
    isActive: Boolean,
) {
    val baseSize = min(size.width, size.height).coerceAtMost(1080f)
    val labelTextSize = (baseSize * if (isActive) 0.026f else 0.02f).coerceIn(18f, 34f)
    val margin = 10f
    val isLeft = corner == DistortCorner.TopLeft || corner == DistortCorner.BottomLeft
    val isTop = corner == DistortCorner.TopLeft || corner == DistortCorner.TopRight
    val textX = (point.x + if (isLeft) margin else -margin).coerceIn(16f, size.width - 16f)
    val textY = (point.y + if (isTop) labelTextSize + margin else -margin)
        .coerceIn(labelTextSize + 8f, size.height - 8f)
    val labelAlign = if (isLeft) Paint.Align.LEFT else Paint.Align.RIGHT
    val label = corner.shortLabel()

    drawIntoCanvas { canvas ->
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCC000000.toInt()
            style = Paint.Style.STROKE
            strokeWidth = if (isActive) 5f else 4f
            textSize = labelTextSize
            textAlign = labelAlign
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isActive) 0xFFFFD166.toInt() else 0xDDF4F7FB.toInt()
            style = Paint.Style.FILL
            textSize = labelTextSize
            textAlign = labelAlign
        }

        canvas.nativeCanvas.drawText(label, textX, textY, outlinePaint)
        canvas.nativeCanvas.drawText(label, textX, textY, fillPaint)
    }
}

private fun DistortCorner.shortLabel(): String = when (this) {
    DistortCorner.TopLeft -> "TL"
    DistortCorner.TopRight -> "TR"
    DistortCorner.BottomRight -> "BR"
    DistortCorner.BottomLeft -> "BL"
}

private fun fittedImageCorners(
    bitmapSize: IntSize,
    viewportSize: IntSize,
    transform: TransformState,
): ImageCorners {
    val bitmapAspect = bitmapSize.width / bitmapSize.height.toFloat()
    val canvasRect = fittedCanvasRect(
        viewportSize = viewportSize,
        orientation = transform.canvasOrientation,
    )
    val canvasAspect = canvasRect.width / canvasRect.height
    val shouldFitByHeight = when (transform.imageFit) {
        ImageFit.Fit -> canvasAspect > bitmapAspect
        ImageFit.Fill -> canvasAspect < bitmapAspect
        ImageFit.Stretch -> false
    }
    val targetWidth = when {
        transform.imageFit == ImageFit.Stretch -> canvasRect.width
        shouldFitByHeight -> canvasRect.height * bitmapAspect
        else -> canvasRect.width
    }
    val targetHeight = when {
        transform.imageFit == ImageFit.Stretch -> canvasRect.height
        shouldFitByHeight -> canvasRect.height
        else -> canvasRect.width / bitmapAspect
    }

    val left = canvasRect.left + (canvasRect.width - targetWidth) / 2f
    val top = canvasRect.top + (canvasRect.height - targetHeight) / 2f
    val right = left + targetWidth
    val bottom = top + targetHeight

    return ImageCorners(
        topLeft = Offset(left, top) + transform.topLeft.asOffset(),
        topRight = Offset(right, top) + transform.topRight.asOffset(),
        bottomRight = Offset(right, bottom) + transform.bottomRight.asOffset(),
        bottomLeft = Offset(left, bottom) + transform.bottomLeft.asOffset(),
    )
}

private fun fittedCanvasRect(
    viewportSize: IntSize,
    orientation: CanvasOrientation,
): CanvasRect {
    val viewportWidth = viewportSize.width.toFloat()
    val viewportHeight = viewportSize.height.toFloat()
    val targetAspect = when (orientation) {
        CanvasOrientation.Auto -> viewportWidth / viewportHeight
        CanvasOrientation.Landscape -> 16f / 9f
        CanvasOrientation.Portrait -> 9f / 16f
    }
    val viewportAspect = viewportWidth / viewportHeight
    val canvasWidth: Float
    val canvasHeight: Float

    if (viewportAspect > targetAspect) {
        canvasHeight = viewportHeight
        canvasWidth = canvasHeight * targetAspect
    } else {
        canvasWidth = viewportWidth
        canvasHeight = canvasWidth / targetAspect
    }

    val left = (viewportWidth - canvasWidth) / 2f
    val top = (viewportHeight - canvasHeight) / 2f

    return CanvasRect(
        left = left,
        top = top,
        width = canvasWidth,
        height = canvasHeight,
    )
}

private fun CornerOffset.asOffset(): Offset = Offset(x, y)

private fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}

private fun Offset.lerp(other: Offset, amount: Float): Offset =
    Offset(
        x = x + (other.x - x) * amount,
        y = y + (other.y - y) * amount,
    )

private fun Matrix.mapSourcePoints(vararg values: Float): List<Offset> {
    val mapped = values.copyOf()
    mapPoints(mapped)
    return mapped.toList()
        .chunked(2)
        .map { point -> Offset(point[0], point[1]) }
}

private fun rotateSourcePoint(
    x: Float,
    y: Float,
    centerX: Float,
    centerY: Float,
    degrees: Float,
): Pair<Float, Float> {
    if (abs(degrees) < 0.001f) return x to y

    val radians = Math.toRadians(degrees.toDouble())
    val cosValue = cos(radians).toFloat()
    val sinValue = sin(radians).toFloat()
    val dx = x - centerX
    val dy = y - centerY
    return (centerX + dx * cosValue - dy * sinValue) to
        (centerY + dx * sinValue + dy * cosValue)
}

private fun DrawScope.drawClosedPolyline(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float,
) {
    if (points.size < 2) return

    points.indices.forEach { index ->
        drawLine(
            color = color,
            start = points[index],
            end = points[(index + 1) % points.size],
            strokeWidth = strokeWidth,
        )
    }
}

private data class CanvasRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

private data class ImageCorners(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomRight: Offset,
    val bottomLeft: Offset,
) {
    fun pointFor(corner: DistortCorner): Offset = when (corner) {
        DistortCorner.TopLeft -> topLeft
        DistortCorner.TopRight -> topRight
        DistortCorner.BottomRight -> bottomRight
        DistortCorner.BottomLeft -> bottomLeft
    }

    fun interpolate(u: Float, v: Float): Offset {
        val top = topLeft.lerp(topRight, u)
        val bottom = bottomLeft.lerp(bottomRight, u)
        return top.lerp(bottom, v)
    }
}

private fun ImageCorners.asPath(): Path = Path().apply {
    moveTo(topLeft.x, topLeft.y)
    lineTo(topRight.x, topRight.y)
    lineTo(bottomRight.x, bottomRight.y)
    lineTo(bottomLeft.x, bottomLeft.y)
    close()
}

private fun Context.persistReadPermission(uri: Uri): Boolean {
    if (hasPersistedReadPermission(uri)) return true

    try {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return hasPersistedReadPermission(uri)
    } catch (_: SecurityException) {
        // Some providers grant temporary read access only.
    } catch (_: IllegalArgumentException) {
        // Ignore providers that do not support persistable URI permissions.
    }

    return false
}

private fun Context.releaseReadPermission(uri: Uri) {
    try {
        contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // Ignore providers that only granted temporary access.
    } catch (_: IllegalArgumentException) {
        // Ignore providers that do not support persistable URI permissions.
    }
}

private fun Context.canRestoreImageUri(uri: Uri): Boolean {
    if (hasPersistedReadPermission(uri)) return true
    if (DocumentsContract.isDocumentUri(this, uri)) {
        val treeId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        if (treeId != null && contentResolver.persistedUriPermissions.any { grant ->
                grant.isReadPermission && grant.uri.authority == uri.authority &&
                    runCatching { DocumentsContract.getTreeDocumentId(grant.uri) }.getOrNull() == treeId
            }) return true
    }
    if (uri.scheme == ContentResolver.SCHEME_FILE) return true

    return uri.scheme == ContentResolver.SCHEME_CONTENT &&
        uri.authority == "media" &&
        hasMediaImagePermission()
}

private fun Context.hasPersistedReadPermission(uri: Uri): Boolean =
    contentResolver.persistedUriPermissions.any { permission ->
        permission.uri == uri && permission.isReadPermission
    }

private fun Context.hasMediaImagePermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val hasFullImageAccess =
            checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val hasSelectedImageAccess =
            checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        return hasFullImageAccess || hasSelectedImageAccess
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    }

    return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

private fun mediaImagePermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun Context.createImageFileBrowserIntent(): Intent {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType("image/*")
        .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

    findDocumentsUiPackage(intent)?.let { documentsPackage ->
        intent.setPackage(documentsPackage)
    }

    return intent
}

private fun Context.findDocumentsUiPackage(intent: Intent): String? {
    val handlers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    return handlers
        .mapNotNull { it.activityInfo?.packageName }
        .firstOrNull { packageName ->
            packageName.contains("documentsui", ignoreCase = true)
        }
}

private fun Context.canOpenDocumentImage(): Boolean {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType("image/*")
    return hasRealImageIntentHandler(intent)
}

private fun Context.canGetContentImage(): Boolean {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType("image/*")
    return hasRealImageIntentHandler(intent)
}

private fun Context.canOpenDocumentTree(): Boolean {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    return hasRealImageIntentHandler(intent)
}

private fun Context.hasRealImageIntentHandler(intent: Intent): Boolean {
    val handlers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    return handlers.any { resolveInfo ->
        val activityInfo = resolveInfo.activityInfo ?: return@any false
        activityInfo.enabled && activityInfo.packageName != "com.android.tv.frameworkpackagestubs"
    }
}

private suspend fun loadImageLibrary(
    context: Context,
    folderUri: Uri?,
    includeMediaStore: Boolean,
): List<MediaImageItem> {
    val mediaItems = if (includeMediaStore) {
        loadMediaImageLibrary(context)
    } else {
        emptyList()
    }
    val folderItems = folderUri?.let { loadFolderImageLibrary(context, it) }.orEmpty()

    return (folderItems + mediaItems)
        .sortedWith(
            compareByDescending<MediaImageItem> { it.dateModifiedSeconds }
                .thenBy { it.displayName.lowercase(Locale.US) }
        )
        .take(MaxLibraryImages)
}

private suspend fun loadMediaImageLibrary(context: Context): List<MediaImageItem> = withContext(Dispatchers.IO) {
    cancellableImageResult {
        val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context).toList().ifEmpty {
                listOf(MediaStore.VOLUME_EXTERNAL)
            }
        } else {
            listOf("external")
        }
        val images = mutableListOf<MediaImageItem>()

        volumes.forEach { volume ->
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(volume)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED,
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                var volumeImageCount = 0

                while (cursor.moveToNext() && volumeImageCount < MaxLibraryImages) {
                    currentCoroutineContext().ensureActive()
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn).orEmpty().ifBlank { "Image $id" }
                    images += MediaImageItem(
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = name,
                        volumeName = volume,
                        dateModifiedSeconds = cursor.getLong(dateColumn),
                    )
                    volumeImageCount += 1
                }
            }
        }

        images
            .sortedWith(
                compareByDescending<MediaImageItem> { it.dateModifiedSeconds }
                    .thenBy { it.displayName.lowercase(Locale.US) }
            )
            .take(MaxLibraryImages)
    }.getOrElse {
        emptyList()
    }
}

private suspend fun loadFolderImageLibrary(
    context: Context,
    treeUri: Uri,
): List<MediaImageItem> = withContext(Dispatchers.IO) {
    cancellableImageResult {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val images = mutableListOf<MediaImageItem>()

        scanFolderImages(
            context = context,
            treeUri = treeUri,
            documentId = rootDocumentId,
            folderLabel = "Selected folder",
            depth = 0,
            images = images,
        )

        images
    }.getOrElse {
        emptyList()
    }
}

private suspend fun scanFolderImages(
    context: Context,
    treeUri: Uri,
    documentId: String,
    folderLabel: String,
    depth: Int,
    images: MutableList<MediaImageItem>,
    visited: MutableSet<String> = mutableSetOf(),
) {
    currentCoroutineContext().ensureActive()
    if (depth > MaxFolderScanDepth || images.size >= MaxLibraryImages || visited.size >= 10_000 || !visited.add(documentId)) return

    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    context.contentResolver.query(
        childrenUri,
        projection,
        null,
        null,
        null,
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
        val modifiedColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

        while (cursor.moveToNext() && images.size < MaxLibraryImages && visited.size < 10_000) {
            currentCoroutineContext().ensureActive()
            val childDocumentId = cursor.getStringOrNull(idColumn) ?: continue
            val name = cursor.getStringOrNull(nameColumn).orEmpty().ifBlank { "Image ${images.size + 1}" }
            val mimeType = cursor.getStringOrNull(mimeColumn).orEmpty()
            val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocumentId)

            when {
                isImageDocument(mimeType = mimeType, displayName = name) -> {
                    val modifiedSeconds = cursor.getLongOrNull(modifiedColumn)?.let { it / 1000L } ?: 0L
                    images += MediaImageItem(
                        uri = childUri,
                        displayName = name,
                        volumeName = folderLabel,
                        dateModifiedSeconds = modifiedSeconds,
                    )
                }

                mimeType == DocumentsContract.Document.MIME_TYPE_DIR -> {
                    scanFolderImages(
                        context = context,
                        treeUri = treeUri,
                        documentId = childDocumentId,
                        folderLabel = name,
                        depth = depth + 1,
                        images = images,
                        visited = visited,
                    )
                }
            }
        }
    }
}

private fun isImageDocument(
    mimeType: String,
    displayName: String,
): Boolean {
    if (mimeType.startsWith("image/")) return true

    val lowerName = displayName.lowercase(Locale.US)
    return lowerName.endsWith(".jpg") ||
        lowerName.endsWith(".jpeg") ||
        lowerName.endsWith(".png") ||
        lowerName.endsWith(".webp") ||
        lowerName.endsWith(".bmp") ||
        lowerName.endsWith(".gif") ||
        lowerName.endsWith(".heic") ||
        lowerName.endsWith(".heif")
}

private fun android.database.Cursor.getStringOrNull(columnIndex: Int): String? {
    if (columnIndex < 0 || isNull(columnIndex)) return null
    return getString(columnIndex)
}

private fun android.database.Cursor.getLongOrNull(columnIndex: Int): Long? {
    if (columnIndex < 0 || isNull(columnIndex)) return null
    return getLong(columnIndex)
}

private suspend fun loadImageDisplayName(
    context: Context,
    uri: Uri,
): String? = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameColumn)
            } else {
                null
            }
        }
    }.getOrNull()
        ?.takeUnless { it.isBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')?.takeUnless { it.isBlank() }
}

private suspend fun loadImageSizeLabel(
    context: Context,
    uri: Uri,
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        var width = options.outWidth
        var height = options.outHeight
        if (width <= 0 || height <= 0) return@runCatching null

        val orientation = run {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }

        if (
            orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE
        ) {
            val previousWidth = width
            width = height
            height = previousWidth
        }

        "$width x $height px"
    }.getOrNull()
}

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other

internal fun Context.loadTransformState(): TransformState {
    val prefs = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    val legacyGuideInset = prefs.getSafeFloat(KeyGuideInset, 80f, 0f, MaxGuideInsetPx)
    return TransformState(
        scale = prefs.getSafeFloat(KeyScale, 1f, MinScale, MaxScale),
        offsetX = prefs.getSafeFloat(KeyOffsetX, 0f, -MaxSavedOffsetPx, MaxSavedOffsetPx),
        offsetY = prefs.getSafeFloat(KeyOffsetY, 0f, -MaxSavedOffsetPx, MaxSavedOffsetPx),
        rotationDegrees = prefs.getSafeFloat(
            key = KeyRotation,
            defaultValue = 0f,
            minValue = -MaxSavedRotationDegrees,
            maxValue = MaxSavedRotationDegrees,
        ),
        opacity = prefs.getSafeFloat(KeyOpacity, 1f, 0.1f, 1f),
        imageBlendMode = prefs.getEnum(KeyImageBlendMode, ImageBlendMode.Normal),
        isBrightnessEnabled = prefs.getBoolean(KeyBrightnessEnabled, false),
        brightness = prefs.getSafeFloat(KeyBrightness, 1f, 0f, 2f),
        isFlippedHorizontal = prefs.getBoolean(KeyFlipHorizontal, false),
        isFlippedVertical = prefs.getBoolean(KeyFlipVertical, false),
        contrast = prefs.getSafeFloat(KeyContrast, 1f, 0.5f, 2f),
        clarity = prefs.getSafeFloat(KeyClarity, 0f, 0f, 1f),
        noiseReduction = prefs.getSafeFloat(KeyNoiseReduction, 0f, 0f, 1f),
        lineArt = prefs.getSafeFloat(KeyLineArt, 0f, 0f, 1f),
        isThresholdEnabled = prefs.getBoolean(KeyThresholdEnabled, false),
        threshold = prefs.getSafeFloat(KeyThreshold, 0.5f, 0f, 1f),
        channelMixer = prefs.getEnum(KeyChannelMixer, ChannelMixerMode.Normal),
        paintDetail = prefs.getSafeFloat(KeyPaintDetail, 0f, 0f, 1f),
        posterize = prefs.getSafeFloat(KeyPosterize, 0f, 0f, 1f),
        volume = prefs.getSafeFloat(KeyVolume, 0f, 0f, 1f),
        magicOutlineStrength = prefs.getSafeFloat(KeyMagicOutlineStrength, 0f, 0f, 1f),
        magicOutlineDetail = prefs.getSafeFloat(KeyMagicOutlineDetail, 0.55f, 0f, 1f),
        magicOutlineThickness = prefs.getSafeFloat(KeyMagicOutlineThickness, 0.35f, 0f, 1f),
        edgeOutlineStrength = prefs.getSafeFloat(KeyEdgeOutlineStrength, 0f, 0f, 1f),
        edgeOutlineThickness = prefs.getSafeFloat(KeyEdgeOutlineThickness, 0.35f, 0f, 1f),
        edgeOutlineDetail = prefs.getSafeFloat(KeyEdgeOutlineDetail, 0.55f, 0f, 1f),
        edgeOutlineSmoothing = prefs.getSafeFloat(KeyEdgeOutlineSmoothing, 0.25f, 0f, 1f),
        isInverted = prefs.getBoolean(KeyInverted, false),
        tracingPreset = prefs.getEnum(KeyTracingPreset, TracingPreset.Custom),
        isLocked = prefs.getBoolean(KeyLocked, false),
        currentMode = prefs.getEnum(KeyMode, TraceMode.Move),
        controlStep = prefs.getEnum(KeyControlStep, ControlStep.Normal),
        rotationStepDegrees = prefs.getSafeFloat(KeyRotationStepDegrees, 1f, 1f, 90f).nearestRotationStep(),
        selectedCorner = prefs.getEnum(KeySelectedCorner, DistortCorner.TopLeft),
        topLeft = CornerOffset(
            x = prefs.getSafeFloat(KeyTopLeftX, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
            y = prefs.getSafeFloat(KeyTopLeftY, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        ),
        topRight = CornerOffset(
            x = prefs.getSafeFloat(KeyTopRightX, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
            y = prefs.getSafeFloat(KeyTopRightY, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        ),
        bottomRight = CornerOffset(
            x = prefs.getSafeFloat(KeyBottomRightX, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
            y = prefs.getSafeFloat(KeyBottomRightY, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        ),
        bottomLeft = CornerOffset(
            x = prefs.getSafeFloat(KeyBottomLeftX, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
            y = prefs.getSafeFloat(KeyBottomLeftY, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        ),
        guideInsetX = prefs.getSafeFloat(KeyGuideInsetX, legacyGuideInset, 0f, MaxGuideInsetPx),
        guideInsetY = prefs.getSafeFloat(KeyGuideInsetY, legacyGuideInset, 0f, MaxGuideInsetPx),
        guideSelectedCorner = prefs.getEnum(KeyGuideSelectedCorner, DistortCorner.TopLeft),
        guideCornerColor = prefs.getEnum(KeyGuideCornerColor, GuideCornerColor.White),
        guideTopLeft = CornerOffset(
            x = prefs.getSafeFloat(KeyGuideTopLeftX, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
            y = prefs.getSafeFloat(KeyGuideTopLeftY, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        ),
        guideTopRight = CornerOffset(
            x = prefs.getSafeFloat(KeyGuideTopRightX, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
            y = prefs.getSafeFloat(KeyGuideTopRightY, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        ),
        guideBottomRight = CornerOffset(
            x = prefs.getSafeFloat(KeyGuideBottomRightX, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
            y = prefs.getSafeFloat(KeyGuideBottomRightY, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        ),
        guideBottomLeft = CornerOffset(
            x = prefs.getSafeFloat(KeyGuideBottomLeftX, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
            y = prefs.getSafeFloat(KeyGuideBottomLeftY, 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        ),
        isGridVisible = prefs.getBoolean(KeyGridVisible, false),
        gridSpacingPx = prefs.getSafeFloat(KeyGridSpacing, 120f, MinGridSpacingPx, MaxGridSpacingPx),
        gridColorMode = prefs.getEnum(KeyGridColorMode, GridColorMode.White),
        isRulersVisible = prefs.getBoolean(KeyRulersVisible, false),
        isCenterCrossVisible = prefs.getBoolean(KeyCenterCrossVisible, true),
        isCanvasFrameVisible = prefs.getBoolean(KeyCanvasFrameVisible, false),
        canvasFramePreset = prefs.getEnum(KeyCanvasFramePreset, CanvasFramePreset.Ratio16x20),
        customCanvasFrameRatio = prefs.getSafeFloat(KeyCustomCanvasFrameRatio, 0.8f, 0.35f, 1.7f),
        canvasFrameColor = prefs.getEnum(KeyCanvasFrameColor, GuideCornerColor.White),
        isCropEnabled = prefs.getBoolean(KeyCropEnabled, false),
        selectedCropEdge = prefs.getEnum(KeySelectedCropEdge, CropEdge.Left),
        selectedCropCorner = prefs.getEnum(KeySelectedCropCorner, CropCorner.TopLeft),
        cropLeft = prefs.getSafeFloat(KeyCropLeft, 0f, 0f, 0.45f),
        cropTop = prefs.getSafeFloat(KeyCropTop, 0f, 0f, 0.45f),
        cropRight = prefs.getSafeFloat(KeyCropRight, 0f, 0f, 0.45f),
        cropBottom = prefs.getSafeFloat(KeyCropBottom, 0f, 0f, 0.45f),
        isGuideFrameVisible = prefs.getBoolean(KeyGuideFrameVisible, true),
        guideShape = prefs.getEnum(KeyGuideShape, GuideShape.Off),
        guideShapeColor = prefs.getEnum(KeyGuideShapeColor, GuideShapeColor.White),
        guideShapeRotationDegrees = prefs.getSafeFloat(KeyGuideShapeRotation, 0f, -MaxSavedRotationDegrees, MaxSavedRotationDegrees),
        colorPaletteCount = prefs.getInt(KeyColorPaletteCount, 0).let { count ->
            if (count <= 0) 0 else count.coerceIn(MinPaletteColors, MaxPaletteColors)
        },
        colorPaletteMode = prefs.getEnum(KeyColorPaletteMode, ColorPaletteMode.Balanced),
        colorPalettePosition = prefs.getEnum(KeyColorPalettePosition, PalettePosition.BottomEnd),
        colorPaletteOffsetX = prefs.getSafeFloat(KeyColorPaletteOffsetX, 0f, -MaxPaletteOffsetPx, MaxPaletteOffsetPx),
        colorPaletteOffsetY = prefs.getSafeFloat(KeyColorPaletteOffsetY, 0f, -MaxPaletteOffsetPx, MaxPaletteOffsetPx),
        colorPaletteScale = prefs.getSafeFloat(KeyColorPaletteScale, 1f, MinPaletteScale, MaxPaletteScale),
        isOverlayVisible = prefs.getBoolean(KeyOverlayVisible, false),
        overlayOpacity = prefs.getSafeFloat(KeyOverlayOpacity, 0.5f, 0f, 1f),
        overlayBlendMode = prefs.getEnum(KeyOverlayBlendMode, OverlayBlendMode.Normal),
        canvasOrientation = prefs.getEnum(KeyCanvasOrientation, CanvasOrientation.Auto),
        imageFit = prefs.getEnum(KeyImageFit, ImageFit.Fit),
        menuOffsetX = prefs.getSafeFloat(KeyMenuOffsetX, 0f, -MaxMenuOffsetPx, MaxMenuOffsetPx),
        menuOffsetY = prefs.getSafeFloat(KeyMenuOffsetY, 0f, -MaxMenuOffsetPx, MaxMenuOffsetPx),
    )
}

internal fun Context.saveTransformState(transform: TransformState) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putFloat(KeyScale, transform.scale)
        .putFloat(KeyOffsetX, transform.offsetX)
        .putFloat(KeyOffsetY, transform.offsetY)
        .putFloat(KeyRotation, transform.rotationDegrees)
        .putBoolean(KeyFlipHorizontal, transform.isFlippedHorizontal)
        .putBoolean(KeyFlipVertical, transform.isFlippedVertical)
        .putFloat(KeyOpacity, transform.opacity)
        .putString(KeyImageBlendMode, transform.imageBlendMode.name)
        .putBoolean(KeyBrightnessEnabled, transform.isBrightnessEnabled)
        .putFloat(KeyBrightness, transform.brightness)
        .putFloat(KeyContrast, transform.contrast)
        .putFloat(KeyClarity, transform.clarity)
        .putFloat(KeyNoiseReduction, transform.noiseReduction)
        .putFloat(KeyLineArt, transform.lineArt)
        .putBoolean(KeyThresholdEnabled, transform.isThresholdEnabled)
        .putFloat(KeyThreshold, transform.threshold)
        .putString(KeyChannelMixer, transform.channelMixer.name)
        .putFloat(KeyPaintDetail, transform.paintDetail)
        .putFloat(KeyPosterize, transform.posterize)
        .putFloat(KeyVolume, transform.volume)
        .putFloat(KeyMagicOutlineStrength, transform.magicOutlineStrength)
        .putFloat(KeyMagicOutlineDetail, transform.magicOutlineDetail)
        .putFloat(KeyMagicOutlineThickness, transform.magicOutlineThickness)
        .putFloat(KeyEdgeOutlineStrength, transform.edgeOutlineStrength)
        .putFloat(KeyEdgeOutlineThickness, transform.edgeOutlineThickness)
        .putFloat(KeyEdgeOutlineDetail, transform.edgeOutlineDetail)
        .putFloat(KeyEdgeOutlineSmoothing, transform.edgeOutlineSmoothing)
        .putBoolean(KeyInverted, transform.isInverted)
        .putString(KeyTracingPreset, transform.tracingPreset.name)
        .putBoolean(KeyLocked, transform.isLocked)
        .putString(KeyMode, transform.currentMode.name)
        .putString(KeyControlStep, transform.controlStep.name)
        .putFloat(KeyRotationStepDegrees, transform.rotationStepDegrees.nearestRotationStep())
        .putString(KeySelectedCorner, transform.selectedCorner.name)
        .putFloat(KeyTopLeftX, transform.topLeft.x)
        .putFloat(KeyTopLeftY, transform.topLeft.y)
        .putFloat(KeyTopRightX, transform.topRight.x)
        .putFloat(KeyTopRightY, transform.topRight.y)
        .putFloat(KeyBottomRightX, transform.bottomRight.x)
        .putFloat(KeyBottomRightY, transform.bottomRight.y)
        .putFloat(KeyBottomLeftX, transform.bottomLeft.x)
        .putFloat(KeyBottomLeftY, transform.bottomLeft.y)
        .putFloat(KeyGuideInset, min(transform.guideInsetX, transform.guideInsetY))
        .putFloat(KeyGuideInsetX, transform.guideInsetX)
        .putFloat(KeyGuideInsetY, transform.guideInsetY)
        .putString(KeyGuideSelectedCorner, transform.guideSelectedCorner.name)
        .putString(KeyGuideCornerColor, transform.guideCornerColor.name)
        .putFloat(KeyGuideTopLeftX, transform.guideTopLeft.x)
        .putFloat(KeyGuideTopLeftY, transform.guideTopLeft.y)
        .putFloat(KeyGuideTopRightX, transform.guideTopRight.x)
        .putFloat(KeyGuideTopRightY, transform.guideTopRight.y)
        .putFloat(KeyGuideBottomRightX, transform.guideBottomRight.x)
        .putFloat(KeyGuideBottomRightY, transform.guideBottomRight.y)
        .putFloat(KeyGuideBottomLeftX, transform.guideBottomLeft.x)
        .putFloat(KeyGuideBottomLeftY, transform.guideBottomLeft.y)
        .putBoolean(KeyGridVisible, transform.isGridVisible)
        .putFloat(KeyGridSpacing, transform.gridSpacingPx)
        .putString(KeyGridColorMode, transform.gridColorMode.name)
        .putBoolean(KeyRulersVisible, transform.isRulersVisible)
        .putBoolean(KeyCenterCrossVisible, transform.isCenterCrossVisible)
        .putBoolean(KeyCanvasFrameVisible, transform.isCanvasFrameVisible)
        .putString(KeyCanvasFramePreset, transform.canvasFramePreset.name)
        .putFloat(KeyCustomCanvasFrameRatio, transform.customCanvasFrameRatio)
        .putString(KeyCanvasFrameColor, transform.canvasFrameColor.name)
        .putBoolean(KeyCropEnabled, transform.isCropEnabled)
        .putString(KeySelectedCropEdge, transform.selectedCropEdge.name)
        .putString(KeySelectedCropCorner, transform.selectedCropCorner.name)
        .putFloat(KeyCropLeft, transform.cropLeft)
        .putFloat(KeyCropTop, transform.cropTop)
        .putFloat(KeyCropRight, transform.cropRight)
        .putFloat(KeyCropBottom, transform.cropBottom)
        .putBoolean(KeyGuideFrameVisible, transform.isGuideFrameVisible)
        .putString(KeyGuideShape, transform.guideShape.name)
        .putString(KeyGuideShapeColor, transform.guideShapeColor.name)
        .putFloat(KeyGuideShapeRotation, transform.guideShapeRotationDegrees)
        .putInt(KeyColorPaletteCount, transform.colorPaletteCount)
        .putString(KeyColorPaletteMode, transform.colorPaletteMode.name)
        .putString(KeyColorPalettePosition, transform.colorPalettePosition.name)
        .putFloat(KeyColorPaletteOffsetX, transform.colorPaletteOffsetX)
        .putFloat(KeyColorPaletteOffsetY, transform.colorPaletteOffsetY)
        .putFloat(KeyColorPaletteScale, transform.colorPaletteScale)
        .putBoolean(KeyOverlayVisible, transform.isOverlayVisible)
        .putFloat(KeyOverlayOpacity, transform.overlayOpacity)
        .putString(KeyOverlayBlendMode, transform.overlayBlendMode.name)
        .putString(KeyCanvasOrientation, transform.canvasOrientation.name)
        .putFloat(KeyMenuOffsetX, transform.menuOffsetX)
        .putFloat(KeyMenuOffsetY, transform.menuOffsetY)
        .putString(KeyImageFit, transform.imageFit.name)
        .apply()
}

private fun Context.loadLastImageUri(): Uri? {
    val value = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getString(KeyLastImageUri, null)
    return value?.let(Uri::parse)
}

private fun Context.saveLastImageUri(uri: Uri) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(KeyLastImageUri, uri.toString())
        .apply()
}

private fun Context.clearLastImageUri() {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .remove(KeyLastImageUri)
        .apply()
}

private fun Context.loadLastOverlayUri(): Uri? {
    val value = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getString(KeyLastOverlayUri, null)
    return value?.let(Uri::parse)
}

private fun Context.saveLastOverlayUri(uri: Uri) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(KeyLastOverlayUri, uri.toString())
        .apply()
}

private fun Context.clearLastOverlayUri() {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .remove(KeyLastOverlayUri)
        .apply()
}

private fun Context.loadLastFolderUri(): Uri? {
    val value = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getString(KeyLastFolderUri, null)
    return value?.let(Uri::parse)
}

private fun Context.saveLastFolderUri(uri: Uri) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(KeyLastFolderUri, uri.toString())
        .apply()
}

private fun Context.clearLastFolderUri() {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .remove(KeyLastFolderUri)
        .apply()
}

private fun Context.hasSavedProfile(): Boolean =
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .contains(KeySavedProfile)

private fun Context.saveProfile(transform: TransformState) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(KeySavedProfile, transform.toProfileJson().toString())
        .apply()
}

private fun Context.loadProfile(): TransformState? {
    val json = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getString(KeySavedProfile, null)
        ?: return null

    return runCatching {
        JSONObject(json).toTransformState()
    }.getOrNull()
}

private fun Context.clearProfile() {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .remove(KeySavedProfile)
        .apply()
}

private fun TransformState.toProfileJson(): JSONObject = JSONObject()
    .put("scale", scale)
    .put("offsetX", offsetX)
    .put("offsetY", offsetY)
    .put("rotationDegrees", rotationDegrees)
    .put("isFlippedHorizontal", isFlippedHorizontal)
    .put("isFlippedVertical", isFlippedVertical)
    .put("opacity", opacity)
    .put("imageBlendMode", imageBlendMode.name)
    .put("isBrightnessEnabled", isBrightnessEnabled)
    .put("brightness", brightness)
    .put("contrast", contrast)
    .put("clarity", clarity)
    .put("noiseReduction", noiseReduction)
    .put("lineArt", lineArt)
    .put("isThresholdEnabled", isThresholdEnabled)
    .put("threshold", threshold)
    .put("channelMixer", channelMixer.name)
    .put("paintDetail", paintDetail)
    .put("posterize", posterize)
    .put("volume", volume)
    .put("magicOutlineStrength", magicOutlineStrength)
    .put("magicOutlineDetail", magicOutlineDetail)
    .put("magicOutlineThickness", magicOutlineThickness)
    .put("edgeOutlineStrength", edgeOutlineStrength)
    .put("edgeOutlineThickness", edgeOutlineThickness)
    .put("edgeOutlineDetail", edgeOutlineDetail)
    .put("edgeOutlineSmoothing", edgeOutlineSmoothing)
    .put("isInverted", isInverted)
    .put("tracingPreset", tracingPreset.name)
    .put("isLocked", isLocked)
    .put("currentMode", currentMode.name)
    .put("controlStep", controlStep.name)
    .put("rotationStepDegrees", rotationStepDegrees.nearestRotationStep())
    .put("selectedCorner", selectedCorner.name)
    .put("topLeftX", topLeft.x)
    .put("topLeftY", topLeft.y)
    .put("topRightX", topRight.x)
    .put("topRightY", topRight.y)
    .put("bottomRightX", bottomRight.x)
    .put("bottomRightY", bottomRight.y)
    .put("bottomLeftX", bottomLeft.x)
    .put("bottomLeftY", bottomLeft.y)
    .put("guideInsetX", guideInsetX)
    .put("guideInsetY", guideInsetY)
    .put("guideSelectedCorner", guideSelectedCorner.name)
    .put("guideCornerColor", guideCornerColor.name)
    .put("guideTopLeftX", guideTopLeft.x)
    .put("guideTopLeftY", guideTopLeft.y)
    .put("guideTopRightX", guideTopRight.x)
    .put("guideTopRightY", guideTopRight.y)
    .put("guideBottomRightX", guideBottomRight.x)
    .put("guideBottomRightY", guideBottomRight.y)
    .put("guideBottomLeftX", guideBottomLeft.x)
    .put("guideBottomLeftY", guideBottomLeft.y)
    .put("isGridVisible", isGridVisible)
    .put("gridSpacingPx", gridSpacingPx)
    .put("gridColorMode", gridColorMode.name)
    .put("isRulersVisible", isRulersVisible)
    .put("isCenterCrossVisible", isCenterCrossVisible)
    .put("isCanvasFrameVisible", isCanvasFrameVisible)
    .put("canvasFramePreset", canvasFramePreset.name)
    .put("customCanvasFrameRatio", customCanvasFrameRatio)
    .put("canvasFrameColor", canvasFrameColor.name)
    .put("isCropEnabled", isCropEnabled)
    .put("selectedCropEdge", selectedCropEdge.name)
    .put("selectedCropCorner", selectedCropCorner.name)
    .put("cropLeft", cropLeft)
    .put("cropTop", cropTop)
    .put("cropRight", cropRight)
    .put("cropBottom", cropBottom)
    .put("isGuideFrameVisible", isGuideFrameVisible)
    .put("guideShape", guideShape.name)
    .put("guideShapeColor", guideShapeColor.name)
    .put("guideShapeRotationDegrees", guideShapeRotationDegrees)
    .put("colorPaletteCount", colorPaletteCount)
    .put("colorPaletteMode", colorPaletteMode.name)
    .put("colorPalettePosition", colorPalettePosition.name)
    .put("colorPaletteOffsetX", colorPaletteOffsetX)
    .put("colorPaletteOffsetY", colorPaletteOffsetY)
    .put("colorPaletteScale", colorPaletteScale)
    .put("isOverlayVisible", isOverlayVisible)
    .put("overlayOpacity", overlayOpacity)
    .put("overlayBlendMode", overlayBlendMode.name)
    .put("canvasOrientation", canvasOrientation.name)
    .put("imageFit", imageFit.name)
    .put("menuOffsetX", menuOffsetX)
    .put("menuOffsetY", menuOffsetY)

private fun JSONObject.toTransformState(): TransformState = TransformState(
    scale = optSafeFloat("scale", 1f, MinScale, MaxScale),
    offsetX = optSafeFloat("offsetX", 0f, -MaxSavedOffsetPx, MaxSavedOffsetPx),
    offsetY = optSafeFloat("offsetY", 0f, -MaxSavedOffsetPx, MaxSavedOffsetPx),
    rotationDegrees = optSafeFloat("rotationDegrees", 0f, -MaxSavedRotationDegrees, MaxSavedRotationDegrees),
    isFlippedHorizontal = optBoolean("isFlippedHorizontal", false),
    isFlippedVertical = optBoolean("isFlippedVertical", false),
    opacity = optSafeFloat("opacity", 1f, 0.1f, 1f),
    imageBlendMode = optEnum("imageBlendMode", ImageBlendMode.Normal),
    isBrightnessEnabled = optBoolean("isBrightnessEnabled", false),
    brightness = optSafeFloat("brightness", 1f, 0f, 2f),
    contrast = optSafeFloat("contrast", 1f, 0.5f, 2f),
    clarity = optSafeFloat("clarity", 0f, 0f, 1f),
    noiseReduction = optSafeFloat("noiseReduction", 0f, 0f, 1f),
    lineArt = optSafeFloat("lineArt", 0f, 0f, 1f),
    isThresholdEnabled = optBoolean("isThresholdEnabled", false),
    threshold = optSafeFloat("threshold", 0.5f, 0f, 1f),
    channelMixer = optEnum("channelMixer", ChannelMixerMode.Normal),
    paintDetail = optSafeFloat("paintDetail", 0f, 0f, 1f),
    posterize = optSafeFloat("posterize", 0f, 0f, 1f),
    volume = optSafeFloat("volume", 0f, 0f, 1f),
    magicOutlineStrength = optSafeFloat("magicOutlineStrength", 0f, 0f, 1f),
    magicOutlineDetail = optSafeFloat("magicOutlineDetail", 0.55f, 0f, 1f),
    magicOutlineThickness = optSafeFloat("magicOutlineThickness", 0.35f, 0f, 1f),
    edgeOutlineStrength = optSafeFloat("edgeOutlineStrength", 0f, 0f, 1f),
    edgeOutlineThickness = optSafeFloat("edgeOutlineThickness", 0.35f, 0f, 1f),
    edgeOutlineDetail = optSafeFloat("edgeOutlineDetail", 0.55f, 0f, 1f),
    edgeOutlineSmoothing = optSafeFloat("edgeOutlineSmoothing", 0.25f, 0f, 1f),
    isInverted = optBoolean("isInverted", false),
    tracingPreset = optEnum("tracingPreset", TracingPreset.Custom),
    isLocked = optBoolean("isLocked", false),
    currentMode = optEnum("currentMode", TraceMode.Move),
    controlStep = optEnum("controlStep", ControlStep.Normal),
    rotationStepDegrees = optSafeFloat("rotationStepDegrees", 1f, 1f, 90f).nearestRotationStep(),
    selectedCorner = optEnum("selectedCorner", DistortCorner.TopLeft),
    topLeft = CornerOffset(
        x = optSafeFloat("topLeftX", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        y = optSafeFloat("topLeftY", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
    ),
    topRight = CornerOffset(
        x = optSafeFloat("topRightX", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        y = optSafeFloat("topRightY", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
    ),
    bottomRight = CornerOffset(
        x = optSafeFloat("bottomRightX", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        y = optSafeFloat("bottomRightY", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
    ),
    bottomLeft = CornerOffset(
        x = optSafeFloat("bottomLeftX", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        y = optSafeFloat("bottomLeftY", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
    ),
    guideInsetX = optSafeFloat("guideInsetX", 80f, 0f, MaxGuideInsetPx),
    guideInsetY = optSafeFloat("guideInsetY", 80f, 0f, MaxGuideInsetPx),
    guideSelectedCorner = optEnum("guideSelectedCorner", DistortCorner.TopLeft),
    guideCornerColor = optEnum("guideCornerColor", GuideCornerColor.White),
    guideTopLeft = CornerOffset(
        x = optSafeFloat("guideTopLeftX", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        y = optSafeFloat("guideTopLeftY", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
    ),
    guideTopRight = CornerOffset(
        x = optSafeFloat("guideTopRightX", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        y = optSafeFloat("guideTopRightY", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
    ),
    guideBottomRight = CornerOffset(
        x = optSafeFloat("guideBottomRightX", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        y = optSafeFloat("guideBottomRightY", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
    ),
    guideBottomLeft = CornerOffset(
        x = optSafeFloat("guideBottomLeftX", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
        y = optSafeFloat("guideBottomLeftY", 0f, -MaxCornerOffsetPx, MaxCornerOffsetPx),
    ),
    isGridVisible = optBoolean("isGridVisible", false),
    gridSpacingPx = optSafeFloat("gridSpacingPx", 120f, MinGridSpacingPx, MaxGridSpacingPx),
    gridColorMode = optEnum("gridColorMode", GridColorMode.White),
    isRulersVisible = optBoolean("isRulersVisible", false),
    isCenterCrossVisible = optBoolean("isCenterCrossVisible", true),
    isCanvasFrameVisible = optBoolean("isCanvasFrameVisible", false),
    canvasFramePreset = optEnum("canvasFramePreset", CanvasFramePreset.Ratio16x20),
    customCanvasFrameRatio = optSafeFloat("customCanvasFrameRatio", 0.8f, 0.35f, 1.7f),
    canvasFrameColor = optEnum("canvasFrameColor", GuideCornerColor.White),
    isCropEnabled = optBoolean("isCropEnabled", false),
    selectedCropEdge = optEnum("selectedCropEdge", CropEdge.Left),
    selectedCropCorner = optEnum("selectedCropCorner", CropCorner.TopLeft),
    cropLeft = optSafeFloat("cropLeft", 0f, 0f, 0.45f),
    cropTop = optSafeFloat("cropTop", 0f, 0f, 0.45f),
    cropRight = optSafeFloat("cropRight", 0f, 0f, 0.45f),
    cropBottom = optSafeFloat("cropBottom", 0f, 0f, 0.45f),
    isGuideFrameVisible = optBoolean("isGuideFrameVisible", true),
    guideShape = optEnum("guideShape", GuideShape.Off),
    guideShapeColor = optEnum("guideShapeColor", GuideShapeColor.White),
    guideShapeRotationDegrees = optSafeFloat("guideShapeRotationDegrees", 0f, -MaxSavedRotationDegrees, MaxSavedRotationDegrees),
    colorPaletteCount = optInt("colorPaletteCount", 0).let { count ->
        if (count <= 0) 0 else count.coerceIn(MinPaletteColors, MaxPaletteColors)
    },
    colorPaletteMode = optEnum("colorPaletteMode", ColorPaletteMode.Balanced),
    colorPalettePosition = optEnum("colorPalettePosition", PalettePosition.BottomEnd),
    colorPaletteOffsetX = optSafeFloat("colorPaletteOffsetX", 0f, -MaxPaletteOffsetPx, MaxPaletteOffsetPx),
    colorPaletteOffsetY = optSafeFloat("colorPaletteOffsetY", 0f, -MaxPaletteOffsetPx, MaxPaletteOffsetPx),
    colorPaletteScale = optSafeFloat("colorPaletteScale", 1f, MinPaletteScale, MaxPaletteScale),
    isOverlayVisible = optBoolean("isOverlayVisible", false),
    overlayOpacity = optSafeFloat("overlayOpacity", 0.5f, 0f, 1f),
    overlayBlendMode = optEnum("overlayBlendMode", OverlayBlendMode.Normal),
    canvasOrientation = optEnum("canvasOrientation", CanvasOrientation.Auto),
    imageFit = optEnum("imageFit", ImageFit.Fit),
    menuOffsetX = optSafeFloat("menuOffsetX", 0f, -MaxMenuOffsetPx, MaxMenuOffsetPx),
    menuOffsetY = optSafeFloat("menuOffsetY", 0f, -MaxMenuOffsetPx, MaxMenuOffsetPx),
)

private fun JSONObject.optSafeFloat(
    key: String,
    defaultValue: Float,
    minValue: Float,
    maxValue: Float,
): Float = if (has(key)) {
    optDouble(key, defaultValue.toDouble()).toFloat().coerceIn(minValue, maxValue)
} else {
    defaultValue
}

private inline fun <reified T : Enum<T>> JSONObject.optEnum(
    key: String,
    defaultValue: T,
): T = runCatching {
    enumValueOf<T>(optString(key, defaultValue.name))
}.getOrDefault(defaultValue)

private fun Context.loadMenuSize(): TraceMenuSize {
    val prefs = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    return prefs.getEnum(KeyMenuSize, TraceMenuSize.Slim)
}

private fun Context.saveMenuSize(menuSize: TraceMenuSize) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(KeyMenuSize, menuSize.name)
        .apply()
}

private fun Context.loadHudDuration(): TraceHudDuration {
    val prefs = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    return prefs.getEnum(KeyHudDuration, TraceHudDuration.FiveSeconds)
}

private fun Context.saveHudDuration(duration: TraceHudDuration) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(KeyHudDuration, duration.name)
        .apply()
}

private fun Context.loadMenuAutoHide(): TraceMenuAutoHide {
    val prefs = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    return prefs.getEnum(KeyMenuAutoHide, TraceMenuAutoHide.FiveSeconds)
}

private fun Context.saveMenuAutoHide(duration: TraceMenuAutoHide) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(KeyMenuAutoHide, duration.name)
        .apply()
}

private fun Context.loadProjectionBlankEnabled(): Boolean =
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getBoolean(KeyProjectionBlankEnabled, false)

private fun Context.saveProjectionBlankEnabled(isEnabled: Boolean) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KeyProjectionBlankEnabled, isEnabled)
        .apply()
}

private fun Context.loadProjectionBlankMode(): ProjectionBlankMode {
    val prefs = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    return when (val mode = prefs.getEnum(KeyProjectionBlankMode, ProjectionBlankMode.Toggle)) {
        ProjectionBlankMode.Hold -> ProjectionBlankMode.Toggle
        else -> mode
    }
}

private fun Context.saveProjectionBlankMode(mode: ProjectionBlankMode) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(KeyProjectionBlankMode, mode.name)
        .apply()
}

private fun Context.loadKeepScreenAwake(): Boolean =
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getBoolean(KeyKeepScreenAwake, false)

private fun Context.saveKeepScreenAwake(isEnabled: Boolean) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KeyKeepScreenAwake, isEnabled)
        .apply()
}

private fun Context.loadProjectorOrientation(): ProjectorOrientation {
    val prefs = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    return prefs.getEnum(KeyProjectorOrientation, ProjectorOrientation.Normal)
}

private fun Context.saveProjectorOrientation(orientation: ProjectorOrientation) {
    getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putString(KeyProjectorOrientation, orientation.name)
        .apply()
}

private inline fun <reified T : Enum<T>> android.content.SharedPreferences.getEnum(
    key: String,
    defaultValue: T,
): T {
    val name = getString(key, null) ?: return defaultValue
    return enumValues<T>().firstOrNull { it.name == name } ?: defaultValue
}

private fun android.content.SharedPreferences.getSafeFloat(
    key: String,
    defaultValue: Float,
    minValue: Float,
    maxValue: Float,
): Float {
    val value = getFloat(key, defaultValue)
    return if (value.isNaN() || value.isInfinite()) {
        defaultValue
    } else {
        value.coerceIn(minValue, maxValue)
    }
}

internal suspend fun loadScaledBitmap(
    context: Context,
    uri: Uri,
    targetWidthPx: Int,
    targetHeightPx: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val memory = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val dimensionLimit = if (memory.isLowRamDevice || memory.memoryClass <= 128) 1920 else MaxDecodedImageDimensionPx
        val maxWidth = (targetWidthPx.toLong() * 2).coerceIn(1L, dimensionLimit.toLong()).toInt()
        val maxHeight = (targetHeightPx.toLong() * 2).coerceIn(1L, dimensionLimit.toLong()).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val sampleSize = calculateSampleSize(
                    width = info.size.width,
                    height = info.size.height,
                    targetWidth = maxWidth,
                    targetHeight = maxHeight,
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(sampleSize)
            }
        } else {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@runCatching null
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                    targetWidth = maxWidth,
                    targetHeight = maxHeight,
                )
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }?.orientedByExif(context = context, uri = uri)
        }
    }.getOrNull()
}

internal fun Bitmap.orientedByExif(
    context: Context,
    uri: Uri,
): Bitmap {
    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()

    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postScale(-1f, 1f)
            matrix.postRotate(270f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postScale(-1f, 1f)
            matrix.postRotate(90f)
        }
        else -> return this
    }

    return runCatching {
        Bitmap.createBitmap(this, 0, 0, width, height, matrix, true).also { oriented ->
            if (oriented !== this) {
                recycleIfNeeded()
            }
        }
    }.getOrDefault(this)
}

private fun Bitmap.recycleIfNeeded() {
    if (!isRecycled) {
        recycle()
    }
}

internal suspend fun createLineArtBitmap(bitmap: Bitmap): Bitmap? = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val source = bitmap.scaledForLineArt()
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val gray = IntArray(width * height)
        val output = IntArray(width * height)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (index in pixels.indices) {
            if (index % 4096 == 0) currentCoroutineContext().ensureActive()
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            gray[index] = (red * 299 + green * 587 + blue * 114) / 1000
        }

        for (y in 1 until height - 1) {
            currentCoroutineContext().ensureActive()
            val row = y * width
            val previousRow = row - width
            val nextRow = row + width

            for (x in 1 until width - 1) {
                val topLeft = gray[previousRow + x - 1]
                val top = gray[previousRow + x]
                val topRight = gray[previousRow + x + 1]
                val left = gray[row + x - 1]
                val right = gray[row + x + 1]
                val bottomLeft = gray[nextRow + x - 1]
                val bottom = gray[nextRow + x]
                val bottomRight = gray[nextRow + x + 1]

                val gradientX = -topLeft - 2 * left - bottomLeft + topRight + 2 * right + bottomRight
                val gradientY = -topLeft - 2 * top - topRight + bottomLeft + 2 * bottom + bottomRight
                val edge = abs(gradientX) + abs(gradientY)
                val alpha = ((edge - 90) * 1.7f).roundToInt().coerceIn(0, 255)

                if (alpha > 0) {
                    output[row + x] = (alpha shl 24) or 0x00FFFFFF
                }
            }
        }

        Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888).also {
            if (source !== bitmap) {
                source.recycleIfNeeded()
            }
        }
    }.getOrNull()
}

internal suspend fun createEdgeOutlineBitmap(
    bitmap: Bitmap,
    thickness: Float,
    detail: Float,
    smoothing: Float,
): Bitmap? = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val source = bitmap.scaledForEdgeOutline()
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val gray = IntArray(width * height)
        val output = IntArray(width * height)
        val safeDetail = detail.coerceIn(0f, 1f)
        val safeSmoothing = smoothing.coerceIn(0f, 1f)
        val radius = (thickness.coerceIn(0f, 1f) * 3f).roundToInt().coerceIn(0, 3)
        val threshold = (175f - safeDetail * 135f + safeSmoothing * 35f).roundToInt().coerceIn(24, 210)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (index in pixels.indices) {
            if (index % 4096 == 0) currentCoroutineContext().ensureActive()
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            gray[index] = (red * 299 + green * 587 + blue * 114) / 1000
        }

        if (safeSmoothing > 0.01f) {
            smoothGrayInPlace(gray = gray, width = width, height = height, passes = (safeSmoothing * 3f).roundToInt().coerceIn(1, 3))
        }

        for (y in 1 until height - 1) {
            currentCoroutineContext().ensureActive()
            val row = y * width
            val previousRow = row - width
            val nextRow = row + width

            for (x in 1 until width - 1) {
                val topLeft = gray[previousRow + x - 1]
                val top = gray[previousRow + x]
                val topRight = gray[previousRow + x + 1]
                val left = gray[row + x - 1]
                val right = gray[row + x + 1]
                val bottomLeft = gray[nextRow + x - 1]
                val bottom = gray[nextRow + x]
                val bottomRight = gray[nextRow + x + 1]
                val gradientX = -topLeft - 2 * left - bottomLeft + topRight + 2 * right + bottomRight
                val gradientY = -topLeft - 2 * top - topRight + bottomLeft + 2 * bottom + bottomRight
                val edge = abs(gradientX) + abs(gradientY)
                val alpha = ((edge - threshold) * (1.15f + safeDetail)).roundToInt().coerceIn(0, 255)

                if (alpha > 0) {
                    writeEdgePixel(output = output, width = width, height = height, x = x, y = y, alpha = alpha, radius = radius)
                }
            }
        }

        Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888).also {
            if (source !== bitmap) {
                source.recycleIfNeeded()
            }
        }
    }.getOrNull()
}

internal suspend fun createMagicOutlineBitmap(
    bitmap: Bitmap,
    detail: Float,
    thickness: Float,
): Bitmap? = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val source = bitmap.scaledForMagicOutline()
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val gray = IntArray(width * height)
        val output = IntArray(width * height)
        val safeDetail = detail.coerceIn(0f, 1f)
        val radius = (thickness.coerceIn(0f, 1f) * 4f).roundToInt().coerceIn(0, 4)
        val edgeThreshold = (185f - safeDetail * 130f).roundToInt().coerceIn(38, 190)
        val contourStep = (56f - safeDetail * 30f).roundToInt().coerceIn(20, 60)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (index in pixels.indices) {
            if (index % 4096 == 0) currentCoroutineContext().ensureActive()
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            gray[index] = (red * 299 + green * 587 + blue * 114) / 1000
        }

        smoothGrayInPlace(
            gray = gray,
            width = width,
            height = height,
            passes = (1 + (1f - safeDetail) * 2f).roundToInt().coerceIn(1, 3),
        )

        for (y in 1 until height - 1) {
            currentCoroutineContext().ensureActive()
            val row = y * width
            val previousRow = row - width
            val nextRow = row + width

            for (x in 1 until width - 1) {
                val topLeft = gray[previousRow + x - 1]
                val top = gray[previousRow + x]
                val topRight = gray[previousRow + x + 1]
                val left = gray[row + x - 1]
                val center = gray[row + x]
                val right = gray[row + x + 1]
                val bottomLeft = gray[nextRow + x - 1]
                val bottom = gray[nextRow + x]
                val bottomRight = gray[nextRow + x + 1]
                val gradientX = -topLeft - 2 * left - bottomLeft + topRight + 2 * right + bottomRight
                val gradientY = -topLeft - 2 * top - topRight + bottomLeft + 2 * bottom + bottomRight
                val edge = abs(gradientX) + abs(gradientY)
                var alpha = ((edge - edgeThreshold) * (1.2f + safeDetail * 0.95f)).roundToInt().coerceIn(0, 255)

                val contour = center / contourStep
                val neighborContourChanged =
                    left / contourStep != contour ||
                        right / contourStep != contour ||
                        top / contourStep != contour ||
                        bottom / contourStep != contour
                if (neighborContourChanged && edge > edgeThreshold * 0.35f) {
                    alpha = maxOf(alpha, (72 + safeDetail * 90f).roundToInt().coerceIn(72, 170))
                }

                if (alpha > 0) {
                    writeEdgePixel(output = output, width = width, height = height, x = x, y = y, alpha = alpha, radius = radius)
                }
            }
        }

        Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888).also {
            if (source !== bitmap) {
                source.recycleIfNeeded()
            }
        }
    }.getOrNull()
}

private suspend fun smoothGrayInPlace(
    gray: IntArray,
    width: Int,
    height: Int,
    passes: Int,
) {
    val temp = IntArray(gray.size)

    repeat(passes) {
        System.arraycopy(gray, 0, temp, 0, gray.size)
        for (y in 1 until height - 1) {
            currentCoroutineContext().ensureActive()
            val row = y * width
            val previousRow = row - width
            val nextRow = row + width
            for (x in 1 until width - 1) {
                gray[row + x] = (
                    temp[previousRow + x] +
                        temp[nextRow + x] +
                        temp[row + x - 1] +
                        temp[row + x + 1] +
                        temp[row + x] * 4
                    ) / 8
            }
        }
    }
}

private fun writeEdgePixel(
    output: IntArray,
    width: Int,
    height: Int,
    x: Int,
    y: Int,
    alpha: Int,
    radius: Int,
) {
    val color = (alpha shl 24)
    if (radius <= 0) {
        val index = y * width + x
        if (alpha > (output[index] ushr 24)) {
            output[index] = color
        }
        return
    }

    for (dy in -radius..radius) {
        val targetY = y + dy
        if (targetY !in 0 until height) continue
        for (dx in -radius..radius) {
            val targetX = x + dx
            if (targetX !in 0 until width) continue
            val falloff = 1f - (abs(dx) + abs(dy)) / ((radius + 1f) * 2f)
            val targetAlpha = (alpha * falloff.coerceIn(0.28f, 1f)).roundToInt().coerceIn(0, 255)
            val index = targetY * width + targetX
            val currentAlpha = output[index] ushr 24
            if (targetAlpha > currentAlpha) {
                output[index] = targetAlpha shl 24
            }
        }
    }
}

internal suspend fun createClarityBitmap(bitmap: Bitmap): Bitmap? = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val source = bitmap.scaledForClarity()
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val output = IntArray(width * height)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 1 until height - 1) {
            currentCoroutineContext().ensureActive()
            val row = y * width
            val previousRow = row - width
            val nextRow = row + width

            for (x in 1 until width - 1) {
                val index = row + x
                val center = pixels[index]
                val left = pixels[row + x - 1]
                val right = pixels[row + x + 1]
                val top = pixels[previousRow + x]
                val bottom = pixels[nextRow + x]

                val red = sharpenChannel(
                    center = center shr 16 and 0xFF,
                    left = left shr 16 and 0xFF,
                    right = right shr 16 and 0xFF,
                    top = top shr 16 and 0xFF,
                    bottom = bottom shr 16 and 0xFF,
                )
                val green = sharpenChannel(
                    center = center shr 8 and 0xFF,
                    left = left shr 8 and 0xFF,
                    right = right shr 8 and 0xFF,
                    top = top shr 8 and 0xFF,
                    bottom = bottom shr 8 and 0xFF,
                )
                val blue = sharpenChannel(
                    center = center and 0xFF,
                    left = left and 0xFF,
                    right = right and 0xFF,
                    top = top and 0xFF,
                    bottom = bottom and 0xFF,
                )

                output[index] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }

        copyBitmapEdges(
            source = pixels,
            target = output,
            width = width,
            height = height,
        )

        Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888).also {
            if (source !== bitmap) {
                source.recycleIfNeeded()
            }
        }
    }.getOrNull()
}

internal suspend fun createThresholdBitmap(
    bitmap: Bitmap,
    threshold: Float,
    channelMixer: ChannelMixerMode,
): Bitmap? = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val source = bitmap.scaledForThreshold()
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val output = IntArray(width * height)
        val cutoff = (threshold * 255f).roundToInt().coerceIn(0, 255)
        val (darkColor, lightColor) = channelMixerEndpoints(channelMixer)
        val dark = darkColor.toArgb()
        val light = lightColor.toArgb()

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (index in pixels.indices) {
            if (index % 4096 == 0) currentCoroutineContext().ensureActive()
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            val luminance = (red * 299 + green * 587 + blue * 114) / 1000
            output[index] = if (luminance >= cutoff) light else dark
        }

        Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888).also {
            if (source !== bitmap) {
                source.recycleIfNeeded()
            }
        }
    }.getOrNull()
}

internal suspend fun createPaintBitmap(
    bitmap: Bitmap,
    detail: Float,
): Bitmap? = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val safeDetail = detail.coerceIn(0f, 1f)
        val source = bitmap.scaledForPaint(safeDetail)
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val output = IntArray(width * height)
        val brushSize = (9f - safeDetail * 8f).roundToInt().coerceIn(1, 9)
        val levels = (4f + safeDetail * 18f).roundToInt().coerceIn(4, 22)
        val retainOriginal = (safeDetail * 0.25f).coerceIn(0f, 0.25f)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var y = 0
        while (y < height) {
            currentCoroutineContext().ensureActive()
            var x = 0
            val blockBottom = (y + brushSize).coerceAtMost(height)

            while (x < width) {
                val blockRight = (x + brushSize).coerceAtMost(width)
                var redSum = 0L
                var greenSum = 0L
                var blueSum = 0L
                var count = 0

                for (sampleY in y until blockBottom) {
                    val row = sampleY * width
                    for (sampleX in x until blockRight) {
                        val pixel = pixels[row + sampleX]
                        redSum += pixel shr 16 and 0xFF
                        greenSum += pixel shr 8 and 0xFF
                        blueSum += pixel and 0xFF
                        count += 1
                    }
                }

                val avgRed = (redSum / count).toInt()
                val avgGreen = (greenSum / count).toInt()
                val avgBlue = (blueSum / count).toInt()
                val paintRed = quantizeChannel(avgRed, levels)
                val paintGreen = quantizeChannel(avgGreen, levels)
                val paintBlue = quantizeChannel(avgBlue, levels)

                for (targetY in y until blockBottom) {
                    val row = targetY * width
                    for (targetX in x until blockRight) {
                        val index = row + targetX
                        val original = pixels[index]
                        val red = blendChannel(
                            paintRed,
                            original shr 16 and 0xFF,
                            retainOriginal,
                        )
                        val green = blendChannel(
                            paintGreen,
                            original shr 8 and 0xFF,
                            retainOriginal,
                        )
                        val blue = blendChannel(
                            paintBlue,
                            original and 0xFF,
                            retainOriginal,
                        )
                        output[index] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
                    }
                }

                x += brushSize
            }
            y += brushSize
        }

        Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888).also {
            if (source !== bitmap) {
                source.recycleIfNeeded()
            }
        }
    }.getOrNull()
}

internal suspend fun createPosterizeBitmap(
    bitmap: Bitmap,
    strength: Float,
): Bitmap? = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val safeStrength = strength.coerceIn(0f, 1f)
        val source = bitmap.scaledForPosterize()
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val output = IntArray(width * height)
        val levels = (32f - safeStrength * 28f).roundToInt().coerceIn(4, 32)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (index in pixels.indices) {
            if (index % 4096 == 0) currentCoroutineContext().ensureActive()
            val pixel = pixels[index]
            val red = quantizeChannel(pixel shr 16 and 0xFF, levels)
            val green = quantizeChannel(pixel shr 8 and 0xFF, levels)
            val blue = quantizeChannel(pixel and 0xFF, levels)
            output[index] = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
        }

        Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888).also {
            if (source !== bitmap) {
                source.recycleIfNeeded()
            }
        }
    }.getOrNull()
}

internal suspend fun createVolumeBitmap(
    bitmap: Bitmap,
    strength: Float,
): Bitmap? = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val safeStrength = strength.coerceIn(0f, 1f)
        val source = bitmap.scaledForVolume(safeStrength)
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val radius = (1f + safeStrength * 22f).roundToInt().coerceIn(1, 24)
        val passes = when {
            safeStrength >= 0.72f -> 3
            safeStrength >= 0.36f -> 2
            else -> 1
        }

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        var blurred = pixels
        repeat(passes) {
            blurred = boxBlurVertical(
                input = boxBlurHorizontal(
                    input = blurred,
                    width = width,
                    height = height,
                    radius = radius,
                ),
                width = width,
                height = height,
                radius = radius,
            )
        }

        Bitmap.createBitmap(blurred, width, height, Bitmap.Config.ARGB_8888).also {
            if (source !== bitmap) {
                source.recycleIfNeeded()
            }
        }
    }.getOrNull()
}

private suspend fun boxBlurHorizontal(
    input: IntArray,
    width: Int,
    height: Int,
    radius: Int,
): IntArray {
    val output = IntArray(input.size)
    val window = radius * 2 + 1

    for (y in 0 until height) {
        currentCoroutineContext().ensureActive()
        val row = y * width
        var alphaSum = 0
        var redSum = 0
        var greenSum = 0
        var blueSum = 0

        for (x in -radius..radius) {
            val pixel = input[row + x.coerceIn(0, width - 1)]
            alphaSum += pixel ushr 24 and 0xFF
            redSum += pixel shr 16 and 0xFF
            greenSum += pixel shr 8 and 0xFF
            blueSum += pixel and 0xFF
        }

        for (x in 0 until width) {
            if (x % 64 == 0) currentCoroutineContext().ensureActive()
            output[row + x] = averageArgb(alphaSum, redSum, greenSum, blueSum, window)

            val removePixel = input[row + (x - radius).coerceIn(0, width - 1)]
            val addPixel = input[row + (x + radius + 1).coerceIn(0, width - 1)]
            alphaSum += (addPixel ushr 24 and 0xFF) - (removePixel ushr 24 and 0xFF)
            redSum += (addPixel shr 16 and 0xFF) - (removePixel shr 16 and 0xFF)
            greenSum += (addPixel shr 8 and 0xFF) - (removePixel shr 8 and 0xFF)
            blueSum += (addPixel and 0xFF) - (removePixel and 0xFF)
        }
    }

    return output
}

private suspend fun boxBlurVertical(
    input: IntArray,
    width: Int,
    height: Int,
    radius: Int,
): IntArray {
    val output = IntArray(input.size)
    val window = radius * 2 + 1

    for (x in 0 until width) {
            if (x % 64 == 0) currentCoroutineContext().ensureActive()
        var alphaSum = 0
        var redSum = 0
        var greenSum = 0
        var blueSum = 0

        for (y in -radius..radius) {
            val pixel = input[y.coerceIn(0, height - 1) * width + x]
            alphaSum += pixel ushr 24 and 0xFF
            redSum += pixel shr 16 and 0xFF
            greenSum += pixel shr 8 and 0xFF
            blueSum += pixel and 0xFF
        }

        for (y in 0 until height) {
        currentCoroutineContext().ensureActive()
            val index = y * width + x
            output[index] = averageArgb(alphaSum, redSum, greenSum, blueSum, window)

            val removePixel = input[(y - radius).coerceIn(0, height - 1) * width + x]
            val addPixel = input[(y + radius + 1).coerceIn(0, height - 1) * width + x]
            alphaSum += (addPixel ushr 24 and 0xFF) - (removePixel ushr 24 and 0xFF)
            redSum += (addPixel shr 16 and 0xFF) - (removePixel shr 16 and 0xFF)
            greenSum += (addPixel shr 8 and 0xFF) - (removePixel shr 8 and 0xFF)
            blueSum += (addPixel and 0xFF) - (removePixel and 0xFF)
        }
    }

    return output
}

private fun averageArgb(
    alphaSum: Int,
    redSum: Int,
    greenSum: Int,
    blueSum: Int,
    count: Int,
): Int =
    ((alphaSum / count).coerceIn(0, 255) shl 24) or
        ((redSum / count).coerceIn(0, 255) shl 16) or
        ((greenSum / count).coerceIn(0, 255) shl 8) or
        (blueSum / count).coerceIn(0, 255)

internal suspend fun createNoiseReductionBitmap(
    bitmap: Bitmap,
    strength: Float,
): Bitmap? = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val safeStrength = strength.coerceIn(0f, 1f)
        val scale = (1f - safeStrength * 0.55f).coerceIn(0.35f, 1f)
        val sourceWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val sourceHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val reduced = Bitmap.createScaledBitmap(bitmap, sourceWidth, sourceHeight, true)
        Bitmap.createScaledBitmap(reduced, bitmap.width, bitmap.height, true).also {
            if (reduced !== bitmap) {
                reduced.recycleIfNeeded()
            }
        }
    }.getOrNull()
}

internal suspend fun extractColorPalette(
    bitmap: Bitmap,
    colorCount: Int,
    mode: ColorPaletteMode,
): List<Int> = withContext(ImageFilterDispatcher) {
    cancellableImageResult {
        val targetCount = colorCount.coerceIn(MinPaletteColors, MaxPaletteColors)
        val source = bitmap.scaledForPalette()
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val bucketCount = 32 * 32 * 32
        val counts = IntArray(bucketCount)
        val redSums = LongArray(bucketCount)
        val greenSums = LongArray(bucketCount)
        val blueSums = LongArray(bucketCount)
        var visiblePixelCount = 0

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            val alpha = pixel ushr 24 and 0xFF
            if (alpha < 32) continue

            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            val key = (red shr 3 shl 10) or (green shr 3 shl 5) or (blue shr 3)

            counts[key] += 1
            redSums[key] += red.toLong()
            greenSums[key] += green.toLong()
            blueSums[key] += blue.toLong()
            visiblePixelCount += 1
        }

        val minimumUsefulCount = (visiblePixelCount / (targetCount * 220)).coerceAtLeast(1)
        val allCandidates = counts.indices
            .asSequence()
            .filter { counts[it] > 0 }
            .map { key ->
                val count = counts[key].coerceAtLeast(1)
                PaletteCandidate(
                    red = (redSums[key] / count).toInt().coerceIn(0, 255),
                    green = (greenSums[key] / count).toInt().coerceIn(0, 255),
                    blue = (blueSums[key] / count).toInt().coerceIn(0, 255),
                    count = count,
                )
            }
            .toList()
        val usefulCandidates = allCandidates
            .filter { it.count >= minimumUsefulCount }
            .ifEmpty { allCandidates }
        val colors = selectPaletteColors(
            candidates = usefulCandidates,
            targetCount = targetCount,
            mode = mode,
            totalPixels = visiblePixelCount.coerceAtLeast(1),
        )
            .map { it.toArgb() }

        if (source !== bitmap) {
            source.recycleIfNeeded()
        }

        colors
    }.getOrElse {
        emptyList()
    }
}

private data class PaletteCandidate(
    val red: Int,
    val green: Int,
    val blue: Int,
    val count: Int,
) {
    fun toArgb(): Int = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

    fun luminance(): Float = red * 0.299f + green * 0.587f + blue * 0.114f

    fun saturation(): Float {
        val max = maxOf(red, green, blue).toFloat()
        val min = minOf(red, green, blue).toFloat()
        if (max <= 0f) return 0f
        return (max - min) / max
    }
}

private fun selectPaletteColors(
    candidates: List<PaletteCandidate>,
    targetCount: Int,
    mode: ColorPaletteMode,
    totalPixels: Int,
): List<PaletteCandidate> {
    if (candidates.isEmpty()) return emptyList()

    val sorted = candidates.sortedWith(
        compareByDescending<PaletteCandidate> { it.paletteScore(mode, totalPixels) }
            .thenByDescending { it.count }
    )
    val selected = mutableListOf<PaletteCandidate>()
    val thresholds = when (mode) {
        ColorPaletteMode.Dominant -> listOf(34f, 26f, 18f, 0f)
        ColorPaletteMode.Balanced -> listOf(50f, 38f, 26f, 0f)
        ColorPaletteMode.Paint -> listOf(62f, 48f, 32f, 0f)
    }

    thresholds.forEach { threshold ->
        for (candidate in sorted) {
            if (selected.size >= targetCount) break
            if (selected.any { it == candidate }) continue
            val distance = selected.minOfOrNull { it.rgbDistance(candidate) } ?: Float.MAX_VALUE
            if (distance >= threshold) {
                selected += candidate
            }
        }
        if (selected.size >= targetCount) return selected.sortForDisplay()
    }

    return selected.sortForDisplay()
}

private fun PaletteCandidate.paletteScore(
    mode: ColorPaletteMode,
    totalPixels: Int,
): Double {
    val frequency = count / totalPixels.toDouble()
    val saturation = saturation().toDouble()
    val midTone = (1.0 - kotlin.math.abs(luminance() - 128f) / 128.0).coerceIn(0.0, 1.0)

    return when (mode) {
        ColorPaletteMode.Dominant -> frequency
        ColorPaletteMode.Balanced -> frequency.pow(0.78) * (0.78 + saturation * 0.22)
        ColorPaletteMode.Paint -> frequency.pow(0.68) * (0.48 + saturation * 0.52) * (0.72 + midTone * 0.28)
    }
}

private fun List<PaletteCandidate>.sortForDisplay(): List<PaletteCandidate> =
    sortedWith(
        compareBy<PaletteCandidate> { it.luminance() }
            .thenBy { it.red }
            .thenBy { it.green }
            .thenBy { it.blue }
    )

private fun PaletteCandidate.rgbDistance(other: PaletteCandidate): Float {
    val dr = red - other.red
    val dg = green - other.green
    val db = blue - other.blue
    return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat())
}

private fun Bitmap.scaledForLineArt(): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= MaxLineArtDimensionPx) return this

    val scale = MaxLineArtDimensionPx / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.scaledForEdgeOutline(): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= MaxEdgeOutlineDimensionPx) return this

    val scale = MaxEdgeOutlineDimensionPx / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.scaledForMagicOutline(): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= MaxMagicOutlineDimensionPx) return this

    val scale = MaxMagicOutlineDimensionPx / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.scaledForClarity(): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= MaxClarityDimensionPx) return this

    val scale = MaxClarityDimensionPx / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.scaledForThreshold(): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= MaxThresholdDimensionPx) return this

    val scale = MaxThresholdDimensionPx / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.scaledForPaint(detail: Float): Bitmap {
    val largestSide = maxOf(width, height)
    val targetLargestSide = (360f + detail.coerceIn(0f, 1f) * (MaxPaintDimensionPx - 360f))
        .roundToInt()
        .coerceIn(240, MaxPaintDimensionPx)
    if (largestSide <= targetLargestSide) return this

    val scale = targetLargestSide / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.scaledForPosterize(): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= MaxPosterizeDimensionPx) return this

    val scale = MaxPosterizeDimensionPx / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.scaledForVolume(strength: Float): Bitmap {
    val largestSide = maxOf(width, height)
    val targetLargestSide = (MaxVolumeDimensionPx - strength.coerceIn(0f, 1f) * 360f)
        .roundToInt()
        .coerceIn(840, MaxVolumeDimensionPx)
    if (largestSide <= targetLargestSide) return this

    val scale = targetLargestSide / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.scaledForPalette(): Bitmap {
    val largestSide = maxOf(width, height)
    if (largestSide <= MaxPaletteDimensionPx) return this

    val scale = MaxPaletteDimensionPx / largestSide.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun quantizeChannel(
    value: Int,
    levels: Int,
): Int {
    val safeLevels = levels.coerceAtLeast(2)
    val step = 255f / (safeLevels - 1)
    return (value / step).roundToInt()
        .times(step)
        .roundToInt()
        .coerceIn(0, 255)
}

private fun blendChannel(
    base: Int,
    detail: Int,
    detailAmount: Float,
): Int =
    (base * (1f - detailAmount) + detail * detailAmount)
        .roundToInt()
        .coerceIn(0, 255)

private fun sharpenChannel(
    center: Int,
    left: Int,
    right: Int,
    top: Int,
    bottom: Int,
): Int = (center * 5 - left - right - top - bottom).coerceIn(0, 255)

private fun copyBitmapEdges(
    source: IntArray,
    target: IntArray,
    width: Int,
    height: Int,
) {
    for (x in 0 until width) {
        target[x] = source[x]
        target[(height - 1) * width + x] = source[(height - 1) * width + x]
    }

    for (y in 0 until height) {
        target[y * width] = source[y * width]
        target[y * width + width - 1] = source[y * width + width - 1]
    }
}


@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private val ImageFilterDispatcher = Dispatchers.Default.limitedParallelism(1)

private inline fun <T> cancellableImageResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
} catch (failure: OutOfMemoryError) {
    Result.failure(failure)
}
