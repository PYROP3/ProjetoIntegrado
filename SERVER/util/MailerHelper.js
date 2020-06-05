const nodemailer = require('nodemailer');
const Constants = require("./Constants");
require('dotenv').config({path: __dirname + '/.env'});

const transporter = nodemailer.createTransport({
    host: Constants.SOURCE_EMAIL_HOST,
    port: 465,
    secure: true,
    auth: {
        type: 'OAuth2',
        user: Constants.SOURCE_EMAIL_ADDRESS,
        clientId:     process.env.SOURCE_EMAIL_CLIENT_ID,
        clientSecret: process.env.SOURCE_EMAIL_CLIENT_SECRET,
        refreshToken: process.env.SOURCE_EMAIL_REFRESH_TOKEN
    }
});

/**
 * Generic send email
 * Uses environment variables to get authentication tokens to communicate with email provider
 *
 * @param emailData {Object} File to be located
 */
module.exports.sendMail = function(emailData) {
    transporter.sendMail(emailData, function(error, info){
        if (error) {
            console.log(error);
        } else {
            console.log('Email sent: ' + info.response);
        }
    });
}