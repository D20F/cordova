var exec = require('cordova/exec');






//showToast为安装后js调用的方法名，参数可以为任意多个
exports.showToast = function (arg0, success, error) {

    // success error 为成功 失败回调
   // ToastDemo 为 plugin.xml 中配置的 feature 的name名
  //"showToast"为给ToastDemo.java判断的action名
    exec(success, error, 'ToastDemo', 'showToast', [arg0]);
};
