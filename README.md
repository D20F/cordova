# 创建项目
创建hello 文件夹 项目名为 HelloWorld
cordova create hello com.example.hello HelloWorld

# 添加两个平台
cordova platform add ios --save
cordova platform add android --save

# 检查你当前平台设置状况:
$ cordova platform ls

# android  环境
Java JDK
Android SDK
Android target
Gradle

# ios 环境
暂不考虑

# 打包安卓
cordova build android

# 白屏
查看 vue 打包路径问题

# apk 图标制作
https://icon.wuruihong.com/


# 插件
启动页   cordova plugin add cordova-plugin-splashscreen
相机     cordova plugin add cordova-plugin-camera

## 删除插件
cordova plugin remove 插件名
cordova plugin remove cordova-plugin-socketdemo

# 自定义插件 plugman
1. 全局安装好插件管理工具 npm install -g plugman
plugman create --name [插件名] --plugin_id [插件id] --plugin_version [插件版本]
2. 记得进去 plugins 目录在创建
3. $ plugman create --name ToastDemo --plugin_id cordova-plugin-toastdemo --plugin_version 1.0.0
4. 进入插件目录，添加插件支持的平台环境
    $ cd ToastDemo
    $ plugman platform add --platform_name android
    $ plugman platform add --platform_name ios
5. 增加代码
6. 进入插件目录 ToastDemo npm init 初始化 packjson
7. 给 Cordova 项目添加 android 环境
$ cd cordova_unpack
安装插件我们刚才写的插件
$ cordova plugin add C:\Users\Administrator\Desktop\test\cordova_unpack\plugins\ToastDemo 

8. js调用
ToastDemo.showToast(“这是Toast内容”);

## cordova 插件参数中文乱码
在打包的index.html 加入头解决乱码问题 <meta http-equiv=“Content-Type” content=“text/html; charset=utf-8” />

## 使用自制插件白屏了 记得去看打包cordova插件内的js 是不是多被注册一行 

# 目录
    ├─ hook                 介绍
    ├─ node_modules         包     
    ├─ platforms            安卓和ios的打包依赖 
    ├─ plugins              插件设置     
    ├─ start_icon           启动icon     
    ├─ config.xml           设置文件     
    ├─ www                  前端打包文件     
    └─              
# 使用步骤
1. 先创建一个 空文件夹名为 platforms
2. cordova platform ls  查看平台是否添加
3. cordova platform add ios --save cordova platform add android --save 添加两个平台
4. cordova requirements 查看是否安装完全的环境 Java JDK Android SDK Android target Gradle
5. 查看 config.xml 是否要修改 之后就可以打包了

## cordova 默认https 想用http需要配置

## 设置可以通过http请求  好像不能照片了 覆盖了
<edit-config file="AndroidManifest.xml" mode="merge" target="/manifest/application">
    <activity android:usesCleartextTraffic="true" />
</edit-config>
## 安卓启动模式
<preference name="AndroidLaunchMode" value="standard"/>


cordova plugin add C:\Users\Administrator\Desktop\test\cordova\pluginsDemo\cordova-amap-location --variable ANDROID_KEY=a8722b4eb3823426c6e1cc9e6e5a340d --variable IOS_KEY=7a39624200180ee16988655ecbba9d59
cordova plugin remove cordova-amap-location
cordova plugin add C:\Users\Administrator\Desktop\test\cordova\pluginsDemo\AmapTrackPlugin