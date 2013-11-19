Model = require './model'

class State extends Model

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.api_base }/state"

    parse: (state) =>
        state.uptimeHuman = moment.duration(state.uptime).humanize()
        state.driverStatusHuman = constants.driverStates[state.driverStatus]
        state.millisSinceLastOfferHuman = moment(+new Date() - state.millisSinceLastOffer).from()
        state

module.exports = State