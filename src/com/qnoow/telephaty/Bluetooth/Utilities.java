package com.qnoow.telephaty.Bluetooth;

import java.util.UUID;

public class Utilities {

	// Debugging
    private static final String TAG = "QnoowBluetoothConnection";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = TAG;

    // Unique UUID for this application
    private static final UUID MY_UUID =  UUID.fromString(TAG);
    
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

   

}