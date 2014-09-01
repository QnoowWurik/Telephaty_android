package com.qnoow.telephaty.Bluetooth;

import java.io.IOException;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * This thread runs while listening for incoming connections. It behaves like a
 * server-side client. It runs until a connection is accepted (or until
 * cancelled).
 */
public class AcceptThread extends Thread {

	// Member fields
	private final Bluetooth mService;
	// The local server socket
	private final BluetoothServerSocket mServerSocket;
	private String mSocketType;

	public AcceptThread(Bluetooth service, boolean secure) {
		mService = service;
		BluetoothServerSocket tmp = null;
		mSocketType = secure ? "Secure" : "Insecure";

		// Create a new listening server socket
		try {
			if (secure) {
				tmp = mService.getAdapter().listenUsingRfcommWithServiceRecord(
						Utilities.NAME_SECURE, Utilities.MY_UUID_SECURE);
			} else {
				tmp = mService.getAdapter()
						.listenUsingInsecureRfcommWithServiceRecord(
								Utilities.NAME_INSECURE,
								Utilities.MY_UUID_INSECURE);
			}
		} catch (IOException e) {
			Log.e(Utilities.TAG, "Socket Type: " + mSocketType
					+ "listen() failed", e);
		}
		mServerSocket = tmp;
	}

	public void run() {
		if (Utilities.D)
			Log.d(Utilities.TAG, "Socket Type: " + mSocketType
					+ "BEGIN mAcceptThread" + this);
		setName("AcceptThread" + mSocketType);

		BluetoothSocket socket = null;

		// Listen to the server socket if we're not connected
		while (mService.getState() != Utilities.STATE_CONNECTED && mService.getState() != Utilities.STATE_CONNECTED_ECDH_FINISH) {
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				socket = mServerSocket.accept();
			} catch (IOException e) {
				Log.e(Utilities.TAG, "Socket Type: " + mSocketType
						+ "accept() failed", e);
				break;
			}

			// If a connection was accepted
			if (socket != null) {
				synchronized (mService) {
					switch (mService.getState()) {
					case Utilities.STATE_LISTEN:
					case Utilities.STATE_CONNECTING:
						// Situation normal. Start the connected thread.
						mService.connected(socket, socket.getRemoteDevice(),
								true);
						break;
					case Utilities.STATE_NONE:
					case Utilities.STATE_CONNECTED:
						// Either not ready or already connected. Terminate new
						// socket.
						try {
							socket.close();
						} catch (IOException e) {
							Log.e(Utilities.TAG,
									"Could not close unwanted socket", e);
						}
						break;

					case Utilities.STATE_CONNECTED_ECDH_FINISH:
						// Either not ready or already connected. Terminate new
						// socket.
						try {
							socket.close();
						} catch (IOException e) {
							Log.e(Utilities.TAG,
									"Could not close unwanted socket", e);
						}
						break;
					}
				}
			}
		}
		if (Utilities.D)
			Log.i(Utilities.TAG, "END mAcceptThread, socket Type: "
					+ mSocketType);

	}

	public void cancel() {
		if (Utilities.D)
			Log.d(Utilities.TAG, "Socket Type" + mSocketType + "cancel " + this);
		try {
			mServerSocket.close();
		} catch (IOException e) {
			Log.e(Utilities.TAG, "Socket Type" + mSocketType
					+ "close() of server failed", e);
		}
	}
}
