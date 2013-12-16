RequestsActive = require './RequestsActive'

class RequestsPaused extends RequestsActive

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/paused"

module.exports = RequestsPaused