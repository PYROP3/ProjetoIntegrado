const express = require('express');
const app = express();
const Constants = require("./util/Constants");
const {spawn} = require('child_process');
const fs = require('fs');
const winston = require('winston');

const path = require('path');

// JSON via post
const bodyParser = require('body-parser');
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));

// Cookies
function parseCookies (request) {
    var list = {},
        rc = request.headers.cookie;

    rc && rc.split(';').forEach(function( cookie ) {
        var parts = cookie.split('=');
        list[parts.shift().trim()] = decodeURI(parts.join('='));
    });

    return list;
}

// Static pages
//app.use(express.static('htmls'));

function fetchFile(filename) { return path.join(__dirname + "/" + filename); }

// Winston config
const logger = winston.createLogger({
    level: 'debug',
    format: winston.format.json(),
    defaultMeta: { service: 'user-service' },
    transports: [
      //
      // - Write to all logs with level `info` and below to `combined.log`
      // - Write all logs error (and below) to `error.log`.
      //
      new winston.transports.File({ filename: fetchFile(Constants.LOG_STORAGE_PATH + 'error.log'), level: 'error' }),
      new winston.transports.File({ filename: fetchFile(Constants.LOG_STORAGE_PATH + 'combined.log') })
    ]
  });

  //
  // If we're not in production then log to the `console` with the format:
  // `${info.level}: ${info.message} JSON.stringify({ ...rest }) `
  //
  if (process.env.NODE_ENV !== 'production') {
    logger.add(new winston.transports.Console({
      format: winston.format.simple()
    }));
  }

// Error handling
function sendErrorMessage(code, request, response) {
    let rawdata = fs.readFileSync(fetchFile(Constants.SCRIPT_ERRORS_PATH));
    let error = JSON.parse(rawdata)[code];
    let errorData = error["Data"][request.header("Locale") != null ? request.header("Locale") : Constants.DEFAULT_LOCALE];
    let thisErr = {
        "Error": errorData["PrettyName"],
        "Description": errorData["Description"],
    }
    response.status(error["HttpReturn"]).header("Content-Type", "application/json").send(JSON.stringify(thisErr));
}

// =================================== Requests ===================================

app.get(Constants.QUALITY_OVERLAY_REQUEST, function(req, res) {
    var query = req.query;

    logger.info("[Server][qualityOverlay] Overlay requested from ("+query.minLatitude+","+query.minLongitude+") to ("+query.maxLatitude+","+query.maxLongitude+")");

    const python = spawn(
        Constants.PYTHON_BIN,
        [
            fetchFile(Constants.SCRIPT_SLICE_OVERLAY),
            parseFloat(query.minLongitude), // x_min
            parseFloat(query.minLatitude),  // y_min
            parseFloat(query.maxLongitude), // x_max
            parseFloat(query.maxLatitude),  // y_max
            "--overlay_folder",
            fetchFile("/overlay/"),         // overlay_folder
            "--errors_file",
            fetchFile(Constants.SCRIPT_ERRORS_PATH),
            //"--DEBUG"
        ]
    );

    var overlayNonce = ""

    // Collect data from script
    python.stdout.on('data', function (data) {
        logger.debug('[Server][qualityOverlay][python/stdout] : ' + data);
        overlayNonce += data.toString();
    });

    // Collect error data from script (for debugging)
    python.stderr.on('data', function (data) {
        logger.error('[Server][qualityOverlay][python/stderr] :' + data);
    });

    // Send status of operation to user on close
    python.on('close', (code) => {
        logger.debug(`[Server][qualityOverlay] Script exit code : ${code}`);

        if (code != 0) {
            sendErrorMessage(code, req, res);
            return;
        }

        res.set('Content-Type', 'image/jpeg');

        // Get file using nonce from script
        const path = fetchFile("/tmp/"+overlayNonce+".jpg");

        // Send customized overlay
        res.sendFile(path, (err) => {
            // Remove tmp file after send
            fs.unlink(path, (err) => {
                if (err) {
                    console.error(err);
                    return;
                }
            });
        });
    });
});


app.post(Constants.LOG_TRIP_REQUEST, function(req, res){
    var data = req.body;

    logger.info("[Server][logTrip] Trip log requested")

    logger.debug("[Server][logTrip] Coordinates : " + JSON.stringify(data["pontos"]))
    logger.debug("[Server][logTrip] Accel data  : " + JSON.stringify(data["dados"]))

    let py_args = [
        fetchFile(Constants.SCRIPT_LOG_TRIP),
        "--coordinates"    , data["pontos"].map(coord => coord.join(",")).join(" "),
        "--overlay_folder" , fetchFile("/overlay/"),
        "--errors_file"    , fetchFile(Constants.SCRIPT_ERRORS_PATH),
        //"--DEBUG"
    ]

    py_args = py_args.concat([].concat.apply([], data["dados"].map(line => ["--accel_data", line.map(data => data.join(",")).join(" ")])))

    logger.debug("[Server][logTrip][debug] py_args = " + py_args)
    const python = spawn(
        Constants.PYTHON_BIN,
        py_args
    );

    var pythonData = ""

    // Collect data from script
    python.stdout.on('data', function (data) {
        logger.debug('[Server] Pipe data from python script : ' + data);
        pythonData += data.toString();
    });

    // Collect error data from script (for debugging)
    python.stderr.on('data', function (data) {
        logger.error('[Server][python/stderr] :' + data);
    });

    // Send status of operation to user on close
    python.on('close', (code) => {
        logger.debug(`[Server] Script exit code : ${code}`);

        if (code != 0) {
            sendErrorMessage(code, req, res);
            return;
        }

        res.send("Obrigado pela contribuição, " + data["usuario"] + "!")
    });

});

// =================================== End page require ===================================

// Listen on port
let port = process.env.PORT;
if (port == undefined) port = 8080;

app.listen(port);
logger.info("[Server] Listening on port " + port);

// var rawdata = fs.readFileSync(fetchFile(Constants.SCRIPT_ERRORS_PATH));
// var error = JSON.parse(rawdata)[code];
// var thisErr = {
//     "Error": error["PrettyName"],
//     "Description": error["Description"],
// }
