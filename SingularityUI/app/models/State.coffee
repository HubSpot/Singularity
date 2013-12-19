Model = require './model'

class State extends Model

    url: -> "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/state"

    parse: (state) =>
        mesosMaster = undefined

        _.each state.hostStates, (hostState) ->
            hostState.uptimeHuman = moment.duration(hostState.uptime).humanize()
            hostState.driverStatusHuman = constants.driverStates[hostState.driverStatus]
            hostState.millisSinceLastOfferHuman = moment(+new Date() - hostState.millisSinceLastOffer).from()

            if hostState.mesosMaster?
                mesosMaster = hostState.mesosMaster

        _.each state.hostStates, (hostState) ->
            if hostState.hostAddress is mesosMaster?.split(':')[0]
                state.masterLogsDomain = "#{ hostState.hostname }:#{ mesosMaster.split(':')[1] }"

        state

module.exports = State