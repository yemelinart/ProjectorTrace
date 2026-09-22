package com.projectortrace.model

data class TransformState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f,
    val isFlippedHorizontal: Boolean = false,
    val isFlippedVertical: Boolean = false,
    val opacity: Float = 1f,
    val imageBlendMode: ImageBlendMode = ImageBlendMode.Normal,
    val isBrightnessEnabled: Boolean = false,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val clarity: Float = 0f,
    val noiseReduction: Float = 0f,
    val lineArt: Float = 0f,
    val isThresholdEnabled: Boolean = false,
    val threshold: Float = 0.5f,
    val channelMixer: ChannelMixerMode = ChannelMixerMode.Normal,
    val paintDetail: Float = 0f,
    val posterize: Float = 0f,
    val volume: Float = 0f,
    val magicOutlineStrength: Float = 0f,
    val magicOutlineDetail: Float = 0.55f,
    val magicOutlineThickness: Float = 0.35f,
    val edgeOutlineStrength: Float = 0f,
    val edgeOutlineThickness: Float = 0.35f,
    val edgeOutlineDetail: Float = 0.55f,
    val edgeOutlineSmoothing: Float = 0.25f,
    val isInverted: Boolean = false,
    val tracingPreset: TracingPreset = TracingPreset.Custom,
    val isLocked: Boolean = false,
    val currentMode: TraceMode = TraceMode.Move,
    val controlStep: ControlStep = ControlStep.Normal,
    val rotationStepDegrees: Float = 1f,
    val selectedCorner: DistortCorner = DistortCorner.TopLeft,
    val topLeft: CornerOffset = CornerOffset(),
    val topRight: CornerOffset = CornerOffset(),
    val bottomRight: CornerOffset = CornerOffset(),
    val bottomLeft: CornerOffset = CornerOffset(),
    val guideInsetX: Float = 80f,
    val guideInsetY: Float = 80f,
    val guideSelectedCorner: DistortCorner = DistortCorner.TopLeft,
    val guideCornerColor: GuideCornerColor = GuideCornerColor.White,
    val guideTopLeft: CornerOffset = CornerOffset(),
    val guideTopRight: CornerOffset = CornerOffset(),
    val guideBottomRight: CornerOffset = CornerOffset(),
    val guideBottomLeft: CornerOffset = CornerOffset(),
    val isGridVisible: Boolean = false,
    val gridSpacingPx: Float = 120f,
    val gridColorMode: GridColorMode = GridColorMode.White,
    val isRulersVisible: Boolean = false,
    val isCenterCrossVisible: Boolean = true,
    val isCanvasFrameVisible: Boolean = false,
    val canvasFramePreset: CanvasFramePreset = CanvasFramePreset.Ratio16x20,
    val customCanvasFrameRatio: Float = 0.8f,
    val canvasFrameColor: GuideCornerColor = GuideCornerColor.White,
    val isCropEnabled: Boolean = false,
    val selectedCropEdge: CropEdge = CropEdge.Left,
    val selectedCropCorner: CropCorner = CropCorner.TopLeft,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 0f,
    val cropBottom: Float = 0f,
    val isGuideFrameVisible: Boolean = true,
    val guideShape: GuideShape = GuideShape.Off,
    val guideShapeColor: GuideShapeColor = GuideShapeColor.White,
    val guideShapeRotationDegrees: Float = 0f,
    val colorPaletteCount: Int = 0,
    val colorPaletteMode: ColorPaletteMode = ColorPaletteMode.Balanced,
    val colorPalettePosition: PalettePosition = PalettePosition.BottomEnd,
    val colorPaletteOffsetX: Float = 0f,
    val colorPaletteOffsetY: Float = 0f,
    val colorPaletteScale: Float = 1f,
    val isOverlayVisible: Boolean = false,
    val overlayOpacity: Float = 0.5f,
    val overlayBlendMode: OverlayBlendMode = OverlayBlendMode.Normal,
    val canvasOrientation: CanvasOrientation = CanvasOrientation.Auto,
    val imageFit: ImageFit = ImageFit.Fit,
    val menuOffsetX: Float = 0f,
    val menuOffsetY: Float = 0f,
)

data class CornerOffset(
    val x: Float = 0f,
    val y: Float = 0f,
)

enum class TraceMode {
    Move,
    Zoom,
    Rotate,
    Corner,
    Distort,
    Frame,
    View,
    Fit,
    MenuMove,
    Guide,
    Rulers,
    CenterCross,
    CanvasFrame,
    CanvasFrameRatio,
    CanvasFrameColor,
    CropEdge,
    CropAdjust,
    Shape,
    ColorPalette,
    PaletteMode,
    PaletteMove,
    PaletteScale,
    PalettePosition,
    Step,
    Preset,
    Opacity,
    BlendMode,
    Brightness,
    Threshold,
    ChannelMixer,
    Paint,
    Posterize,
    Volume,
    MagicOutlineDetail,
    MagicOutlineThickness,
    EdgeStrength,
    EdgeThickness,
    EdgeDetail,
    EdgeSmoothing,
    Contrast,
    Clarity,
    NoiseReduction,
    LineArt,
    GuideCorner,
    GuideCornerColor,
    GuideCornerMove,
    ShapeColor,
    ShapeRotation,
    OverlayOpacity,
    OverlayBlend,
    Invert,
    Lock,
    Setup,
    Open;

    fun next(): TraceMode = when (this) {
        Move -> Zoom
        Zoom -> Rotate
        Rotate -> Corner
        Corner -> Distort
        Distort -> Frame
        Frame -> View
        View -> Fit
        Fit -> MenuMove
        MenuMove -> Guide
        Guide -> Rulers
        Rulers -> CenterCross
        CenterCross -> CanvasFrame
        CanvasFrame -> CanvasFrameRatio
        CanvasFrameRatio -> CanvasFrameColor
        CanvasFrameColor -> CropEdge
        CropEdge -> CropAdjust
        CropAdjust -> Shape
        Shape -> ColorPalette
        ColorPalette -> PaletteMode
        PaletteMode -> PaletteMove
        PaletteMove -> PaletteScale
        PaletteScale -> Step
        PalettePosition -> Step
        Step -> Preset
        Preset -> Opacity
        Opacity -> BlendMode
        BlendMode -> Brightness
        Brightness -> Threshold
        Threshold -> ChannelMixer
        ChannelMixer -> Paint
        Paint -> Posterize
        Posterize -> Volume
        Volume -> MagicOutlineDetail
        MagicOutlineDetail -> MagicOutlineThickness
        MagicOutlineThickness -> EdgeStrength
        EdgeStrength -> EdgeThickness
        EdgeThickness -> EdgeDetail
        EdgeDetail -> EdgeSmoothing
        EdgeSmoothing -> Contrast
        Contrast -> Clarity
        Clarity -> NoiseReduction
        NoiseReduction -> LineArt
        LineArt -> GuideCorner
        GuideCorner -> GuideCornerColor
        GuideCornerColor -> GuideCornerMove
        GuideCornerMove -> ShapeColor
        ShapeColor -> ShapeRotation
        ShapeRotation -> OverlayOpacity
        OverlayOpacity -> OverlayBlend
        OverlayBlend -> Invert
        Invert -> Lock
        Lock -> Setup
        Setup -> Open
        Open -> Move
    }

    fun previous(): TraceMode = when (this) {
        Move -> Open
        Zoom -> Move
        Rotate -> Zoom
        Corner -> Rotate
        Distort -> Corner
        Frame -> Distort
        View -> Frame
        Fit -> View
        MenuMove -> Fit
        Guide -> MenuMove
        Shape -> CropAdjust
        CropAdjust -> CropEdge
        CropEdge -> CanvasFrameColor
        CanvasFrameRatio -> CanvasFrame
        CanvasFrameColor -> CanvasFrameRatio
        CanvasFrame -> CenterCross
        CenterCross -> Rulers
        Rulers -> Guide
        ColorPalette -> Shape
        PaletteMode -> ColorPalette
        PaletteMove -> PaletteMode
        PaletteScale -> PaletteMove
        PalettePosition -> PaletteMode
        Step -> PaletteScale
        Preset -> Step
        Opacity -> Preset
        BlendMode -> Opacity
        Brightness -> BlendMode
        Threshold -> Brightness
        ChannelMixer -> Threshold
        Paint -> ChannelMixer
        Posterize -> Paint
        Volume -> Posterize
        MagicOutlineDetail -> Volume
        MagicOutlineThickness -> MagicOutlineDetail
        Contrast -> EdgeSmoothing
        EdgeSmoothing -> EdgeDetail
        EdgeDetail -> EdgeThickness
        EdgeThickness -> EdgeStrength
        EdgeStrength -> Volume
        Clarity -> Contrast
        NoiseReduction -> Clarity
        LineArt -> NoiseReduction
        GuideCorner -> LineArt
        GuideCornerMove -> GuideCornerColor
        GuideCornerColor -> GuideCorner
        ShapeRotation -> ShapeColor
        ShapeColor -> GuideCornerMove
        OverlayOpacity -> ShapeRotation
        OverlayBlend -> OverlayOpacity
        Invert -> OverlayBlend
        Lock -> Invert
        Setup -> Lock
        Open -> Setup
    }

    fun nextGroup(): TraceMode = when (this) {
        Move,
        Zoom,
        Rotate -> Corner
        Corner,
        Distort,
        Frame,
        View,
        Fit,
        MenuMove,
        Guide,
        Rulers,
        CenterCross,
        CanvasFrame,
        CanvasFrameRatio,
        CanvasFrameColor,
        CropEdge,
        CropAdjust,
        Shape,
        ColorPalette,
        PaletteMode,
        PaletteMove,
        PaletteScale,
        PalettePosition,
        Step -> Preset
        Preset,
        Opacity,
        BlendMode,
        Brightness,
        Threshold,
        ChannelMixer,
        Paint,
        Posterize,
        Volume,
        MagicOutlineDetail,
        MagicOutlineThickness,
        EdgeStrength,
        EdgeThickness,
        EdgeDetail,
        EdgeSmoothing,
        Contrast,
        Clarity,
        NoiseReduction,
        LineArt,
        GuideCorner,
        GuideCornerColor,
        GuideCornerMove,
        ShapeColor,
        ShapeRotation,
        OverlayOpacity,
        OverlayBlend,
        Invert -> Lock
        Lock,
        Setup,
        Open -> Move
    }

    fun previousGroup(): TraceMode = when (this) {
        Move,
        Zoom,
        Rotate -> Lock
        Corner,
        Distort,
        Frame,
        View,
        Fit,
        MenuMove,
        Guide,
        Rulers,
        CenterCross,
        CanvasFrame,
        CanvasFrameRatio,
        CanvasFrameColor,
        CropEdge,
        CropAdjust,
        Shape,
        ColorPalette,
        PaletteMode,
        PaletteMove,
        PaletteScale,
        PalettePosition,
        Step -> Move
        Preset,
        Opacity,
        BlendMode,
        Brightness,
        Threshold,
        ChannelMixer,
        Paint,
        Posterize,
        Volume,
        MagicOutlineDetail,
        MagicOutlineThickness,
        EdgeStrength,
        EdgeThickness,
        EdgeDetail,
        EdgeSmoothing,
        Contrast,
        Clarity,
        NoiseReduction,
        LineArt,
        GuideCorner,
        GuideCornerColor,
        GuideCornerMove,
        ShapeColor,
        ShapeRotation,
        OverlayOpacity,
        OverlayBlend,
        Invert -> Corner
        Lock,
        Setup,
        Open -> Preset
    }
}

enum class ControlStep {
    Fine,
    Normal,
    Coarse;

    fun next(): ControlStep = when (this) {
        Fine -> Normal
        Normal -> Coarse
        Coarse -> Fine
    }

    fun previous(): ControlStep = when (this) {
        Fine -> Coarse
        Normal -> Fine
        Coarse -> Normal
    }
}

enum class CanvasOrientation {
    Auto,
    Landscape,
    Portrait;

    fun next(): CanvasOrientation = when (this) {
        Auto -> Landscape
        Landscape -> Portrait
        Portrait -> Auto
    }

    fun previous(): CanvasOrientation = when (this) {
        Auto -> Portrait
        Landscape -> Auto
        Portrait -> Landscape
    }
}

enum class ImageFit {
    Fit,
    Fill,
    Stretch;

    fun next(): ImageFit = when (this) {
        Fit -> Fill
        Fill -> Stretch
        Stretch -> Fit
    }

    fun previous(): ImageFit = when (this) {
        Fit -> Stretch
        Fill -> Fit
        Stretch -> Fill
    }
}

enum class GuideShape {
    Off,
    Thirds,
    ThirdsDense,
    GoldenRatio,
    GoldenSpiral,
    Diagonal,
    DynamicSymmetry,
    Triangle,
    CenterCross;

    fun next(): GuideShape = when (this) {
        Off -> Thirds
        Thirds -> ThirdsDense
        ThirdsDense -> GoldenRatio
        GoldenRatio -> GoldenSpiral
        GoldenSpiral -> Diagonal
        Diagonal -> DynamicSymmetry
        DynamicSymmetry -> Triangle
        Triangle -> CenterCross
        CenterCross -> Off
    }

    fun previous(): GuideShape = when (this) {
        Off -> CenterCross
        Thirds -> Off
        ThirdsDense -> Thirds
        GoldenRatio -> ThirdsDense
        GoldenSpiral -> GoldenRatio
        Diagonal -> GoldenSpiral
        DynamicSymmetry -> Diagonal
        Triangle -> DynamicSymmetry
        CenterCross -> Triangle
    }
}

enum class GuideShapeColor {
    Black,
    White,
    Magenta,
    Yellow,
    Green;

    fun next(): GuideShapeColor = when (this) {
        Black -> White
        White -> Magenta
        Magenta -> Yellow
        Yellow -> Green
        Green -> Black
    }

    fun previous(): GuideShapeColor = when (this) {
        Black -> Green
        White -> Black
        Magenta -> White
        Yellow -> Magenta
        Green -> Yellow
    }
}

enum class GuideCornerColor {
    White,
    Black,
    Magenta,
    Red,
    Yellow,
    Green,
    Cyan;

    fun next(): GuideCornerColor = when (this) {
        White -> Black
        Black -> Magenta
        Magenta -> Red
        Red -> Yellow
        Yellow -> Green
        Green -> Cyan
        Cyan -> White
    }

    fun previous(): GuideCornerColor = when (this) {
        White -> Cyan
        Black -> White
        Magenta -> Black
        Red -> Magenta
        Yellow -> Red
        Green -> Yellow
        Cyan -> Green
    }
}

enum class ColorPaletteMode {
    Balanced,
    Dominant,
    Paint;

    fun next(): ColorPaletteMode = when (this) {
        Balanced -> Dominant
        Dominant -> Paint
        Paint -> Balanced
    }

    fun previous(): ColorPaletteMode = when (this) {
        Balanced -> Paint
        Dominant -> Balanced
        Paint -> Dominant
    }
}

enum class PalettePosition {
    TopStart,
    TopCenter,
    TopEnd,
    CenterEnd,
    BottomEnd,
    BottomCenter,
    BottomStart,
    CenterStart;

    fun next(): PalettePosition = when (this) {
        TopStart -> TopCenter
        TopCenter -> TopEnd
        TopEnd -> CenterEnd
        CenterEnd -> BottomEnd
        BottomEnd -> BottomCenter
        BottomCenter -> BottomStart
        BottomStart -> CenterStart
        CenterStart -> TopStart
    }

    fun previous(): PalettePosition = when (this) {
        TopStart -> CenterStart
        TopCenter -> TopStart
        TopEnd -> TopCenter
        CenterEnd -> TopEnd
        BottomEnd -> CenterEnd
        BottomCenter -> BottomEnd
        BottomStart -> BottomCenter
        CenterStart -> BottomStart
    }
}

enum class ImageBlendMode {
    Normal,
    Color,
    Overlay;

    fun next(): ImageBlendMode = when (this) {
        Normal -> Color
        Color -> Overlay
        Overlay -> Normal
    }

    fun previous(): ImageBlendMode = when (this) {
        Normal -> Overlay
        Color -> Normal
        Overlay -> Color
    }
}

enum class OverlayBlendMode {
    Normal,
    Multiply,
    Screen,
    Overlay;

    fun next(): OverlayBlendMode = when (this) {
        Normal -> Multiply
        Multiply -> Screen
        Screen -> Overlay
        Overlay -> Normal
    }

    fun previous(): OverlayBlendMode = when (this) {
        Normal -> Overlay
        Multiply -> Normal
        Screen -> Multiply
        Overlay -> Screen
    }
}

enum class GridColorMode {
    White,
    Black;

    fun next(): GridColorMode = when (this) {
        White -> Black
        Black -> White
    }
}

enum class CanvasFramePreset {
    Ratio1x1,
    Ratio2x3,
    Ratio3x2,
    Ratio4x5,
    Ratio5x4,
    Ratio9x16,
    Ratio16x9,
    Ratio16x20,
    Ratio18x24,
    Ratio24x36,
    Custom;

    fun next(): CanvasFramePreset = when (this) {
        Ratio1x1 -> Ratio2x3
        Ratio2x3 -> Ratio3x2
        Ratio3x2 -> Ratio4x5
        Ratio4x5 -> Ratio5x4
        Ratio5x4 -> Ratio9x16
        Ratio9x16 -> Ratio16x9
        Ratio16x9 -> Ratio16x20
        Ratio16x20 -> Ratio18x24
        Ratio18x24 -> Ratio24x36
        Ratio24x36 -> Custom
        Custom -> Ratio1x1
    }

    fun previous(): CanvasFramePreset = when (this) {
        Ratio1x1 -> Custom
        Ratio2x3 -> Ratio1x1
        Ratio3x2 -> Ratio2x3
        Ratio4x5 -> Ratio3x2
        Ratio5x4 -> Ratio4x5
        Ratio9x16 -> Ratio5x4
        Ratio16x9 -> Ratio9x16
        Ratio16x20 -> Ratio16x9
        Ratio18x24 -> Ratio16x20
        Ratio24x36 -> Ratio18x24
        Custom -> Ratio24x36
    }
}

enum class CropCorner {
    TopLeft,
    TopRight,
    BottomRight,
    BottomLeft;

    fun next(): CropCorner = when (this) {
        TopLeft -> TopRight
        TopRight -> BottomRight
        BottomRight -> BottomLeft
        BottomLeft -> TopLeft
    }

    fun previous(): CropCorner = when (this) {
        TopLeft -> BottomLeft
        TopRight -> TopLeft
        BottomRight -> TopRight
        BottomLeft -> BottomRight
    }
}

enum class CropEdge {
    Left,
    Top,
    Right,
    Bottom;

    fun next(): CropEdge = when (this) {
        Left -> Top
        Top -> Right
        Right -> Bottom
        Bottom -> Left
    }

    fun previous(): CropEdge = when (this) {
        Left -> Bottom
        Top -> Left
        Right -> Top
        Bottom -> Right
    }
}

enum class ProjectorOrientation {
    Normal,
    UpsideDown,
    RotateLeft,
    RotateRight;

    fun next(): ProjectorOrientation = when (this) {
        Normal -> UpsideDown
        UpsideDown -> RotateLeft
        RotateLeft -> RotateRight
        RotateRight -> Normal
    }
}

enum class ProjectionBlankMode {
    Hold,
    Toggle,
    White,
    Compare;

    fun next(): ProjectionBlankMode = when (this) {
        Hold -> Toggle
        Toggle -> White
        White -> Compare
        Compare -> Toggle
    }
}

enum class ChannelMixerMode {
    Normal,
    BlackWhite,
    RedGreen,
    YellowBlue,
    MagentaCyan;

    fun next(): ChannelMixerMode = when (this) {
        Normal -> BlackWhite
        BlackWhite -> RedGreen
        RedGreen -> YellowBlue
        YellowBlue -> MagentaCyan
        MagentaCyan -> Normal
    }

    fun previous(): ChannelMixerMode = when (this) {
        Normal -> MagentaCyan
        BlackWhite -> Normal
        RedGreen -> BlackWhite
        YellowBlue -> RedGreen
        MagentaCyan -> YellowBlue
    }
}

enum class TracingPreset {
    Custom,
    Photo,
    Sketch,
    Lines,
    InvertedLines;

    fun next(): TracingPreset = when (this) {
        Custom -> Photo
        Photo -> Sketch
        Sketch -> Lines
        Lines -> InvertedLines
        InvertedLines -> Custom
    }

    fun previous(): TracingPreset = when (this) {
        Custom -> InvertedLines
        Photo -> Custom
        Sketch -> Photo
        Lines -> Sketch
        InvertedLines -> Lines
    }
}

enum class DistortCorner {
    TopLeft,
    TopRight,
    BottomRight,
    BottomLeft;

    fun next(): DistortCorner = when (this) {
        TopLeft -> TopRight
        TopRight -> BottomRight
        BottomRight -> BottomLeft
        BottomLeft -> TopLeft
    }

    fun previous(): DistortCorner = when (this) {
        TopLeft -> BottomLeft
        TopRight -> TopLeft
        BottomRight -> TopRight
        BottomLeft -> BottomRight
    }
}
