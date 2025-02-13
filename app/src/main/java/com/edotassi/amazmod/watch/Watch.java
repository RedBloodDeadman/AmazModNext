package com.edotassi.amazmod.watch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.edotassi.amazmod.event.BatteryStatus;
import com.edotassi.amazmod.event.Directory;
import com.edotassi.amazmod.event.OtherData;
import com.edotassi.amazmod.event.ResultDeleteFile;
import com.edotassi.amazmod.event.ResultDownloadFileChunk;
import com.edotassi.amazmod.event.ResultShellCommand;
import com.edotassi.amazmod.event.ResultWidgets;
import com.edotassi.amazmod.event.Sleep;
import com.edotassi.amazmod.event.WatchStatus;
import com.edotassi.amazmod.support.DownloadHelper;
import com.edotassi.amazmod.support.PermissionsHelper;
import com.edotassi.amazmod.transport.TransportService;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.huami.watch.transport.DataBundle;
import com.pixplicity.easyprefs.library.Prefs;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.Transportable;
import amazmod.com.transport.data.BrightnessData;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.RequestDeleteFileData;
import amazmod.com.transport.data.RequestDirectoryData;
import amazmod.com.transport.data.RequestDownloadFileChunkData;
import amazmod.com.transport.data.RequestShellCommandData;
import amazmod.com.transport.data.RequestUploadFileChunkData;
import amazmod.com.transport.data.ResultDownloadFileChunkData;
import amazmod.com.transport.data.SettingsData;
import amazmod.com.transport.data.SleepData;
import amazmod.com.transport.data.WatchfaceData;
import amazmod.com.transport.data.WidgetsData;

public class Watch {

    private static Watch instance;

    private Context context;
    private TransportService transportService;

    private ThreadPoolExecutor threadPoolExecutor;

    private Watch() {
        threadPoolExecutor = new ThreadPoolExecutor(1, 2,
                30L, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
    }

    public static void init(Context context) {
        instance = new Watch();
        instance.context = context;
    }

    public static Watch get() {
        if (instance == null) {
            throw new RuntimeException("Watch not initialized");
        }

        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public Task<WatchStatus> getStatus() {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<WatchStatus>>() {
            @Override
            public Task<WatchStatus> then(@NonNull Task<TransportService> task) {
                return Objects.requireNonNull(task.getResult()).sendWithResult(Transport.REQUEST_WATCHSTATUS, Transport.WATCH_STATUS);
            }
        });
    }

    public Task<BatteryStatus> getBatteryStatus() {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<BatteryStatus>>() {
            @Override
            public Task<BatteryStatus> then(@NonNull Task<TransportService> task) {
                return Objects.requireNonNull(task.getResult()).sendWithResult(Transport.REQUEST_BATTERYSTATUS, Transport.BATTERY_STATUS);
            }
        });
    }

    public Task<Directory> listDirectory(final RequestDirectoryData requestDirectoryData) {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<Directory>>() {
            @Override
            public Task<Directory> then(@NonNull Task<TransportService> task) {
                return Objects.requireNonNull(task.getResult()).sendWithResult(Transport.REQUEST_DIRECTORY, Transport.DIRECTORY, requestDirectoryData);
            }
        });
    }

    public Task<ResultDeleteFile> deleteFile(final RequestDeleteFileData requestDeleteFileData) {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<ResultDeleteFile>>() {
            @Override
            public Task<ResultDeleteFile> then(@NonNull Task<TransportService> task) {
                return Objects.requireNonNull(task.getResult()).sendWithResult(Transport.REQUEST_DELETE_FILE, Transport.RESULT_DELETE_FILE, requestDeleteFileData);
            }
        });
    }

    public Task<Void> downloadFile(Activity activity, final String path, final String name,
                                   final long size,
                                   final byte mode,
                                   final OperationProgress operationProgress,
                                   final CancellationToken cancellationToken) {
        String transferMethod = Prefs.getString(Constants.PREF_DATA_TRANSFER_METHOD, "1");
        if (Constants.PREF_DATA_TRANSFER_METHOD_BLUETOOTH.equals(transferMethod)){
            return downloadFileViaBt(activity, path, name, size, mode, operationProgress, cancellationToken);
        }else if (Constants.PREF_DATA_TRANSFER_METHOD_WIFI.equals(transferMethod)){
            return downloadFileViaFTP(activity, path, name, size, mode, operationProgress, cancellationToken);
        }
        return null;
    }

    public Task<Void> downloadFileViaBt(Activity activity, final String path, final String name,
                                   final long size,
                                   final byte mode,
                                   final OperationProgress operationProgress,
                                   final CancellationToken cancellationToken) {
        final TaskCompletionSource taskCompletionSource = new TaskCompletionSource<Void>();

        PermissionsHelper
                .checkPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .continueWith(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) {
                        if (!task.isSuccessful()) {
                            if (task.getException() != null)
                                taskCompletionSource.setException(task.getException());
                            return null;
                        }

                        Tasks.call(threadPoolExecutor, new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                TransportService transportService = Tasks.await(getServiceInstance());

                                long lastChunkSize = size % Constants.CHUNK_SIZE;
                                long totalChunks = size / Constants.CHUNK_SIZE;
                                long startedAt = System.currentTimeMillis();

                                if (!DownloadHelper.checkDownloadDirExist(mode)) {
                                    taskCompletionSource.setException(new Exception("cant_create_download_directory"));
                                    return null;
                                }

                                for (int i = 0; i < totalChunks; i++) {
                                    if (cancellationToken.isCancellationRequested()) {
                                        DownloadHelper.deleteDownloadedFile(name, mode);

                                        taskCompletionSource.setException(new CancellationException());
                                        return null;
                                    }

                                    RequestDownloadFileChunkData requestDownloadFileChunkData = new RequestDownloadFileChunkData();
                                    requestDownloadFileChunkData.setPath(path);
                                    requestDownloadFileChunkData.setIndex(i);
                                    ResultDownloadFileChunk resultDownloadFileChunk = (ResultDownloadFileChunk) Tasks.await(transportService.sendWithResult(Transport.REQUEST_DOWNLOAD_FILE_CHUNK, Transport.RESULT_DOWNLOAD_FILE_CHUNK, requestDownloadFileChunkData));

                                    ResultDownloadFileChunkData resultDownloadFileChunkData = resultDownloadFileChunk.getResultDownloadFileChunkData();

                                    File destinationFile = DownloadHelper.getDownloadedFile(name, mode);
                                    RandomAccessFile randomAccessFile = new RandomAccessFile(destinationFile, "rw");
                                    randomAccessFile.seek(resultDownloadFileChunkData.getIndex() * Constants.CHUNK_SIZE);
                                    randomAccessFile.write(resultDownloadFileChunkData.getBytes());
                                    randomAccessFile.close();

                                    double progress = (((double) (i + 1)) / totalChunks) * 100f;
                                    long duration = System.currentTimeMillis() - startedAt;
                                    long byteSent = (i + 1) * Constants.CHUNK_SIZE;
                                    double speed = ((double) byteSent) / ((double) duration); // byte/ms
                                    long remainingBytes = size - byteSent;
                                    long remainTime = (long) (remainingBytes / speed);

                                    operationProgress.update(duration, byteSent, remainTime, progress);
                                }

                                if (lastChunkSize > 0) {
                                    RequestDownloadFileChunkData requestDownloadFileChunkData = new RequestDownloadFileChunkData();
                                    requestDownloadFileChunkData.setPath(path);
                                    requestDownloadFileChunkData.setIndex((int) totalChunks);
                                    ResultDownloadFileChunk resultDownloadFileChunk = (ResultDownloadFileChunk) Tasks.await(transportService.sendWithResult(Transport.REQUEST_DOWNLOAD_FILE_CHUNK, Transport.RESULT_DOWNLOAD_FILE_CHUNK, requestDownloadFileChunkData));

                                    ResultDownloadFileChunkData resultDownloadFileChunkData = resultDownloadFileChunk.getResultDownloadFileChunkData();

                                    File destinationFile = DownloadHelper.getDownloadedFile(name, mode);
                                    RandomAccessFile randomAccessFile = new RandomAccessFile(destinationFile, "rw");
                                    randomAccessFile.seek(resultDownloadFileChunkData.getIndex() * Constants.CHUNK_SIZE);
                                    randomAccessFile.write(resultDownloadFileChunkData.getBytes());
                                    randomAccessFile.close();
                                }

                                taskCompletionSource.setResult(null);

                                return null;
                            }
                        });

                        return null;
                    }
                });

        return taskCompletionSource.getTask();
    }

    public Task<Void> downloadFileViaFTP(Activity activity, final String path, final String name,
                                         final long size,
                                         final byte mode,
                                         final OperationProgress operationProgress,
                                         final CancellationToken cancellationToken) {
        final TaskCompletionSource taskCompletionSource = new TaskCompletionSource<Void>();

        PermissionsHelper
                .checkPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .continueWith(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) {
                        if (!task.isSuccessful()) {
                            if (task.getException() != null)
                                taskCompletionSource.setException(task.getException());
                            return null;
                        }

                        Tasks.call(threadPoolExecutor, new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                FtpWifiDownloadHelper ftpWifiDownloadHelper = new FtpWifiDownloadHelper();
                                long startedAt = System.currentTimeMillis();
                                ftpWifiDownloadHelper.download(name, path, size, mode, context, taskCompletionSource, operationProgress, cancellationToken, startedAt);
                                return null;
                            }
                        });

                        return null;
                    }
                });

        return taskCompletionSource.getTask();
    }

    public Task<Void> uploadFile(final File file, final String destPath, final OperationProgress operationProgress, final CancellationToken cancellationToken) {
        String transferMethod = Prefs.getString(Constants.PREF_DATA_TRANSFER_METHOD, "1");
        if (Constants.PREF_DATA_TRANSFER_METHOD_BLUETOOTH.equals(transferMethod)){
            return uploadFileViaBt(file, destPath, operationProgress, cancellationToken);
        }else if (Constants.PREF_DATA_TRANSFER_METHOD_WIFI.equals(transferMethod)){
            return uploadFileViaFTP(file, destPath, operationProgress, cancellationToken);
        }
        return null;
    }

    public Task<Void> uploadFileViaBt(final File file, final String destPath, final OperationProgress operationProgress, final CancellationToken cancellationToken) {
        final TaskCompletionSource taskCompletionSource = new TaskCompletionSource<Void>();

        Tasks.call(threadPoolExecutor, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                TransportService transportService = Tasks.await(getServiceInstance());
                long size = file.length();
                long lastChunkSize = size % Constants.CHUNK_SIZE;
                long totalChunks = size / Constants.CHUNK_SIZE;
                long startedAt = System.currentTimeMillis();

                for (int i = 0; i < totalChunks; i++) {
                    if (cancellationToken.isCancellationRequested()) {
                        RequestDeleteFileData requestDeleteFileData = new RequestDeleteFileData();
                        requestDeleteFileData.setPath(destPath);
                        Tasks.await(Watch.get().deleteFile(requestDeleteFileData));

                        taskCompletionSource.setException(new CancellationException());
                        return null;
                    }

                    RequestUploadFileChunkData requestUploadFileChunkData = RequestUploadFileChunkData.fromFile(file, destPath, Constants.CHUNK_SIZE, i, Constants.CHUNK_SIZE);
                    Tasks.await(transportService.sendAndWait(Transport.REQUEST_UPLOAD_FILE_CHUNK, requestUploadFileChunkData));

                    double progress = (((double) (i + 1)) / totalChunks) * 100f;
                    long duration = System.currentTimeMillis() - startedAt;
                    long byteSent = (i + 1) * Constants.CHUNK_SIZE;
                    double speed = ((double) byteSent) / ((double) duration); // byte/ms
                    long remainingBytes = size - byteSent;
                    long remainTime = (long) (remainingBytes / speed);

                    operationProgress.update(duration, byteSent, remainTime, progress);
                }

                if (lastChunkSize > 0) {
                    RequestUploadFileChunkData requestUploadFileChunkData = RequestUploadFileChunkData.fromFile(file, destPath, Constants.CHUNK_SIZE, totalChunks, (int) lastChunkSize);
                    Tasks.await(transportService.sendAndWait(Transport.REQUEST_UPLOAD_FILE_CHUNK, requestUploadFileChunkData));
                }

                taskCompletionSource.setResult(null);

                return null;
            }
        });

        return taskCompletionSource.getTask();
    }

    public Task<Void> uploadFileViaFTP(final File file, final String destPath, final OperationProgress operationProgress, final CancellationToken cancellationToken) {
        String ftpDestPath = destPath.substring(0, destPath.lastIndexOf("/"));
        return uploadFileViaFTP(Collections.singletonList(file), ftpDestPath, operationProgress, cancellationToken);
    }

    public Task<Void> uploadFileViaFTP(final List<File> files, final String ftpDestPath, final OperationProgress operationProgress, final CancellationToken cancellationToken) {
        final TaskCompletionSource taskCompletionSource = new TaskCompletionSource<Void>();
        Tasks.call(threadPoolExecutor, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                FtpWifiUploadHelper ftpWifiUploadHelper = new FtpWifiUploadHelper();
                long startedAt = System.currentTimeMillis();
                ftpWifiUploadHelper.upload(files, ftpDestPath, context, taskCompletionSource, operationProgress, cancellationToken, startedAt);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Void> postNotification(final NotificationData notificationData) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) {
                if (task.getResult() != null)
                    task.getResult().send(Transport.INCOMING_NOTIFICATION, notificationData, taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Void> syncSettings(final SettingsData settingsData) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) {
                if (task.getResult() != null)
                    task.getResult().send(Transport.SYNC_SETTINGS, settingsData, taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Void> setBrightness(final BrightnessData brightnessData) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) {
                if (task.getResult() != null)
                    task.getResult().send(Transport.BRIGHTNESS, brightnessData, taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<ResultShellCommand> executeShellCommand(final String command) {
        return executeShellCommand(command, false, false);
    }

    public Task<ResultShellCommand> executeShellCommand(final String command, boolean waitOutput, boolean reboot) {
        final RequestShellCommandData requestShellCommandData = new RequestShellCommandData();
        requestShellCommandData.setCommand(command);
        requestShellCommandData.setWaitOutput(waitOutput);
        requestShellCommandData.setReboot(reboot);

        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<ResultShellCommand>>() {
            @Override
            public Task<ResultShellCommand> then(@NonNull Task<TransportService> task) {
                return Objects.requireNonNull(task.getResult()).sendWithResult(Transport.REQUEST_SHELL_COMMAND, Transport.RESULT_SHELL_COMMAND, requestShellCommandData);
            }
        });
    }

    public Task<Void> enableLowPower() {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) {
                if (task.getResult() != null)
                    task.getResult().send(Transport.ENABLE_LOW_POWER, null, taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Void> revokeAdminOwner() {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) {
                if (task.getResult() != null)
                    task.getResult().send(Transport.REVOKE_ADMIN_OWNER, null, taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    private Task<TransportService> getServiceInstance() {
        if (transportService != null) {
            return Tasks.forResult(transportService);
        }

        final TaskCompletionSource<TransportService> taskCompletionSource = new TaskCompletionSource<>();

        Intent intent = new Intent(context, TransportService.class);
        context.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                transportService = ((TransportService.LocalBinder) binder).getService();
                taskCompletionSource.setResult(transportService);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                transportService = null;
            }
        }, 0);

        return taskCompletionSource.getTask();
    }

    public Task<Void> sendWatchfaceData(final WatchfaceData watchfaceData) {
        final TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        getServiceInstance().continueWith(new Continuation<TransportService, Object>() {
            @Override
            public Object then(@NonNull Task<TransportService> task) {
                if (task.getResult() != null)
                    task.getResult().send(Transport.WATCHFACE_DATA, watchfaceData, taskCompletionSource);
                return null;
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<ResultWidgets> sendWidgetsData(final WidgetsData widgetsData) {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<ResultWidgets>>() {
            @Override
            public Task<ResultWidgets> then(@NonNull Task<TransportService> task) {
                return Objects.requireNonNull(task.getResult()).sendWithResult(Transport.REQUEST_WIDGETS, Transport.REQUEST_WIDGETS, widgetsData);
            }
        });
    }

    public void sendSleepData(final SleepData sleepData) {
        transportService.sendWithSleep(Transport.SLEEP_DATA, sleepData, null);
    }

    public interface OperationProgress {
        void update(long duration, long byteSent, long remainingTime, double progress);
    }

    public Task<OtherData> sendSimpleData(String action) {
        return sendSimpleData(action, null);
    }

    public Task<OtherData> sendSimpleData(String action, Transportable data) {
        return sendSimpleData(action, new String[]{action}, data, TransportService.TRANSPORT_AMAZMOD);
    }

    // Send data with custom transporter
    public Task<OtherData> sendSimpleData(String action, char transporter) {
        return sendSimpleData(action, new String[]{action}, (Transportable) null, transporter);
    }

    public Task<OtherData> sendSimpleData(String action, char transporter, DataBundle data) {
        return sendSimpleData(action, new String[]{action}, data, transporter);
    }

    public Task<OtherData> sendSimpleData(String action, String replyAction, char transporter, DataBundle data) {
        return sendSimpleData(action, new String[]{replyAction}, data, transporter);
    }

    public Task<OtherData> sendSimpleData(String action, String[] replyActions, char transporter, DataBundle data) {
        return sendSimpleData(action, replyActions, data, transporter);
    }

    public Task<OtherData> sendSimpleData(String action, String replyAction, char transporter) {
        return sendSimpleData(action, new String[]{replyAction}, (Transportable) null, transporter);
    }

    public Task<OtherData> sendSimpleData(String action, String[] replyActions, char transporter) {
        return sendSimpleData(action, replyActions, (DataBundle) null, transporter);
    }

    public Task<OtherData> sendSimpleData(String action, String[] replyActions, Transportable data, char transporter) {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<OtherData>>() {
            @Override
            public Task<OtherData> then(@NonNull Task<TransportService> task) {
                return Objects.requireNonNull(task.getResult()).sendWithResult(action, replyActions, data, transporter);
            }
        });
    }

    public Task<OtherData> sendSimpleData(String action, String[] replyActions, DataBundle data, char transporter) {
        return getServiceInstance().continueWithTask(new Continuation<TransportService, Task<OtherData>>() {
            @Override
            public Task<OtherData> then(@NonNull Task<TransportService> task) {
                return Objects.requireNonNull(task.getResult()).sendWithResult(action, replyActions, data, transporter);
            }
        });
    }
}
