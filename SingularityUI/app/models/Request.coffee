Model = require './model'

class Request extends Model
            
    parse: (data) ->
        if data.request?
            data.request.daemon = if _.isNull(data.request.daemon) then true else data.request.daemon
            data.daemon = data.request.daemon
            
            data.scheduled = utils.isScheduledRequest data.request
            data.onDemand = utils.isOnDemandRequest data.request

            data.displayState = constants.requestStates[data.state]

        data

    url: => "#{ config.apiRoot }/requests/request/#{ @get('id') }"

    deletePaused: =>
        $.ajax
            url: "#{ @url() }/paused"
            type: 'DELETE'

    unpause: =>
        $.ajax
            url: "#{ @url() }/unpause?user=#{app.getUsername()}"
            type: 'POST'

    pause: =>
        $.ajax
            url: "#{ @url() }/pause?user=#{app.getUsername()}"
            type: 'POST'

    run: (confirmedOrPromptData) ->
        options =
            url: "#{ @url() }/run"
            type: 'POST'
            contentType: 'application/json'

        if _.isString confirmedOrPromptData
            options.data = confirmedOrPromptData
            options.processData = false
            options.contentType = 'text/plain'

        $.ajax options
        
    bounce: =>
        $.ajax
            url: "#{ @url() }/bounce"
            type: "POST"

    destroy: =>
        $.ajax
            url: "#{ @url() }?user=#{app.getUsername()}"
            type: "DELETE"

module.exports = Request