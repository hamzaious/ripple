package com.example.rippleeffect

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.ripple.effect.RippleBackgroundView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {

    private lateinit var ripple: RippleBackgroundView
    private lateinit var demoButton: MaterialButton
    private lateinit var shapeGroup: MaterialButtonToggleGroup
    private lateinit var typeGroup: MaterialButtonToggleGroup
    private lateinit var speedBar: SeekBar
    private lateinit var amountBar: SeekBar
    private lateinit var scaleBar: SeekBar
    private lateinit var cornerBar: SeekBar
    private lateinit var speedLabel: TextView
    private lateinit var amountLabel: TextView
    private lateinit var scaleLabel: TextView
    private lateinit var cornerLabel: TextView
    private lateinit var toggleBtn: Button

    private val density: Float
        get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        ripple = findViewById(R.id.ripple)
        demoButton = findViewById(R.id.demoButton)
        shapeGroup = findViewById(R.id.shapeGroup)
        typeGroup = findViewById(R.id.typeGroup)
        speedBar = findViewById(R.id.speedBar)
        amountBar = findViewById(R.id.amountBar)
        scaleBar = findViewById(R.id.scaleBar)
        cornerBar = findViewById(R.id.cornerBar)
        speedLabel = findViewById(R.id.speedLabel)
        amountLabel = findViewById(R.id.amountLabel)
        scaleLabel = findViewById(R.id.scaleLabel)
        cornerLabel = findViewById(R.id.cornerLabel)
        toggleBtn = findViewById(R.id.toggleBtn)

        wireShapeToggle()
        wireTypeToggle()
        wireSliders()

        toggleBtn.setOnClickListener {
            if (ripple.isRunning) {
                ripple.stop()
                toggleBtn.text = getString(R.string.start)
            } else {
                ripple.start()
                toggleBtn.text = getString(R.string.stop)
            }
        }

        ripple.start()
        toggleBtn.text = getString(R.string.stop)
    }

    private fun wireShapeToggle() {
        shapeGroup.check(R.id.btnCircle)
        shapeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnMatch -> {
                    // Auto-derive the ripple shape from the wrapped MaterialButton.
                    ripple.sourceView = demoButton
                }
                else -> {
                    // Detach from source so the manual rippleRadius applies again.
                    ripple.sourceView = null
                    ripple.rippleShape = when (checkedId) {
                        R.id.btnStar -> RippleBackgroundView.Shape.STAR
                        R.id.btnArrow -> RippleBackgroundView.Shape.ARROW
                        R.id.btnDiamond -> RippleBackgroundView.Shape.DIAMOND
                        R.id.btnMoon -> RippleBackgroundView.Shape.MOON
                        else -> RippleBackgroundView.Shape.CIRCLE
                    }
                }
            }
        }
    }

    private fun wireTypeToggle() {
        typeGroup.check(R.id.btnFill)
        typeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            ripple.rippleType = if (checkedId == R.id.btnStroke) {
                RippleBackgroundView.Type.STROKE
            } else {
                RippleBackgroundView.Type.FILL
            }
        }
    }

    private fun wireSliders() {
        // Duration: 500..6000 ms
        speedBar.max = 5500
        speedBar.progress = 2500 // 500 + 2500 = 3000ms default
        updateSpeedLabel(speedBar.progress)
        speedBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                ripple.rippleDuration = (500 + value).toLong()
                updateSpeedLabel(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) = Unit
            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        })

        // Amount: 1..8
        amountBar.max = 7
        amountBar.progress = 3 // 1 + 3 = 4 default
        updateAmountLabel(amountBar.progress)
        amountBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                ripple.rippleAmount = 1 + value
                updateAmountLabel(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) = Unit
            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        })

        // Scale: 2..10
        scaleBar.max = 8
        scaleBar.progress = 4 // 2 + 4 = 6 default
        updateScaleLabel(scaleBar.progress)
        scaleBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                ripple.rippleScale = (2 + value).toFloat()
                updateScaleLabel(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) = Unit
            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        })

        // Match-view corner radius: 0 = Auto, 1..64 = 0..63 dp.
        cornerBar.max = 64
        cornerBar.progress = 0 // Auto
        updateCornerLabel(cornerBar.progress)
        cornerBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                ripple.matchViewCornerRadius = when (value) {
                    0 -> RippleBackgroundView.MATCH_VIEW_CORNER_RADIUS_AUTO
                    else -> (value - 1) * density
                }
                updateCornerLabel(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) = Unit
            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        })
    }

    private fun updateSpeedLabel(value: Int) {
        speedLabel.text = getString(R.string.speed_value, 500 + value)
    }

    private fun updateAmountLabel(value: Int) {
        amountLabel.text = getString(R.string.amount_value, 1 + value)
    }

    private fun updateScaleLabel(value: Int) {
        scaleLabel.text = getString(R.string.scale_value, 2 + value)
    }

    private fun updateCornerLabel(value: Int) {
        cornerLabel.text = if (value == 0) {
            getString(R.string.corner_auto)
        } else {
            getString(R.string.corner_value, value - 1)
        }
    }
}
