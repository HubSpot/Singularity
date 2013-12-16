RequestsActive = require './Requests'

class RequestsPaused extends RequestsActive

    url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/paused"

module.exports = RequestsPaused