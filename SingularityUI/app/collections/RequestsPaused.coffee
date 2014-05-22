RequestsActive = require './RequestsActive'

class RequestsPaused extends RequestsActive

    url: "#{ window.singularity.config.apiBase }/requests/paused"

module.exports = RequestsPaused