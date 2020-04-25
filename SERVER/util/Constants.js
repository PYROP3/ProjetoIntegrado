// Requests
const QUALITY_OVERLAY_REQUEST = "/qualityOverlay";
const LOG_TRIP_REQUEST = "/logTrip";

// Executables
const PYTHON_BIN = "python"

// Scripts
const SCRIPT_PATH = "script/"
const SCRIPT_SLICE_OVERLAY = SCRIPT_PATH + "sliceOverlay.py"
const SCRIPT_LOG_TRIP      = SCRIPT_PATH + "logTrip.py"

// Error data
const SCRIPT_ERRORS_PATH   = SCRIPT_PATH + "errorCodes.json"

module.exports = {
    QUALITY_OVERLAY_REQUEST: QUALITY_OVERLAY_REQUEST,
    LOG_TRIP_REQUEST:        LOG_TRIP_REQUEST,
    PYTHON_BIN:              PYTHON_BIN,
    SCRIPT_ERRORS_PATH:      SCRIPT_ERRORS_PATH,
    SCRIPT_SLICE_OVERLAY:    SCRIPT_SLICE_OVERLAY,
    SCRIPT_LOG_TRIP:         SCRIPT_LOG_TRIP
}