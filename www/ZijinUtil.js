var exec = require('cordova/exec');

/**
 * 打开UHF设备
 * @param successCallback
 * @param errorCallback
 */
exports.openUHF = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, "ZijinUtil", "openUHF", []);
};

/**
 * 关闭UHF设备
 * @param successCallback
 * @param errorCallback
 */
exports.closeUHF = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, "ZijinUtil", "closeUHF", []);
};

/**
 * 开始盘点任务
 * @param successCallback
 * @param errorCallback
 */
exports.startInventoryReal = function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, "ZijinUtil", "startInventoryReal", []);
};

/**
 * 停止盘点任务
 */
exports.stopInventoryReal = function () {
    exec(null, null, "ZijinUtil", "stopInventoryReal", []);
};

/**
 * 设置设备读取功率
 * @param outputPower
 * @param successCallback
 * @param errorCallback
 */
exports.setOutputPower = function (outputPower, successCallback, errorCallback) {
    exec(successCallback, errorCallback, "ZijinUtil", "setOutputPower", [{outputPower: outputPower}]);
};

/**
 * 开启条码扫描
 * @param successCallback
 * @param errorCallback
 */
exports.openScanReceiver = function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'ZijinUtil', 'openScanReceiver', []);
}

/**
 * 关闭条码扫描
 */
exports.closeScanReceiver = function() {
    exec(null, null, 'ZijinUtil', 'closeScanReceiver', []);
}

