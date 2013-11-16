Model = require './model'

class State extends Model

    url: -> "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/state"

    parse: (state) =>
        state.uptimeHuman = moment.duration(state.uptime).humanize()
        state

module.exports = State