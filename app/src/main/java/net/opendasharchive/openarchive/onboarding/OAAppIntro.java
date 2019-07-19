package net.opendasharchive.openarchive.onboarding;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;

import net.opendasharchive.openarchive.R;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Created by n8fr8 on 8/3/16.
 */
public class OAAppIntro extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFadeAnimation();

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance(getString(R.string.title_welcome), getString(R.string.onboarding_intro), R.drawable.oafeature, getResources().getColor(R.color.oablue)));
        addSlide(AppIntroFragment.newInstance("Save to a safe place.", "Connect to a secure server or the internet archive to upload photos and videos from your phone.", R.drawable.onboarding2, getResources().getColor(R.color.oablue)));
        addSlide(AppIntroFragment.newInstance("Stay organized.", "Organize your media into projects.", R.drawable.onboarding4, getResources().getColor(R.color.oablue)));
        addSlide(AppIntroFragment.newInstance("Store the facts.", "Capture notes, location and people with each piece of media.", R.drawable.onboarding3, getResources().getColor(R.color.oablue)));
        addSlide(AppIntroFragment.newInstance("Ensure authenticity.", "Include your credentials while Save adds extra metadata to help with chain of custody and verification workflows.", R.drawable.onboarding1, getResources().getColor(R.color.oablue)));


        // OPTIONAL METHODS
        // Override bar/separator color.
       // setBarColor(Color.parseColor("#3F51B5"));
       // setSeparatorColor(Color.parseColor("#2196F3"));

        // Hide Skip/Done button.
        showSkipButton(false);
       // setProgressButtonEnabled(false);

    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.


        finish();
        startActivity(new Intent(this,FirstStartActivity.class));
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}
