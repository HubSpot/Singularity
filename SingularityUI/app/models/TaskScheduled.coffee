Model = require './model'

class TaskScheduled extends Model

    run: ->
        $.ajax
            url: "#{ window.singularity.config.apiBase }/requests/request/#{ @get('requestId') }/run"
            type: 'POST'
            contentType: 'application/json'

module.exports = TaskScheduled