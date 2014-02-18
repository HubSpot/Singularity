Model = require './model'

class Request extends Model

    url: => "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }"

    deletePaused: =>
        $.ajax
            url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }/paused"
            type: 'DELETE'

    unpause: =>
        $.ajax
            url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }/unpause"
            type: 'POST'

    run: ->
        $.ajax
            url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('id') }/run"
            type: 'POST'
            contentType: 'application/json'

module.exports = Request