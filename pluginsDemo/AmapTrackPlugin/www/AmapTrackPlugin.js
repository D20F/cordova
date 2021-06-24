var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'AmapTrackPlugin', 'coolMethod', [arg0]);
};

exports.startTrack = function (arg0,arg1,arg2, success, error) {
    exec(success, error, 'AmapTrackPlugin', 'startTrack', [arg0,arg1,arg2]);
};

exports.stopTrack = function ( success, error) {
    exec(success, error, 'AmapTrackPlugin', 'stopTrack', []);
};

exports.queryDistance = function (arg0,arg1,arg2,arg3, success, error) {
    exec(success, error, 'AmapTrackPlugin', 'queryDistance', [arg0,arg1,arg2,arg3]);
};

exports.queryHistoryPoint = function (arg0,arg1,arg2,arg3, success, error) {
    exec(success, error, 'AmapTrackPlugin', 'queryHistoryPoint', [arg0,arg1,arg2,arg3]);
};