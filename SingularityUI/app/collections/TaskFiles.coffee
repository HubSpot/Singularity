Collection = require './collection'

class TaskFiles extends Collection

    url: ->
        params =
            path: @path

        "#{ config.apiRoot }/sandbox/#{ @taskId }/browse?#{ $.param params }"

    initialize: (models, { @taskId, @offerHostname, @directory, @path }) ->

    parse: (taskFiles) ->
        _.map taskFiles, (taskLogFile) =>
            relPath = taskLogFile.path.replace(@directory, '')
            downloadParams = $.param {path: relPath}

            taskLogFile.shortPath = taskLogFile.path.split(/\//).reverse()[0]
            taskLogFile.mtimeHuman = utils.humanTimeAgo(taskLogFile.mtime * 1000)
            taskLogFile.sizeHuman = Humanize.fileSize(taskLogFile.size)
            taskLogFile.downloadLink = "#{ config.apiRoot }/sandbox/#{ @taskId }/download?#{ downloadParams }"
            taskLogFile.isDirectory = taskLogFile.mode[0] is 'd'
            taskLogFile.relPath = relPath
            taskLogFile.taskId = @taskId
            taskLogFile

    comparator: (a, b) ->
        if a.get('isDirectory') and not b.get('isDirectory')
            return 1
        else if not a.get('isDirectory') and b.get('isDirectory')
            return -1
        return a.get('size') - b.get('size')

    testSandbox: ->
        $.ajax
            url: @url()
            suppressErrors: true

module.exports = TaskFiles