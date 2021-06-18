var mixin = {
    data() {
        return {
        }
    },
    created() {
        this.initSerial();
    },
    destroyed() {
        // 停止多次读取
        this.writeHex('BB00280000287E');
    },
    methods: {
        initSerial() {
            var that = this;
            if (typeof (Serial) != 'undefined') {
                // 打开串口
                Serial.open(
                    {},
                    // 串口已被打开
                    function () {
                        // 注册一个read回调
                        Serial.registerReadCallback(
                            function success(res) {
                                if(Object.prototype.hasOwnProperty.call(res, 'data')){
                                    // 帧类型Type:         0x02
                                    // 指令代码 Command:    0x27
                                    // 指令参数长度PL:      0x0011
                                    // RSSI:               0xC9
                                    // PC:                 0x3400
                                    // EPC:                0x30751FEB705C5904E3D50D70
                                    // CRC :               0x3A76
                                    // 校验位Checksum:      0xEF
                                    console.log('result:', res.data);
                                }
                            },
                            //read错误回调
                            function (err) {
                                console.error(err);
                            }
                        );

                        // 发送多次读取
                        that.writeHex('BB00270003222710837E');
                    },
                    // 打开串口失败的回调
                    function (err) {
                        console.error(err);
                    }
                );
            }
        },
        writeHex(cmd){
            if(typeof (Serial) != "undefined"
            && typeof (Serial.writeHex) != "undefined"){
                Serial.writeHex(cmd);
            }
        },
    },
    computed: {},
}
export default mixin;



