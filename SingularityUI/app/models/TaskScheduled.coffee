Model = require './model'

class TaskScheduled extends Model

    run: ->
        $.ajax
            url: "#{ env.SINGULARITY_BASE }/#{ constants.apiBase }/requests/request/#{ @get('requestId') }/run"
            type: 'POST'
            contentType: 'application/json'

module.exports = TaskScheduled