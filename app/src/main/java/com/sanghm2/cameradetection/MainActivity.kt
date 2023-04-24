package com.sanghm2.cameradetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sanghm2.cameradetection.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.w3c.dom.Text

class MainActivity : AppCompatActivity() {
    val paint = Paint()
    lateinit var labels:List<String>
    lateinit var imageProcess: ImageProcessor
    lateinit var cameraManger : CameraManager
    lateinit var texttureView : TextureView
    lateinit var handler: Handler
    lateinit var cameraDevice: CameraDevice
    lateinit var imageView: ImageView
    lateinit var bitmap: Bitmap
    lateinit var model : SsdMobilenetV11Metadata1
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getPermission()

        labels = FileUtil.loadLabels(this,"labels.txt")
        var colors = listOf<Int>(
            Color.BLUE , Color.GREEN,Color.RED,Color.CYAN,Color.GRAY,Color.BLACK,Color.DKGRAY,Color.MAGENTA,Color.YELLOW,Color.RED
        )
        imageProcess = ImageProcessor.Builder().add(ResizeOp(300,300,ResizeOp.ResizeMethod.BILINEAR)).build()
        model = SsdMobilenetV11Metadata1.newInstance(this)
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        texttureView = findViewById(R.id.texttureView)
        imageView = findViewById(R.id.imageView)
        cameraManger=  getSystemService(Context.CAMERA_SERVICE) as CameraManager
        texttureView.surfaceTextureListener = object :TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                TODO("Not yet implemented")
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitmap = texttureView.bitmap!!


                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcess.process(image)
                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray
                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888,true)
                val canvas = Canvas(mutable)


                val h = mutable.height
                val w = mutable.width
                paint.textSize = h/15f
                paint.strokeWidth = h/85f
                var x = 0
                scores.forEachIndexed{ index , fl ->
                    x =index
                    x*= 4
                    if(fl >0.5){
                        paint.setColor(colors.get(index))
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(RectF(locations.get(x+1)*w,locations.get(x)*h,locations.get(x+3)*w,locations.get(x+2)*h),paint)
                        paint.style = Paint.Style.FILL
                        canvas.drawText(labels.get(classes.get(index).toInt()),locations.get(x+1)*w,locations.get(x)*h,paint)
                    }

                }
                imageView.setImageBitmap(mutable)

            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {

        cameraManger.openCamera(cameraManger.cameraIdList[0], object :CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0
                var surfaceTextture = texttureView.surfaceTexture
                var surface = Surface(surfaceTextture)
                var captureRequest  = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface),object :CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(),null,null)
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                        TODO("Not yet implemented")
                    }
                },handler)
            }

            override fun onDisconnected(p0: CameraDevice) {
                TODO("Not yet implemented")
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                TODO("Not yet implemented")
            }

        },handler)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getPermission() {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),101)
        }else {
            return
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
            getPermission()
        }
    }
}