const express = require('express');
const server = express();
const Constants = require("./util/Constants");
const {spawn} = require('child_process');
const fs = require('fs');
const winston = require('winston');
const path = require('path');
const mongo = require("./mongodb/MongoHelper.js");
//const oauth2 = require("./oauth2/Oauth2Helper.js").oauth2;
const assert = require('assert');
const userModel = require("./mongodb/model/User.js");
const serverUtils = require("./util/Util.js");
const mailer = require("./util/MailerHelper.js");

// JSON via post
const bodyParser = require('body-parser');
server.use(bodyParser.json());
server.use(bodyParser.urlencoded({ extended: false }));

// Environment variables
require('dotenv').config({path: __dirname + '/util/.env'});

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
//server.use(express.static('htmls'));

function fetchFile(filename) { return path.join(__dirname + "/" + filename); }

// Oauth2 setup
// server.use(oauth2.inject());
// server.post('/token', oauth2.controller.token);
// server.get('/authorization', isAuthorized, oauth2.controller.authorization, function(req, res) {
//     // Render our decision page
//     // Look into ./test/server for further information
//     res.render('authorization', {layout: false});
// });
// server.post('/authorization', isAuthorized, oauth2.controller.authorization);
// function isAuthorized(req, res, next) {
//     if (req.session.authorized) next();
//     else {
//         var params = req.query;
//         params.backUrl = req.path;
//         res.redirect('/login?' + query.stringify(params));
//     }
// };

// Winston setup
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
    response.status(error["HttpReturn"]).header("Content-Type", "serverlication/json").send(JSON.stringify(thisErr));
}

// =================================== Requests ===================================

server.post(Constants.CREATE_ACCOUNT_REQUEST, async function(req, res) {
    let data = req.body;
    let authToken = req.token;

    let findResult = await mongo.db.collection('users').findOne({'email':data['email']});
    if (findResult) { 
        logger.info("Account requested for email " + data['email'] + " already in use");
        sendErrorMessage(3, req, res); //TODO find a better way to reply
        return 
    }
    var newUser = new userModel.User(data['email'], data['name'], data['password']).toJSON();
    newUser['authToken'] = serverUtils.generateToken(32);
    logger.info("Creating user : ", newUser);
    let result = await mongo.db.collection('pendingUsers').insertOne(newUser);
    if (result == null) {
        sendErrorMessage(1, req, res); 
    } else {
        sendErrorMessage(0, req, res); //TODO find a better way to reply
        //TODO des-gambiarrar esse processo de enviar email
        mailer.sendMail({
            from: Constants.SOURCE_EMAIL_ADDRESS,
            to: newUser['email'],
            subject: 'Street analyzer account validation',
            text: 'That was easy!\n' + 
                'Now just click on this link to validate your account: ' +
                Constants.SERVER_URL + 
                Constants.VERIFY_ACCOUNT_REQUEST + 
                '?token='+newUser['authToken']
        });
    }
});

server.get(Constants.VERIFY_ACCOUNT_REQUEST, async function(req, res) {
    let query = req.query;
    let authToken = query.token;

    let auth = await mongo.db.collection('pendingUsers').findOneAndDelete({'authToken':authToken});
    if (auth == null) {
        sendErrorMessage(7, req, res); 
    } else {
        logger.info("Validating user : ", auth);
        delete(auth['authToken']);
        await mongo.db.collection('users').insertOne(new userModel.User(auth).toJSON());
        sendErrorMessage(0, req, res); //TODO find a better way to reply
    }
});

server.get(Constants.QUALITY_OVERLAY_REQUEST, function(req, res) {
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


server.post(Constants.LOG_TRIP_REQUEST, function(req, res){
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

    py_args = py_args.concat([].concat.serverly([], data["dados"].map(line => ["--accel_data", line.map(data => data.join(",")).join(" ")])))

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
if (port == undefined) port = Constants.SERVER_PORT;

server.listen(port);
logger.info("[Server] Listening on port " + port);

// var rawdata = fs.readFileSync(fetchFile(Constants.SCRIPT_ERRORS_PATH));
// var error = JSON.parse(rawdata)[code];
// var thisErr = {
//     "Error": error["PrettyName"],
//     "Description": error["Description"],
// }
