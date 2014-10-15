package com.qnoow.telephaty.Bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.util.Random;

import org.spongycastle.asn1.eac.PublicKeyDataObject;
import org.spongycastle.crypto.util.PublicKeyFactory;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.TextView;

import com.qnoow.telephaty.MainActivity;
import com.qnoow.telephaty.Msg;
import com.qnoow.telephaty.R;
import com.qnoow.telephaty.security.ECDH;
import com.qnoow.telephaty.security.Support;

/**
 * This thread runs during a connection with a remote device. It handles all incoming and outgoing transmissions.
 */
public class ConnectedThread extends Thread {

	// Member fields
	private final Bluetooth mService;
	private BluetoothSocket mSocket;
	private final InputStream mInStream;
	private final OutputStream mOutStream;
	private BluetoothDevice mRemoteDevice;
	private ECDH ecdh;
	private byte[] sharedKey;
	private boolean mWait;
	String TAG = "ConnectedThread";
	
	public ConnectedThread(Bluetooth service, BluetoothSocket socket, boolean wait) {
		if(Utilities.DEBUG)
			Log.d(TAG, "create ConnectedThread.");
		mService = service;
		mSocket = socket;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;
		mRemoteDevice = mSocket.getRemoteDevice();
		mWait = wait;
		if(Utilities.DEBUG)
			Log.d("DEBUGGING", "Entrando en Connectedthread");
		// Get the BluetoothSocket input and output streams
		try {
			tmpIn = socket.getInputStream();
			tmpOut = socket.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG, "temp sockets not created", e);
		}
		mInStream = tmpIn;
		mOutStream = tmpOut;
	}
    
	// The Main function to share everything 
	public void run() {
		if(Utilities.DEBUG)
			Log.i(TAG, "BEGIN mConnectedThread");
		try {
			ecdh = new ECDH();
			PublicKey pubKey = ecdh.getPubKey();
			PrivateKey privKey = ecdh.getPrivKey();
			ObjectOutputStream oos = new ObjectOutputStream(mSocket.getOutputStream());
			// to send the public key to the server
			oos.writeObject(pubKey);
		} catch (InvalidAlgorithmParameterException e1) {
			e1.printStackTrace();
		} catch (NoSuchProviderException e1) {
			e1.printStackTrace();
		} catch (InvalidKeySpecException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(Utilities.DEBUG)
			Log.d("DEBUGGING", "Antes de while != setECDH Connectedthread");
		// get the public key of the other part and calculate the shared key
		try {
			// Read from the InputStream
			if (mSocket.getInputStream() != null) {
				// Listening to receive a message
				ObjectInputStream ois = new ObjectInputStream(mSocket.getInputStream());
				Object line = ois.readObject();
				PublicKey pubk = (PublicKey) line;
				// The shared key is generated
				ecdh.setSharedKey(ecdh.Generate_Shared(pubk));
				sharedKey = ecdh.getSharedKey();
				Connection.sharedKey = sharedKey;
				// Send the obtained bytes to the UI Activity
				mService.getHandler().obtainMessage(Utilities.getMessageSharedKey(), sharedKey.length, -1, sharedKey).sendToTarget();
				mService.setState(Utilities.STATE_CONNECTED_ECDH_FINISH);
			}
		} catch (IOException e) {
			Log.e(TAG, "disconnected", e);
			mService.connectionLost();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		if (mWait) {
			if(Utilities.DEBUG)
				Log.d("DEBUGGING", "Antes de while true Connectedthread");
			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// Read from the InputStream
					if (mSocket.getInputStream() != null) {
						// Listening to receive a message
						ObjectInputStream ois = new ObjectInputStream(mSocket.getInputStream());
						Object line = ois.readObject();
						byte[] decryptedData = Support.decrypt(sharedKey, (byte[]) line);
						String receivedMsg = new String(decryptedData, "UTF-8");
						if (receivedMsg.substring(0, 1).equals("1")) {
							String msgId = receivedMsg.substring(1, 15);
							String jump = receivedMsg.substring(15, 16);
							byte[] originalMsg = receivedMsg.substring(16).getBytes();
							Utilities.lastMsg = new Msg(mService.getRemoteDevice().toString(), new String(originalMsg), new Timestamp(new java.util.Date().getTime()));
							// Send the obtained bytes to the UI Activity
							mService.getHandler().obtainMessage(Utilities.getMessageRead(), originalMsg.length, -1, originalMsg).sendToTarget();
							mService.stop();
							if (Integer.parseInt(jump) > 1 && !Connection.BBDDmensajes.search(msgId)) {
								Connection.BBDDmensajes.insert(msgId, mSocket.getRemoteDevice().toString());
								Utilities.identifier = msgId;
								Connection.difussion = true;
								Utilities.jump = Integer.toString(Integer.parseInt(jump) - 1);
								Utilities.message = receivedMsg.substring(16); 
								Connection.mAdapter.startDiscovery();
							} else {
								Connection.difussion = false;
							}
						}
						else if (receivedMsg.substring(0, 1).equals("2")) {
							String msgId = receivedMsg.substring(1, 15);
							String jump = receivedMsg.substring(15, 16);
							String mac = receivedMsg.substring(16, 33);
							byte[] originalMsg = receivedMsg.substring(33).getBytes();
							Utilities.lastMsg = new Msg(mService.getRemoteDevice().toString(), new String(originalMsg), new Timestamp(new java.util.Date().getTime()));
							if (mService.getAdapter().getAddress().toString().equals(mac)){
								// Send the obtained bytes to the UI Activity
								mService.getHandler().obtainMessage(Utilities.getMessageRead(), originalMsg.length, -1, originalMsg).sendToTarget();
								mService.stop();
							}
							else if (Integer.parseInt(jump) > 1 && !Connection.BBDDmensajes.search(msgId) && !mService.getAdapter().getAddress().toString().equals(mac)) {
								mService.stop();
								Connection.BBDDmensajes.insert(msgId, mSocket.getRemoteDevice().toString());
								Utilities.identifier = msgId;
								Connection.difussion = true;
								Connection.privates = true;
								Utilities.jump = Integer.toString(Integer.parseInt(jump) - 1);
								Utilities.message = receivedMsg.substring(33); 
								Connection.mAdapter.startDiscovery();
							} else {
								mService.stop();
								Connection.difussion = false;
								Connection.privates = false;
							}
						}
						else {
							byte[] originalMsg = receivedMsg.substring(1).getBytes();
							// Send the obtained bytes to the UI Activity
							mService.getHandler().obtainMessage(Utilities.getMessageRead(), originalMsg.length, -1, originalMsg).sendToTarget();
						}
					}
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					mService.connectionLost();
					break;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(Utilities.DEBUG)
				Log.d("DEBUGGING", "Saliendo de while true Connectedthread");
		} else {
			if(Utilities.DEBUG)
				Log.d("ConnectedThread", "mWait is false");
		}
	}

	// Write to the connected OutStream.
	// Buffer has the bytes to write
	public void write(byte[] buffer, boolean diffusion) {
		if(Utilities.DEBUG)
			Log.d("DEBUGGING", "En funci�n write Connectedthread");
		
		try {
			String msg = new String(buffer, "UTF-8");
			if (diffusion && !Connection.privates) {
				if (Utilities.jump.equals(Connection.MAXJUMP)) {
					Utilities.message = msg;
					Connection.BBDDmensajes.insert(Utilities.identifier, BluetoothAdapter.getDefaultAdapter().getAddress());
				}
				msg = Utilities.difusion.concat(Utilities.identifier).concat(Utilities.jump).concat(Utilities.message);

			} else if (diffusion && Connection.privates) {
				if (Utilities.jump.equals(Connection.MAXJUMP)) {
					Utilities.message = msg;
					Connection.BBDDmensajes.insert(Utilities.identifier, BluetoothAdapter.getDefaultAdapter().getAddress());
				}
				msg = Utilities.privates.concat(Utilities.identifier).concat(Utilities.jump).concat(Utilities.receiverMac).concat(Utilities.message);

			} else {
				msg = "0".concat(msg);
			}
			byte[] encryptedData = Support.encrypt(sharedKey, msg.getBytes());
			ObjectOutputStream oos = new ObjectOutputStream(mSocket.getOutputStream());
			oos.writeObject(encryptedData);
			// Share the sent message back to the UI Activity
			Utilities.lastMsg = new Msg("me", new String(buffer), new Timestamp(new java.util.Date().getTime()));
			Utilities.progressDialog.dismiss();
			mService.getHandler().obtainMessage(Utilities.getMessageWrite(), -1, -1, encryptedData).sendToTarget();
			
			if (diffusion == true && (mSocket.getInputStream() != null)) {
				try {
					ObjectInputStream ois = new ObjectInputStream(mSocket.getInputStream());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			Log.e(TAG, "disconnected", e);
			mService.connectionLost();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cancel() {
		try {
			mSocket.close();
		} catch (IOException e) {
			Log.e(TAG, "close() of connect socket failed", e);
		}
	}

	public BluetoothDevice getRemoteDevice() {
		return mRemoteDevice;
	}
}