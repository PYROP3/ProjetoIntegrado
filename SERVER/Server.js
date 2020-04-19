const express = require('express');
const app = express();
const Constants = require("./util/Constants");
const {spawn} = require('child_process');
const fs = require('fs')

//M.init();

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

function fetchFile(filename) { return path.join(__dirname + filename); }

// =================================== Requests ===================================

app.get(Constants.QUALITY_OVERLAY_REQUEST, function(req, res) {
    var query = req.query;

    console.log("[Server] Overlay requested from ("+query.minLatitude+","+query.minLongitude+") to ("+query.maxLatitude+","+query.maxLongitude+")");

    const python = spawn(
        Constants.PYTHON_BIN, 
        [
            fetchFile(Constants.SCRIPT_SLICE_OVERLAY), 
            parseFloat(query.minLongitude), // x_min
            parseFloat(query.minLatitude),  // y_min
            parseFloat(query.maxLongitude), // x_max
            parseFloat(query.maxLatitude),  // y_max
            fetchFile("/overlay/"),         // overlay_folder
        ]
    );

    var overlayNonce = ""

    // collect data from script
    python.stdout.on('data', function (data) {
        console.log('[Server] Pipe data from python script : ' + data);
        overlayNonce += data.toString();
    });

    // in close event we are sure that stream from child process is closed
    python.on('close', (code) => {
        console.log(`[Server] Script exit code : ${code}`);

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

    console.log("[Server] Coordinates : " + data["pontos"])
    console.log("[Server] Quality     : " + data["scores"])

    console.log("[Server][debug] --coordinates "    + data["pontos"].map(coord => coord.join(",")).join(" "))
    console.log("[Server][debug] --quality "        + data["scores"].join(" "))
    console.log("[Server][debug] --overlay_folder " + fetchFile("/overlay/"))

    // res.send("OK");

    const python = spawn(
        Constants.PYTHON_BIN, 
        [
            fetchFile(Constants.SCRIPT_LOG_TRIP),
            "--coordinates"    , data["pontos"].map(coord => coord.join(",")).join(" "),
            "--quality"        , data["scores"].join(" "),
            "--overlay_folder" , fetchFile("/overlay/"),
            "--DEBUG"
        ]
    );

    var pythonData = ""

    // collect data from script
    python.stdout.on('data', function (data) {
        console.log('[Server] Pipe data from python script : ' + data);
        pythonData += data.toString();
    });

    // in close event we are sure that stream from child process is closed
    python.on('close', (code) => {
        console.log(`[Server] Script exit code : ${code}`);

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
console.log("[Server] Listening on port " + port);