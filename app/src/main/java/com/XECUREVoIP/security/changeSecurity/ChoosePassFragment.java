package com.XECUREVoIP.security.changeSecurity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.XECUREVoIP.R;
import com.XECUREVoIP.security.FragmentViewPagerAdapter;

import static android.content.Context.MODE_PRIVATE;

public class ChoosePassFragment extends Fragment {
    private TextView descPattern, descPass;
    private RelativeLayout imgPattern, imgPass;
    private boolean bIsPattern;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_choose_pass, container, false);
        descPattern = view.findViewById(R.id.desc_pattern);
        descPass= view.findViewById(R.id.desc_pass);

        bIsPattern = isPatten();
        if (bIsPattern){
            descPattern.setText(R.string.change);
            descPass.setText(R.string.set);
            view.findViewById(R.id.next_arrow00).setVisibility(View.GONE);
            view.findViewById(R.id.before_arrow00).setVisibility(View.VISIBLE);
            view.findViewById(R.id.next_arrow01).setVisibility(View.VISIBLE);
            view.findViewById(R.id.before_arrow01).setVisibility(View.GONE);
        }else {
            descPattern.setText(R.string.set);
            descPass.setText(R.string.change);
            view.findViewById(R.id.next_arrow00).setVisibility(View.VISIBLE);
            view.findViewById(R.id.before_arrow00).setVisibility(View.GONE);
            view.findViewById(R.id.next_arrow01).setVisibility(View.GONE);
            view.findViewById(R.id.before_arrow01).setVisibility(View.VISIBLE);
        }

        imgPattern = (RelativeLayout) view.findViewById(R.id.manage_pattern);
        imgPass = (RelativeLayout) view.findViewById(R.id.manage_pass);

        imgPattern.setOnClickListener(listener);
        imgPass.setOnClickListener(listener);

        return view;
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id){
                case R.id.manage_pass:
                    if (bIsPattern)
                        ChangeSecurityActivity.viewPager.setCurrentItem(2);
                    else
                        ChangeSecurityActivity.viewPager.setCurrentItem(0);
                    break;
                case R.id.manage_pattern:
                    if (bIsPattern)
                        ChangeSecurityActivity.viewPager.setCurrentItem(0);
                    else
                        ChangeSecurityActivity.viewPager.setCurrentItem(2);
            }
        }
    };

    private boolean isPatten() {
        SharedPreferences preXecue = getActivity().getSharedPreferences("Xecure", MODE_PRIVATE);
        String strCrrEncryptionPass = preXecue.getString("pass", "");

        return strCrrEncryptionPass.isEmpty()? true: false;
    }
}
