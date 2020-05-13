const MongoClient = require('mongodb').MongoClient;
const assert = require('assert');

// Connection URL
const mongoUrl = 'mongodb://localhost:27017';

// Database Name
const dbName = 'ProjetoIntegrado';

// Default collection
const defaultCollection = 'users'

// Mongo db client
const client = new MongoClient(mongoUrl, { useUnifiedTopology: true });

/**
 * Executes a generic function on the database
 *
 * @param callback {Function} Function callback ->(collection, onClose)
 * @param collection {String} Name of the collection to access
 */
// module.exports.execute = async function(callback, collection) {
    // if (collection == null) collection = defaultCollection;
const load = async () => {
    let cclient = await client.connect()//function(err) {
    module.exports.db = cclient.db(dbName);
    
}

load()
