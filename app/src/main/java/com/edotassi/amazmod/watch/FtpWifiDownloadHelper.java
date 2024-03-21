package com.edotassi.amazmod.watch;

import android.content.Context;
import android.net.ConnectivityManager;

import com.edotassi.amazmod.support.DownloadHelper;
import com.edotassi.amazmod.transport.TransportService;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.tinylog.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;

import amazmod.com.transport.Transport;

public class FtpWifiDownloadHelper {

    public void download(final String name, final String path, long size, final byte mode, Context context,
                     TaskCompletionSource taskCompletionSource, Watch.OperationProgress operationProgress, CancellationToken cancellationToken, long startedAt){
        WiFiHelper wiFiHelper = new WiFiHelper();
        wiFiHelper.getTransferringMethod(context, new WiFiHelper.WiFiHelperListener() {
            @Override
            public void onWiFiReady(ConnectivityManager mConnectivityManager, ConnectivityManager.NetworkCallback networkCallback) {
                retrieveFileViaFtpClient(name, path, size, mode, mConnectivityManager, networkCallback, taskCompletionSource, operationProgress, cancellationToken, startedAt);
            }

            @Override
            public void onError(String message) {
                Logger.error(message);
                taskCompletionSource.trySetResult(null);
            }
        });
    }

    private void retrieveFileViaFtpClient(final String name, final String path, long size, final byte mode,
                                          ConnectivityManager mConnectivityManager,
                                          ConnectivityManager.NetworkCallback networkCallback, TaskCompletionSource taskCompletionSource,
                                          Watch.OperationProgress operationProgress,
                                          CancellationToken cancellationToken,
                                          long startedAt) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // We create ftp connections
                FTPClient ftpClient = new FTPClient();
                ftpClient.setDataTimeout(0);
                ftpClient.setControlKeepAliveTimeout(0);
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
                        String relative_path = path.replace("/sdcard", "");

                        CopyStreamAdapter streamListener = new CopyStreamAdapter() {
                            @Override
                            public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                                // This method will be called every time bytes are transferred
                                double progress = ((double) (totalBytesTransferred * 100) / size);
                                long duration = System.currentTimeMillis() - startedAt;
                                long byteSent = totalBytesTransferred;
                                double speed = ((double) byteSent) / ((double) duration); // byte/ms
                                long remainingBytes = size - byteSent;
                                long remainTime = (long) (remainingBytes / speed);
                                operationProgress.update(duration, byteSent, remainTime, progress);
                            }
                        };
                        ftpClient.setCopyStreamListener(streamListener);

                        String downloadDir = DownloadHelper.getDownloadDir(mode);
                        File destinationFile = new File(downloadDir + "/" + name);
                        Logger.debug("Dest path: " + downloadDir + "/" + name);

                        if (cancellationToken.isCancellationRequested()) {
                            Logger.debug("FTP: Download canceled");
                            ftpClient.logout();
                            ftpClient.disconnect();
                            taskCompletionSource.trySetResult(new CancellationException());
                            return;
                        }

                        if (destinationFile.exists() || destinationFile.createNewFile()) {
                            FileOutputStream stream = new FileOutputStream(destinationFile);
                            Logger.debug("Remote path: " + relative_path);
                            if (ftpClient.retrieveFile(relative_path, stream)) {
                                Logger.debug("FTP: file " + destinationFile.getName() + " transfer finished.");
                            } else {
                                Logger.debug("FTP: file " + destinationFile.getName() + " transfer failed: " + ftpClient.getReplyString());
                            }
                        } else {
                            Logger.error("FTP: file is not exist or it is a directory");
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
