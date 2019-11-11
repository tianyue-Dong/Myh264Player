package com.example.myh264player;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //private String h264Path = "android.resource://" + getPackageName() + "/" + ;
    @SuppressLint("SdCardPath")
    private String h264Path = "/mnt/sdcard/Pictures/vid.h264";
    //AssetManager assets= getAssets();
    //private String h264Path ="file:///android_asset/text1.txt";
    //InputStream h264Path = getResources().openRawResource(R.raw.text1);
    //String filePath= "android.resource://" + getPackageName() + "/" + R.;
    private File h264File = new File(h264Path);
    private InputStream is = null;
    private FileInputStream fs = null;

    private SurfaceView mSurfaceView;
    private Button mReadButton;
    private MediaCodec mCodec;

    Thread readFileThread;
    boolean isInit = false;

    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int VIDEO_WIDTH = 1280;
    private final static int VIDEO_HEIGHT = 720;
    private final static int TIME_INTERNAL = 30;
    private final static int HEAD_OFFSET = 512;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView =  findViewById(R.id.surfaceView1);
        mReadButton =  findViewById(R.id.bt1);
        mReadButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View view) {
//                if(h264File.exists())
//                {
//                    Toast.makeText(getApplicationContext(),
//                            "ture", Toast.LENGTH_SHORT).show();
//                }
//                if(!h264File.exists())
//                {
//                    Toast.makeText(getApplicationContext(),
//                            "false", Toast.LENGTH_SHORT).show();
//                }
                if (h264File.exists()) {
                    if (!isInit) {
                        try {
                            initDecoder();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        isInit = true;
                    }
                    readFileThread = new Thread(readFile);
                    readFileThread.start();
                }
                else {
                    Toast.makeText(getApplicationContext(),
                            "H264 file not found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        readFileThread.interrupt();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initDecoder() throws IOException {
        mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                VIDEO_WIDTH, VIDEO_HEIGHT);//MediaFormat为媒体格式
        mCodec.configure(mediaFormat, mSurfaceView.getHolder().getSurface(),
                null, 0);
        mCodec.start();//当创建编解码器的时候处于未初始化状态。
        // 首先你需要调用configure(…)方法让它处于Configured状态，然后调用start()方法让其处于Executing状态。
        // 在Executing状态下，你就可以使用上面提到的缓冲区来处理数据。

    }

    int mCount =0;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public boolean onFrame(byte[] buf, int offset, int length) {
        Log.e("Media", "onFrame start");
        Log.e("Media", "onFrame Thread:" + Thread.currentThread().getId());
        // Get input buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();//获取需要编码数据的输入流队列，返回的是一个ByteBuffer数组
        int inputBufferIndex = mCodec.dequeueInputBuffer(100);//dequeueInputBuffer：从输入流队列中取数据进行编码操作

        Log.e("Media", "onFrame index:" + inputBufferIndex);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();//初始化
            inputBuffer.put(buf, offset, length);//从buf数组中的offset到offset+length区域读取数据并使用相对写写入此byteBuffer
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount
                    * TIME_INTERNAL, 0);
            mCount++;
        } else {
            return false;
        }
        // Get output buffer index
        //getInputBuffers：获取需要编码数据的输入流队列，返回的是一个ByteBuffer数组
        //queueInputBuffer：输入流入队列
        //dequeueInputBuffer：从输入流队列中取数据进行编码操作
        //getOutputBuffers：获取编解码之后的数据输出流队列，返回的是一个ByteBuffer数组
        //dequeueOutputBuffer：从输出队列中取出编码操作之后的数据
        //releaseOutputBuffer：处理完成，释放ByteBuffer数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);
        while (outputBufferIndex >= 0) {
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        Log.e("Media", "onFrame end");
        return true;
    }

    /**
     * Find H264 frame head
     *
     * @param buffer
     * @param len
     * @return the offset of frame head, return 0 if can not find one
     */
    static int findHead(byte[] buffer, int len) {
        int i;
        for (i = HEAD_OFFSET; i < len; i++) {
            if (checkHead(buffer, i))
                break;
        }
        if (i == len)
            return 0;
        if (i == HEAD_OFFSET)
            return 0;
        return i;
    }

    /**
     * Check if is H264 frame head
     *
     * @param buffer
     * @param offset
     * @return whether the src buffer is frame head
     */
    static boolean checkHead(byte[] buffer, int offset) {
        // 00 00 00 01
        if (buffer[offset] == 0 && buffer[offset + 1] == 0
                && buffer[offset + 2] == 0 && buffer[3] == 1)
            return true;
        // 00 00 01
        if (buffer[offset] == 0 && buffer[offset + 1] == 0
                && buffer[offset + 2] == 1)
            return true;
        return false;
    }

    //对runnable类的实例化。只有实例化了才可以调用该类的方法
    Runnable readFile = new Runnable() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void run() {
            int h264Read = 0;
            int frameOffset = 0;
            byte[] buffer = new byte[100000];
            byte[] framebuffer = new byte[200000];
            boolean readFlag = true;
            try {
                fs = new FileInputStream(h264File);
                is = new BufferedInputStream(fs);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            while (!Thread.interrupted() && readFlag) {
                try {
                    int length = is.available();
                    if (length > 0) {
                        // Read file and fill buffer
                        int count = is.read(buffer);
                        Log.i("count", "" + count);
                        h264Read += count;
                        Log.d("Read", "count:" + count + " h264Read:"
                                + h264Read);
                        // Fill frameBuffer
                        if (frameOffset + count < 200000) {
                            System.arraycopy(buffer, 0, framebuffer,
                                    frameOffset, count);
                            frameOffset += count;
                        } else {
                            frameOffset = 0;
                            System.arraycopy(buffer, 0, framebuffer,
                                    frameOffset, count);
                            frameOffset += count;
                        }

                        // Find H264 head
                        int offset = findHead(framebuffer, frameOffset);
                        Log.i("find head", " Head:" + offset);
                        while (offset > 0) {
                            if (checkHead(framebuffer, 0)) {
                                // Fill decoder
                                boolean flag = onFrame(framebuffer, 0, offset);
                                if (flag) {
                                    byte[] temp = framebuffer;
                                    framebuffer = new byte[200000];
                                    System.arraycopy(temp, offset, framebuffer,
                                            0, frameOffset - offset);
                                    frameOffset -= offset;
                                    Log.e("Check", "is Head:" + offset);
                                    // Continue finding head
                                    offset = findHead(framebuffer, frameOffset);
                                }
                            } else {

                                offset = 0;
                            }

                        }
                        Log.d("loop", "end loop");
                    } else {
                        h264Read = 0;
                        frameOffset = 0;
                        readFlag = false;
                        // Start a new thread
                        readFileThread = new Thread(readFile);
                        readFileThread.start();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(TIME_INTERNAL);
                } catch (InterruptedException e) {

                }
            }
        }
    };
}
