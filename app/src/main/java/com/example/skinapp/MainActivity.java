package com.example.skinapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.skinapp.ml.ModelMobile2;
import com.theartofdev.edmodo.cropper.CropImage;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;


//android:startColor="@color/purple_700"
//        android:centerColor="@color/ColoPrimary"
//        android:endColor="@color/teal_200"


public class MainActivity extends AppCompatActivity {

    private static  final int GALLERY_REQUEST_CODE = 123;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private ImageView imageView;
    private Button select, takephoto;
    private ImageButton info;
    private TextView textView, textView2;
    private Bitmap img;
    private Uri uri;
    private  String currentPhotoPath;
    private  String GALLERY_LOCATION = "SkinGallery";
    private File mGalleryFolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = findViewById(R.id.info);
        imageView = findViewById(R.id.ImageView1);
        select = findViewById(R.id.select);
        textView = findViewById(R.id.TextView1);
        takephoto = findViewById(R.id.takephoto);
        textView2 = findViewById(R.id.textView2);

        creatSkinGallery();

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                Manifest.permission.CAMERA }, 100);
        }


        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            
                Intent activity2Intent = new Intent(getApplicationContext(), Activity2.class);
                startActivity(activity2Intent);

            }
        });


        takephoto.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {

                Log.d("fds", "antes do dispatch intent") ;

                dispatchTakePictureIntent();
            }
        });

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");

                startActivityForResult(Intent.createChooser(intent,
                        "Pick an image"), GALLERY_REQUEST_CODE);

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Importar imagem da galeria
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            uri = data.getData();
            CropImage.activity(uri)
                    .setAspectRatio(1,1)
                    .setMinCropResultSize(224,224)
                    .start(this);
            //.setMaxCropResultSize(224,224)
            //imageView.setImageURI(uri);
        }

        //tirar foto diretamente
        if (requestCode == REQUEST_TAKE_PHOTO) {

            Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
            Uri ImageUri = Uri.fromFile(new File(currentPhotoPath));

            //imageView.setImageURI(ImageUri);

            CropImage.activity(ImageUri)
                    .setAspectRatio(1,1)
                    .setMinCropResultSize(224,224)
                    .start(this);

            //.setMaxCropResultSize(224,224)
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                imageView.setImageURI(resultUri);

                 //conveter o crop em bitmap para ser lido pelo modelo
                //IMPORTANTE

                try {
                    img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    makeInference(img);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void makeInference(Bitmap img) {

        @NonNull float[] outputFeature0 = Model(img);
        double scale = Math.pow(10, 2);
        //textView.setText(outputFeature0.getFloatArray()[0] + "\n" + outputFeature0.getFloatArray()[1]);
        int benValue = Math.round(outputFeature0[0] );
        if(benValue == 1){

            textView.setText("Diagnosis: Benign");
            textView2.setText("Confidence: " + Math.round(outputFeature0[0]*100*scale)/scale + "%");

            //textView2.setText("Confidence: " + (outputFeature0[0])*100+"%");
            //Log.d("aqui", (outputFeature0[0]).getClass().getSimpleName());
        }
        else {
            textView.setText("Diagnosis: Malignant");
            textView2.setText("Confidence: " + Math.round(outputFeature0[1]*100*scale)/scale + "%");

        }

    }

    //Camera intent com FileIntent incluido

    private void dispatchTakePictureIntent() {

        Log.d("fds", "inicio do dispatch") ;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        Log.d("fds", "depois do action") ;
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                //por um toast
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                Log.d("fds", "nao é nulo");

                Uri photoURI = FileProvider.getUriForFile(this, "com.example.skinapp.fileprovider",photoFile);

                //Uri  photoURI = Uri.fromFile(photoFile);

                Log.d("fds", "Uri from file" + photoURI) ;

                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,  Uri.fromFile(photoFile));
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
           }
        }
    }

    //Creat File intent, sem isto nao conseguia ir buscar os URI e a qualidade baixava mt

    private void creatSkinGallery(){

        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mGalleryFolder = new File(storageDirectory, GALLERY_LOCATION);

        if(!mGalleryFolder.exists()){
            mGalleryFolder.mkdir();

        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        Log.d("fds", "entrou no creat file");

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        String path = Environment.getExternalStorageDirectory().toString()+"/Pictures/skin gallery/";  //"/DCIM/BenignTeste or DCIM/naevus "
        File directory = new File(path);

        File direct = new File(Environment.getExternalStorageDirectory() + "/Pictures/Ski");
        Log.d("fds", storageDir.getAbsolutePath());
        Log.d("fds", direct.getAbsolutePath());
        Log.d("fds", path);

        File image = File.createTempFile(      imageFileName,  ".jpg", storageDir );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        Log.d("fds", currentPhotoPath);
        return image;

    }



    protected @NonNull float[] Model(Bitmap img) {

        /*ModelEficient model = null;
        try {
            model = ModelEficient.newInstance(getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        /*Skin1model model = null;
        try {
            model = Skin1model.newInstance(getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        ModelMobile2 model = null;
        try {
            model = ModelMobile2.newInstance(getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }




        // Initialization code
        // Create an ImageProcessor with all ops required. For more ops, please
        // refer to the ImageProcessor Architecture section in this README.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                        .add(new NormalizeOp(0f, 255.0f)) //this makes all the diference
                        .build();

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(img);
        tensorImage = imageProcessor.process(tensorImage);
        ByteBuffer byteBuffer = tensorImage.getBuffer();

        inputFeature0.loadBuffer(byteBuffer);

        // Runs model inference and gets result.
        //Skin1model.Outputs outputs = model.process(inputFeature0);
        //ModelEficient.Outputs outputs = model.process(inputFeature0);

        long startTime = System.nanoTime();

        ModelMobile2.Outputs outputs = model.process(inputFeature0);



        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();


        Log.d("Saídas do modelo", String.valueOf(outputFeature0.getFloatArray()[0]));
        Log.d("Saídas do modelo", String.valueOf(outputFeature0.getFloatArray()[1]));

        // Releases model resources if no longer used.
        model.close();
        //textView.setText(outputFeature0.getFloatArray()[0] + "\n" + outputFeature0.getFloatArray()[1]);

        long stopTime = System.nanoTime();
        Log.d("tempo", String.valueOf(stopTime - startTime));

        return outputFeature0.getFloatArray();

    }
}

