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
            //"--DEBUG"
        ]
    );

    var overlayNonce = ""

    // collect data from script
    python.stdout.on('data', function (data) {
        logger.debug('[Server][qualityOverlay][python/stdout] : ' + data);
        overlayNonce += data.toString();
    });

    // collect error data from script (for debugging)
    python.stderr.on('data', function (data) {
        logger.error('[Server][qualityOverlay][python/stderr] :' + data);
    });

    // in close event we are sure that stream from child process is closed
    python.on('close', (code) => {
        logger.debug(`[Server][qualityOverlay] Script exit code : ${code}`);

        if (code != 0) {
            res.status(400).send("Bad request");
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

    logger.debug("[Server][logTrip] Coordinates : " + data["pontos"])
    logger.debug("[Server][logTrip] Quality     : " + data["scores"])

    logger.debug("[Server][logTrip][debug] --coordinates "    + data["pontos"].map(coord => coord.join(",")).join(" "))
    logger.debug("[Server][logTrip][debug] --quality "        + data["scores"].join(" "))
    logger.debug("[Server][logTrip][debug] --overlay_folder " + fetchFile("/overlay/"))

    // res.send("OK");

    const python = spawn(
        Constants.PYTHON_BIN, 
        [
            fetchFile(Constants.SCRIPT_LOG_TRIP),
            "--coordinates"    , data["pontos"].map(coord => coord.join(",")).join(" "),
            "--quality"        , data["scores"].join(" "),
            "--overlay_folder" , fetchFile("/overlay/"),
            //"--DEBUG"
        ]
    );

    var pythonData = ""

    // collect data from script
    python.stdout.on('data', function (data) {
        logger.debug('[Server] Pipe data from python script : ' + data);
        pythonData += data.toString();
    });

    // collect error data from script (for debugging)
    python.stderr.on('data', function (data) {
        logger.error('[Server][python/stderr] :' + data);
    });

    // in close event we are sure that stream from child process is closed
    python.on('close', (code) => {
        logger.debug(`[Server] Script exit code : ${code}`);

        if (code != 0) {
            res.status(500).send("Internal error");
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