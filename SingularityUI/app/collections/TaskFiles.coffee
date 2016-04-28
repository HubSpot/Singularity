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

            taskLogFile.fullPath = sandbox.fullPathToRoot + "/" + taskLogFile.uiPath.replace('$TASK_ID', @taskId)

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

    sortBy: (field, sortDirectionAscending) ->
        if field is 'name'
            sorted = _.sortBy @models, (file) =>
                if file.attributes.isDirectory
                    "a#{file.attributes.name}"
                else
                    "b#{file.attributes.name}"
        else if field is 'size'
            sorted = _.sortBy @models, (file) =>
                if file.attributes.isDirectory
                    -1
                else
                    file.attributes.size
        else # Sorting by last modified mixes in directories by design
            sorted = _.sortBy @models, (file) => file.attributes[field]
        sorted.reverse() unless sortDirectionAscending
        @models = sorted

    comparator: (a, b) ->
        return @nameComparator(a, b)

    nameComparator: (a, b) ->
        if a.get('isDirectory') and not b.get('isDirectory')
            return -1
        else if not a.get('isDirectory') and b.get('isDirectory')
            return 1
        else if a.get('name') > b.get('name')
            return 1
        else if a.get('name') < b.get('name')
            return -1
        else
            return 0

    testSandbox: ->
        $.ajax
            url: @url()
            suppressErrors: true

module.exports = TaskFiles
