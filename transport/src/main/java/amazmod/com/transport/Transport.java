package amazmod.com.transport;

public class Transport {

    public static final int RESULT_OK = 1;
    public static final int RESULT_PERMISSION_DENIED = 2;
    public static final int RESULT_NOT_FOUND = 3;
    public static final int RESULT_UNKNOW_ERROR = 4;

    public static final String NAME = "com.edotassi.amazmod";
    public static final String NAME_BATTERY = "com.edotassi.amazmod.battery";
    public static final String NAME_NOTIFICATION = "com.edotassi.amazmod.notification";
    public static final String NAME_INTERNET = "com.kieronquinn.app.amazfitinternetcompanion";
    public static final String NAME_SLEEP = "com.amazmod.sleep";

    public static final String NAME_HUAMI_NOTIFICATION = "com.huami.action.notification";
    public static final String NAME_XDRIP = "com.eveningoutpost.dexdrip.wearintegration";

    public static final String INCOMING_NOTIFICATION = "incoming_notification";
    public static final String DELETE_NOTIFICATION = "del";
    public static final String REQUEST_WATCHSTATUS = "request_watchstatus";
    public static final String REQUEST_BATTERYSTATUS = "request_batterystatus";
    public static final String REQUEST_DIRECTORY = "request_directory";
    public static final String REQUEST_DELETE_FILE = "request_delete_file";
    public static final String REQUEST_UPLOAD_FILE_CHUNK = "request_upload_file_chunk";
    public static final String REQUEST_DOWNLOAD_FILE_CHUNK = "request_download_file_chunk";
    public static final String REQUEST_SHELL_COMMAND = "request_shell_command";
    public static final String SYNC_SETTINGS = "sync_settings";
    public static final String BRIGHTNESS = "brightness";
    public static final String ENABLE_LOW_POWER = "enable_low_power";
    public static final String REVOKE_ADMIN_OWNER = "revoke_admin_owner";
    public static final String WATCHFACE_DATA = "watchface_data";
    public static final String REQUEST_WIDGETS = "request_widgets";
    public static final String XDRIP_DATA = "xDrip_synced_SGV_data";
    public static final String SLEEP_DATA = "sleepdata";
    public static final String TAKE_PICTURE = "take_pict";

    public static final String WATCH_STATUS = "watch_status";
    public static final String BATTERY_STATUS = "battery_status";
    public static final String WIDGETS_DATA = "request_widgets";
    public static final String DIRECTORY = "directory";
    public static final String REPLY = "reply";
    public static final String ACTION = "action";
    public static final String INTENT = "intent";
    public static final String TOGGLE_MUSIC = "toggle_music";
    public static final String NEXT_MUSIC = "next_music";
    public static final String PREV_MUSIC = "prev_music";
    public static final String VOL_UP = "vol_up";
    public static final String VOL_DOWN = "vol_down";
    public static final String VOL_MUTE = "vol_mute";
    public static final String RESULT_DELETE_FILE = "result_delete_file";
    public static final String RESULT_DOWNLOAD_FILE_CHUNK = "result_download_file_chunk";
    public static final String RESULT_SHELL_COMMAND = "result_shell_command";
    public static final String FILE_UPLOAD = "file_upload";
    public static final String SILENCE = "silence";
    public static final String LOCAL_IP = "request_local_IP";
    public static final String FTP_ON_STATE_CHANGED = "ftp_on_state_changed";
    public static final String ON_AP_ENABLE_RESULT = "on_ap_enable_result";
    public static final String ON_AP_STATE_CHANGED = "on_ap_state_changed";

    // Official API actions
    public static final String OFFICIAL_REQUEST_DEVICE_INFO = "com.huami.watch.companion.transport.RequestDeviceInfo";
    public static final String OFFICIAL_REPLY_DEVICE_INFO = "com.huami.watch.companion.transport.SyncDeviceInfo";
    public static final String OFFICIAL_SYNC_BATTERY = "com.huami.watch.companion.transport.SyncBattery";

    public static final String HTTP_REQUEST = "com.huami.watch.companion.transport.amazfitcommunication.HTTP_REQUEST";
    public static final String HTTP_PINGBACK = "com.huami.watch.companion.transport.amazfitcommunication.HTTP_PINGBACK";
    public static final String HTTP_RESULT = "com.huami.watch.companion.transport.amazfitcommunication.HTTP_RESULT";


    public static final String WIFI_FTP_ENABLE = "enable_ftp";
    public static final String WIFI_FTP_DISABLE = "disable_ftp";
    public static final String WIFI_ENABLE_AP = "enable_ap";
    public static final String WIFI_DISABLE_AP = "disable_ap";
    public static final String WIFI_START_SERVICE = "start_service";
    public static final String WIFI_SECURITY_MODE = "key_keymgmt";
    public static final String WIFI_SSID = "key_ssid";
    public static final String WIFI_PASSWORD = "key_pswd";
    public static final int WIFI_WPA2 = 4;
}
