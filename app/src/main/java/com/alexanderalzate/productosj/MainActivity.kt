package com.alexanderalzate.productosj

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.jar.Manifest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE: Int = 1
    private val PERMISSION_CODE = 1000
    private var selectedPhoto:Uri?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        bnCapture.setOnClickListener {

           if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
               if(checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
               } else {
                   //permission granted
                   val permission = arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                   requestPermissions(permission,PERMISSION_CODE)

                   openCamara()
               }
           }else{
               //System < Marshmallow
               openCamara()
           }
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("message")

            myRef.setValue("Hello, World!")
        }

        bnGaleria.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent,0)
        }

        bnEnviar.setOnClickListener {
            uploadImageToFireBaseStorage()
            saveImageFromTakedPhoto()
        }

    }

    private fun openCamara() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE->{
                if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openCamara()
                }
                else{
                    Toast.makeText(this,"Permiso denegado",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            Log.e("MainActivity Bitmap",imageBitmap.toString())
            image_view.setImageBitmap(imageBitmap)

        }
        if(requestCode == 0 && resultCode== Activity.RESULT_OK && data!=null){
            Log.e("MainActivity fotoSele","Foto seleccionada")
            selectedPhoto = data.data
            Log.e("MainActivity","Desde la galería: $selectedPhoto")
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver,selectedPhoto)
            Log.e("MainActivity","bitmap: $bitmap")
            image_view.setImageBitmap(bitmap)
        }
    }

    private fun uploadImageToFireBaseStorage(){
        if(selectedPhoto==null)return

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference(filename)
        ref.putFile(selectedPhoto!!).addOnSuccessListener {
            Log.e("MainActivity upload","imagen cargada exitosamente ${it.metadata?.path}")
            ref.downloadUrl.addOnSuccessListener {
                Log.e("MainActivity","Ubicación de archivo: $it")
            }
        }

    }

    private fun saveImageFromTakedPhoto(){

        val filename = UUID.randomUUID().toString()
        val photoRef = FirebaseStorage.getInstance().getReference(filename)

        image_view.isDrawingCacheEnabled = true
        image_view.buildDrawingCache()
        val bitmap = (image_view.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100, baos)
        val data = baos.toByteArray()
        Log.e("MainActivity","data: $data")

        var uploadTask = photoRef.putBytes(data)


        val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot,Task<Uri>>{
            task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation photoRef.downloadUrl
        }).addOnCompleteListener{
            task ->
                if(task.isSuccessful){
                    val downloadUri = task.result
                    Log.e("MainActivity","URL desde take photo:  ${downloadUri.toString()}")
                    Log.e("MainActivity","file:  $filename")
                }
        }
    }

}
