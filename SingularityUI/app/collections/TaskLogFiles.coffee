Collection = require './collection'

class TaskLogFiles extends Collection

    url: =>
    	fullPath = "#{ @directory }/#{ @path ? ''}"
    	"http://#{ @offerHostname }:#{ constants.mesosLogsPort }/files/browse.json?path=#{ escape fullPath }"

    initialize: (models, { @taskId, @offerHostname, @directory, @path }) =>

    parse: (taskLogFiles) =>
        _.map taskLogFiles, (taskLogFile) =>
            taskLogFile.shortPath = taskLogFile.path.split(/\//).reverse()[0]
            taskLogFile.mtimeHuman = moment(taskLogFile.mtime * 1000).from()
            taskLogFile.sizeHuman = Humanize.fileSize(taskLogFile.size)
            taskLogFile.downloadLink = "http://#{ @offerHostname }:#{ constants.mesosLogsPort }/files/download.json?path=#{ taskLogFile.path }"
            taskLogFile.isDirectory = taskLogFile.mode[0] is 'd'
            taskLogFile.relPath = taskLogFile.path.replace(@directory, '')
            taskLogFile.taskId = @taskId
            taskLogFile

    comparator: 'size'

module.exports = TaskLogFiles