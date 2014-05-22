Model = require './model'

class TaskScheduled extends Model

    run: ->
        $.ajax
            url: "#{ config.apiBase }/requests/request/#{ @get('requestId') }/run"
            type: 'POST'
            contentType: 'application/json'

module.exports = TaskScheduled