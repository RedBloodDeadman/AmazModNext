package com.edotassi.amazmod.watch;

import static com.edotassi.amazmod.ui.FileExplorerActivity.getSSID;
import static com.edotassi.amazmod.ui.FileExplorerActivity.isConnected;
import static com.edotassi.amazmod.ui.FileExplorerActivity.wifiState;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import androidx.annotation.NonNull;

import com.edotassi.amazmod.event.OtherData;
import com.edotassi.amazmod.transport.TransportService;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.huami.watch.transport.DataBundle;

import org.tinylog.Logger;

import amazmod.com.transport.Transport;

public class WiFiHelper {
    private ConnectivityManager.NetworkCallback networkCallback;
    public static String SSID = "huami-amazfit-amazmod-4E68";
    public static String pswd = "12345678";
    public static String localIP = "N/A";
    public static String defaultFTPip = "192.168.43.1";

    interface WiFiHelperListener{
        void onWiFiReady(ConnectivityManager mConnectivityManager, ConnectivityManager.NetworkCallback networkCallback);
        void onError(String message);
    }


    private void connectToTheWatchApNetwork(Context context, WiFiHelperListener wiFiHelperListener) {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Connect to the network
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            newApiWifiConnection(mConnectivityManager, wiFiHelperListener);
        } else {
            int netId = -1;
            WifiConfiguration wc = new WifiConfiguration();
            wc.SSID = "\"" + SSID + "\"";
            wc.preSharedKey = "\"" + pswd + "\"";
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (mWifiManager != null)
                netId = mWifiManager.addNetwork(wc);

            if (netId >= 0) {
                Logger.debug("FTP: watch's WiFi AP found: net ID = " + netId);

                //mWifiManager.disconnect(); // Disconnect from current network
                // Try to connect to watch network
                if (mWifiManager.enableNetwork(netId, true)) {
                    //mWifiManager.reconnect();
                    mWifiManager.saveConfiguration();

                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            try {
                                int seconds_waiting = 0;
                                int waiting_limit = 15;
                                // Check if connected!
                                while ((!getSSID(context).equals("\"" + SSID + "\"") || !isConnected(mConnectivityManager)) && seconds_waiting < waiting_limit) {
                                    Logger.debug("FTP: Waiting for WiFi connection to be established... (Current wifi:" + getSSID(context) + ", State: " + wifiState(mConnectivityManager) + ")");
                                    // Wait to connect
                                    seconds_waiting++;
                                    Thread.sleep(1000);
                                }

                                // Within time?
                                if (seconds_waiting < waiting_limit) {
                                    Logger.debug("FTP: WiFi connection established (Current wifi:" + getSSID(context) + "). Sending command to enable FTP.");
                                    Watch.get().sendSimpleData(Transport.WIFI_FTP_ENABLE, Transport.FTP_ON_STATE_CHANGED, TransportService.TRANSPORT_FTP).continueWith(new Continuation<OtherData, Object>() {
                                        @Override
                                        public Object then(@NonNull Task<OtherData> task) throws Exception {
                                            if (task.isSuccessful()) {
                                                wiFiHelperListener.onWiFiReady(mConnectivityManager, networkCallback);
                                            } else {
                                                wiFiHelperListener.onError("WIFI_FTP_ENABLE error");
                                            }
                                            return null;
                                        }
                                    });
                                } else {
                                    TransportService.sendWithTransporterFtp(Transport.WIFI_DISABLE_AP, null);
                                    wiFiHelperListener.onError("WiFi connection to server could not be established.");
                                }
                            } catch (Exception e) {
                                TransportService.sendWithTransporterFtp(Transport.WIFI_DISABLE_AP, null);
                                wiFiHelperListener.onError("FTP: WiFi connection thread crashed: " + e.getMessage());
                            }
                        }
                    };

                    t.start();
                } else {
                    wiFiHelperListener.onError("FTP: WiFi connection to server could not be established.");
                }
            } else {
                TransportService.sendWithTransporterFtp(Transport.WIFI_DISABLE_AP, null);
                wiFiHelperListener.onError("FTP: watch's WiFi AP not found.");
            }
        }
    }

    public void getTransferringMethod(Context context, WiFiHelperListener wiFiHelperListener) {
        // Get watch's local IP and choose transferring method to use
        Watch.get().sendSimpleData(Transport.LOCAL_IP, null).continueWith(new Continuation<OtherData, Object>() {
            @Override
            public Object then(@NonNull Task<OtherData> task) {
                localIP = "N/A";
                if (task.isSuccessful()) {
                    OtherData returnedData = task.getResult();
                    try {
                        if (returnedData == null)
                            throw new NullPointerException("Returned data are null");

                        DataBundle otherData = returnedData.getOtherData();
                        localIP = otherData.getString("ip");
                        Logger.debug("Watch IP is: " + localIP);

                    } catch (Exception e) {
                        wiFiHelperListener.onError("failed reading IP data: " + e);
                    }
                }
                if (TransportService.isTransporterFtpConnected()) {
                    Logger.debug("FTP: sending actions.");
                    TransportService.sendWithTransporterFtp(Transport.WIFI_START_SERVICE, null);
                    DataBundle dataBundle = new DataBundle();
                    dataBundle.putInt(Transport.WIFI_SECURITY_MODE, Transport.WIFI_WPA2);
                    dataBundle.putString(Transport.WIFI_SSID, SSID);
                    dataBundle.putString(Transport.WIFI_PASSWORD, pswd);

                    if (!localIP.equals("N/A") && !localIP.equals(defaultFTPip)) {
                        Logger.debug("Watch IP found, you are connected on the same WiFi");
                        Watch.get().sendSimpleData(Transport.WIFI_FTP_ENABLE, Transport.FTP_ON_STATE_CHANGED, TransportService.TRANSPORT_FTP).continueWith(new Continuation<OtherData, Object>() {
                            @Override
                            public Object then(@NonNull Task<OtherData> task) throws Exception {
                                if (task.isSuccessful()) {
                                    wiFiHelperListener.onWiFiReady(null, networkCallback);
                                } else {
                                    wiFiHelperListener.onError("WIFI_FTP_ENABLE error");
                                }
                                return null;
                            }
                        });
                    } else {
                        // Enable watch WiFi AP
                        Watch.get().sendSimpleData(Transport.WIFI_ENABLE_AP, Transport.ON_AP_ENABLE_RESULT, TransportService.TRANSPORT_FTP, dataBundle).continueWith(new Continuation<OtherData, Object>() {
                            @Override
                            public Object then(@NonNull Task<OtherData> task) throws Exception {
                                if (task.isSuccessful()) {
                                    connectToTheWatchApNetwork(context, wiFiHelperListener);
                                } else {
                                    wiFiHelperListener.onError("WIFI_ENABLE_AP error");
                                }
                                return null;
                            }
                        });
                        Logger.debug("Watch IP in empty, so will go with WiFi AP");
                    }
                } else {
                    Logger.debug("FTP: transporter is not connected.");
                    wiFiHelperListener.onError("FTP: transporter is not connected.");
                }
                return null;
            }
        });
    }

    @TargetApi(29)
    private void newApiWifiConnection(ConnectivityManager mConnectivityManager, WiFiHelperListener wiFiHelperListener) {
        //This is used to connect to WiFi AP on api >=29
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Watch.get().sendSimpleData(Transport.WIFI_FTP_ENABLE, Transport.FTP_ON_STATE_CHANGED, TransportService.TRANSPORT_FTP).continueWith(new Continuation<OtherData, Object>() {
                    @Override
                    public Object then(@NonNull Task<OtherData> task) throws Exception {
                        if (task.isSuccessful()) {
                            wiFiHelperListener.onWiFiReady(mConnectivityManager, networkCallback);
                        } else {
                            wiFiHelperListener.onError("WIFI_FTP_ENABLE error");
                        }
                        return null;
                    }
                });
                Logger.debug("FTP api29: watch's WiFi available");
                super.onAvailable(network);
                mConnectivityManager.bindProcessToNetwork(network);
            }

            @Override
            public void onUnavailable() {
                TransportService.sendWithTransporterFtp(Transport.WIFI_DISABLE_AP, null);
                wiFiHelperListener.onError("FTP api29: watch's WiFi Unavailable");
                super.onUnavailable();
            }
        };

        final NetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(SSID)
                .setWpa2Passphrase(pswd)
                .build();
        final NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        mConnectivityManager.requestNetwork(request, networkCallback);
    }
}
