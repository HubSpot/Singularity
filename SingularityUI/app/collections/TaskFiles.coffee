Collection = require './collection'
TaskFile = require '../models/TaskFile'

class TaskFiles extends Collection

    url: -> "#{ config.apiRoot }/sandbox/#{ @taskId }/browse"

    model: TaskFile

    initialize: (models, { @taskId, @path }) ->

    fetch: (params) ->
        data = if @path isnt null then {@path} else {}
        super _.extend params or {},
            data: data

    parse: (sandbox) ->
        taskFiles = sandbox.files

        @currentDirectory = sandbox.currentDirectory

        for taskLogFile in taskFiles
            taskLogFile.isDirectory = taskLogFile.mode[0] is 'd'

            if sandbox.currentDirectory
              taskLogFile.uiPath = sandbox.currentDirectory + "/" + taskLogFile.name
            else
              taskLogFile.uiPath = taskLogFile.name

            if !taskLogFile.isDirectory
              taskLogFile.uiPath = taskLogFile.uiPath.replace(@taskId, '$TASK_ID')

            taskLogFile.fullPath = sandbox.fullPathToRoot + "/" + taskLogFile.uiPath

            taskLogFile.mtime = taskLogFile.mtime * 1000

            httpPrefix = "http"
            httpPort = config.slaveHttpPort

            if config.slaveHttpsPort
              httpPrefix = "https"
              httpPort = config.slaveHttpsPort

            taskLogFile.downloadLink = "#{httpPrefix}://#{ sandbox.slaveHostname }:#{httpPort}/files/download.json?path=#{ taskLogFile.fullPath }"

            taskLogFile.taskId = @taskId

            if not taskLogFile.isDirectory
                extension = _.clone(taskLogFile.name).replace /^.*?\.(.*?)$/g, '$1'
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
