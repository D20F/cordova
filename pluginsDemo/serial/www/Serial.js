cordova.define("cordova-plugin-serial.Serial", function(require, exports, module) {
    var serial = {
        open: function(opts, successCallback, errorCallback) {
            cordova.exec(
                successCallback,
                errorCallback,
                'Serial',
                'openSerial',
                [{'opts': opts}]
            );
        },
        write: function(data, successCallback, errorCallback) {
            cordova.exec(
                successCallback,
                errorCallback,
                'Serial',
                'writeSerial',
                [{'data': data}]
            );
        },
        writeHex: function(hexString, successCallback, errorCallback) {
            cordova.exec(
                successCallback,
                errorCallback,
                'Serial',
                'writeSerialHex',
                [{'data': hexString}]
            );
        },
        close: function(successCallback, errorCallback) {
            cordova.exec(
                successCallback,
                errorCallback,
                'Serial',
                'closeSerial',
                []
            );
        },
        registerReadCallback: function(successCallback, errorCallback) {
            cordova.exec(
                successCallback,
                errorCallback,
                'Serial',
                'registerReadCallback',
                []
            );
        }
    
    };
    module.exports = serial;
    
    });
    