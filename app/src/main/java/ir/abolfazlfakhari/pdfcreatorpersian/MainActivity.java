package ir.abolfazlfakhari.pdfcreatorpersian;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText ed_content;
    Button btn_get_pic, btn_create_pdf;
    List<String> imagesUri;
    Image image;
    Uri mCapturedImageURI;
    Font font;

    String[] array_permission = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
    private static final int PERMISSION_ALL = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ed_content = (EditText) findViewById(R.id.ed_content);
        btn_get_pic = (Button) findViewById(R.id.btn_get_pic);
        btn_create_pdf = (Button) findViewById(R.id.btn_create_pdf);

        imagesUri = new ArrayList<>();

        btn_get_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CheckingPermissionIsEnabledOrNot()) {
                    try {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            String fileName = "test.jpg";
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.TITLE, fileName);
                            mCapturedImageURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        }
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "دوربین دردسترس نیست،لطفابعداامتحان کنید", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    ActivityCompat.requestPermissions(MainActivity.this, array_permission, PERMISSION_ALL);
                }


            }
        });

        btn_create_pdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (imagesUri.size() != 0) {
                            new creatingPDF().execute();
                        } else {
                            Toast.makeText(MainActivity.this, "هیچ عکسی انتخاب نشده است", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

    }

    public boolean CheckingPermissionIsEnabledOrNot() {
        int CameraPermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        int StoragePermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return CameraPermission == PackageManager.PERMISSION_GRANTED &&
                StoragePermission == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL:
                if (grantResults.length > 0) {

                    boolean WRITE_EXTERNAL_STORAGE = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean CameraPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (CameraPermission && WRITE_EXTERNAL_STORAGE) {

                    } else {
                        Toast.makeText(this, "لطفادسترسی های مربوطه را تنظیم کنید", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = managedQuery(mCapturedImageURI, projection, null, null, null);
            int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String picturePath = cursor.getString(column_index_data);
            imagesUri.add(picturePath);
            Toast.makeText(MainActivity.this, "عکس با موفقیت اضافه شد", Toast.LENGTH_SHORT).show();
        }
    }


    public class creatingPDF extends AsyncTask<String, String, String> {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        // Progress dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage("لطفاچندلحظه صبرکنید");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {

            //setFileName PDF
            Date date = new Date();
            Calendar calendar = Calendar.getInstance();

            int hours = date.getHours();
            int minutes = date.getMinutes();
            int second = date.getSeconds();

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            StringBuilder file_name = new StringBuilder();
            file_name
                    .append(year).append("-")
                    .append(month).append("-")
                    .append(day).append("_")
                    .append(hours).append(":")
                    .append(minutes).append(":")
                    .append(second);


            File root = Environment.getExternalStorageDirectory();

            File dir = new File(root.getAbsolutePath() + "/PDFCreatorPersian/");
            if (!dir.exists()) {
                dir.mkdir();
            }
            dir.mkdirs();
            String file_name_pdf = file_name + ".pdf";
            File file = new File(dir, file_name_pdf);


            font = FontFactory.getFont("assets/fonts/Nazanin.ttf", BaseFont.IDENTITY_H, 18);

            //Create Document With iTextPDF
            Document document = new Document(PageSize.A4, 38, 38, 50, 38);

            //Write File .pdf To Folder PDFCreatorPersian
            try {
                PdfWriter.getInstance(document, new FileOutputStream(file));
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


            document.open();

            String content = ed_content.getText().toString();

            PdfPTable table = new PdfPTable(1);
            table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

            PdfPCell pdfCell = new PdfPCell(new Phrase(content, font));
            pdfCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(pdfCell);

            Paragraph paragraph = new Paragraph("This is a Test");
            try {
                document.add(paragraph);
            } catch (DocumentException e) {
                e.printStackTrace();
            }

            try {
                document.add(table);
            } catch (DocumentException e) {
                e.printStackTrace();
            }

            //Get Image From imagedUri And set To document Object
            try {
                image = Image.getInstance(imagesUri.get(0));
                //Scale Image
                image.scaleAbsolute(400, 400);
                //PositionImage
                image.setAbsolutePosition(100, 120);

                try {
                    document.add(image);
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
                document.newPage();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "ذخیره فایل با مشکل مواجه شد", Toast.LENGTH_SHORT).show();
            } catch (BadElementException e) {
                e.printStackTrace();
            }

            document.close();
            imagesUri.clear();

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            Toast.makeText(MainActivity.this, "فایل با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.author_action:
                AlertDialog.Builder builder=new AlertDialog.Builder(this);
                View view_alert= LayoutInflater.from(this).inflate(R.layout.author_dialog,null,false);
                builder.setView(view_alert);
                TextView txt_site_me=(TextView)view_alert.findViewById(R.id.txt_site_me);
                TextView txt_github_me=(TextView)view_alert.findViewById(R.id.txt_github_me);
                Linkify.addLinks(txt_site_me,Linkify.WEB_URLS);
                Linkify.addLinks(txt_github_me,Linkify.WEB_URLS);

                builder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
