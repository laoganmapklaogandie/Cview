package com.huanguo.cview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class TestCv extends AppCompatActivity {

    private static final String TAG = "TestCv";
    private TextureView mTextureView;
    private Handler mChildHandler;
    private CameraManager mCameraManager;

    //github  本地第一次修改


    String mCurrentCameraId;
    private CameraDevice.StateCallback mCameraDeviceStateCallback;
    private CameraDevice mCameraDevice;
    private Surface mSurface;
    private CaptureRequest.Builder mCaptureRequest;
    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraCaptureSession.CaptureCallback mCameraCaptureSessionCaptureCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_test);
        mTextureView = findViewById(R.id.textureview);


        initChildThread();
        initCameraManager();
        initSelectCamera();
//        initHandlerMatchingSize();
//        initImageReader();
        initTextureViewListener();
        initCameraDeviceStateCallbackListener();
        initCameraCaptureSessionStateCallbackListener();
        initCameraCaptureSessionCaptureCallbackListener();

    }

    private void initChildThread() {
        HandlerThread handlerThread = new HandlerThread("faceCamera");
        handlerThread.start();
        mChildHandler = new Handler(handlerThread.getLooper());
    }

    // 初始化相机管理
    private void initCameraManager() {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    //初始化计算适合当前屏幕分辨率的拍照分辨率
    private Size getMatchingSize() {

        Size selectSize = null;
        float selectProportion = 0;
        try {
            float viewProportion = (float) mTextureView.getWidth() / (float) mTextureView.getHeight();
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            for (int i = 0; i < sizes.length; i++) {
                Size itemSize = sizes[i];
                float itemSizeProportion = (float) itemSize.getHeight() / (float) itemSize.getWidth();
                float differenceProportion = Math.abs(viewProportion - itemSizeProportion);
                Log.e(TAG, "相减差值比例=" + differenceProportion);
                if (i == 0) {
                    selectSize = itemSize;
                    selectProportion = differenceProportion;
                    continue;
                }
                if (differenceProportion <= selectProportion) {
                    if (differenceProportion == selectProportion) {
                        if (selectSize.getWidth() + selectSize.getHeight() < itemSize.getWidth() + itemSize.getHeight()) {
                            selectSize = itemSize;
                            selectProportion = differenceProportion;
                        }

                    } else {
                        selectSize = itemSize;
                        selectProportion = differenceProportion;
                    }
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "getMatchingSize: 选择的比例是=" + selectProportion);
        Log.e(TAG, "getMatchingSize: 选择的尺寸是 宽度=" + selectSize.getWidth() + "高度=" + selectSize.getHeight());
        return selectSize;
    }

    // 初始化选择摄像头
    private void initSelectCamera() {
        try {
            String[] cameraIdArray = mCameraManager.getCameraIdList();
            for (String itemId : cameraIdArray) {
                CameraCharacteristics itemCharacteristics = mCameraManager.getCameraCharacteristics(itemId);
                //Todo ？
                Integer facing = itemCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mCurrentCameraId = itemId;
                    break;
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (mCurrentCameraId == null) {
            finish();
            Toast.makeText(this, "此设备不支持前摄像头", Toast.LENGTH_SHORT).show();
        }
    }


    private void initTextureViewListener() {
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }


    private void initCameraDeviceStateCallbackListener() {
        mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                //相机开启
                mCameraDevice = camera;
                try {
                    SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                    surfaceTexture.setDefaultBufferSize(getMatchingSize().getWidth(), getMatchingSize().getHeight());
                    mSurface = new Surface(surfaceTexture);
                    mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    mCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);//自动爆光
                    mCaptureRequest.addTarget(mSurface);
                    mCameraDevice.createCaptureSession(Arrays.asList(mSurface)
                            , mCameraCaptureSessionStateCallback
                            , mChildHandler);

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                finish();
                Toast.makeText(TestCv.this, "相机打开失败", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "CameraDevice.StateCallback onError : 相机异常 error code=" + error);

            }
        };
    }

    private void initCameraCaptureSessionStateCallbackListener() {
        mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                mCameraCaptureSession = session;
                startPreview();

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                finish();
                Toast.makeText(TestCv.this, "相机打开失败", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "CameraCaptureSession.StateCallback onConfigureFailed : CameraCaptureSession会话通道创建失败");

            }
        };
    }


    private void initCameraCaptureSessionCaptureCallbackListener() {
        mCameraCaptureSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
                //获取开始
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                super.onCaptureProgressed(session, request, partialResult);
                //获取中
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                //获取结束
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                //获取失败

                Toast.makeText(TestCv.this, "拍照失败", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "失败报告Reason=" + failure.getReason());

            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openCamera() {
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
            mCameraManager.openCamera(mCurrentCameraId, mCameraDeviceStateCallback, mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 开始预览
     */
    private void startPreview(){
        try {
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest.build(), mCameraCaptureSessionCaptureCallback, mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止预览
     */
    private void stopPreview(){
        try {
            mCameraCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mImageReader != null){
//            mImageReader.close();
//            mImageReader = null;
//        }
        if (mCameraCaptureSession != null){
            stopPreview();
            try {
                mCameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCaptureRequest != null){
            mCaptureRequest.removeTarget(mSurface);//注意释放mSurface
            mCaptureRequest = null;
        }
        if (mSurface != null){
            mSurface.release();//注意释放mSurface
            mSurface = null;
        }
//也可以用onSurfaceTextureDestroyed这种方式释放SurfaceTexture 但是在上面的public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) 回调中你需要返回true或者自己执行 surface.release(); 这步资源释放很重要
        mTextureView.getSurfaceTextureListener().onSurfaceTextureDestroyed(mTextureView.getSurfaceTexture());
        mCameraDeviceStateCallback = null;
        mCameraCaptureSessionStateCallback = null;
        mCameraCaptureSessionCaptureCallback = null;
        mCameraManager = null;
        if (mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
        mCameraManager = null;
        if (mChildHandler != null){
            mChildHandler.removeCallbacksAndMessages(null);
            mChildHandler = null;

        }
    }



}
