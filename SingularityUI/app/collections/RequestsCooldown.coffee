RequestsActive = require './RequestsActive'

class RequestsCooldown extends RequestsActive

    url: "#{ config.apiRoot }/requests/cooldown"

module.exports = RequestsCooldown