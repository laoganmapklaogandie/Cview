package com.huanguo.cview;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
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

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    TextureView mTextureView;
    private CameraManager mCameraManager;
    private CameraDevice.StateCallback mStateCallback;                        //摄像头状态回调
    private CameraDevice mCameraDevice;


    private HandlerThread mHandlerThread;
    private Handler mChildHandler;


    private String mCurrentCameraId = "2";
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private CameraCaptureSession.StateCallback mSessionstateCallback;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = findViewById(R.id.textureview);

        initCameraManager();


    }

    // 初始化相机管理
    private void initCameraManager() {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }



    private void initTextureView() {
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d("SurfaceTextureListener", "yj onSurfaceTextureAvailable() TextureView 启用成功 ");
                initCameraManager();
                initCameraCallback();
                initCameraCaptureSessionStateCallback();
                selectCamera();
                openCamera();

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {

        try {
            mCameraManager.openCamera(mCurrentCameraId, mStateCallback, mChildHandler);
        } catch (CameraAccessException e) {
            Log.d(TAG,"openCamera()  异常 ");
            e.printStackTrace();
        }

    }

    // 摄像头获取会话状态回调
    private void initCameraCaptureSessionStateCallback() {
        mSessionstateCallback = new CameraCaptureSession.StateCallback() {

            //摄像头完成配置，可以处理Capture请求了。
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                mCameraCaptureSession = session;
                //注意这里使用的是 setRepeatingRequest() 请求通过此捕获会话无休止地重复捕获图像。用它来一直请求预览图像
                //TODO ?
                //mCameraCaptureSession.setRepeatingRequest(mCaptureRequest.build(), mSessionCaptureCallback, mChildHandler);

                //    mCameraCaptureSession.stopRepeating();//停止重复   取消任何正在进行的重复捕获集
//                    mCameraCaptureSession.abortCaptures();//终止获取   尽可能快地放弃当前挂起和正在进行的所有捕获。请只在销毁activity的时候调用它

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        };
    }

    //摄像头获取会话数据回调
    private void initCameraCaptureSessionCaptureCallback() {
        mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                super.onCaptureProgressed(session, request, partialResult);
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
//                Log.e(TAG, "onCaptureCompleted: 触发接收数据");
//                Size size = request.get(CaptureRequest.JPEG_THUMBNAIL_SIZE);

            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
            }

            @Override
            public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            }

            @Override
            public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
                super.onCaptureSequenceAborted(session, sequenceId);
            }

            @Override
            public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
                super.onCaptureBufferLost(session, request, target, frameNumber);
            }
        };
    }


    // 获取匹配大小
    public Size getMatchSize() {
        Size selectSize = null;
        float selectproportion = 0;

        try {
            float viewProportion = (float)mTextureView.getWidth() / (float) mTextureView.getHeight();
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
            //获取该设备是否具有闪光灯
            Boolean aBoolean = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            for (int i = 0; i < sizes.length; i++) {
                Size itemSize = sizes[i];
                float itemSizeProportion = (float) itemSize.getHeight()/(float) itemSize.getWidth();
                float differenceProportion = Math.abs(viewProportion - itemSizeProportion);
                Log.e(TAG,"相减差值比例= "+differenceProportion);
                if(i ==0){
                    selectSize = itemSize;
                    selectproportion = differenceProportion;
                    continue;
                }
                if(differenceProportion <= selectproportion){
                    if(differenceProportion == selectproportion){
                        if(selectSize.getWidth() + selectSize.getHeight() < itemSize.getWidth() + itemSize.getHeight()){
                            selectSize = itemSize;
                            selectproportion = differenceProportion;
                        }
                    }else {
                        selectSize = itemSize;
                        selectproportion = differenceProportion;
                    }
                }
            }


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "getMatchingSize: 选择的比例是=" + selectproportion);
        Log.e(TAG, "getMatchingSize: 选择的尺寸是 宽度=" + selectSize.getWidth() + "高度=" + selectSize.getHeight());

        return selectSize;



    }

    private Size getMatchingSize2() {
        Size selectSize = null;
        try {
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
            StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics(); //因为我这里是将预览铺满屏幕,所以直接获取屏幕分辨率
            int deviceWidth = displayMetrics.widthPixels; //屏幕分辨率宽
            int deviceHeigh = displayMetrics.heightPixels; //屏幕分辨率高
            Log.e(TAG, "getMatchingSize2: 屏幕密度宽度=" + deviceWidth);
            Log.e(TAG, "getMatchingSize2: 屏幕密度高度=" + deviceHeigh);
            /**
             * 循环40次,让宽度范围从最小逐步增加,找到最符合屏幕宽度的分辨率,
             * 你要是不放心那就增加循环,肯定会找到一个分辨率,不会出现此方法返回一个null的Size的情况
             * ,但是循环越大后获取的分辨率就越不匹配
             */
            for (int j = 1; j < 41; j++) {
                for (int i = 0; i < sizes.length; i++) { //遍历所有Size
                    Size itemSize = sizes[i];
                    Log.e(TAG, "当前itemSize 宽=" + itemSize.getWidth() + "高=" + itemSize.getHeight());
                    //判断当前Size高度小于屏幕宽度+j*5  &&  判断当前Size高度大于屏幕宽度-j*5
                    if (itemSize.getHeight() < (deviceWidth + j * 5) && itemSize.getHeight() > (deviceWidth - j * 5)) {
                        if (selectSize != null) { //如果之前已经找到一个匹配的宽度
                            if (Math.abs(deviceHeigh - itemSize.getWidth()) < Math.abs(deviceHeigh - selectSize.getWidth())) { //求绝对值算出最接近设备高度的尺寸
                                selectSize = itemSize;
                                continue;
                            }
                        } else {
                            selectSize = itemSize;
                        }

                    }
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
//        Log.e(TAG, "getMatchingSize2: 选择的分辨率宽度="+selectSize.getWidth());
//        Log.e(TAG, "getMatchingSize2: 选择的分辨率高度="+selectSize.getHeight());
        return selectSize;
    }

    // 选择摄像头
    private void selectCamera() {

        try {

            String[] cameraIdList = mCameraManager.getCameraIdList();
            Log.d(TAG, "yj  获取到的cameralist：" + Arrays.asList(cameraIdList));
            if (cameraIdList.length == 0) {
                Log.d(TAG, "yj cameraIdList.length=0");
                return;
            }

            for (String cameraId : cameraIdList) {
                Log.e(TAG, "selectCamera: cameraId=" + cameraId);
                //获取相机特征,包含前后摄像头信息，分辨率等
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                Log.d(TAG, "yj facing: " + facing);
                //TODO   yanjiu
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mCurrentCameraId = cameraId;
                }

            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 初始化摄像头状态回调
    private void initCameraCallback() {

        mStateCallback = new CameraDevice.StateCallback() {
            //摄像头打开时
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d(TAG, "yj onOpened() 相机开启了");
                mCameraDevice = camera;

                try {
                    mSurfaceTexture = mTextureView.getSurfaceTexture();   //surfaceTexture    需要手动释放
                    Size matchingSize = getMatchSize();
//                // 设置预览的图像尺寸
                    mSurfaceTexture.setDefaultBufferSize(matchingSize.getWidth(),matchingSize.getHeight());
                    //surface最好在销毁的时候要释放,surface.release();
                    mSurface = new Surface(mSurfaceTexture);
                    //创建预览请求
                    CaptureRequest.Builder mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    //添加surface   实际使用中这个surface最好是全局变量 在onDestroy的时候mCaptureRequest.removeTarget(mSurface);清除,否则会内存泄露
                    mCaptureRequest.addTarget(mSurface);
                    mCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    //TODO ?
                    //mCameraDevice.createCaptureSession(Arrays.asList(mSurface), mSessionCaptureCallback, mChildHandler);

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }


            }

            //摄像头断开时
            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.d(TAG, "yj onDisconnected() 相机断开了");
            }

            //出现异常情况时
            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.d(TAG, "yj onError() 相机onError了 e: " + error);
            }

            //摄像头关闭时
            @Override
            public void onClosed(@NonNull CameraDevice camera) {
                super.onClosed(camera);
                Log.d(TAG, "yj 关闭了 相机onClosed()了");
            }
        };
    }
}
