package org.apache.cordova.serial;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.example.aaaaa.SerialPort;

import com.hc.pda.HcPowerCtrl;

/**
 * This class echoes a string called from JavaScript.
 */
public class Serial extends CordovaPlugin {
	// logging tag
	private final String TAG = Serial.class.getSimpleName();
	// actions definitions
	private static final String ACTION_OPEN = "openSerial";
	private static final String ACTION_READ = "readSerial";
	private static final String ACTION_WRITE = "writeSerial";
	private static final String ACTION_WRITE_HEX = "writeSerialHex";
	private static final String ACTION_CLOSE = "closeSerial";
	private static final String ACTION_READ_CALLBACK = "registerReadCallback";

	protected SerialPort mSerialPort = null;
    protected InputStream mInputStream = null;
	protected OutputStream mOutputStream = null;

	private HcPowerCtrl ctrl;
	
	// callback that will be used to send back data to the cordova app
	private CallbackContext readCallback;

	/**
	 * Overridden execute method
	 * @param action the string representation of the action to execute
	 * @param args
	 * @param callbackContext the cordova {@link CallbackContext}
	 * @return true if the action exists, false otherwise
	 * @throws JSONException if the args parsing fails
	 */
	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
		Log.d(TAG, "Action: " + action);
		JSONObject arg_object = args.optJSONObject(0);
		
		// open serial port
		if (ACTION_OPEN.equals(action)) {
			JSONObject opts = arg_object.has("opts")? arg_object.getJSONObject("opts") : new JSONObject();
			openSerial(opts, callbackContext);
			return true;
		}
		// write to the serial port
		else if (ACTION_WRITE.equals(action)) {
			String data = arg_object.getString("data");
			writeSerial(data, callbackContext);
			return true;
		}
		// write hex to the serial port
		else if (ACTION_WRITE_HEX.equals(action)) {
			String data = arg_object.getString("data");
			writeSerialHex(data, callbackContext);
			return true;
		}
		// close the serial port
		else if (ACTION_CLOSE.equals(action)) {
			closeSerial(callbackContext);
			return true;
		}
		// Register read callback
		else if (ACTION_READ_CALLBACK.equals(action)) {
			registerReadCallback(callbackContext);
			return true;
		}
		// the action doesn't exist
		return false;
	}

	/**
	 * 
	 */
	public static String toHexString(byte[] byteArray, int size) {
        final StringBuilder hexString = new StringBuilder("");
        if (byteArray == null || byteArray.length <= 0)
            return null;
        for (int i = 0; i < size; i++) {
            int v = byteArray[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                hexString.append(0);
            }
            hexString.append(hv);
        }
        return hexString.toString().toLowerCase();
	}
	
	/**
	 * 
	 */
	public static String byteToString(byte[] byteArray, int size) {
        byte[] array = new byte[size];
        if (byteArray == null || byteArray.length <= 0)
            return null;
        for (int i = 0; i < size; i++) {
            array[i] = byteArray[i];
        }
        return new String(array);
    }

	/**
     * 接受消息，启动线程
     */
    private void ReceiveThread() {
        cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				while (true) {
                    int size;
                    try {
						byte[] buffer = new byte[512];
						if (mInputStream == null) 
							return;
						size = mInputStream.read(buffer);
						if (size > 0) {
							String receiveMgs = toHexString(buffer, size);
							Log.d(TAG, "data: " + receiveMgs);
							
							if( readCallback != null ) {
								JSONObject returnObj = new JSONObject();
								addProperty(returnObj, "data", receiveMgs);
								PluginResult result = new PluginResult(PluginResult.Status.OK, returnObj);
								result.setKeepCallback(true);
								readCallback.sendPluginResult(result);
							}
						}
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
			}
		});
    }

	/**
	 * Open the serial port from Cordova
	 * @param opts a {@link JSONObject} containing the connection paramters
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void openSerial(final JSONObject opts, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					if(mSerialPort == null){
						String path = opts.has("path") ? opts.getString("path") : "/dev/ttysWK1";
						int baudrate = opts.has("baudRate") ? opts.getInt("baudRate") : 115200;
						mSerialPort = new SerialPort(new File(path), baudrate, 0);
						mInputStream = mSerialPort.getInputStream();
						mOutputStream = mSerialPort.getOutputStream();
						ReceiveThread();
                        
                        ctrl = new HcPowerCtrl();
						ctrl.identityPower(1);//上电
					}

					Log.d(TAG, "Serial port opened!");
					callbackContext.success("Serial port opened!");
				} 
				catch (SecurityException | IOException e) {
					e.printStackTrace();
					Log.e(TAG, "打开失败");
				}
				catch (JSONException e) {
					// deal with error
					Log.d(TAG, e.getMessage());
					callbackContext.error(e.getMessage());
				}
			}
		});
	}

	/**
	 * Write on the serial port
	 * @param data the {@link String} representation of the data to be written on the port
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void writeSerial(final String data, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				if (mOutputStream == null) {
					callbackContext.error("Writing a closed mOutputStream.");
				}
				else {
					try {
						Log.d(TAG, data);
						byte[] buffer = data.getBytes();
						mOutputStream.write(buffer);
						callbackContext.success();
					}
					catch (IOException e) {
						// deal with error
						Log.d(TAG, e.getMessage());
						callbackContext.error(e.getMessage());
					}
				}
			}
		});
	}

	/**
	 * Write hex on the serial port
	 * @param data the {@link String} representation of the data to be written on the port as hexadecimal string
	 *             e.g. "ff55aaeeef000233"
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void writeSerialHex(final String data, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				if (mOutputStream == null) {
					callbackContext.error("Writing a closed mOutputStream.");
				}
				else {
					try {
						Log.d(TAG, data);
						byte[] buffer = hexStringToByteArray(data);
						mOutputStream.write(buffer);
						callbackContext.success(" bytes written.");
					}
					catch (IOException e) {
						// deal with error
						Log.d(TAG, e.getMessage());
						callbackContext.error(e.getMessage());
					}
				}
			}
		});
	}

	/**
	 * Convert a given string of hexadecimal numbers
	 * into a byte[] array where every 2 hex chars get packed into
	 * a single byte.
	 *
	 * E.g. "ffaa55" results in a 3 byte long byte array
	 *
	 * @param s
	 * @return
	 */
	private byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	/**
	 * Close the serial port
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void closeSerial(final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				try {
					// Make sure we don't die if we try to close an non-existing port!
					if (mSerialPort != null)
						mSerialPort.close();

					if (mInputStream != null) {
						mInputStream.close();
					}
					if (mOutputStream != null) {
						mOutputStream.close();
					}

					mSerialPort = null;
					mInputStream = null;
					mOutputStream = null;

					ctrl.identityPower(0);//下电

					callbackContext.success();
				}
				catch (IOException e) {
					// deal with error
					Log.d(TAG, e.getMessage());
					callbackContext.error(e.getMessage());
				}
			}
		});
	}

	/**
	 * Dispatch read data to javascript
	 * @param data the array of bytes to dispatch
	 */
	private void updateReceivedData(byte[] data) {
		if( readCallback != null ) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, data);
			result.setKeepCallback(true);
			readCallback.sendPluginResult(result);
		}
	}

	/**
	 * Register callback for read data
	 * @param callbackContext the cordova {@link CallbackContext}
	 */
	private void registerReadCallback(final CallbackContext callbackContext) {
		Log.d(TAG, "Registering callback");
		cordova.getThreadPool().execute(new Runnable() {
			public void run() {
				Log.d(TAG, "Registering Read Callback");
				readCallback = callbackContext;
				JSONObject returnObj = new JSONObject();
				addProperty(returnObj, "registerReadCallback", "true");
				// Keep the callback
				PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
				pluginResult.setKeepCallback(true);
				callbackContext.sendPluginResult(pluginResult);
			}
		});
	}

	/**
	 * Utility method to add some properties to a {@link JSONObject}
	 * @param obj the json object where to add the new property
	 * @param key property key
	 * @param value value of the property
	 */
	private void addProperty(JSONObject obj, String key, Object value) {
		try {
			obj.put(key, value);
		}
		catch (JSONException e){}
	}
}
