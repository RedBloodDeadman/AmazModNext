package com.amazmod.service.springboard;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazmod.service.R;
import com.amazmod.service.helper.SimpleItemTouchHelperCallback;
import com.amazmod.service.springboard.settings.BaseSetting;
import com.amazmod.service.springboard.settings.SpringboardWidgetAdapter;
import com.amazmod.service.util.ButtonListener;
import com.amazmod.service.util.SystemProperties;
import com.amazmod.service.util.WidgetsUtil;

import java.util.ArrayList;

public class WidgetsReorderActivity extends Activity {
    private RecyclerView recyclerView;
    ArrayList<BaseSetting> baseSettings = new ArrayList<>();
    private ButtonListener buttonListener = new ButtonListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //this.onStateNotSaved(); <- this is unsupported if extend Activity instead of AppCompatActivity but with Activity can use different language setting

        baseSettings = WidgetsUtil.loadWidgetList(this);
        SpringboardWidgetAdapter adapter = WidgetsUtil.getAdapter(this);

        //Create recyclerview as layout
        recyclerView = new RecyclerView(this);
        recyclerView.setSaveEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        //Setup drag to move using the helper
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
        //Add padding for the watch
        recyclerView.setPadding((int) getResources().getDimension(R.dimen.padding_round_small), 0, (int) getResources().getDimension(R.dimen.padding_round_small), (int) getResources().getDimension(R.dimen.padding_round_large));
        recyclerView.setClipToPadding(false);
        //Set the view
        setContentView(recyclerView);

        setupBtnListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        buttonListener.stop();
    }

    boolean isMainViewShow = false;
    @Override
    public void onPause() {
        super.onPause();
        isMainViewShow = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        isMainViewShow = true;
    }

    int pos = 0;
    private void setupBtnListener() {
        Activity activity = this;
        buttonListener.start(activity, keyEvent -> {
            if (isMainViewShow && SystemProperties.isStratos3())
                switch (keyEvent.getCode()) {
                    case ButtonListener.S3_KEY_MIDDLE_UP:
                        if (pos > 0) {
                            pos-=1;
                            recyclerView.smoothScrollToPosition(pos);
                        }
                        break;
                    case ButtonListener.S3_KEY_MIDDLE_DOWN:
                        if (pos < baseSettings.size() - 1) {
                            pos+=1;
                            recyclerView.smoothScrollToPosition(pos);
                        }
                        break;
                }
        });
    }
}
