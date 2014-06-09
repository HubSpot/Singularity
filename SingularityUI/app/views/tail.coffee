View = require './view'

TaskHistory = require '../models/TaskHistory'

TailerView = require './tailer'

class TailView extends View

    template: require './templates/tail'

    className: 'tail-wrapper'

    events:
        'click .tail-top-button': 'tailerToTop'
        'click .tail-bottom-button': 'tailerToBottom'

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
            parent: @

        @tailer.render()

        @

    tailerToTop: =>
        @tailer.goToTop()
    
    tailerToBottom: =>
        @tailer.goToBottom()

module.exports = TailView