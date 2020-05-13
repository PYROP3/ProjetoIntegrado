// Server data
module.exports.SERVER_PORT = 8080
module.exports.SERVER_URL = "http://localhost:8080"

// Requests
module.exports.QUALITY_OVERLAY_REQUEST = "/qualityOverlay";
module.exports.LOG_TRIP_REQUEST = "/logTrip";
module.exports.CREATE_ACCOUNT_REQUEST = "/createAccount"
module.exports.VERIFY_ACCOUNT_REQUEST = "/verifyAccount";

// Executables
module.exports.PYTHON_BIN = "python"

// Scripts
module.exports.SCRIPT_PATH = "script/"
module.exports.SCRIPT_SLICE_OVERLAY = module.exports.SCRIPT_PATH + "sliceOverlay.py"
module.exports.SCRIPT_LOG_TRIP      = module.exports.SCRIPT_PATH + "logTrip.py"

// Log storage
module.exports.LOG_STORAGE_PATH = "log/"

// Error data
module.exports.SCRIPT_ERRORS_PATH   = module.exports.SCRIPT_PATH + "errorCodes.json"

// Localization defaults
module.exports.DEFAULT_LOCALE = "English"

// Mailer data
module.exports.SOURCE_EMAIL_ADDRESS = "streetAnalyzer@gmail.com"
module.exports.SOURCE_EMAIL_SERVICE = "gmail"
module.exports.SOURCE_EMAIL_HOST    = "smtp.gmail.com"

// module.exports = {
//     QUALITY_OVERLAY_REQUEST: QUALITY_OVERLAY_REQUEST,
//     LOG_TRIP_REQUEST:        LOG_TRIP_REQUEST,
//     PYTHON_BIN:              PYTHON_BIN,
//     SCRIPT_ERRORS_PATH:      SCRIPT_ERRORS_PATH,
//     SCRIPT_SLICE_OVERLAY:    SCRIPT_SLICE_OVERLAY,
//     SCRIPT_LOG_TRIP:         SCRIPT_LOG_TRIP,
//     LOG_STORAGE_PATH:        LOG_STORAGE_PATH,
//     DEFAULT_LOCALE:          DEFAULT_LOCALE
// }