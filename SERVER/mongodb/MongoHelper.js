const MongoClient = require('mongodb').MongoClient;
const assert = require('assert');
const Constants = require("../util/Constants.js");
const logger = require("../util/Logger.js").logger;
const serverUtils = require("../util/Util.js");
const {spawn} = require('child_process');

// Environment variables
require('dotenv').config({path: __dirname + '/.env'});

// Connection URL
//const mongoUrl = 'mongodb://localhost:27017';

// Database Name
const dbName = 'ProjetoIntegrado';

// Collections
const usersCollectionStr        = 'users'
const pendingUsersCollectionStr = 'pendingUsers'
const sessionsCollectionStr     = 'sessions'

// Mongo db client
const client = new MongoClient(process.env.MONGOD_URL, { useUnifiedTopology: true });

const spawnMongod = () => {
    let mongodb_path = process.env.MONGO_DBPATH;
    if (mongodb_path[0] == '.') { // Relative path
        mongodb_path = __dirname + mongodb_path.substr(1);
    }
    logger.debug("Mongodb path set as [" + mongodb_path + "]");
    module.exports._mongodProcess = spawn(
        process.env.MONGOD_BIN,
        [
            "--dbpath="+mongodb_path
        ]
    );

    // Collect data from script
    module.exports._mongodProcess.stdout.on('data', function (data) {
        for (line of String(data).split('\n').slice(0, -1))
            logger.debug('[mongod/stdout] ' + line);
    });

    // Collect error data from script (for debugging)
    module.exports._mongodProcess.stderr.on('data', function (data) {
        for (line of String(data).split('\n').slice(0, -1))
            logger.error('[mongod/stderr] ' + line);
    });

    // Handle mongod process exit
    module.exports._mongodProcess.on('close', function (code) {
        logger.warn('[mongod/close] ' + code);
        if (code == 100) { // Instance already running
            logger.warn('[mongod/close] Mongod instance already running, logger is unavailable');
        }
    });

    logger.debug("Created mongod process");
}

const load = async () => {
    spawnMongod()

    let cclient = await client.connect()
    module.exports.db = cclient.db(dbName);
    logger.info("Mongo db loaded");

    /**
     * Checks for an existing session for that user
     *
     * @param user {String} Primary key identifying the user to be checked
     */
    module.exports.checkForSession = function(user) {
        let key = Constants.USER_PRIMARY_KEY;
        let result = module.exports.db.collection(sessionsCollectionStr).findOne({key:user});
        if (result) {
            return true
        }
        return false
    }

    /**
     * Checks for an existing session for the given user
     *
     * @param user {String} Primary key identifying the user
     * @param password {String} Password that will be checked against the password in database
     */
    module.exports.createSession = async function(user, password) {
        // Check for correct credentials
        let result = await module.exports.db.collection(usersCollectionStr).findOne({
            [Constants.USER_PRIMARY_KEY]:user, 
            [Constants.USER_PASSWORD_KEY]:password 
        });
        logger.debug(JSON.stringify(result));
        if (result == null) { return null; }

        //TODO check if a session already exists
        let token = serverUtils.generateToken(Constants.AUTH_TOKEN_LENGTH);
        result = await module.exports.db.collection(sessionsCollectionStr).insertOne({
            [Constants.USER_PRIMARY_KEY]:user, 
            [Constants.AUTH_TOKEN_KEY]:token,
            [Constants.TIMESTAMP_KEY]:Date.now()
        });
        logger.debug(JSON.stringify(result));
        if (result == null) { return null; }
        return token;
    }

    /**
     * Validate if there exists an active session with the given token
     *
     * @param token {String} Token to be authenticated
     */
    module.exports.validateSession = async function(token) {
        let result = await module.exports.db.collection(sessionsCollectionStr).findOne({[Constants.AUTH_TOKEN_KEY]:token});
        logger.debug("Got result", result);
        return result;
    }

    /**
     * Validate if there exists an active session with the given token for the given user
     *
     * @param token {String} Token to be authenticated
     * @param user {String} Primary key identifying the user to be checked
     */
    module.exports.validateUserSession = async function(token, user) {
        let result = await module.exports.db.collection(sessionsCollectionStr).findOne({[Constants.USER_PRIMARY_KEY]:user, [Constants.AUTH_TOKEN_KEY]:token});
        if (result) {
            return true
        }
        return false
    }

    /**
     * Destroy an active session (deauthenticates all future calls using the provided token)
     *
     * @param token {String} Token to be authenticated
     * @param user {String} Primary key identifying the user to be checked
     */
    module.exports.destroySession = async function(token) {
        let result = await module.exports.db.collection(sessionsCollectionStr).findOneAndDelete({[Constants.AUTH_TOKEN_KEY]:token});
        if (result.value) {
            return result
        }
        return null
    }

    // Tests
    logger.info("Destroying nonexistent session ", await module.exports.destroySession("abc"));
    var tok = await module.exports.createSession("caiotsan@gmail.com", "HelloWorld");
    logger.info("Created session, token = " + tok);
    var res = await module.exports.validateSession(tok);
    logger.info("Checking for session: " + (res ? "ok" : "fail"));
    var res = await module.exports.validateUserSession(tok, "caiotsan@gmail.com");
    logger.info("Checking for user session: " + (res ? "ok" : "fail"));
    var res = await module.exports.validateUserSession(tok, "bgmarini@hotmail.com");
    logger.info("Checking for wrong user session: " + (res ? "ok" : "fail"));
    var res = await module.exports.destroySession(tok);
    logger.info("Destroying session " + (res ? "ok" : "fail"));
    var res = await module.exports.destroySession(tok);
    logger.info("Destroying session again " + (res ? "ok" : "fail"));
}

load();