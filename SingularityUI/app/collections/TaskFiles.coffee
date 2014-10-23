Collection = require './collection'

class TaskFiles extends Collection

    url: -> "#{ config.apiRoot }/sandbox/#{ @taskId }/browse"

    initialize: (models, { @taskId, @path }) ->
    
    fetch: (params) ->
        data = if @path then {@path} else {}
        super _.extend params or {},
            data: data

    parse: (sandbox) ->
        taskFiles = sandbox.files
        
        for taskLogFile in taskFiles
            taskLogFile.requestPath = taskLogFile.path
            downloadParams = $.param {path: taskLogFile.requestPath}

            taskLogFile.shortPath = taskLogFile.path.split(/\//).reverse()[0]
            taskLogFile.mtime = taskLogFile.mtime * 1000
            taskLogFile.downloadLink = "#{ config.apiRoot }/sandbox/#{ @taskId }/download?#{ downloadParams }"
            taskLogFile.isDirectory = taskLogFile.mode[0] is 'd'
            taskLogFile.taskId = @taskId

            if not taskLogFile.isDirectory
                extension = _.clone(taskLogFile.shortPath).replace /^.*?\.(.*?)$/g, '$1'
                isZip = extension.indexOf('zip') isnt -1 or extension.indexOf('gz') isnt -1

                taskLogFile.isTailable = not isZip

        taskFiles

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