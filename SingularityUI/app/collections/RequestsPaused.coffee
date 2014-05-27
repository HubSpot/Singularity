RequestsActive = require './RequestsActive'

class RequestsPaused extends RequestsActive

    url: "#{ config.apiRoot }/requests/paused"

module.exports = RequestsPaused