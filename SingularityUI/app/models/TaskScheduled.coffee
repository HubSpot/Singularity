Model = require './model'

class TaskScheduled extends Model

    run: ->
        $.ajax
            url: "#{ config.apiRoot }/requests/request/#{ @get('requestId') }/run"
            type: 'POST'
            contentType: 'application/json'

module.exports = TaskScheduled