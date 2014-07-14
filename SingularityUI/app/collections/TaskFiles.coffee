Collection = require './collection'

class TaskFiles extends Collection

    url: -> "#{ config.apiRoot }/sandbox/#{ @taskId }/browse"

    initialize: ({ @taskId, @path }) ->
        @path = if not @path? then @taskId else @path

    fetch: (params) ->
        super _.extend params or {},
            data: {@path}

    parse: (taskFiles) ->
        _.map taskFiles, (taskLogFile) =>
            taskLogFile.requestPath = taskLogFile.path.replace new RegExp("^.*\/(#{ @taskId }.*?)$"), '$1'
            downloadParams = $.param {path: taskLogFile.requestPath}

            taskLogFile.shortPath = taskLogFile.path.split(/\//).reverse()[0]
            taskLogFile.mtimeHuman = utils.humanTimeAgo(taskLogFile.mtime * 1000)
            taskLogFile.sizeHuman = Humanize.fileSize(taskLogFile.size)
            taskLogFile.downloadLink = "#{ config.apiRoot }/sandbox/#{ @taskId }/download?#{ downloadParams }"
            taskLogFile.isDirectory = taskLogFile.mode[0] is 'd'
            taskLogFile.taskId = @taskId

            taskLogFile

    comparator: (a, b) ->
        if a.get('isDirectory') and not b.get('isDirectory')
            return -1
        else if not a.get('isDirectory') and b.get('isDirectory')
            return 1
        return a.get('path') - b.get('path')

    testSandbox: ->
        $.ajax
            url: @url()
            suppressErrors: true

module.exports = TaskFiles