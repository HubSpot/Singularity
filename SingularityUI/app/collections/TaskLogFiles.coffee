Collection = require './collection'

class TaskLogFiles extends Collection

    url: => "http://#{ @offerHostname }:#{ constants.mesosLogsPort }/files/browse.json?path=#{ @directory }"

    initialize: (models, { @offerHostname, @directory }) =>

    parse: (taskLogFiles) =>
        _.map taskLogFiles, (taskLogFile) =>
            taskLogFile.shortPath = taskLogFile.path.substr @directory.length + 1
            taskLogFile.mtimeHuman = moment(taskLogFile.mtime * 1000).from()
            taskLogFile.downloadLink = "http://#{ @offerHostname }:#{ constants.mesosLogsPort }/files/download.json?path=#{ taskLogFile.path }"
            taskLogFile

    comparator: 'size'

module.exports = TaskLogFiles