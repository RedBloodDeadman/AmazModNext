package com.edotassi.amazmod.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.databinding.ActivityTweakingBinding;
import com.edotassi.amazmod.event.OtherData;
import com.edotassi.amazmod.event.RequestFileUpload;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.support.DownloadHelper;
import com.edotassi.amazmod.support.ShellCommandHelper;
import com.edotassi.amazmod.support.ThemeHelper;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.util.FilesUtil;
import com.edotassi.amazmod.util.Screen;
import com.edotassi.amazmod.watch.Watch;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.huami.watch.transport.DataBundle;
import com.tingyik90.snackprogressbar.SnackProgressBar;
import com.tingyik90.snackprogressbar.SnackProgressBarManager;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tinylog.Logger;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.CancellationException;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.FileUploadData;
import amazmod.com.transport.data.ResultShellCommandData;
import de.mateware.snacky.Snacky;

import static android.graphics.Bitmap.CompressFormat.PNG;

public class TweakingActivity extends BaseAppCompatActivity {
    public final static int REQ_CODE_COMMAND_HISTORY = 100;
    String SSID = "huami-amazfit-amazmod-4E68";
    String pswd = "12345678";
    String defaultFTPip = "192.168.43.1";
    String defaultPort = "5210";
    String TAG = "Tweak-menu-FTP: ";
    private SnackProgressBarManager snackProgressBarManager;
    private Context mContext;
    private ActivityTweakingBinding binding;

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Screen.isDarkTheme() || MainActivity.systemThemeIsDark) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppTheme);
        }


        mContext = this;
        binding = ActivityTweakingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.tweaking);
        } catch (NullPointerException ex) {
            Logger.error(ex, "TweakingActivity onCreate exception: {}", ex.getMessage());
        }

        snackProgressBarManager = new SnackProgressBarManager(findViewById(android.R.id.content))
                .setProgressBarColor(ThemeHelper.getThemeColorAccentId(this))
                .setActionTextColor(ThemeHelper.getThemeColorAccentId(this))
                .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
                .setTextSize(14)
                .setMessageMaxLines(2)
                .setOnDisplayListener(new SnackProgressBarManager.OnDisplayListener() {
                    @Override
                    public void onShown(@NonNull SnackProgressBar snackProgressBar, int onDisplayId) {
                        // do something
                    }

                    @Override
                    public void onDismissed(@NonNull SnackProgressBar snackProgressBar, int onDisplayId) {
                        // do something
                    }
                });

        binding.activityTweakingSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                binding.activityTweakingBrightnessValue.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //brightnessEditText.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //updateBrightness(seekBar.getProgress());
            }
        });
        boolean autoBrightness = (AmazModApplication.currentScreenBrightnessMode == Constants.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        if (Screen.isStratos3()) {
            binding.activityTweakingSwitchtAutoBrightness.setChecked(false);
            binding.activityTweakingSwitchtAutoBrightness.setEnabled(false);
            binding.activityTweakingSeekbar.setEnabled(true);
            binding.activityTweakingBrightnessValue.setEnabled(true);
            binding.activityTweakingButtonUpdateBrightness.setEnabled(true);
        } else {
            binding.activityTweakingSwitchtAutoBrightness.setChecked(autoBrightness);
            binding.activityTweakingSeekbar.setEnabled(!autoBrightness);
            binding.activityTweakingBrightnessValue.setEnabled(!autoBrightness);
            binding.activityTweakingButtonUpdateBrightness.setEnabled(!autoBrightness);
        }
        binding.activityTweakingSeekbar.setProgress(AmazModApplication.currentScreenBrightness);

        EventBus.getDefault().register(this);

        binding.activityTweakingWatchLocalIp.setOnClickListener(v -> {
            getWatchLocalIP();
        });

        binding.activityTweakingSwitchtAutoBrightness.setOnClickListener(v -> {
            changeAutoBrightness();
        });

        binding.activityTweakingReboot.setOnClickListener(v -> {
            execCommandInternally(ShellCommandHelper.getReboot(), false);
        });

        binding.activityTweakingRestartLauncher.setOnClickListener(v -> {
            execCommandInternally(ShellCommandHelper.getForceStopHuamiLauncher(), false);
        });

        binding.activityTweakingEnableAppsList.setOnClickListener(v -> {
            execCommandInternally(ShellCommandHelper.getEnableAppsList());
        });

        binding.activityTweakingDisableAppsList.setOnClickListener(v -> {
            execCommandInternally(ShellCommandHelper.getDisableAppsList());
        });

        binding.activityTweakingRebootBootloader.setOnClickListener(v -> {
            execCommandInternally(ShellCommandHelper.getRebootBootloader(), false);
        });

        binding.activityTweakingSetAdmin.setOnClickListener(v -> {
            execCommandInternally(ShellCommandHelper.getDPM());
        });

        binding.activityTweakingScreenshot.setOnClickListener(v -> {
            execCommandInternally(ShellCommandHelper.getScreenshot());
        });

        binding.activityTweakingButtonUpdateBrightness.setOnClickListener(v -> {
            updateBrightness();
        });

        binding.activityTweakingEnableLpm.setOnClickListener(v -> {
            enableLpm();
        });

        binding.activityTweakingRevokeAdmin.setOnClickListener(v -> {
            revokeAdminOwner();
        });

        binding.activityTweakingClearAdb.setOnClickListener(v -> {
            clearAdb();
        });

        binding.activityTweakingExecCommandRun.setOnClickListener(v -> {
            execCommand();
        });

        binding.activityTweakingCommandHistory.setOnClickListener(v -> {
            loadCommandHistory();
        });

        binding.activityTweakingWifiApOn.setOnClickListener(v -> {
            enableWifiAp();
        });

        binding.activityTweakingWifiApOff.setOnClickListener(v -> {
            disableWifiAp();
        });

        binding.activityTweakingFtpOn.setOnClickListener(v -> {
            enableFtpServer();
        });

        binding.activityTweakingFtpOff.setOnClickListener(v -> {
            disableFtpServer();
        });
    }

    private void disableFtpServer() {
        snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.ftp_dissabling)), SnackProgressBarManager.LENGTH_LONG);
        Watch.get().sendSimpleData(Transport.WIFI_FTP_DISABLE, Transport.FTP_ON_STATE_CHANGED, TransportService.TRANSPORT_FTP).continueWith(new Continuation<OtherData, Object>() {
            @Override
            public Object then(@NonNull Task<OtherData> task) throws Exception {
                if (task.isSuccessful()) {
                    Logger.debug(TAG + "FTP server disabled");
                    snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, "FTP server " + getString(R.string.disabled)), SnackProgressBarManager.LENGTH_SHORT);
                } else {
                    snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.error)), SnackProgressBarManager.LENGTH_LONG);
                    Logger.debug(TAG + "WIFI_FTP_DISABLE error");
                }
                return null;
            }
        });
    }

    private void enableFtpServer() {
        snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.ftp_enabling)), SnackProgressBarManager.LENGTH_LONG);
        Watch.get().sendSimpleData(Transport.WIFI_FTP_ENABLE, Transport.FTP_ON_STATE_CHANGED, TransportService.TRANSPORT_FTP).continueWith(new Continuation<OtherData, Object>() {
            @Override
            public Object then(@NonNull Task<OtherData> task) throws Exception {
                if (task.isSuccessful()) {
                    // FTP enabled
                    Logger.debug(TAG + "FTP server enabled.");
                    getWatchLocalIP(true);
                } else {
                    snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.error)), SnackProgressBarManager.LENGTH_LONG);

                    Logger.debug(TAG + "WIFI_FTP_ENABLE error");
                }
                return null;
            }
        });
    }

    private void disableWifiAp() {
        snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.wifi_ap_dissabling)), SnackProgressBarManager.LENGTH_LONG);
        TransportService.sendWithTransporterFtp(Transport.WIFI_DISABLE_AP, null);
        Logger.debug(TAG + "watch's WiFi AP disabled");
        snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, "WiFi Access Point " + getString(R.string.disabled)), SnackProgressBarManager.LENGTH_SHORT);
    }

    private void enableWifiAp() {
        snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.wifi_ap_enabling)), SnackProgressBarManager.LENGTH_LONG);

        TransportService.sendWithTransporterFtp(Transport.WIFI_START_SERVICE, null);
        DataBundle dataBundle = new DataBundle();
        dataBundle.putInt(Transport.WIFI_SECURITY_MODE, Transport.WIFI_WPA2);
        dataBundle.putString(Transport.WIFI_SSID, SSID);
        dataBundle.putString(Transport.WIFI_PASSWORD, pswd);
        Watch.get().sendSimpleData(Transport.WIFI_ENABLE_AP, Transport.ON_AP_ENABLE_RESULT, TransportService.TRANSPORT_FTP).continueWith(new Continuation<OtherData, Object>() {
            @Override
            public Object then(@NonNull Task<OtherData> task) throws Exception {
                if (task.isSuccessful()) {
                    // (State 13 watch WiFi AP is on)
                    Logger.debug(TAG + "watch's WiFi AP is enabled");
                    // WiFi AP enabled.
                    runOnUiThread(() -> snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, "WiFi    : " + SSID + "\nPassword: " + pswd)
                            .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                @Override
                                public void onActionClick() {
                                    snackProgressBarManager.dismissAll();
                                }
                            }), SnackProgressBarManager.LENGTH_INDEFINITE));
                } else {
                    snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.error)), SnackProgressBarManager.LENGTH_LONG);
                    Logger.debug(TAG + "WIFI_ENABLE_AP error");
                }
                return null;
            }
        });
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            System.out.println("D/AmazMod TweakingActivity ORIENTATION PORTRAIT");
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            System.out.println("D/AmazMod TweakingActivity ORIENTATION LANDSCAPE");
        }
    }

    private void changeAutoBrightness() {
        boolean autoBrightness = binding.activityTweakingSwitchtAutoBrightness.isChecked();
        binding.activityTweakingSeekbar.setEnabled(!autoBrightness);
        binding.activityTweakingBrightnessValue.setEnabled(!autoBrightness);
        binding.activityTweakingButtonUpdateBrightness.setEnabled(!autoBrightness);
        if (autoBrightness) {
            updateBrightness(Constants.SCREEN_BRIGHTNESS_VALUE_AUTO);
        }
    }

    private void updateBrightness() {
        try {
            String textValue = binding.activityTweakingBrightnessValue.getText().toString();
            int value = Integer.valueOf(textValue);

            if ((value < 1) || (value > 255)) {
                Snacky.builder()
                        .setActivity(this)
                        .setText(R.string.brightness_bad_value_entered)
                        .build()
                        .show();
            } else {
                updateBrightness(value);
            }
        } catch (Exception ex) {
            Snacky.builder()
                    .setActivity(this)
                    .setText(R.string.brightness_bad_value_entered)
                    .build()
                    .show();
        }
    }

    public void enableLpm() {
        new MaterialDialog.Builder(this)
                .title(R.string.enable_low_power)
                .content(R.string.enable_lpm_content)
                .positiveText(R.string.continue_label)
                .negativeText(R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();

                        final SnackProgressBar progressBar = new SnackProgressBar(
                                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending))
                                .setIsIndeterminate(true)
                                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                                    @Override
                                    public void onActionClick() {
                                        snackProgressBarManager.dismissAll();
                                    }
                                });
                        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

                        Watch.get()
                                .enableLowPower()
                                .continueWith(new Continuation<Void, Object>() {
                                    @Override
                                    public Object then(@NonNull Task<Void> task) {
                                        SnackProgressBar snackbar;
                                        if (task.isSuccessful()) {
                                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.shell_command_sent));

                                        } else {
                                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.cant_send_shell_command));
                                        }

                                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                                        return null;
                                    }
                                });
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public void revokeAdminOwner() {
        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending))
                .setIsIndeterminate(true)
                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                    @Override
                    public void onActionClick() {
                        snackProgressBarManager.dismissAll();
                    }
                });
        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        Watch.get()
                .revokeAdminOwner()
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(@NonNull Task<Void> task) {
                        SnackProgressBar snackbar;
                        if (task.isSuccessful()) {
                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.shell_command_sent));

                        } else {
                            snackbar = new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.cant_send_shell_command));
                        }

                        snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                        return null;
                    }
                });
    }

    private void clearAdb() {
        execCommandInternally(ShellCommandHelper.getClearAdb());
        snackProgressBarManager.show(new SnackProgressBar(SnackProgressBar.TYPE_CIRCULAR, getString(R.string.adb_clear_command_sent)), SnackProgressBarManager.LENGTH_LONG);
    }

    private void execCommand() {
        try {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ex) {
            Logger.error(ex);
        }

        String command = binding.activityTweakingExecCommand.getText().toString();
        execCommandInternally(command);
        FilesExtrasActivity.saveCommandToHistory(command);

    }

    public void loadCommandHistory() {
        Intent child = new Intent(this, CommandHistoryActivity.class);
        startActivityForResult(child, REQ_CODE_COMMAND_HISTORY);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE_COMMAND_HISTORY) {
            try {
                String command = data.getExtras().getString("COMMAND");
                binding.activityTweakingExecCommand.setText(command);
            } catch (NullPointerException e) {
                Logger.error("Returned from CommandHistoryActivity without selecting any command");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void execCommandInternally(String command) {
        execCommandInternally(command, true);
    }

    private void execCommandInternally(String command, boolean wait) {
        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.sending))
                .setIsIndeterminate(true)
                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                    @Override
                    public void onActionClick() {
                        snackProgressBarManager.dismissAll();
                    }
                });
        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        Watch.get().executeShellCommand(command, wait, false).continueWith(new Continuation<ResultShellCommand, Object>() {
            @Override
            public Object then(@NonNull Task<ResultShellCommand> task) {

                snackProgressBarManager.dismissAll();
                String snackBarText;

                if (task.isSuccessful()) {
                    ResultShellCommand resultShellCommand = task.getResult();
                    if (resultShellCommand != null) {
                        ResultShellCommandData resultShellCommandData = resultShellCommand.getResultShellCommandData();

                        if (resultShellCommandData.getResult() == 0) {
                            binding.activityTweakingShellResultCode.setText(String.valueOf(resultShellCommandData.getResult()));
                            binding.activityTweakingShellResult.setText(resultShellCommandData.getOutputLog());
                            snackBarText = "success";

                        } else {
                            binding.activityTweakingShellResultCode.setText(String.valueOf(resultShellCommandData.getResult()));
                            binding.activityTweakingShellResult.setText(String.format("%s\n%s", resultShellCommandData.getOutputLog(), resultShellCommandData.getErrorLog()));
                            snackBarText = getString(R.string.shell_command_failed);
                        }
                    } else
                        snackBarText = getString(R.string.shell_command_failed);

                } else {
                    binding.activityTweakingShellResultCode.setText("");
                    binding.activityTweakingShellResult.setText("");
                    snackBarText = getString(R.string.cant_send_shell_command);
                }

                if (!snackBarText.equals("success")) {
                    SnackProgressBar snackbar = new SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, snackBarText);
                    snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                }

                return null;
            }
        });
    }

    private void updateBrightness(final int value) {
        BrightnessData brightnessData = new BrightnessData();
        brightnessData.setLevel(value);
        binding.activityTweakingSeekbar.setProgress(value);

        Watch.get().setBrightness(brightnessData).continueWith(new Continuation<Void, Object>() {
            @Override
            public Object then(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Snacky.builder()
                            .setActivity(TweakingActivity.this)
                            .setText(R.string.brightness_applied)
                            .setDuration(Snacky.LENGTH_SHORT)
                            .build().show();

                } else {
                    Snacky.builder()
                            .setActivity(TweakingActivity.this)
                            .setText(R.string.failed_to_set_brightness)
                            .setDuration(Snacky.LENGTH_SHORT)
                            .build().show();
                }
                return null;
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void requestFileUpload(RequestFileUpload requestFileUpload) {
        final FileUploadData fileUploadData = requestFileUpload.getFileUploadData();
        Logger.debug("TweakingActivity requestFileUpload path: " + fileUploadData.getPath());

        //Toast.makeText(this, "ScreenShot taken\nwait for download", Toast.LENGTH_LONG).show();

        final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        final SnackProgressBar progressBar = new SnackProgressBar(
                SnackProgressBar.TYPE_CIRCULAR, getString(R.string.downloading))
                .setIsIndeterminate(false)
                .setProgressMax(100)
                .setAction(getString(R.string.cancel), new SnackProgressBar.OnActionClickListener() {
                    @Override
                    public void onActionClick() {
                        snackProgressBarManager.dismissAll();
                        cancellationTokenSource.cancel();
                    }
                })
                .setShowProgressPercentage(true);
        snackProgressBarManager.show(progressBar, SnackProgressBarManager.LENGTH_INDEFINITE);

        final long size = fileUploadData.getSize();
        final long startedAt = System.currentTimeMillis();

        Watch.get().downloadFile(this, fileUploadData.getPath(), fileUploadData.getName(), size, Constants.MODE_SCREENSHOT,
                        new Watch.OperationProgress() {
                            @Override
                            public void update(final long duration, final long byteSent, final long remainingTime, final double progress) {
                                TweakingActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String remaingSize = Formatter.formatShortFileSize(TweakingActivity.this, size - byteSent);
                                        double kbSent = byteSent / 1024d;
                                        double speed = kbSent / (duration / 1000);
                                        DecimalFormat df = new DecimalFormat("#.00");

                                        String duration = DurationFormatUtils.formatDuration(remainingTime, "mm:ss", true);
                                        String message = getString(R.string.sending) + " - " + duration + " - " + remaingSize + " - " + df.format(speed) + " kb/s";

                                        progressBar.setMessage(message);
                                        snackProgressBarManager.setProgress((int) progress);
                                        snackProgressBarManager.updateTo(progressBar);
                                    }
                                });
                            }
                        }, cancellationTokenSource.getToken())
                .continueWith(new Continuation<Void, Object>() {
                    @Override
                    public Object then(@NonNull Task<Void> task) {
                        snackProgressBarManager.dismissAll();
                        if (task.isSuccessful()) {
                            SnackProgressBar snackbar = new SnackProgressBar(
                                    SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.file_downloaded))
                                    .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                        @Override
                                        public void onActionClick() {
                                            snackProgressBarManager.dismissAll();
                                        }
                                    });
                            snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);

                            final File screenshot = new File(DownloadHelper.getDownloadDir(Constants.MODE_SCREENSHOT) + "/" + fileUploadData.getName());

                            if (screenshot.exists()) {
                                Drawable drawable = Drawable.createFromPath(screenshot.getAbsolutePath());
                                if (drawable != null) {

                                    // Rotate and re-save image on Verge
                                    if (Screen.isVerge()) {
                                        // Rotate
                                        drawable = FilesUtil.getRotateDrawable(drawable, 180f);
                                        // Re-Save (reopen because drawable is bad quality)
                                        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
                                        BitmapFactory.Options options = new BitmapFactory.Options();
                                        options.inDensity = dm.densityDpi;
                                        options.inScreenDensity = dm.densityDpi;
                                        options.inTargetDensity = dm.densityDpi;
                                        Bitmap bmp = BitmapFactory.decodeFile(screenshot.getAbsolutePath(), options);
                                        Matrix matrix = new Matrix();
                                        matrix.postRotate(180);
                                        Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                                        if (!FilesUtil.saveBitmapToFile(screenshot, rotatedBitmap, PNG, 100))
                                            Logger.error("Verge's screenshot could not be saved after rotation");
                                    }

                                    new MaterialDialog.Builder(mContext)
                                            .canceledOnTouchOutside(false)
                                            .icon(drawable)
                                            .title("Screenshot")
                                            .positiveText(R.string.open)
                                            .negativeText(R.string.cancel)
                                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                @Override
                                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                    final Intent intent = new Intent(Intent.ACTION_VIEW)//
                                                            .setDataAndType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                                                                    FileProvider.getUriForFile(mContext, Constants.FILE_PROVIDER, screenshot)
                                                                    : Uri.fromFile(screenshot), "image/*")
                                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                                    startActivity(intent);
                                                }
                                            })
                                            .show();
                                }
                            }

                        } else {
                            if (task.getException() instanceof CancellationException) {
                                SnackProgressBar snackbar = new SnackProgressBar(
                                        SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.file_download_canceled))
                                        .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                            @Override
                                            public void onActionClick() {
                                                snackProgressBarManager.dismissAll();
                                            }
                                        });
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                            } else {
                                SnackProgressBar snackbar = new SnackProgressBar(
                                        SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.cant_download_file))
                                        .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                                            @Override
                                            public void onActionClick() {
                                                snackProgressBarManager.dismissAll();
                                            }
                                        });
                                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_LONG);
                            }
                        }
                        return null;
                    }
                });
    }

    public void getWatchLocalIP() {
        getWatchLocalIP(false);
    }

    public void getWatchLocalIP(boolean ftp) {
        // Get watch's local IP
        Watch.get().sendSimpleData(Transport.LOCAL_IP, null).continueWith(new Continuation<OtherData, Object>() {
            @Override
            public Object then(@NonNull Task<OtherData> task) {
                String message = getString(R.string.error);
                if (task.isSuccessful()) {
                    OtherData returnedData = task.getResult();
                    try {
                        if (returnedData == null)
                            throw new NullPointerException("Returned data are null");

                        DataBundle otherData = returnedData.getOtherData();

                        String localIP = otherData.getString("ip");
                        if (ftp) {
                            if (localIP.equals("N/A"))
                                localIP = defaultFTPip;
                            localIP = localIP + ":" + defaultPort;
                            message = "FTP server " + getString(R.string.enabled) + ".\n" + getString(R.string.local_ip) + ": " + localIP;
                        } else {
                            if (localIP.equals("N/A"))
                                message = getString(R.string.watch_no_wifi);
                            else if (localIP.equals(defaultFTPip))
                                message = getString(R.string.local_ip) + ": " + localIP + " (localhost)";
                            else
                                message = getString(R.string.local_ip) + ": " + localIP;
                        }
                        Logger.debug(TAG + "watch local IP is " + localIP);
                        //Toast.makeText(mContext, "Watch's local IP is " + localIP, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Logger.debug(TAG + "failed reading IP data: " + e);
                    }
                } else {
                    Logger.error(task.getException(), "Task sendSimpleData action \"local_ip\" failed");
                }

                // Show notification
                SnackProgressBar snackbar = new SnackProgressBar(
                        SnackProgressBar.TYPE_HORIZONTAL, message)
                        .setAction(getString(R.string.close), new SnackProgressBar.OnActionClickListener() {
                            @Override
                            public void onActionClick() {
                                snackProgressBarManager.dismissAll();
                            }
                        });
                snackProgressBarManager.show(snackbar, SnackProgressBarManager.LENGTH_INDEFINITE);
                return null;
            }
        });
    }
}
