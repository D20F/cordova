// package cordova-plugin-toastdemo;   包名
package org.apache.cordova.toastdemo;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.widget.Toast;



// 类名需要和文件名一致
public class ToastDemo extends CordovaPlugin {

    // args 参数数组  CallbackContext 回调
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        // 判断是那种事件 
        if (action.equals("showToast")) {
            // 获取到第一位参数
            String message = args.getString(0);
           
            // 调用弹窗函数
            this.showToast(message, callbackContext);
            return true;
        }
        return false;
    }

    // 弹窗 
    private void showToast(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            // cordova.getContext() 获取到包名  message 信息   Toast.LENGTH_SHORT可能是位置吧
            Toast.makeText(cordova.getContext(),message,Toast.LENGTH_SHORT).show();//

            // 成功回调
            callbackContext.success(message);
        } else {
            // 错误回调
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}

