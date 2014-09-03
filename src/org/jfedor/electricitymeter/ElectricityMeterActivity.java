package org.jfedor.electricitymeter;

import java.io.IOException;
import java.util.List;

import org.jfedor.electricitymeter.R;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class ElectricityMeterActivity extends Activity {
	
	SurfaceView mSurfaceView;
	TextView mStatusBar;
	TextView mStatusBar2;
	Camera mCamera;
	boolean state = false;
	long threshold = 40;
	long[] intervals;
	int intervalsIdx = 0;
	long prevTime = 0;
    int intervalsPerKwh = 1000;
	
    private Size getSmallestPreviewSize(Camera camera){
        List<Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        Size smallest = previewSizes.get(0);
        for (Size previewSize : previewSizes) {
            if (previewSize.width*previewSize.height < smallest.width*smallest.height){
                smallest = previewSize;
            }
        }
        return smallest;
    }
    
    private int[] getBestFps(Camera camera){
    	List<int[]> ranges = camera.getParameters().getSupportedPreviewFpsRange();
    	int[] best = ranges.get(0);
    	for (int[] range: ranges){
    		if (range[1] > best[1]) best = range;
    	}
    	return best;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mStatusBar = (TextView) findViewById(R.id.TextView01);
        mStatusBar2 = (TextView) findViewById(R.id.TextView02);
        
        intervals = new long[3];
    }

	@Override
	protected void onResume() {
		super.onResume();
		
        mCamera = Camera.open();
        final Size smallest = this.getSmallestPreviewSize(mCamera);
        final int[] previewRange = this.getBestFps(mCamera);
        
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
			
			public void onPreviewFrame(byte[] data, Camera camera) {
				//Log.i("ElectricityMeterActivity", "onPreviewFrame() " + data.length);
				long sum = 0;
				for (int x = 0; x < smallest.width; x++) {
					for (int y = 0; y < smallest.height; y++) {
						if (data[x*smallest.height+y] < 0) { 
							sum++;
						}
					}
				}
				mStatusBar2.setText(Long.toString(sum));
				if (state && (sum < threshold)) {
					state = false;
				} else if (!state && (sum > threshold)) {
					long currTime = System.currentTimeMillis();
					if (prevTime > 0) {
						intervals[intervalsIdx] = currTime - prevTime;
						intervalsIdx = (intervalsIdx + 1) % (intervals.length);
						
						double avg = 0.0;
						double samples = 0.0;
						for (int i = 0; i < intervals.length; i++) {
							if (intervals[i] > 0) {
								avg += intervals[i];
								samples += 1.0;
							}
						}
						if (samples > 0) {
                            int f = 1000*3600/intervalsPerKwh;
							//mStatusBar.setText(String.format("%.0f W", 1000.0*562.5*samples/avg));
							mStatusBar.setText(String.format("%.0f W", 1000.0*f*samples/avg));
						}
					}
					prevTime = currTime;
					state = true;
				}
			}
		});
		Camera.Parameters parameters = mCamera.getParameters();
        Log.i("ElectricityMeterActivity", parameters.flatten());
	    parameters.setPreviewSize(smallest.width, smallest.height);
	    parameters.setPreviewFpsRange(previewRange[0], previewRange[1]);
	    mCamera.setParameters(parameters);
		mCamera.startPreview();        
        mSurfaceView = (SurfaceView) findViewById(R.id.SurfaceView01);
        SurfaceHolder sh1 = mSurfaceView.getHolder();
        sh1.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        sh1.addCallback(new SurfaceHolder.Callback() {

			public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
				// TODO Auto-generated method stub
				Log.i("ElectricityMeterActivity", "surfaceChanged()");
			}

			public void surfaceCreated(SurfaceHolder sh) {
				Log.i("ElectricityMeterActivity", "surfaceCreated()");
				try {
					mCamera.setPreviewDisplay(sh);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			public void surfaceDestroyed(SurfaceHolder arg0) {
				// TODO Auto-generated method stub
				Log.i("ElectricityMeterActivity", "surfaceDestroyed()");
			}
        	
        });
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.release();
		}
	}
}
