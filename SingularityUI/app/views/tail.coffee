View = require './view'

TaskHistory = require '../models/TaskHistory'
TailerView = require './tailer'

class TailView extends View
    template: require './templates/tail'

    initialize: ({@taskId, @path}) ->
        @tailer = new TailerView
            taskId: @taskId
            path: @path

        @subfolders = []

        pieces = @path.split /\//
        fullPath = ''

        for name in _.initial(pieces)
            fullPath += "#{name}/"
            @subfolders.push {name, fullPath}

        @filename = _.last(pieces)

        @tailer.setup()

    render: =>
        @$el.html @template {@taskId, @path, @subfolders, @filename}

        @$el.find('div.tail-container').html @tailer.$el

        @

module.exports = TailView