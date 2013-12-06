Model = require './model'

class TaskScheduled extends Model

    run: ->
        $.ajax
            url: "/requests/request/#{ @get('requestId') }/run"
            type: 'POST'
            contentType: 'application/json'

module.exports = TaskScheduled