Model = require './model'

class State extends Model

    url: -> "#{ config.apiRoot }/state"

    parse: (state) =>
        mesosMaster = undefined

        _.each state.hostStates, (hostState) ->
            hostState.uptimeHuman = moment.duration(hostState.uptime).humanize()
            hostState.driverStatusHuman = constants.driverStates[hostState.driverStatus]

            now = +new Date()
            if hostState.millisSinceLastOffer?
                if now - hostState.millisSinceLastOffer > hostState.millisSinceLastOffer
                    hostState.millisSinceLastOfferHuman = utils.humanTimeAgo(+new Date() - hostState.millisSinceLastOffer)
                else
                    hostState.millisSinceLastOfferHuman = utils.humanTimeAgo hostState.millisSinceLastOffer
            else
                hostState.millisSinceLastOfferHuman = '—'

            if hostState.mesosMaster?
                mesosMaster = hostState.mesosMaster

        _.each state.hostStates, (hostState) ->
            if hostState.hostAddress is mesosMaster?.split(':')[0]
                state.masterLogsDomain = "#{ hostState.hostname }:#{ mesosMaster.split(':')[1] }"

        state

module.exports = State