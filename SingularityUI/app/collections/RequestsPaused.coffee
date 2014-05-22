RequestsActive = require './RequestsActive'

class RequestsPaused extends RequestsActive

    url: "#{ config.apiBase }/requests/paused"

module.exports = RequestsPaused