var exec = require('cordova/exec');

module.exports.detectMockLocation = function (success, error) {
    exec(success, error, 'CordovaDetectMockLocationPlugin', 'detectMockLocation', []);
};
