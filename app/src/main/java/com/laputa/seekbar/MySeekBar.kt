package com.laputa.seekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.lang.IllegalStateException
import kotlin.math.abs
import kotlin.math.roundToLong

class MySeekBar(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    /**
     * 进度
     */
    var progress: Float = 0f
        set(value) {
            field = when {
                value > max -> {
                    max
                }
                value < 0 -> {
                    0f
                }
                else -> {
                    calibration(value)
                }
            }
            invalidate()
            onChanged()
        }
    var max: Float = DEFAULT_MAX.toFloat()
        set(value) {
            if (max <= progress) {
                throw IllegalStateException("max必须比progress大")
            }
            field = value
            //invalidate()
        }

    /**
     * 精度
     */
    var accuracy: Float = 1f
    var onProgressChanged: OnProgressChangedListener? = null
    var onProgressSelectedListener: OnProgressSelectedListener? = null

    // 每一个进度的宽度
    private var perProgressWidth: Float

    // 每一个空隙的宽度
    private var perSpaceWidth: Float

    // 进度个数
    private var mSize = 20

    // 整数个进度的个数
    private var integerProgress: Int = 0
    private val rectF: RectF by lazy { RectF() }

    // 进度画笔
    private val progressPaint: Paint by lazy {
        Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = PAINT_WIDTH
        }
    }

    init {
        val obtainStyledAttributes = context.obtainStyledAttributes(attrs, R.styleable.MySeekBar)
        with(obtainStyledAttributes) {
            max = getInt(R.styleable.MySeekBar_sb_max, DEFAULT_MAX).toFloat()
            progress = getInt(R.styleable.MySeekBar_sb_progress, 0).toFloat()
            perProgressWidth =
                getDimension(R.styleable.MySeekBar_sb_per_progress, DEFAULT_PER_PROGRESS_WIDTH)
            perSpaceWidth = perProgressWidth / 2
        }
        obtainStyledAttributes.recycle()
        isClickable = true
        isFocusable = true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        //post(mRunnable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val strokeWidth = progressPaint.strokeWidth
        val mWidth: Float =
            perProgressWidth * mSize + perSpaceWidth * (mSize - 1) + strokeWidth * mSize * 2
        val mHeight: Float =
            when (MeasureSpec.getMode(heightMeasureSpec)) {
                MeasureSpec.EXACTLY -> {
                    MeasureSpec.getSize(heightMeasureSpec).toFloat()
                }
                else -> {
                    // 高度 = 格子宽度*4+笔画宽度*2
                    perProgressWidth * 4F + strokeWidth * 2
                }
            }
        setMeasuredDimension(mWidth.toInt(), mHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        calculateProgress()
        (0 until mSize).forEach { index ->
            val strokeWidth = progressPaint.strokeWidth
            val perTemp = perProgressWidth + strokeWidth
            var rLeft =
                index * (perProgressWidth + perSpaceWidth) + index * 2 * strokeWidth + strokeWidth / 2
            var rTop = strokeWidth / 2
            var rRight = rLeft + perTemp
            var rBottom = height - strokeWidth / 2
            when {
                index < integerProgress -> {
                    progressPaint.style = Paint.Style.FILL
                    progressPaint.color = Color.parseColor("#ff9988")
                    // Paint.Style.FILL和 Paint.Style.STROKE有差别
                    // 实心的比空心的小
                    rLeft -= strokeWidth / 2
                    rRight += strokeWidth / 2
                }
                index == integerProgress -> {
                    progressPaint.style = Paint.Style.STROKE
                    progressPaint.color = Color.parseColor("#ff9988")
                    // Paint.Style.FILL和 Paint.Style.STROKE有差别
                    // 实心的比空心的小
                    rTop += strokeWidth / 2
                    rBottom -= strokeWidth / 2
                }
                else -> {
                    // Paint.Style.FILL和 Paint.Style.STROKE有差别
                    // 实心的比空心的小
                    rTop += strokeWidth / 2
                    rBottom -= strokeWidth / 2
                    progressPaint.style = Paint.Style.STROKE
                    progressPaint.color = Color.RED
                }
            }
            rectF.set(rLeft, rTop, rRight, rBottom)
            canvas.drawRoundRect(rectF, ROUND, ROUND, progressPaint)
        }
        drawNonIntegerProgress(canvas)
    }

    // 画不满的格子
    private fun drawNonIntegerProgress(canvas: Canvas) {
        val strokeWidth = progressPaint.strokeWidth
        val realTotalWidth = realWidth()
        if (mSize * progress % max == 0f) {
            logger("整除")
            return
        }
        val nonInterProgress =
            realTotalWidth * progress / max - integerProgress * perProgressWidth
        logger("nonInterProgress = $nonInterProgress")

        val rLeft =
            integerProgress * (perSpaceWidth + perProgressWidth) + integerProgress * 2 * strokeWidth + strokeWidth
        val rTop = strokeWidth / 2
        val rRight = rLeft + nonInterProgress
        val rBottom = height.toFloat() - strokeWidth / 2
        rectF.set(rLeft, rTop, rRight, rBottom)
        progressPaint.style = Paint.Style.FILL
        progressPaint.color = Color.parseColor("#ff9988")
        //canvas.drawRoundRect(rectF, ROUND, ROUND, progressPaint)
        val path = Path().apply {
            addRoundRect(rectF, ROUND, ROUND, Path.Direction.CW)
        }
        canvas.clipPath(path, Region.Op.UNION)
        canvas.drawPath(path, progressPaint)
    }

    private fun realWidth() = mSize * perProgressWidth

    private fun accuracyWidth() = width * accuracy / max

    // 根据max:progress确定进度
    private fun calculateProgress() {
        integerProgress = (mSize * progress / max).toInt()
        logger("$mSize*$progress/$max integerProgress = $integerProgress")
    }

    private fun onChanged() {
        logger("onChanged $progress")
        onProgressChanged?.invoke(this, progress, max)
    }

    private fun onSelected() {
        logger("onSelected $progress")
        onProgressSelectedListener?.invoke(this, progress, max)
        removeCallbacks(mRunnable)
        post(mRunnable)
    }

    // 精度调节
    private fun calibration(value: Float) =
        calibrationCalculation?.invoke(value) ?: (value * 1.0f / accuracy).roundToLong()
            .toFloat() * accuracy

    // 自定义精度调节实现
    var calibrationCalculation: ((Float) -> Float)? = null

    fun changeSize(size: Int) {
        mSize = size
        requestLayout()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(mRunnable)
    }

    private val mHandler: android.os.Handler = android.os.Handler {
        when (it.what) {
            1 -> {
                post(mRunnable)
            }
        }
        false
    }

    // 自动增加progress测试
    private val mRunnable = Runnable {
        if (progress >= max) {
            progress = 0f
        }
        progress += accuracy
        invalidate()
        mHandler.sendEmptyMessageDelayed(1, 20)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var downX = 0f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                logger("ACTION_DOWN $downX")
                downX = event.x
                val currentProgress = max * downX / width
                progress = currentProgress
                calculateProgress()
                invalidate()
                onChanged()
            }
            MotionEvent.ACTION_MOVE -> {
                logger("ACTION_MOVE")
                val moveX = event.x
                if (abs(downX - moveX) >= accuracyWidth()) {
                    val currentProgress = max * moveX / width
                    progress = currentProgress
                    calculateProgress()
                    invalidate()
                    onChanged()
                }
            }
            MotionEvent.ACTION_UP -> {
                logger("ACTION_UP")
                val upX = event.x
                if (abs(downX - upX) >= accuracyWidth()) {
                    val currentProgress = max * upX / width
                    progress = currentProgress
                    calculateProgress()
                    invalidate()
                    onSelected()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                logger("ACTION_CANCEL")
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private var DEBUG = true
        private const val TAG = "MySeekBar"
        private const val ROUND = 5f
        private const val PAINT_WIDTH = 5f
        private const val DEFAULT_MAX = 100
        private const val DEFAULT_PER_PROGRESS_WIDTH = 10f
        private fun logger(msg: String) {
            if (!DEBUG) return
            Log.i(TAG, msg)
        }
    }
}

typealias OnProgressSelectedListener = (View, Float, Float) -> Unit

typealias OnProgressChangedListener = (View, Float, Float) -> Unit