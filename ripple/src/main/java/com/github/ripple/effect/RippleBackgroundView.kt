package com.github.ripple.effect

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A lightweight, Kotlin-first ripple background view.
 *
 * The view renders all ripples on its own canvas with a single [ValueAnimator],
 * which is dramatically cheaper than instantiating one child view per ripple.
 * Children added to this view (e.g. an [android.widget.ImageView]) are drawn
 * on top of the ripples, so it can wrap a logo or icon as a backdrop effect.
 *
 * Supported XML attributes (see `res/values/attrs.xml`):
 *  - `app:rb_color`            ripple color
 *  - `app:rb_radius`           base ripple radius
 *  - `app:rb_rippleAmount`     concurrent ripples on screen
 *  - `app:rb_duration`         duration of one ripple in ms
 *  - `app:rb_scale`            final scale factor at the end of a cycle
 *  - `app:rb_type`             `fillRipple` | `strokeRipple`
 *  - `app:rb_strokeWidth`      stroke width when type is `strokeRipple`
 *  - `app:rb_shape`            `circle` | `star` | `arrow` | `diamond` | `moon`
 *  - `app:rb_starPoints`       number of star points (>= 3)
 *  - `app:rb_starInnerRatio`   inner-radius ratio for star (0..1)
 *  - `app:rb_shapeRotation`    rotation applied to each ripple, in degrees
 *  - `app:rb_startAlpha`       alpha at progress = 0 (0..1)
 *  - `app:rb_endAlpha`         alpha at progress = 1 (0..1)
 *  - `app:rb_autoStart`        spawn ripples when attached
 *  - `app:rb_interpolator`     reference to an interpolator resource
 */
class RippleBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** Available paint styles for a ripple. */
    enum class Type { FILL, STROKE }

    /**
     * Available ripple shapes. [MATCH_VIEW] derives the shape from a target
     * view's outline / `GradientDrawable` background, so the ripple matches
     * a wrapped Button, ImageView, MaterialButton, CardView, etc.
     */
    enum class Shape { CIRCLE, STAR, ARROW, DIAMOND, MOON, MATCH_VIEW }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val unitPath = Path()
    private val scratchPath = Path()
    private val scratchMatrix = Matrix()
    private val outerScratch = Path()
    private val innerScratch = Path()
    private val scratchOutline = Outline()

    private var animator: ValueAnimator? = null
    private var masterProgress: Float = -1f

    private val sourceLayoutListener =
        View.OnLayoutChangeListener { _, _, _, _, _, oldL, oldT, oldR, oldB ->
            if (rippleShape != Shape.MATCH_VIEW) return@OnLayoutChangeListener
            val v = sourceView ?: return@OnLayoutChangeListener
            // Only rebuild when bounds actually changed.
            if (v.right - v.left != oldR - oldL || v.bottom - v.top != oldB - oldT) {
                rebuildUnitPath()
                invalidate()
            }
        }

    var rippleColor: Int = DEFAULT_COLOR
        set(value) {
            field = value
            paint.color = value
            invalidate()
        }

    var rippleRadius: Float = dp(DEFAULT_RADIUS_DP)
        set(value) {
            field = value
            invalidate()
        }

    var rippleAmount: Int = DEFAULT_RIPPLE_AMOUNT
        set(value) {
            field = value.coerceAtLeast(1)
            invalidate()
        }

    var rippleDuration: Long = DEFAULT_DURATION_MS
        set(value) {
            field = value.coerceAtLeast(1L)
            // Restart animator so the new duration takes effect immediately.
            if (animator?.isStarted == true) {
                stop()
                start()
            }
        }

    var rippleScale: Float = DEFAULT_SCALE
        set(value) {
            field = value
            invalidate()
        }

    var rippleType: Type = Type.FILL
        set(value) {
            field = value
            paint.style = if (value == Type.STROKE) Paint.Style.STROKE else Paint.Style.FILL
            invalidate()
        }

    var strokeWidth: Float = dp(DEFAULT_STROKE_WIDTH_DP)
        set(value) {
            field = value
            paint.strokeWidth = value
            invalidate()
        }

    var rippleShape: Shape = Shape.CIRCLE
        set(value) {
            field = value
            rebuildUnitPath()
            invalidate()
        }

    var starPoints: Int = DEFAULT_STAR_POINTS
        set(value) {
            field = value.coerceAtLeast(3)
            if (rippleShape == Shape.STAR) rebuildUnitPath()
            invalidate()
        }

    var starInnerRatio: Float = DEFAULT_STAR_INNER_RATIO
        set(value) {
            field = value.coerceIn(0.05f, 0.95f)
            if (rippleShape == Shape.STAR) rebuildUnitPath()
            invalidate()
        }

    var shapeRotation: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var startAlpha: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var endAlpha: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var autoStart: Boolean = false

    var rippleInterpolator: android.view.animation.Interpolator = AccelerateDecelerateInterpolator()
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Override for the ripple's corner radius when [rippleShape] is
     * [Shape.MATCH_VIEW]. Value is in pixels.
     *
     *  - `< 0` (the default) means **Auto** — follow the source view's
     *    natural shape (Material button corners, circular outline, etc.).
     *  - `0` forces a sharp rectangle.
     *  - A positive value replaces the auto-detected radius. If the value
     *    is greater than or equal to half the view's shorter side, the
     *    ripple becomes a perfect circle / oval.
     *
     * Example:
     * ```
     * ripple.matchViewCornerRadius = 24f * resources.displayMetrics.density
     * ```
     *
     * Use [MATCH_VIEW_CORNER_RADIUS_AUTO] to reset back to auto-detection.
     */
    var matchViewCornerRadius: Float = MATCH_VIEW_CORNER_RADIUS_AUTO
        set(value) {
            val normalized = if (value < 0f) MATCH_VIEW_CORNER_RADIUS_AUTO else value
            if (field == normalized) return
            field = normalized
            if (rippleShape == Shape.MATCH_VIEW) {
                rebuildUnitPath()
                invalidate()
            }
        }

    /**
     * When set, ripples automatically take the shape of [value] (round-rect,
     * circle, oval or rect — derived from the view's outline or `GradientDrawable`
     * background) and center on it. Setting this also switches [rippleShape]
     * to [Shape.MATCH_VIEW].
     *
     * Pass `null` to detach.
     */
    var sourceView: View? = null
        set(value) {
            if (field === value) return
            field?.removeOnLayoutChangeListener(sourceLayoutListener)
            field = value
            value?.addOnLayoutChangeListener(sourceLayoutListener)
            if (value != null && rippleShape != Shape.MATCH_VIEW) {
                // Triggers rebuildUnitPath() via the rippleShape setter.
                rippleShape = Shape.MATCH_VIEW
            } else {
                rebuildUnitPath()
                invalidate()
            }
        }

    init {
        setWillNotDraw(false)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.RippleBackgroundView,
                defStyleAttr,
                0
            )
            try {
                rippleColor = a.getColor(R.styleable.RippleBackgroundView_rb_color, DEFAULT_COLOR)
                rippleRadius = a.getDimension(
                    R.styleable.RippleBackgroundView_rb_radius,
                    dp(DEFAULT_RADIUS_DP)
                )
                rippleAmount = a.getInt(
                    R.styleable.RippleBackgroundView_rb_rippleAmount,
                    DEFAULT_RIPPLE_AMOUNT
                )
                rippleDuration = a.getInt(
                    R.styleable.RippleBackgroundView_rb_duration,
                    DEFAULT_DURATION_MS.toInt()
                ).toLong()
                rippleScale = a.getFloat(
                    R.styleable.RippleBackgroundView_rb_scale,
                    DEFAULT_SCALE
                )
                rippleType = Type.entries[
                    a.getInt(R.styleable.RippleBackgroundView_rb_type, Type.FILL.ordinal)
                        .coerceIn(0, Type.entries.size - 1)
                ]
                strokeWidth = a.getDimension(
                    R.styleable.RippleBackgroundView_rb_strokeWidth,
                    dp(DEFAULT_STROKE_WIDTH_DP)
                )
                starPoints = a.getInt(
                    R.styleable.RippleBackgroundView_rb_starPoints,
                    DEFAULT_STAR_POINTS
                )
                starInnerRatio = a.getFloat(
                    R.styleable.RippleBackgroundView_rb_starInnerRatio,
                    DEFAULT_STAR_INNER_RATIO
                )
                shapeRotation = a.getFloat(R.styleable.RippleBackgroundView_rb_shapeRotation, 0f)
                startAlpha = a.getFloat(R.styleable.RippleBackgroundView_rb_startAlpha, 1f)
                endAlpha = a.getFloat(R.styleable.RippleBackgroundView_rb_endAlpha, 0f)
                autoStart = a.getBoolean(R.styleable.RippleBackgroundView_rb_autoStart, false)
                // Corner radius override (px). Negative or absent → auto.
                matchViewCornerRadius = a.getDimension(
                    R.styleable.RippleBackgroundView_rb_matchViewCornerRadius,
                    MATCH_VIEW_CORNER_RADIUS_AUTO
                )
                // Shape last so rebuildUnitPath() sees the final starPoints / starInnerRatio.
                rippleShape = Shape.entries[
                    a.getInt(R.styleable.RippleBackgroundView_rb_shape, Shape.CIRCLE.ordinal)
                        .coerceIn(0, Shape.entries.size - 1)
                ]
                val interpRes = a.getResourceId(
                    R.styleable.RippleBackgroundView_rb_interpolator,
                    0
                )
                if (interpRes != 0) {
                    AnimationUtils.loadInterpolator(context, interpRes)?.let {
                        rippleInterpolator = it
                    }
                }
            } finally {
                a.recycle()
            }
        } else {
            // No attrs: still need to seed the path / paint state.
            paint.color = rippleColor
            paint.strokeWidth = strokeWidth
            rebuildUnitPath()
        }
    }

    // region Public API ---------------------------------------------------------

    /** Starts the ripple animation. Idempotent: calling it twice has no effect. */
    fun start() {
        if (animator?.isStarted == true) return
        val a = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = rippleDuration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LINEAR
            addUpdateListener { anim ->
                masterProgress = anim.animatedValue as Float
                invalidate()
            }
        }
        animator = a
        a.start()
    }

    /** Stops the ripple animation and clears the canvas. */
    fun stop() {
        animator?.cancel()
        animator = null
        masterProgress = -1f
        invalidate()
    }

    /** Whether the animation is currently running. */
    val isRunning: Boolean
        get() = animator?.isStarted == true

    // endregion ------------------------------------------------------------------

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autoStart) start()
    }

    override fun onDetachedFromWindow() {
        // Always cancel to avoid leaks; respect autoStart on re-attach.
        animator?.cancel()
        animator = null
        masterProgress = -1f
        super.onDetachedFromWindow()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        // Allow XML users to drop the target view inside the wrapper and just
        // declare `app:rb_shape="matchView"` — auto-pick the first child.
        if (rippleShape == Shape.MATCH_VIEW && sourceView == null && childCount > 0) {
            sourceView = getChildAt(0)
        } else if (rippleShape == Shape.MATCH_VIEW && sourceView != null) {
            // Wrapper size may have changed, child bounds may have moved.
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (masterProgress < 0f) return

        // Center ripples on the source view if it's a child of this wrapper;
        // otherwise fall back to the wrapper's own center.
        val cx: Float
        val cy: Float
        val src = sourceView
        if (src != null && src.parent === this && src.width > 0 && src.height > 0) {
            cx = src.left + src.width * 0.5f
            cy = src.top + src.height * 0.5f
        } else {
            cx = width * 0.5f
            cy = height * 0.5f
        }
        val baseR = effectiveBaseRadius()
        val finalScale = rippleScale
        val amount = rippleAmount
        val invAmount = 1f / amount

        var i = 0
        while (i < amount) {
            // Phase-offset each ripple so they appear at evenly-spaced points
            // in the cycle without needing separate animators.
            var phase = masterProgress + i * invAmount
            if (phase >= 1f) phase -= 1f
            val t = rippleInterpolator.getInterpolation(phase)

            val scale = 1f + (finalScale - 1f) * t
            val r = baseR * scale
            val alphaF = startAlpha + (endAlpha - startAlpha) * t
            val alpha = (alphaF * 255f).toInt().coerceIn(0, 255)
            if (alpha != 0 && r > 0f) {
                paint.alpha = alpha
                drawShape(canvas, cx, cy, r)
            }
            i++
        }
    }

    private fun drawShape(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        if (rippleShape == Shape.CIRCLE) {
            // Fast path: native draw call, no path allocation per frame.
            canvas.drawCircle(cx, cy, r, paint)
            return
        }
        scratchMatrix.setScale(r, r)
        if (shapeRotation != 0f) scratchMatrix.postRotate(shapeRotation)
        scratchMatrix.postTranslate(cx, cy)
        unitPath.transform(scratchMatrix, scratchPath)
        canvas.drawPath(scratchPath, paint)
    }

    /**
     * Effective base radius. For [Shape.MATCH_VIEW] we ignore the user-supplied
     * `rippleRadius` and use half the source view's longest dimension so the
     * initial ripple matches the view exactly.
     */
    private fun effectiveBaseRadius(): Float {
        val src = sourceView
        if (rippleShape == Shape.MATCH_VIEW && src != null && src.width > 0 && src.height > 0) {
            return max(src.width, src.height) * 0.5f
        }
        return rippleRadius
    }

    private fun rebuildUnitPath() {
        unitPath.reset()
        when (rippleShape) {
            Shape.CIRCLE -> Unit // drawn directly via canvas.drawCircle
            Shape.STAR -> buildStar(unitPath, starPoints, starInnerRatio)
            Shape.ARROW -> buildArrow(unitPath)
            Shape.DIAMOND -> buildDiamond(unitPath)
            Shape.MOON -> buildMoon(unitPath)
            Shape.MATCH_VIEW -> sourceView?.let { buildViewOutlinePath(unitPath, it) }
        }
    }

    /**
     * Build a unit-space path matching the outline / drawable shape of [view].
     * The result fits within [-1, 1] in the longer axis (aspect ratio preserved).
     *
     * If [matchViewCornerRadius] is `>= 0` it overrides the auto-detected
     * corner radius (the source view's natural shape is ignored).
     */
    private fun buildViewOutlinePath(path: Path, view: View) {
        val w = view.width.toFloat()
        val h = view.height.toFloat()
        if (w <= 0f || h <= 0f) return

        val halfMaxRaw = max(w, h) * 0.5f
        val halfMinRaw = min(w, h) * 0.5f
        val halfW = (w * 0.5f) / halfMaxRaw
        val halfH = (h * 0.5f) / halfMaxRaw

        // User-supplied corner radius wins over anything we'd auto-detect.
        if (matchViewCornerRadius >= 0f) {
            applyRoundRectShape(
                path,
                halfW, halfH,
                halfMinRaw, halfMaxRaw,
                matchViewCornerRadius
            )
            return
        }

        // 1) GradientDrawable background — most reliable shape source.
        val bg = view.background
        if (bg is GradientDrawable) {
            when (bg.shape) {
                GradientDrawable.OVAL -> {
                    path.addOval(-halfW, -halfH, halfW, halfH, Path.Direction.CW)
                    return
                }
                GradientDrawable.RECTANGLE -> {
                    val cr = bg.cornerRadius
                    if (cr > 0f) {
                        applyRoundRectShape(path, halfW, halfH, halfMinRaw, halfMaxRaw, cr)
                        return
                    }
                    // Fall through to rect.
                }
            }
        }

        // 2) View outline (Material buttons, CardView, custom outline providers).
        var cornerR = 0f
        try {
            val provider = view.outlineProvider
            if (provider != null) {
                provider.getOutline(view, scratchOutline)
                if (!scratchOutline.isEmpty) {
                    cornerR = scratchOutline.radius
                    if (cornerR < 0f) cornerR = 0f
                }
            }
        } catch (_: Throwable) {
            // Some custom outline providers throw before first measure — ignore.
        }

        applyRoundRectShape(path, halfW, halfH, halfMinRaw, halfMaxRaw, cornerR)
    }

    /**
     * Apply a rectangle / round-rect / oval to [path] in unit space depending on
     * the raw-pixel [cornerR]. Promotes to a perfect oval once the radius is at
     * least half the view's shorter side.
     */
    private fun applyRoundRectShape(
        path: Path,
        halfW: Float,
        halfH: Float,
        halfMinRaw: Float,
        halfMaxRaw: Float,
        cornerR: Float
    ) {
        when {
            cornerR > 0f && cornerR >= halfMinRaw * 0.99f -> {
                path.addOval(-halfW, -halfH, halfW, halfH, Path.Direction.CW)
            }
            cornerR > 0f -> {
                val n = (cornerR / halfMaxRaw).coerceAtMost(1f)
                path.addRoundRect(-halfW, -halfH, halfW, halfH, n, n, Path.Direction.CW)
            }
            else -> {
                path.addRect(-halfW, -halfH, halfW, halfH, Path.Direction.CW)
            }
        }
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        resources.displayMetrics
    )

    // region Shape builders (unit space: inscribed in a circle of radius 1) ----

    private fun buildStar(path: Path, points: Int, innerRatio: Float) {
        val step = (PI / points).toFloat()
        var angle = -PI.toFloat() * 0.5f
        val total = points * 2
        var i = 0
        while (i < total) {
            val r = if (i and 1 == 0) 1f else innerRatio
            val x = cos(angle) * r
            val y = sin(angle) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            angle += step
            i++
        }
        path.close()
    }

    private fun buildArrow(path: Path) {
        // Up-pointing arrow contained within the unit circle.
        path.moveTo(0f, -1f)
        path.lineTo(0.7f, -0.2f)
        path.lineTo(0.28f, -0.2f)
        path.lineTo(0.28f, 1f)
        path.lineTo(-0.28f, 1f)
        path.lineTo(-0.28f, -0.2f)
        path.lineTo(-0.7f, -0.2f)
        path.close()
    }

    private fun buildDiamond(path: Path) {
        path.moveTo(0f, -1f)
        path.lineTo(1f, 0f)
        path.lineTo(0f, 1f)
        path.lineTo(-1f, 0f)
        path.close()
    }

    private fun buildMoon(path: Path) {
        // Crescent: outer circle minus a circle offset to the right.
        outerScratch.reset()
        outerScratch.addCircle(0f, 0f, 1f, Path.Direction.CW)
        innerScratch.reset()
        innerScratch.addCircle(0.42f, 0f, 0.95f, Path.Direction.CW)
        path.op(outerScratch, innerScratch, Path.Op.DIFFERENCE)
    }

    // endregion -----------------------------------------------------------------

    companion object {
        /**
         * Sentinel value for [matchViewCornerRadius] meaning "use the source
         * view's natural shape" (auto-detect).
         */
        const val MATCH_VIEW_CORNER_RADIUS_AUTO: Float = -1f

        private const val DEFAULT_RADIUS_DP = 64f
        private const val DEFAULT_STROKE_WIDTH_DP = 2f
        private const val DEFAULT_RIPPLE_AMOUNT = 4
        private const val DEFAULT_DURATION_MS = 3000L
        private const val DEFAULT_SCALE = 6f
        private const val DEFAULT_STAR_POINTS = 5
        private const val DEFAULT_STAR_INNER_RATIO = 0.45f
        private val DEFAULT_COLOR: Int = Color.parseColor("#FF0099CC")
        private val LINEAR = LinearInterpolator()

        /**
         * Wraps [target] in a new [RippleBackgroundView] inside [target]'s
         * existing parent so that ripples automatically match its shape
         * (round-rect for Material buttons, circle for circular outlines,
         * oval for ovals, rectangle otherwise).
         *
         * The wrapper takes over [target]'s original `LayoutParams`, and
         * [target] is centered inside it. `clipChildren` / `clipToPadding`
         * are disabled on the parent so ripples can grow past the view's
         * bounds.
         *
         * The animation is started automatically; call [stop] on the returned
         * view to pause it. Returns the wrapper for further configuration.
         */
        @JvmStatic
        fun attach(target: View): RippleBackgroundView {
            // Idempotent: if `target` is already wrapped, just return the wrapper.
            val existing = target.parent
            if (existing is RippleBackgroundView && existing.sourceView === target) {
                if (!existing.isRunning) existing.start()
                return existing
            }
            val parent = existing as? ViewGroup
                ?: error("RippleBackgroundView.attach: target must be in a ViewGroup")
            val index = parent.indexOfChild(target)
            val originalLp = target.layoutParams
            parent.removeView(target)

            val wrapper = RippleBackgroundView(target.context).apply {
                clipChildren = false
                clipToPadding = false
            }
            wrapper.addView(
                target,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            wrapper.sourceView = target

            if (originalLp != null) {
                parent.addView(wrapper, index, originalLp)
            } else {
                parent.addView(wrapper, index)
            }
            // Let ripples overflow into the parent.
            parent.clipChildren = false
            parent.clipToPadding = false

            wrapper.start()
            return wrapper
        }
    }
}

/**
 * Kotlin-idiomatic one-liner that wraps this view in a [RippleBackgroundView]
 * matching its shape, applies [configure], and starts the animation.
 *
 * Example:
 * ```
 * findViewById<MaterialButton>(R.id.cta).addRippleEffect {
 *     rippleColor = Color.MAGENTA
 *     rippleAmount = 5
 * }
 * ```
 */
fun View.addRippleEffect(configure: RippleBackgroundView.() -> Unit = {}): RippleBackgroundView =
    RippleBackgroundView.attach(this).apply(configure)
