package com.journeyapps.barcodescanner;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.DecodeFormatManager;
import com.google.zxing.client.android.DecodeHintManager;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.journeyapps.barcodescanner.camera.CameraParametersCallback;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates BarcodeView, ViewfinderView and status text.
 *
 * To customize the UI, use BarcodeView and ViewfinderView directly.
 */
public class DecoratedBarcodeView extends FrameLayout {
    private BarcodeView barcodeView;
    private ViewfinderView viewFinder;
    private TargetView targetView;

    /**
     * The instance of @link TorchListener to send events callback.
     */
    private TorchListener torchListener;

    private class WrappedCallback implements BarcodeCallback {
        private BarcodeCallback delegate;

        public WrappedCallback(BarcodeCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void barcodeResult(BarcodeResult result) {
            delegate.barcodeResult(result);
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            for (ResultPoint point : resultPoints) {
                viewFinder.addPossibleResultPoint(point);
            }
            delegate.possibleResultPoints(resultPoints);
        }
    }

    public DecoratedBarcodeView(Context context) {
        super(context);
        initialize();
    }

    public DecoratedBarcodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs);
    }

    public DecoratedBarcodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(attrs);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            setTargetViewSize();
        }else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            setTargetViewSize();
        }
    }

    /**
     * Initialize the view with the xml configuration based on styleable attributes.
     *
     * @param attrs The attributes to use on view.
     */
    private void initialize(AttributeSet attrs) {
        // Get attributes set on view
        TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.zxing_view);

        int scannerLayout = attributes.getResourceId(
                R.styleable.zxing_view_zxing_scanner_layout, R.layout.zxing_barcode_scanner);

        attributes.recycle();

        inflate(getContext(), scannerLayout, this);

        barcodeView = findViewById(R.id.zxing_barcode_surface);

        if (barcodeView == null) {
            throw new IllegalArgumentException(
                "There is no a com.journeyapps.barcodescanner.BarcodeView on provided layout " +
                "with the id \"zxing_barcode_surface\".");
        }

        // Pass on any preview-related attributes
        barcodeView.initializeAttributes(attrs);


        viewFinder = findViewById(R.id.zxing_viewfinder_view);
        targetView = findViewById(R.id.target_view);

        if (viewFinder == null) {
            throw new IllegalArgumentException(
                "There is no a com.journeyapps.barcodescanner.ViewfinderView on provided layout " +
                "with the id \"zxing_viewfinder_view\".");
        }

        setTargetViewSize();

        viewFinder.setCameraPreview(barcodeView);
    }

    public void setTargetViewSize(){
        if (targetView != null){

            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);

            if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                int width = (int) (metrics.widthPixels / 1.5);
                int height = metrics.heightPixels / 3;
                ViewGroup.LayoutParams params = targetView.getLayoutParams();
                params.width = width;
                params.height = height;
            } else if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                int width = (int) (metrics.widthPixels / 1.5);
                int height = (int) (metrics.heightPixels / 1.5);
                ViewGroup.LayoutParams params = targetView.getLayoutParams();
                params.width = width;
                params.height = height;
            }

        }
    }

    /**
     * Initialize with no custom attributes set.
     */
    private void initialize() {
        initialize(null);
    }

    /**
     * Convenience method to initialize camera id, decode formats and prompt message from an intent.
     *
     * @param intent the intent, as generated by IntentIntegrator
     */
    public void initializeFromIntent(Intent intent) {
        // Scan the formats the intent requested, and return the result to the calling activity.
        Set<BarcodeFormat> decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
        Map<DecodeHintType, Object> decodeHints = DecodeHintManager.parseDecodeHints(intent);

        CameraSettings settings = new CameraSettings();

        if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
            int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
            if (cameraId >= 0) {
                settings.setRequestedCameraId(cameraId);
            }
        }

        if (intent.hasExtra(Intents.Scan.TORCH_ENABLED)) {
            if (intent.getBooleanExtra(Intents.Scan.TORCH_ENABLED, false)) {
                this.setTorchOn();
            }
        }

        String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);


        // Check what type of scan. Default: normal scan
        int scanType = intent.getIntExtra(Intents.Scan.SCAN_TYPE, 0);

        String characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);

        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(decodeHints);

        barcodeView.setCameraSettings(settings);
        barcodeView.setDecoderFactory(new DefaultDecoderFactory(decodeFormats, decodeHints, characterSet, scanType));
    }

    public void setCameraSettings(CameraSettings cameraSettings) {
        barcodeView.setCameraSettings(cameraSettings);
    }

    public void setDecoderFactory(DecoderFactory decoderFactory) {
        barcodeView.setDecoderFactory(decoderFactory);
    }

    public DecoderFactory getDecoderFactory() {
        return barcodeView.getDecoderFactory();
    }

    public CameraSettings getCameraSettings() {
        return barcodeView.getCameraSettings();
    }


    /**
     * @see BarcodeView#pause()
     */
    public void pause() {
        barcodeView.pause();
    }

    /**
     * @see BarcodeView#pauseAndWait()
     */
    public void pauseAndWait() {
        barcodeView.pauseAndWait();
    }

    /**
     * @see BarcodeView#resume()
     */
    public void resume() {
        barcodeView.resume();
    }

    public BarcodeView getBarcodeView() {
        return findViewById(R.id.zxing_barcode_surface);
    }

    public ViewfinderView getViewFinder() {
        return viewFinder;
    }

    public TargetView getTargetView(){
        return targetView;
    }

    public Rect getPreviewSize(){
        return viewFinder.framingRect;
    }

    /**
     * @see BarcodeView#decodeSingle(BarcodeCallback)
     */
    public void decodeSingle(BarcodeCallback callback) {
        barcodeView.decodeSingle(new WrappedCallback(callback));
    }

    /**
     * @see BarcodeView#decodeContinuous(BarcodeCallback)
     */
    public void decodeContinuous(BarcodeCallback callback) {
        barcodeView.decodeContinuous(new WrappedCallback(callback));
    }

    /**
     * Turn on the device's flashlight.
     */
    public void setTorchOn() {
        barcodeView.setTorch(true);

        if (torchListener != null) {
            torchListener.onTorchOn();
        }
    }

    /**
     * Turn off the device's flashlight.
     */
    public void setTorchOff() {
        barcodeView.setTorch(false);

        if (torchListener != null) {
            torchListener.onTorchOff();
        }
    }

    /**
     * Changes the settings for Camera.
     * Must be called after {@link #resume()}.
     *
     * @param callback {@link CameraParametersCallback}
     */
    public void changeCameraParameters(CameraParametersCallback callback) {
        barcodeView.changeCameraParameters(callback);
    }

    /**
     * Handles focus, camera, volume up and volume down keys.
     *
     * Note that this view is not usually focused, so the Activity should call this directly.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                setTorchOff();
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                setTorchOn();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void setTorchListener(TorchListener listener) {
        this.torchListener = listener;
    }

    /**
     * The Listener to torch/fflashlight events (turn on, turn off).
     */
    public interface TorchListener {

        void onTorchOn();

        void onTorchOff();
    }
}
