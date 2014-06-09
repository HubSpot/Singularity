Model = require './model'

class Request extends Model

    initialize: =>
        @set "displayState", @getState()

    parse: (data) ->
        if data.request?
            data.request.daemon = if _.isNull(data.request.daemon) then true else data.request.daemon
            data.daemon = data.request.daemon
        data

    url: => "#{ config.apiRoot }/requests/request/#{ @get('id') }"

    deletePaused: =>
        $.ajax
            url: "#{ config.apiRoot }/requests/request/#{ @get('id') }/paused"
            type: 'DELETE'

    unpause: =>
        $.ajax
            url: "#{ config.apiRoot }/requests/request/#{ @get('id') }/unpause"
            type: 'POST'

    pause: =>
        $.ajax
            url: "#{ config.apiRoot }/requests/request/#{ @get('id') }/pause"
            type: 'POST'

    run: (confirmedOrPromptData) ->
        options =
            url: "#{ config.apiRoot }/requests/request/#{ @get('id') }/run"
            type: 'POST'
            contentType: 'application/json'

        if _.isString confirmedOrPromptData
            options.data = confirmedOrPromptData
            options.processData = false
            options.contentType = 'text/plain'

        $.ajax options
    
    getState: =>
        if @get("cleanupType")?
            return contants.requestStates.CLEANUP
        else if @get("requestDeployState").pendingDeploy != null
            return constants.requestStates.PENDING
        else
            apiState = @get("state")
            return constants.requestStates[apiState]
        

module.exports = Request