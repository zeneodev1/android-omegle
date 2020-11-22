package com.zeneo.omechle;

import android.content.Context;
import android.widget.VideoView;

public class DynamicVideoView extends VideoView {

    int mVideoWidth, mVideoHeight;

    public DynamicVideoView(Context context, int mVideoWidth, int mVideoHeight) {
        super(context);
        this.mVideoWidth = mVideoWidth;
        this.mVideoHeight = mVideoHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);

        /**Adjust according to your desired ratio*/
        if (mVideoWidth > 0 && mVideoHeight > 0) {
            if (mVideoWidth * height > width * mVideoHeight) {
                // Log.i("@@@", "image too tall, correcting");
                height = (width * mVideoHeight / mVideoWidth);
            } else if (mVideoWidth * height < width * mVideoHeight) {
                // Log.i("@@@", "image too wide, correcting");
                width = (height * mVideoWidth / mVideoHeight);
            } else {
                // Log.i("@@@", "aspect ratio is correct: " +
                // width+"/"+height+"="+
                // mVideoWidth+"/"+mVideoHeight);
            }
        }

        setMeasuredDimension(width, height);
    }
}
