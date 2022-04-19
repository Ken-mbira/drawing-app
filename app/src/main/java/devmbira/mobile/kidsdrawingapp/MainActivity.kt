package devmbira.mobile.kidsdrawingapp

import android.annotation.SuppressLint
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView? = null
    private var mBrushSizeSliderValue:Float = 10.0F
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val ib_brush : ImageButton = findViewById(R.id.ib_brush)

        ib_brush.setOnClickListener{
            showBrushSizeChooserSlider()
        }

    }
    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn:ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        val mediumBtn:ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        val largeBtn:ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    private fun showBrushSizeChooserSlider(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.slider_brush_size)
        brushDialog.setTitle("Brush size:")
        val brushSlider: Slider = brushDialog.findViewById(R.id.brush_slider)
        brushSlider.value = mBrushSizeSliderValue
        brushSlider.addOnSliderTouchListener(object :
        Slider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {
            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                brushDialog.dismiss()
            }

        })
        brushSlider.addOnChangeListener{
            _,value,_ ->
            drawingView?.setSizeForBrush(value)
            mBrushSizeSliderValue = value
        }
        brushDialog.show()
    }
}