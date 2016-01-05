package project.comp3617.com.frequency.Frequency;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import project.comp3617.com.frequency.FFT_Package.RealDoubleFFT;

public class Frequency extends Activity implements OnClickListener {
    private AudioRecord record;
    private RealDoubleFFT fft;
    private int bs;
    private int frequencyKHz = 16000;
    private int channel = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audio = AudioFormat.ENCODING_PCM_16BIT;
    private int width;
    private int height;
    private Button startButton;
    private boolean start = false;
    private RecordAudio recordAudio;
    private ImageView imageView;
    private Bitmap bitmap;
    private Paint paint;
    private Canvas canvas;
    private LinearLayout linear;
    private CustomScale viewScale;
    private Paint scalePaint;
    private Bitmap scaleBitmap;
    private Canvas scaleCanvas;
    private int bitmapLeft;
    private int displayLeft;
    private static Frequency frequency;
    private final static int ID_BITMAP = 1;
    private final static int ID_VIEWSCALE = 2;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Display display = getWindowManager().getDefaultDisplay();
        width = display.getWidth();
        height = display.getHeight();
        bs = 256;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        CustomScale scale = (CustomScale) linear.findViewById(ID_VIEWSCALE);
        ImageView bitmap = (ImageView) linear.findViewById(ID_BITMAP);
        bitmapLeft = scale.getLeft();
        displayLeft = bitmap.getLeft();
    }

    public void onClick(View view) {
        if (start == true) {
            start = false;
            startButton.setText("Start");
            recordAudio.cancel(true);
            canvas.drawColor(Color.WHITE);
        } else {
            start = true;
            startButton.setText("Stop");
            recordAudio = new RecordAudio();
            recordAudio.execute();
        }
    }

    static Frequency getFrequency() {
        return frequency;
    }

    public void onStop() {
        super.onStop();
        recordAudio.cancel(true);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void onStart() {
        super.onStart();
        linear = new LinearLayout(this);
        linear.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.FILL_PARENT));
        linear.setOrientation(LinearLayout.VERTICAL);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        fft = new RealDoubleFFT(bs);

        imageView = new ImageView(this);
        if (width > 512) {
            bitmap = Bitmap.createBitmap(512, 300, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(256, 150, Bitmap.Config.ARGB_8888);
        }
        LinearLayout.LayoutParams displayStartButton = null;

        canvas = new Canvas(bitmap);

        paint = new Paint();
        paint.setColor(Color.BLUE);
        imageView.setImageBitmap(bitmap);
        if (width > 512) {
            LinearLayout.LayoutParams displayGraph = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ((ViewGroup.MarginLayoutParams) displayGraph).setMargins(300, 600, 0, 0);
            imageView.setLayoutParams(displayGraph);
            displayStartButton = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            ((ViewGroup.MarginLayoutParams) displayStartButton).setMargins(1024, 512, 0, 0);
        }

        imageView.setId(ID_BITMAP);
        linear.addView(imageView);

        viewScale = new CustomScale(this);
        viewScale.setLayoutParams(displayStartButton);
        viewScale.setId(ID_VIEWSCALE);

        linear.addView(viewScale);

        startButton = new Button(this);
        startButton.setText("Start");
        startButton.setOnClickListener(this);
        startButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        linear.addView(startButton);

        setContentView(linear);

        frequency = this;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        recordAudio.cancel(true);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordAudio.cancel(true);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private class RecordAudio extends AsyncTask <Void, double[], Void> {
        @Override
        protected Void doInBackground(Void...params) {
            if (isCancelled()) {
                return null;
            }

            int bufferSize = AudioRecord.getMinBufferSize(frequencyKHz, channel, audio);
            record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, frequencyKHz, channel, audio, bufferSize);
            int bufferResult;
            short[] buffer = new short[bs];
            double[] translate = new double[bs];
            try {
                record.startRecording();
            } catch (IllegalStateException e) {
                Log.e("Record fail", e.toString());

            }

            while (start) {
                bufferResult = record.read(buffer, 0, bs);

                if (isCancelled()) {
                    break;
                }

                for (int i = 0; i < bs && i < bufferResult; i++) {
                    translate[i] = (double) buffer[i] / 32768.0;
                }

                fft.ft(translate);

                publishProgress(translate);
                if (isCancelled())
                    break;
            }

            try {
                record.stop();
            } catch (IllegalStateException e) {
                Log.e("Record Stop", e.toString());
            }
            return null;
        }

        protected void onProgressUpdate(double[]...translate) {
            Log.d("Recording", "Record working");

            if (width > 512) {
                for (int i = 0; i < translate[0].length; i++) {
                    int x = 2 * i;
                    int down = (int)(150 - (translate[0][i] * 10));
                    int up = 150;
                    canvas.drawLine(x, down, x, up, paint);
                }
                imageView.invalidate();
            } else {
                for (int i = 0; i < translate[0].length; i++) {
                    int x = i;
                    int down = (int)(150 - (translate[0][i] * 10));
                    int up = 150;
                    canvas.drawLine(x, down, x, up, paint);
                }
                imageView.invalidate();
            }

        }

        protected void onPostExecute(Void result) {
            try {
                record.stop();
            } catch (IllegalStateException e) {
                Log.e("Stop failed", e.toString());

            }
            recordAudio.cancel(true);

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

    }

    public class CustomScale extends ImageView {
        public CustomScale(Context context) {
            super(context);
            if (width > 512) {
                scaleBitmap = Bitmap.createBitmap(300, 50, Bitmap.Config.ARGB_8888);
            } else {
                scaleBitmap = Bitmap.createBitmap(150, 50, Bitmap.Config.ARGB_8888);
            }

            scalePaint = new Paint();
            scalePaint.setColor(Color.WHITE);
            scalePaint.setStyle(Paint.Style.FILL);

            scaleCanvas = new Canvas(scaleBitmap);

            setImageBitmap(scaleBitmap);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (width > 512) {
                scaleCanvas.drawLine(0, 30, 512, 30, scalePaint);
                for (int i = 0, j = 0; i < 512; i += 128, j++) {
                    for (int k = i; k < i + 128; k += 16) {
                        scaleCanvas.drawLine(k, 30, k, 25, scalePaint);
                    }
                    scaleCanvas.drawLine(i, 40, i, 25, scalePaint);
                    String s = Integer.toString(j) + " KHz";
                    scaleCanvas.drawText(s, i, 45, scalePaint);
                }
                canvas.drawBitmap(scaleBitmap, 0, 0, scalePaint);
            } else if (width > 320 && width < 512) {
                scaleCanvas.drawLine(0, 30, 256, 30, scalePaint);
                for (int i = 0, j = 0; i < 256; i += 64, j++) {
                    for (int k = i; k < i + 64; k += 8) {
                        scaleCanvas.drawLine(k, 30, k, 25, scalePaint);
                    }
                    scaleCanvas.drawLine(i, 40, i, 25, scalePaint);
                    String s = Integer.toString(j) + " KHz";
                    scaleCanvas.drawText(s, i, 45, scalePaint);
                }
                canvas.drawBitmap(scaleBitmap, 0, 0, scalePaint);
            } else if (width < 320) {
                scaleCanvas.drawLine(0, 30, 256, 30, scalePaint);
                for (int i = 0, j = 0; i < 256; i += 64, j++) {
                    for (int k = i; k < i + 64; k += 8) {
                        scaleCanvas.drawLine(k, 30, k, 25, scalePaint);
                    }
                    scaleCanvas.drawLine(i, 40, i, 25, scalePaint);
                    String s = Integer.toString(j) + " KHz";
                    scaleCanvas.drawText(s, i, 45, scalePaint);
                }
                canvas.drawBitmap(scaleBitmap, 0, 0, scalePaint);
            }
        }
    }
}