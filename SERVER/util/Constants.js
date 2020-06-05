// Requests
// -- API
module.exports.QUALITY_OVERLAY_REQUEST = "/qualityOverlay";
module.exports.LOG_TRIP_REQUEST = "/logTrip";
// -- Account
module.exports.CREATE_ACCOUNT_REQUEST = "/createAccount";
module.exports.VERIFY_ACCOUNT_REQUEST = "/verifyAccount";
module.exports.AUTH_REQUEST           = "/auth";
module.exports.DEAUTH_REQUEST         = "/deauth";

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

// Mongo keys
module.exports.USER_PRIMARY_KEY  = "email"
module.exports.USER_PASSWORD_KEY = "password"
module.exports.AUTH_TOKEN_KEY    = "authToken"
module.exports.TIMESTAMP_KEY     = "timestamp"

// Authentication info
module.exports.AUTH_TOKEN_LENGTH = 64
module.exports.AUTH_TOKEN_TYPE = "Bearer"
module.exports.AUTH_TOKEN_NAME = module.exports.AUTH_TOKEN_TYPE + " "

module.exports.SERVER_PORT_DEFAULT = 8080
module.exports.SERVER_URL_DEFAULT = "http://localhost:"+module.exports.SERVER_PORT_DEFAULT