package com.edotassi.amazmod.watch;

import android.content.Context;
import android.net.ConnectivityManager;

import com.edotassi.amazmod.transport.TransportService;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.RequestDeleteFileData;

public class FtpWifiUploadHelper {

    public void upload(List<File> files, String ftpDestpath, Context context,
                       TaskCompletionSource taskCompletionSource, Watch.OperationProgress operationProgress,
                       CancellationToken cancellationToken, long startedAt){
        WiFiHelper wiFiHelper = new WiFiHelper();
        wiFiHelper.getTransferringMethod(context, new WiFiHelper.WiFiHelperListener() {
            @Override
            public void onWiFiReady(ConnectivityManager mConnectivityManager, ConnectivityManager.NetworkCallback networkCallback) {
                sendFileViaFtpClient(files, ftpDestpath, mConnectivityManager, networkCallback, taskCompletionSource, operationProgress, cancellationToken, startedAt);
            }

            @Override
            public void onError(String message) {
                Logger.error(message);
                taskCompletionSource.trySetResult(null);
            }
        });
    }

    private void sendFileViaFtpClient(List<File> files, String ftpDestPath, ConnectivityManager mConnectivityManager, ConnectivityManager.NetworkCallback networkCallback, TaskCompletionSource taskCompletionSource, Watch.OperationProgress operationProgress, CancellationToken cancellationToken, long startedAt) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // We create ftp connections
                FTPClient ftpClient = new FTPClient();
                ftpClient.setDataTimeout(7200);
                ftpClient.setControlKeepAliveTimeout(7200);
                int total_files = files.size();
                try {
                    if (!WiFiHelper.localIP.equals("N/A")) {
                        ftpClient.connect(WiFiHelper.localIP, 5210);
                    } else {
                        ftpClient.connect(WiFiHelper.defaultFTPip, 5210);
                    }
                    ftpClient.login("anonymous", "");

                    // After connection attempt, you should check the reply code to verify success.
                    int reply = ftpClient.getReplyCode();
                    if (!FTPReply.isPositiveCompletion(reply)) {
                        ftpClient.disconnect();
                        Logger.debug("FTP: FTP server refused connection.");

                        // close ftp & wifi ap
                        Watch.get().sendSimpleData(Transport.WIFI_FTP_DISABLE, Transport.FTP_ON_STATE_CHANGED, TransportService.TRANSPORT_FTP);
                        TransportService.sendWithTransporterFtp(Transport.WIFI_DISABLE_AP, null);
                    } else {
                        Logger.debug("FTP: FTP server connection granted.");
                        // Set FTP transferred as BINARY to avoid corrupted file
                        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                        //If it doesn't exist create it
                        String relative_path = ftpDestPath.replace("/sdcard", "");
                        if (!ftpClient.changeWorkingDirectory(relative_path)) {
                            Logger.debug("FTP: target path doesn't exit. Creating path: " + relative_path);
                            ftpClient.makeDirectory(relative_path);
                            ftpClient.changeWorkingDirectory(relative_path);
                        }

                        File[] filesArray = new File[1];
                        CopyStreamAdapter streamListener = new CopyStreamAdapter() {
                            @Override
                            public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                                File file = filesArray[0];
                                if (file != null) {
                                    // This method will be called every time bytes are transferred
                                    double progress = ((double) (totalBytesTransferred * 100) / file.length());
                                    long duration = System.currentTimeMillis() - startedAt;
                                    long byteSent = totalBytesTransferred;
                                    double speed = ((double) byteSent) / ((double) duration); // byte/ms
                                    long remainingBytes = file.length() - byteSent;
                                    long remainTime = (long) (remainingBytes / speed);
                                    operationProgress.update(duration, byteSent, remainTime, progress);
                                }
                            }
                        };
                        ftpClient.setCopyStreamListener(streamListener);

                        // Loop through selected files
                        for (File file : files) {
                            if (cancellationToken.isCancellationRequested()) {
                                Logger.debug("FTP: Upload canceled");
                                ftpClient.logout();
                                ftpClient.disconnect();
                                taskCompletionSource.trySetResult(new CancellationException());
                                return;
                            }
                            filesArray[0] = file;
                            if (file.exists() && !file.isDirectory()) {
                                // Create an InputStream of the zipped file to be uploaded
                                FileInputStream stream = new FileInputStream(file);
                                // Store file to server
                                if (ftpClient.storeFile(file.getName(), stream)) {
                                    Logger.debug("FTP: file " + (1 + total_files - files.size()) + " transfer finished.");
                                } else {
                                    Logger.debug("FTP: file " + (1 + total_files - files.size()) + " transfer failed: " + ftpClient.getReplyString());
                                }
                            }
                        }
                        //Finish up
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch (IOException e) {
                    if (ftpClient.isConnected()) {
                        Logger.debug("FTP: connection to server error, but server is connected... Disconnecting...");
                        try {
                            ftpClient.disconnect();
                        } catch (IOException f) {
                            // do nothing
                        }
                    }
                    unregisterConnectionManager(mConnectivityManager, networkCallback);

                    Logger.debug("FTP: connection to server error: " + e.toString());

                    taskCompletionSource.trySetResult(null);
                }
                // Close ftp & wifi ap
                Watch.get().sendSimpleData(Transport.WIFI_FTP_DISABLE, Transport.FTP_ON_STATE_CHANGED, TransportService.TRANSPORT_FTP);
                TransportService.sendWithTransporterFtp(Transport.WIFI_DISABLE_AP, null);
                unregisterConnectionManager(mConnectivityManager, networkCallback);
                taskCompletionSource.trySetResult(null);
            }
        }).start();
    }


    private void unregisterConnectionManager(ConnectivityManager mConnectivityManager, ConnectivityManager.NetworkCallback networkCallback) {
        if (mConnectivityManager != null && networkCallback != null)
            try {
                mConnectivityManager.unregisterNetworkCallback(networkCallback);
                mConnectivityManager.bindProcessToNetwork(null);
            } catch (Exception e) {
                Logger.debug("unregisterConnectionManager: " + e);
            }
    }
}
