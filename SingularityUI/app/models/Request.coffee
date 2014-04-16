Model = require './model'

class Request extends Model

    parse: (data) ->
        if data.request?
            data.request.daemon = if _.isNull(data.request.daemon) then true else data.request.daemon
            data.daemon = data.request.daemon
        data

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }"

    deletePaused: =>
        $.ajax
            url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }/paused"
            type: 'DELETE'

    unpause: =>
        $.ajax
            url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }/unpause"
            type: 'POST'

    pause: =>
        $.ajax
            url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }/pause"
            type: 'POST'

    run: (confirmedOrPromptData) ->
        options =
            url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }/run"
            type: 'POST'
            contentType: 'application/json'

        if _.isString confirmedOrPromptData
            options.data = confirmedOrPromptData
            options.processData = false
            options.contentType = 'text/plain'

        $.ajax options

module.exports = Request