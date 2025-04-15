package com.amazmod.service.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.amazmod.service.R;
import com.amazmod.service.util.ButtonListener;
import com.amazmod.service.util.ExecCommand;
import com.amazmod.service.util.SystemProperties;

import org.tinylog.Logger;

import java.util.List;

public class InputMethodActivity extends Activity {

    LinearLayout linearLayout;
    TextView title;
    RadioGroup radioGroup;
    EditText  editText;
    Button button;
    ScrollView radioScrollView;

    private Context mContext;
    private static String selectedIME;
    private ButtonListener buttonListener = new ButtonListener();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("InputMethodActivity onCreate");

        this.mContext = this;
        setContentView(R.layout.activity_inputmethod);

        radioScrollView = findViewById(R.id.radio_scrollview);
        linearLayout = findViewById(R.id.radio_linearlayout);
        title = findViewById(R.id.radio_textview);
        radioGroup = findViewById(R.id.radio_group);
        editText = findViewById(R.id.radio_edittext);
        button = findViewById(R.id.radio_button);

        title.setText(getResources().getString(R.string.input_methods));
        setButtonTheme(button, getResources().getString(R.string.close));

        updateContent();
        setupBtnListener();
    }

    private void updateContent(){

        InputMethodManager inputMethodManager = (InputMethodManager) mContext.getSystemService(INPUT_METHOD_SERVICE);
        selectedIME = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (inputMethodManager != null) {
            final List<InputMethodInfo> inputMethodInfos = inputMethodManager.getInputMethodList();

            for (InputMethodInfo inputMethodInfo : inputMethodInfos) {
                Logger.info("InputMethodActivity updateContent inputMethodInfo getID: " + inputMethodInfo.getId()
                        + " getPackageName: " + inputMethodInfo.getPackageName()
                        + " getServiceName: " + inputMethodInfo.getServiceName()
                        + " getSettingsActivity: " + inputMethodInfo.getSettingsActivity());

                    RadioButton radioButton = new RadioButton(this);
                    radioButton.setText(inputMethodInfo.loadLabel(mContext.getPackageManager()).toString());
                    radioGroup.addView(radioButton);

                    if (inputMethodInfo.getId().equals(selectedIME))
                        radioButton.setChecked(true);
            }

            List<InputMethodInfo> mInputMethodProperties = inputMethodManager.getEnabledInputMethodList();
            for (InputMethodInfo inputMethodInfo : mInputMethodProperties) {
                Logger.info("InputMethodActivity updateContent EnabledInputMethodInfo getID: " + inputMethodInfo.getId()
                        + " getPackageName: " + inputMethodInfo.getPackageName()
                        + " getServiceName: " + inputMethodInfo.getServiceName()
                        + " getSettingsActivity: " + inputMethodInfo.getSettingsActivity());
            }
            Logger.info("InputMethodActivity updateContent DEFAULT_INPUT_METHOD: "
                    + Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD));

            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    int childCount = group.getChildCount();
                    for (int x = 0; x < childCount; x++) {
                        RadioButton btn = (RadioButton) group.getChildAt(x);
                        if (btn.getId() == checkedId) {
                            selectedIME = inputMethodInfos.get(x).getId();
                            Logger.info("InputMethodActivity selected RadioButton: " + btn.getText().toString() + " x: " + x);
                            new ExecCommand(ExecCommand.ADB, "adb shell ime enable " + selectedIME + ";ime set " + selectedIME);
                        }
                    }
                }
            });

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    private void setButtonTheme(Button button, String string) {
        button.setIncludeFontPadding(false);
        button.setMinHeight(24);
        button.setMinWidth(120);
        button.setText(string);
        button.setAllCaps(true);
        button.setTextColor(Color.parseColor("#000000"));
        button.setBackground(mContext.getDrawable(R.drawable.reply_grey));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        buttonListener.stop();
    }

    boolean isMainViewShow = false;

    @Override
    protected void onResume() {
        super.onResume();
        isMainViewShow = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        isMainViewShow = false;
    }

    private void setupBtnListener() {
        Activity activity = this;
        buttonListener.start(activity, keyEvent -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isMainViewShow && SystemProperties.isStratos3())
                        switch (keyEvent.getCode()) {
                            case ButtonListener.S3_KEY_MIDDLE_UP:
                                radioScrollView.smoothScrollTo(radioScrollView.getScrollX(), radioScrollView.getScrollY() - 100);
                                break;
                            case ButtonListener.S3_KEY_MIDDLE_DOWN:
                                radioScrollView.smoothScrollTo(radioScrollView.getScrollX(), radioScrollView.getScrollY() + 100);
                                break;
                        }
                }
            });
        });
    }
}
