package devmbira.mobile.kidsdrawingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView? = null
    private var mBrushSizeSliderValue:Float = 10.0F
    private var mImageButtonCurrentPaint:ImageButton? = null
    private var mImageChooserButton:ImageButton? = null

    private val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackGround : ImageView = findViewById(R.id.iv_background)

                imageBackGround.setImageURI(result.data?.data)
            }
        }

    private fun showExternalStorageDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setPositiveButton("I've changed my mind",
                    DialogInterface.OnClickListener {dialog,_ ->
                        dialog.dismiss()
                requestPermissionLauncher.launch(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ))

            })

            setNegativeButton("I stand by my choice",
                    DialogInterface.OnClickListener{dialog,_ ->
                        dialog.cancel()
                    })
        }
        builder.setTitle("External Storage Access")
        builder.setMessage("Without this permissions, you will not be able to utilise the applications storage functionality")
        val alertDialog:AlertDialog = builder.create()
        alertDialog.show()

    }

    private val requestPermissionLauncher :ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ permissionStatus -> permissionStatus.entries.forEach{
        val permissionName = it.key
        val isGranted = it.value
        if(!isGranted){
            showExternalStorageDialog()
        }
    }
    }

    private fun brushSizeDialogFunction(){
        val builder = AlertDialog.Builder(this)
        var placeholderWidth:Float = 0.0F

        val rootView = this.layoutInflater.inflate(R.layout.slider_brush_size,null)
        val brushSizeSlider : Slider? = rootView.findViewById(R.id.brush_slider)
        brushSizeSlider?.value = mBrushSizeSliderValue
        brushSizeSlider?.addOnChangeListener{
                _,value,_ ->
            mBrushSizeSliderValue = value
        }

        builder.setView(rootView)
            .setTitle("Choose your brush size")
            .setIcon(R.drawable.img)
            .setPositiveButton("Ok",
                DialogInterface.OnClickListener{dialog,_ ->
                    drawingView?.setSizeForBrush(mBrushSizeSliderValue)
                    dialog.dismiss()
                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener{dialog,_ ->
                    dialog.cancel()
                })
            .setOnCancelListener{
                if(drawingView!!.run {
                            getSizeForBrush() < 100.00 &&
                            getSizeForBrush() > 10.00
                    }){
                    mBrushSizeSliderValue = drawingView!!.getSizeForBrush()
                }
            }

        val alertDialog:AlertDialog = builder.create()
        alertDialog.setCancelable(true)
        alertDialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors:LinearLayout = findViewById(R.id.ll_paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallete_pressed)
        )

        val ibBrush : ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener{
            brushSizeDialogFunction()
        }

        val ibUndo : ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener{
            drawingView?.undoOnClick()
        }

        val ibRedo : ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener{
            drawingView?.redoOnClick()
        }

        mImageChooserButton = findViewById(R.id.ib_image_chooser)
        mImageChooserButton?.setOnClickListener {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) -> {
                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                    openGalleryLauncher.launch(pickIntent)
                }
                else -> {
                    requestPermissionLauncher.launch(arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ))
                }
            }
        }
    }

    fun paintClicked(view: View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallete_pressed)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallete_normal)
            )

            mImageButtonCurrentPaint = view
        }
    }
}