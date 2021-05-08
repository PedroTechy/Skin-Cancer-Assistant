package com.example.skinapp;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skinapp.ml.Skin1model;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import static android.provider.MediaStore.Images.Media.getBitmap;

public class MainActivity extends AppCompatActivity {

    private static  final int GALLERY_REQUEST_CODE = 123;
    private ImageView imageView;
    private Button select, predict, takephoto;
    private TextView textView;
    private Bitmap img,bitmap;
    private Uri uri;
    private  String currentPhotoPath;
    Intent CropIntent;
    DisplayMetrics displayMetrics;
    int width, heigth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        imageView = findViewById(R.id.ImageView1);
        select = findViewById(R.id.select);
        textView = findViewById(R.id.TextView1);
        predict = findViewById(R.id.predict);
        takephoto = findViewById(R.id.takephoto);

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                Manifest.permission.CAMERA }, 100);
        }



        takephoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = "SkinLesion";
                File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                try {
                    File imageFile = File.createTempFile(fileName, ".jpg", storageDirectory);
                    currentPhotoPath = imageFile.getAbsolutePath();
                    Uri imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.skinapp.fileprovider", imageFile);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, 100);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });


        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*GalIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(GalIntent,"Select Image from Gallery"),2);*/
                //intent.setAction(Intent.ACTION_GET_CONTENT);

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");

                startActivityForResult(Intent.createChooser(intent,
                        "Pick an image"), GALLERY_REQUEST_CODE);

            }
        });

        predict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                img = Bitmap.createScaledBitmap(img,  224,224,true); //para converter imagem para o tamanho certo

                try {
                    Skin1model model = Skin1model.newInstance(getApplicationContext());

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);

                    TensorImage tensorImage  = new TensorImage(DataType.FLOAT32);
                    tensorImage.load(img);
                    ByteBuffer byteBuffer = tensorImage.getBuffer();

                    inputFeature0.loadBuffer(byteBuffer);

                    // Runs model inference and gets result.
                    Skin1model.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    // Releases model resources if no longer used.
                    model.close();
                    //textView.setText(outputFeature0.getFloatArray()[0] + "\n" + outputFeature0.getFloatArray()[1]);
                    int benValue = Math.round(outputFeature0.getFloatArray()[0] );
                    if(benValue == 1){
                        textView.setText("Low risk");
                    }
                    else {
                        textView.setText("High Risk");
                    }

                } catch (IOException e) {
                    textView.setText("You need to select or take a photo before predicting ");
                }

            }
        });


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Importar imagem da galeria
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            uri = data.getData();
           /* try {
                img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            CropImage();
            //imageView.setImageURI(uri);

        }

        //tirar foto diretamente
        if (requestCode == 100) {
            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            //imageView.setImageBitmap(bitmap);
            //Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            //ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            String path = MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), bitmap, "val", null);

            uri = Uri.parse(path);
            //imageView.setImageURI(uri);
            /*try {
                img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            CropImage();
            //usar bitmap, direto, daria na mesma, pelo menos numa primeira parte;
        }

        else if(requestCode == 1){
            if(data != null){
                Bundle b = data.getExtras();
                bitmap = b.getParcelable("data");
                img = bitmap;
                imageView.setImageBitmap(bitmap);
                // save();
            }
        }


    }

    private void CropImage(){
        try{

            CropIntent = new Intent("com.android.camera.action.CROP");
            CropIntent.setDataAndType(uri, "image/*");
            CropIntent.putExtra("crop", true);
            CropIntent.putExtra("outputX", 224);
            CropIntent.putExtra("outputY", 224);
            CropIntent.putExtra("aspectX", 1);
            CropIntent.putExtra("aspectY", 1);
            CropIntent.putExtra("scaleUpIfNeeded", false);
            CropIntent.putExtra("scale", false);
            CropIntent.putExtra("return-data", true);
            startActivityForResult(CropIntent, 1);


        }catch (ActivityNotFoundException e){
            Toast.makeText(this, "No activity found to resolve this intent", Toast.LENGTH_SHORT).show();
        }
    }

}

