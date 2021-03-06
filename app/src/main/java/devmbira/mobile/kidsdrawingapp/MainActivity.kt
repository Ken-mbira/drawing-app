package devmbira.mobile.kidsdrawingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView? = null
    private var mBrushSizeSliderValue:Float = 10.0F
    private var mImageButtonCurrentPaint:ImageButton? = null
    private var mImageChooserButton:ImageButton? = null
    private var imageBackground : ImageView? = null
    private var progressDialog : Dialog? = null

    private val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                imageBackground = findViewById(R.id.iv_background)
                imageBackground?.setImageURI(result.data?.data)
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

        imageBackground = findViewById<ImageView?>(R.id.iv_background)
        imageBackground?.setBackgroundColor(Color.WHITE)

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

        val ibSave : ImageButton = findViewById(R.id.ib_store)
        ibSave.setOnClickListener{
            when (PackageManager.PERMISSION_GRANTED){
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) -> {
                    launchProgressDialog()
                    lifecycleScope.launch {
                        val flDrawingView : FrameLayout = findViewById(R.id.fl_drawing_view_container)
                        saveBitmapFile(getBitmapFromView(flDrawingView))
                    }
                }else -> {
                    requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
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

    private fun getBitmapFromView(view: View): Bitmap {

        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)

        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {

                try {
                    val bytes = ByteArrayOutputStream()

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )

                    val fo =
                        FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread {
                        if (!result.isEmpty()) {
                            cancelProgressDialog()
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun launchProgressDialog(){
        progressDialog = Dialog(this@MainActivity)

        progressDialog?.setContentView(R.layout.custom_progress_dialog)
        progressDialog?.show()

    }
    private fun cancelProgressDialog(){
        if(progressDialog!=null){
            progressDialog?.dismiss()
            progressDialog = null
        }
    }

    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(
            this@MainActivity,
            arrayOf(result),
            null
        ){
            _,uri ->

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                uri
            )
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(
                shareIntent,"Share"
            ))
        }
    }
}