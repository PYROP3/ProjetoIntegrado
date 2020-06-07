const express = require('express');
const server = express();
const Constants = require("./util/Constants");
const {spawn} = require('child_process');
const fs = require('fs');
const mongo = require("./mongodb/MongoHelper.js");
//const oauth2 = require("./oauth2/Oauth2Helper.js").oauth2;
const assert = require('assert');
const userModel = require("./mongodb/model/User.js");
const serverUtils = require("./util/Util.js");
const logger = require("./util/Logger.js").logger;
const mailer = require("./util/MailerHelper.js");

// JSON via post
const bodyParser = require('body-parser');
server.use(bodyParser.json());
server.use(bodyParser.urlencoded({ extended: false }));

// Environment variables
require('dotenv').config({path: __dirname + '/util/.env'});
require('dotenv').config({path: __dirname + '/script/.env'});
require('dotenv').config({path: __dirname + '/mongodb/.env'});

//Email
EmailTemplate = require('email-templates').EmailTemplate,
path = require('path'),
Promise = require('bluebird');

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
server.use('/static', express.static('public'));

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

// Error handling
function sendErrorMessage(code, request, response) {
    let rawdata = fs.readFileSync(serverUtils.fetchFile(Constants.SCRIPT_ERRORS_PATH));
    let parsedData = JSON.parse(rawdata);
    if (typeof(code)==='string') {
        var name = code
        code = parsedData.length - 1
        while (1) {
            if (parsedData[code]["Name"] == name) break;
            code -= 1;
            if (code < 0) { code = 1; break; }
        }
    }
    let error = parsedData[code];
    let errorData = error["Data"][request.header("Locale") != null ? request.header("Locale") : Constants.DEFAULT_LOCALE];
    let thisErr = {
        "Error": errorData["PrettyName"],
        "Description": errorData["Description"],
        "Code": error["id"]
    }
    response.status(error["HttpReturn"]).header("Content-Type", "application/json").send(JSON.stringify(thisErr));
}

//Email template
function loadTemplate(templateName, contexts){
    let template = new EmailTemplate(path.join(__dirname, '/templates', templateName));
    return Promise.all([contexts].map((context) => {
        return new Promise((resolve, reject) => {
            template.render(context, (err, result) => {
                if (err) reject(err);
                else resolve({
                    email: result,
                    context,
                });
            });
        });
    }));
}

// =================================== Requests ===================================

server.post(Constants.CREATE_ACCOUNT_REQUEST, async function(req, res) {
    let data = req.body;
    let authToken = req.token;
    
    let findResult = await mongo.db.collection('users').findOne({'email':data['email']});
    if (findResult) {
        logger.info("Account requested for email " + data['email'] + " already in use");
        sendErrorMessage("PrimaryKeyInUse", req, res); //TODO find a better way to reply
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
        loadTemplate('validation', newUser).then((results) => {
            return Promise.all(results.map((result) =>{         
                mailer.sendMail({
                    from: Constants.SOURCE_EMAIL_ADDRESS,
                    to: newUser['email'],
                    subject: 'Street analyzer account validation',
                    text: 'That was easy!\n' + 
                        'Now just click on this link to validate your account: ' +
                        Constants.SERVER_URL + 
                        Constants.VERIFY_ACCOUNT_REQUEST + 
                        '?token='+newUser['authToken'],
                    html: result.email.html,
                });
            }));
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
        auth = auth['value'];
        logger.info("Validating user : ", auth);
        delete(auth['authToken']);
        await mongo.db.collection('users').insertOne(new userModel.User(auth).toJSON());
        sendErrorMessage(0, req, res); //TODO find a better way to reply
    }
});

server.post(Constants.AUTH_REQUEST, async function(req, res) {
    let data = req.body;
    // TODO use SHA256 of password
    let authResult = await mongo.createSession(data.user, data.pass);
    logger.debug("Authentication result for " + JSON.stringify(data) + " is " + String(authResult))
    if (authResult) {
        res.status(200).header("Content-Type", "application/json").send(JSON.stringify({[Constants.AUTH_TOKEN_KEY]:authResult}));
    } else {
        sendErrorMessage("InvalidCredentials", req, res);
    }
});

server.get(Constants.DEAUTH_REQUEST, async function(req, res) {
    let authToken = serverUtils.parseAuthToken(req.get("Authorization"));
    logger.debug("Got authorization = " + req.get("Authorization"));

    if (authToken == null) {
        sendErrorMessage("MalformedToken", req, res);
        return;
    }

    let result = await mongo.destroySession(authToken);

    if (result == null) {
        sendErrorMessage("SessionNotFound", req, res);
        return;
    }

    sendErrorMessage(0, req, res);
});

server.get(Constants.QUALITY_OVERLAY_REQUEST, function(req, res) {
    var query = req.query;
    query.minLatitude   = parseFloat(query.minLatitude);
    query.minLongitude  = parseFloat(query.minLongitude);
    query.maxLatitude   = parseFloat(query.maxLatitude);
    query.maxLongitude  = parseFloat(query.maxLongitude);

    logger.info("[Server][qualityOverlay] Overlay requested from ("+query.minLatitude+","+query.minLongitude+") to ("+query.maxLatitude+","+query.maxLongitude+")");

    if(query.minLatitude > query.maxLatitude || query.minLongitude > query.maxLongitude){
        sendErrorMessage(6, req, res);
        return;
    }
    if(query.minLatitude < -90 || query.maxLatitude > 90 || query.maxLongitude > 180 || query.minLongitude < -180){
        sendErrorMessage(5, req, res);
        return;
    }

    if(query.minLatitude == query.maxLatitude || query.minLongitude == query.maxLatitude){
        sendErrorMessage(14, req, res);
        return;
    }


    const python = spawn(
        process.env.PYTHON_BIN,
        [
            serverUtils.fetchFile(Constants.SCRIPT_SLICE_OVERLAY), 
            query.minLongitude, // x_min
            query.minLatitude,  // y_min
            query.maxLongitude, // x_max
            query.maxLatitude,  // y_max
            "--overlay_folder",
            serverUtils.fetchFile("/overlay/"),         // overlay_folder
            "--errors_file",
            serverUtils.fetchFile(Constants.SCRIPT_ERRORS_PATH),
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
        const path = serverUtils.fetchFile("/tmp/"+overlayNonce+".jpg");

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

server.post(Constants.LOG_TRIP_REQUEST, async function(req, res){
    var data = req.body;
    let authToken = serverUtils.parseAuthToken(req.get("Authorization"));
    logger.debug("Got authorization = " + req.get("Authorization"));

    if (authToken == null) {
        sendErrorMessage("MalformedToken", req, res);
        return;
    }

    let authResult = await mongo.validateSession(authToken);

    if (authResult == null) {
        sendErrorMessage("AuthorizationNotRecognized", req, res);
        return;
    }

    logger.info("[Server][logTrip] Trip log requested")

    logger.debug("[Server][logTrip] Authentication : " + JSON.stringify(authResult))
    logger.debug("[Server][logTrip] Coordinates    : " + JSON.stringify(data["pontos"]))
    logger.debug("[Server][logTrip] Accel data     : " + JSON.stringify(data["dados"]))


    for(let i = 0; i < (data["pontos"]).length; i++){
        if(data["pontos"][i][0] > 180 || data["pontos"][i][0] < -180 || data["pontos"][i][1] > 90 || data["pontos"][i][1] < -90){
            sendErrorMessage(5, req, res);
            return;
        }
    }



    if((data["pontos"]).length != (data["dados"]).length + 1){
        sendErrorMessage(13, req, res);
        return;
    }

    let py_args = [
        serverUtils.fetchFile(Constants.SCRIPT_LOG_TRIP),
        "--coordinates"    , data["pontos"].map(coord => coord.join(",")).join(" "),
        "--overlay_folder" , serverUtils.fetchFile("/overlay/"),
        "--errors_file"    , serverUtils.fetchFile(Constants.SCRIPT_ERRORS_PATH),
        //"--DEBUG"
    ]

    py_args = py_args.concat([].concat.apply([], data["dados"].map(line => ["--accel_data", line.map(data => data.join(",")).join(" ")])))

    logger.debug("[Server][logTrip][debug] py_args = " + py_args)
    const python = spawn(
        process.env.PYTHON_BIN,
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
if (port == undefined) port = Constants.SERVER_PORT_DEFAULT;

logger.info("Starting server...");
server.listen(port);
logger.info("[Server] Listening on port " + port);
