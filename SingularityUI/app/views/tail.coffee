View = require './view'

TaskHistory = require '../models/TaskHistory'

TailerView = require './tailer'

class TailView extends View

    template: require './templates/tail'

    initialize: ({@taskId, @path}) ->
        @subfolders = []

        pieces = @path.split /\//
        fullPath = ''

        for name in _.initial(pieces)
            fullPath += "#{name}/"
            @subfolders.push {name, fullPath}

        @filename = _.last(pieces)

    remove: =>
        @tailer?.remove()
        super

    render: =>
        @$el.html @template {@taskId, @path, @subfolders, @filename}

        @tailer = new TailerView
            el: @$('.tail-outer')
            taskId: @taskId
            path: @path

        @tailer.render()

        @

module.exports = TailView