// Requests
const QUALITY_OVERLAY_REQUEST = "/qualityOverlay";
const LOG_TRIP_REQUEST = "/logTrip";

// Executables
const PYTHON_BIN = "python"

// Scripts
const SCRIPT_PATH = "script/"
const SCRIPT_SLICE_OVERLAY = SCRIPT_PATH + "sliceOverlay.py"
const SCRIPT_LOG_TRIP      = SCRIPT_PATH + "logTrip.py"

// Log storage
const LOG_STORAGE_PATH = "log/"

module.exports = {
    QUALITY_OVERLAY_REQUEST: QUALITY_OVERLAY_REQUEST,
    LOG_TRIP_REQUEST:        LOG_TRIP_REQUEST,
    PYTHON_BIN:              PYTHON_BIN,
    SCRIPT_SLICE_OVERLAY:    SCRIPT_SLICE_OVERLAY,
    SCRIPT_LOG_TRIP:         SCRIPT_LOG_TRIP,
    LOG_STORAGE_PATH:        LOG_STORAGE_PATH
}