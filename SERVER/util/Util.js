/**
 * Create a random hex string with a given size
 *
 * @param len {Integer} Length of string to be created
 */
module.exports.generateToken = function(len) {
    if (len == null) len = 32;
    var maxlen = 8,
    min = Math.pow(16,Math.min(len,maxlen)-1) 
    max = Math.pow(16,Math.min(len,maxlen)) - 1,
    n   = Math.floor( Math.random() * (max-min+1) ) + min,
    r   = n.toString(16);
    while ( r.length < len ) {
        r = r + module.exports.generateToken( len - maxlen );
    }
    return r;
};