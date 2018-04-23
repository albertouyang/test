package com.example.usbeacon;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

public class show extends AppCompatActivity {

    private static final int GOTO_MAIN_ACTIVITY = 0;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);

        //移除action bar
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

//        View view =  View.inflate(this, R.layout.activity_splash_page, null);

        imageView = (ImageView)findViewById(R.id.welcome);
        //由於使用了三种动画效果合在一起，所以要使用AnimationSet动画集
        AnimationSet set = new AnimationSet(false);
        RotateAnimation rtAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        rtAnimation.setDuration(2000);
        rtAnimation.setFillAfter(true);

        ScaleAnimation scAnimation = new ScaleAnimation(0, 1, 0, 1,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        scAnimation.setDuration(2000);
        scAnimation.setFillAfter(true);

        AlphaAnimation alAnimation = new AlphaAnimation(0, 1);
        alAnimation.setDuration(2000);
        alAnimation.setFillAfter(true);

        set.addAnimation(rtAnimation);
        set.addAnimation(scAnimation);
        set.addAnimation(alAnimation);
        //执行动画
        imageView.startAnimation(set);

        set.setAnimationListener(listener);
//        mHandler.sendEmptyMessageDelayed(GOTO_MAIN_ACTIVITY, 2000); //2秒跳轉
    }

    public Animation.AnimationListener listener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            Intent intent = new Intent(show.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    };


//    private Handler mHandler = new Handler() {
//        public void handleMessage(android.os.Message msg) {
//
//            switch (msg.what) {
//                case GOTO_MAIN_ACTIVITY:
//                    Intent intent = new Intent();
//                    //將原本Activity的換成MainActivity
//                    intent.setClass(show.this, welcome.class);
//                    startActivity(intent);
//                    finish();
//                    break;
//
//                default:
//                    break;
//            }
//        }

//    };
}
