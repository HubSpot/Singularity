Model = require './model'

class State extends Model

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/state"

    parse: (state) =>
        _.each state.hostStates, (hostState) ->
            hostState.uptimeHuman = moment.duration(hostState.uptime).humanize()
            hostState.driverStatusHuman = constants.driverStates[hostState.driverStatus]
            hostState.millisSinceLastOfferHuman = moment(+new Date() - hostState.millisSinceLastOffer).from()

            # Ask wsorenson...
            if hostState.hostAddress is hostState.mesosMaster?.split(':')[0]
                state.masterLogsDomain = "#{ hostState.hostname }:#{ hostState.mesosMaster.split(':')[1] }"

        state

module.exports = State