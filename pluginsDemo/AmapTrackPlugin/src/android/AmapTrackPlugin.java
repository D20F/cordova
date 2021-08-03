package com.plugin.wly.amaptrack;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
//轨迹上报
import com.amap.api.track.AMapTrackClient;
import com.amap.api.track.ErrorCode;
import com.amap.api.track.OnTrackLifecycleListener;
import com.amap.api.track.TrackParam;
import com.amap.api.track.query.model.AddTerminalRequest;
import com.amap.api.track.query.model.AddTerminalResponse;
import com.amap.api.track.query.model.AddTrackRequest;
import com.amap.api.track.query.model.AddTrackResponse;
import com.amap.api.track.query.model.QueryTerminalRequest;
import com.amap.api.track.query.model.QueryTerminalResponse;
import com.amap.api.track.query.model.OnTrackListener;
//距离查询
import com.amap.api.track.query.entity.Point;
import com.amap.api.track.query.model.DistanceRequest;
import com.amap.api.track.query.model.DistanceResponse;
import com.amap.api.track.query.model.LatestPointRequest;
import com.amap.api.track.query.model.LatestPointResponse;
import com.amap.api.track.query.model.QueryTerminalRequest;
import com.amap.api.track.query.model.QueryTerminalResponse;
//轨迹查询
import com.amap.api.track.query.entity.DriveMode;
import com.amap.api.track.query.entity.HistoryTrack;
import com.amap.api.track.query.entity.Track;
import com.amap.api.track.query.model.HistoryTrackRequest;
import com.amap.api.track.query.model.HistoryTrackResponse;
import com.amap.api.track.query.model.QueryTrackRequest;
import com.amap.api.track.query.model.QueryTrackResponse;
//自定义
import com.amap.trackdemo.util.SimpleOnTrackLifecycleListener;
import com.amap.trackdemo.util.SimpleOnTrackListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.LinkedList;
import java.util.List;
import java.lang.*;

/**
 * 高德地图轨迹上报插件
 * https://lbs.amap.com/api/android-track/guide/guijishangbao/start-guijishangbao
 */
public class AmapTrackPlugin extends CordovaPlugin {

    private static final String TAG = "TrackServiceActivity";
    private static final String CHANNEL_ID_SERVICE_RUNNING = "CHANNEL_ID_SERVICE_RUNNING";

    private AMapTrackClient aMapTrackClient;
    private long terminalId = -1;        //amap终端唯一标识
    private long trackId = -1;
    private boolean isServiceRunning;
    private boolean isGatherRunning;
    private boolean uploadToTrack = false;

    private long startTime;  //查询开始时间
    private long endTime;    //查询结束时间
    /**
     * 终端名称，该名称可以根据使用方业务而定，比如可以是用户名、用户手机号等唯一标识
     *
     * 通常应该使用该名称查询对应终端id（terminalId），确定该终端是否存在，如果不存在则创建，然后就可以开启轨迹上报，将上报的轨迹关联到该终端
     */
    private String TERMINAL_NAME;

    /**
     * 服务id，请修改成您创建的服务的id
     *
     * 猎鹰轨迹服务，同一个开发者账号下的key可以直接使用同账号下的sid，不再需要人工绑定
     */
    public long SERVICE_ID = 28518;

    private CallbackContext callbackContext = null;
    private Context context;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
        r.setKeepCallback(true);
        callbackContext.sendPluginResult(r);

        if(action.equals("startTrack")){    //开始轨迹上报
            SERVICE_ID = args.getLong(0);
            TERMINAL_NAME = args.getString(1);
            uploadToTrack = args.getBoolean(2);
            trackId = args.getLong(3);
            this.Start(context);
            return true;
        }
        else if(action.equals("stopTrack")){    //停止轨迹上报
            this.Stop();
            return  true;
        }
        else if(action.equals("queryDistance")){
            SERVICE_ID = args.getLong(0);
            TERMINAL_NAME = args.getString(1);
            startTime = args.getLong(2);
            endTime = args.getLong(3);
            //long trid = args.getLong(2);
            this.queryDistance();
            return true;
        }
        else if(action.equals("queryHistoryPoint")){
            SERVICE_ID = args.getLong(0);
            TERMINAL_NAME = args.getString(1);
            startTime = args.getLong(2);
            endTime = args.getLong(3);
            this.queryHistoryPoint();
            return true;
        }
        else if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }
        return false;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        context = this.cordova.getActivity().getApplicationContext();
        super.initialize(cordova, webView);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceRunning) {
            aMapTrackClient.stopTrack(new TrackParam(SERVICE_ID, terminalId), new SimpleOnTrackLifecycleListener());
        }
    }

    private OnTrackLifecycleListener onTrackListener = new SimpleOnTrackLifecycleListener() {
        @Override
        public void onBindServiceCallback(int status, String msg) {
            //Log.w(TAG, "onBindServiceCallback, status: " + status + ", msg: " + msg);
        }

        @Override
        public void onStartTrackCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.START_TRACK_SUCEE || status == ErrorCode.TrackListen.START_TRACK_SUCEE_NO_NETWORK || status == ErrorCode.TrackListen.START_TRACK_ALREADY_STARTED) {
                // 成功启动 或已经启动
                isServiceRunning = true;
                //如果指定轨迹则加载轨迹id，并启动轨迹采集
                if(uploadToTrack) {
                    aMapTrackClient.setTrackId(trackId);
                }
                aMapTrackClient.startGather(this);
            }  else {
                callbackContext.error("error onStartTrackCallback, status: " + status + ", msg: " + msg);
            }
        }

        @Override
        public void onStopTrackCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.STOP_TRACK_SUCCE) {
                // 成功停止
                isServiceRunning = false;
                isGatherRunning = false;
                callbackContext.success(Long.toString(trackId));     //停止坐标采集成功后返回轨迹id标识
            } else {
                //2003：寻迹服务未启动
                if(status!=2003){
                    callbackContext.error("error onStopTrackCallback, status: " + status + ", msg: " + msg);
                }
            }
        }

        @Override
        public void onStartGatherCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.START_GATHER_SUCEE || status == ErrorCode.TrackListen.START_GATHER_ALREADY_STARTED) {
                isGatherRunning = true;
                callbackContext.success(Long.toString(trackId));     //开始坐标采集成功后返回高德轨迹id标识
            }  else {
                callbackContext.error("error onStartGatherCallback, status: " + status + ", msg: " + msg);
            }
        }

        @Override
        public void onStopGatherCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.STOP_GATHER_SUCCE) {
                isGatherRunning = false;
            } else {
                callbackContext.error("error onStopGatherCallback, status: " + status + ", msg: " + msg);
            }
        }
    };

    //开启轨迹上传
    private  void Start(Context context){
        if(aMapTrackClient == null){
            aMapTrackClient = new AMapTrackClient(context);
            aMapTrackClient.setInterval(5, 30);
        }

        //this.Stop();
        startTrack();
    }
    private  void Stop(){
        if(isGatherRunning){
            aMapTrackClient.stopGather(onTrackListener);
        }
        if(terminalId != -1){   //停止时terminalId要有值
            aMapTrackClient.stopTrack(new TrackParam(SERVICE_ID, terminalId), onTrackListener);
        }        
    }
    private void startTrack() {
        // 先根据Terminal名称查询Terminal ID，如果Terminal还不存在，就尝试创建，拿到Terminal ID后，
        // 用Terminal ID开启轨迹服务
        aMapTrackClient.queryTerminal(new QueryTerminalRequest(SERVICE_ID, TERMINAL_NAME), new SimpleOnTrackListener() {
            @Override
            public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {
                if (queryTerminalResponse.isSuccess()) {
                    if (queryTerminalResponse.isTerminalExist()) {
                        // 当前终端已经创建过，直接使用查询到的terminal id
                        terminalId = queryTerminalResponse.getTid();
                        if (uploadToTrack) {
                            if(trackId == -1){
                                aMapTrackClient.addTrack(new AddTrackRequest(SERVICE_ID, terminalId), new SimpleOnTrackListener() {
                                    @Override
                                    public void onAddTrackCallback(AddTrackResponse addTrackResponse) {
                                        if (addTrackResponse.isSuccess()) {
                                            // trackId需要在启动服务后设置才能生效，因此这里不设置，而是在startGather之前设置了track id
                                            trackId = addTrackResponse.getTrid();
                                            TrackParam trackParam = new TrackParam(SERVICE_ID, terminalId);
    //                                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    //                                            trackParam.setNotification(createNotification());
    //                                        }
                                            aMapTrackClient.startTrack(trackParam, onTrackListener);
                                        } else {
                                            callbackContext.error("网络请求失败，"+ addTrackResponse.getErrorMsg());
                                        }
                                    }
                                });
                            }else{
                                TrackParam trackParam = new TrackParam(SERVICE_ID, terminalId);
                                aMapTrackClient.startTrack(trackParam, onTrackListener);
                            }
                        } else {
                            // 不指定track id，上报的轨迹点是该终端的散点轨迹
                            TrackParam trackParam = new TrackParam(SERVICE_ID, terminalId);
//                            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                trackParam.setNotification(createNotification());
//                            }
                            aMapTrackClient.startTrack(trackParam, onTrackListener);
                        }
                    } else {
                        // 当前终端是新终端，还未创建过，创建该终端并使用新生成的terminal id
                        aMapTrackClient.addTerminal(new AddTerminalRequest(TERMINAL_NAME, SERVICE_ID), new SimpleOnTrackListener() {
                            @Override
                            public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {
                                if (addTerminalResponse.isSuccess()) {
                                    terminalId = addTerminalResponse.getTid();
                                    TrackParam trackParam = new TrackParam(SERVICE_ID, terminalId);
//                                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                        trackParam.setNotification(createNotification());
//                                    }
                                    aMapTrackClient.startTrack(trackParam, onTrackListener);
                                } else {
                                    callbackContext.error("网络请求失败，"+ addTerminalResponse.getErrorMsg());
                                }
                            }
                        });
                    }
                } else {
                    callbackContext.error("网络请求失败，"+ queryTerminalResponse.getErrorMsg());
                }
            }
        });
    }

    /**
     * 在8.0以上手机，如果app切到后台，系统会限制定位相关接口调用频率
     * 可以在启动轨迹上报服务时提供一个通知，这样Service启动时会使用该通知成为前台Service，可以避免此限制
     */
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//    private Notification createNotification() {
//        Notification.Builder builder;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_SERVICE_RUNNING, "app service", NotificationManager.IMPORTANCE_LOW);
//            nm.createNotificationChannel(channel);
//            builder = new Notification.Builder(getApplicationContext(), CHANNEL_ID_SERVICE_RUNNING);
//        } else {
//            builder = new Notification.Builder(getApplicationContext());
//        }
//        Intent nfIntent = new Intent(TrackServiceActivity.this, TrackServiceActivity.class);
//        nfIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        builder.setContentIntent(PendingIntent.getActivity(TrackServiceActivity.this, 0, nfIntent, 0))
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle("猎鹰sdk运行中")
//                .setContentText("猎鹰sdk运行中");
//        Notification notification = builder.build();
//        return notification;
//    }

    //查询行驶里程
    private  void queryDistance(){
        if(aMapTrackClient == null){
            aMapTrackClient = new AMapTrackClient(context);
            aMapTrackClient.setInterval(5, 30);
        }
        aMapTrackClient.queryTerminal(new QueryTerminalRequest(SERVICE_ID, TERMINAL_NAME), new SimpleOnTrackListener() {
            @Override
            public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {
                if (queryTerminalResponse.isSuccess()) {
                    long terminalId = queryTerminalResponse.getTid();
                    if (terminalId > 0) {
                        long curr = System.currentTimeMillis();
                        DistanceRequest distanceRequest = new DistanceRequest(
                                SERVICE_ID,
                                terminalId,
                                //curr - 12 * 60 * 60 * 1000, // 开始时间
                                //curr,   // 结束时间
                                startTime,
                                endTime,
                                -1  // 轨迹id，传-1表示包含散点在内的所有轨迹点
                        );
                        aMapTrackClient.queryDistance(distanceRequest, new SimpleOnTrackListener() {
                            @Override
                            public void onDistanceCallback(DistanceResponse distanceResponse) {
                                if (distanceResponse.isSuccess()) {
                                    //行驶里程查询（米）
                                    double distance = distanceResponse.getDistance();
                                    callbackContext.success((int)distance);
                                } else {
                                    callbackContext.error("error, msg: "+ distanceResponse.getErrorMsg());
                                }
                            }
                        });
                    } else {
                        callbackContext.error("error, msg: 终端不存在，请先创建终端和上报轨迹");
                    }
                } else {
                    callbackContext.error("error, msg: 终端查询失败，"+ queryTerminalResponse.getErrorMsg());
                }
            }
        });
    }

    //查询历史轨迹点
    private void queryHistoryPoint(){
        if(aMapTrackClient == null){
            aMapTrackClient = new AMapTrackClient(context);
            aMapTrackClient.setInterval(5, 30);
            //callbackContext.success(TERMINAL_NAME);
        }
        // else{
        //     callbackContext.error("已创建client");
        // }
        // 先查询terminal id，然后用terminal id查询轨迹
        // 查询符合条件的所有轨迹点，并绘制
        aMapTrackClient.queryTerminal(new QueryTerminalRequest(SERVICE_ID, TERMINAL_NAME), new SimpleOnTrackListener() {
            @Override
            public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {
                if (queryTerminalResponse.isSuccess()) {
                    if (queryTerminalResponse.isTerminalExist()) {
                        long tid = queryTerminalResponse.getTid();
                        // 搜索最近12小时以内上报的轨迹
                        HistoryTrackRequest historyTrackRequest = new HistoryTrackRequest(
                                SERVICE_ID,
                                tid,
                                //System.currentTimeMillis() - 12 * 60 * 60 * 1000,
                                //System.currentTimeMillis(),
                                startTime,
                                endTime,
                                0,      //不绑路
                                0,      //不做距离补偿
                                5000,   // 距离补偿，只有超过5km的点才启用距离补偿
                                0,  // 由旧到新排序
                                1,  // 返回第1页数据
                                100,    // 一页不超过100条
                                ""  // 暂未实现，该参数无意义，请留空
                        );
                        aMapTrackClient.queryHistoryTrack(historyTrackRequest, new SimpleOnTrackListener() {
                            @Override
                            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {
                                if (historyTrackResponse.isSuccess()) {
                                    HistoryTrack historyTrack = historyTrackResponse.getHistoryTrack();
                                    if (historyTrack == null || historyTrack.getCount() == 0) {
                                        callbackContext.error("未获取到轨迹点");
                                    }
                                    else{
                                        //String str = "[";
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("[");
                                        List<Point> points = historyTrack.getPoints();
                                        if (points.size() > 0) {
                                            for (int i = 0; i < points.size(); i++) {
                                                Point p = points.get(i);
                                                if(i==0){
                                                    sb.append("{\"lng\":").append(p.getLng()).append(",\"lat\":").append(p.getLat()).append(",\"time\":").append(p.getTime()).append("}");
                                                }
                                                else{
                                                    sb.append(",{\"lng\":").append(p.getLng()).append(",\"lat\":").append(p.getLat()).append(",\"time\":").append(p.getTime()).append("}");
                                                }
                                            }
                                        }
                                        sb.append("]");
                                        callbackContext.success(sb.toString());
                                    }

                                } else {
                                    callbackContext.error("查询历史轨迹点失败，" + historyTrackResponse.getErrorMsg());
                                }
                            }
                        });
                    } else {
                        callbackContext.error("error, msg: 终端不存在，请先创建终端和上报轨迹");
                    }
                } else {
                    callbackContext.error("网络请求失败，错误原因："+ queryTerminalResponse.getErrorMsg());
                }
            }
        });
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}
