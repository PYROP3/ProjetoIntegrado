module.exports.User = class {
    constructor(email, name, password) {
        if (typeof(email)==='object') {
            this._email    = email['email'];
            this._name     = email['name'];
            this._password = email['password'];
        } else {
            this._email    = email;
            this._name     = name;
            this._password = password;
        }
    }

    toJSON() {
        return {
            'email':this._email,
            'name':this._name,
            'password':this._password
        }
    }
}