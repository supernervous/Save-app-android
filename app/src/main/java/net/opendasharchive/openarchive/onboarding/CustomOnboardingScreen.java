package net.opendasharchive.openarchive.onboarding;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.opendasharchive.openarchive.R;

public class CustomOnboardingScreen extends Fragment {

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private static final String ARG_LAYOUT_TITLE = "layoutTitle";
    private static final String ARG_LAYOUT_SUBTITLE = "layoutSubtitle";
    private static final String ARG_LAYOUT_IMAGE = "layoutImage";

    private int layoutResId;
    private String mTitle;
    private String mSubTitle;
    private int mImageResource;

    private Button mButton;

    public static CustomOnboardingScreen newInstance(int layoutResId, String title, String subtitle, int image) {
        CustomOnboardingScreen sampleSlide = new CustomOnboardingScreen();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        args.putString(ARG_LAYOUT_TITLE,title);
        args.putString(ARG_LAYOUT_SUBTITLE,subtitle);
        args.putInt(ARG_LAYOUT_IMAGE,image);
        sampleSlide.setArguments(args);

        return sampleSlide;
    }

    public void setTitle (String title)
    {
        mTitle = title;
    }

    public void setSubTitle(String subTitle) { mSubTitle = subTitle; }

    public void setImageResource (int resource) { mImageResource = resource; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_TITLE)) {
            mTitle = getArguments().getString(ARG_LAYOUT_TITLE);
        }

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_SUBTITLE)) {
            mSubTitle = getArguments().getString(ARG_LAYOUT_SUBTITLE);
        }

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_IMAGE)) {
            mImageResource = getArguments().getInt(ARG_LAYOUT_IMAGE);
        }


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layoutResId, container, false);
        ((TextView)view.findViewById(R.id.custom_slide_big_text)).setText(mTitle);

        if (!TextUtils.isEmpty(mSubTitle)) {

            TextView tv =
                    (TextView)view.findViewById(R.id.custom_slide_big_text_sub);
            tv.setText(mSubTitle);
            tv.setVisibility(View.VISIBLE);
        }

        ((ImageView)view.findViewById(R.id.custom_slide_image)).setImageResource(mImageResource);

        mButton = view.findViewById(R.id.custom_slide_button);
        if (mButton != null && mButtonListener != null) {
            mButton.setText(mButtonText);
            mButton.setVisibility(View.VISIBLE);
            mButton.setOnClickListener(mButtonListener);
        }
        return view;

    }

    private String mButtonText;
    private View.OnClickListener mButtonListener;

    public void enableButton (String buttonText, View.OnClickListener listener)
    {
        mButtonText = buttonText;
        mButtonListener = listener;
    }
}
