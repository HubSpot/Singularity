LogLine = require '../models/LogLine'

class LogLines extends Backbone.Collection
    @instances: {}
    @getInstance: ({taskHistory, path}) ->
        taskId = taskHistory.get('task').id
        key = "#{taskId}-#{path}"

        # create if necessary
        if not LogLines.instances[key]
            LogLines.instances[key] = new LogLines [],
                offerHostname: taskHistory.get('task').offer.hostname
                directory: taskHistory.get('directory')
                taskId: taskId
                path: path

        LogLines.instances[key]

    model: LogLine
    comparator: 'offset'
    delimiter: /\n/

    initialize: (models, {@offerHostname, @directory, @path}) ->

    getSlaveUrlBase: =>
        if constants.mesosLogsPortHttps
            "https://#{ @offerHostname }:#{ constants.mesosLogsPortHttps }"
        else
            "http://#{ @offerHostname }:#{ constants.mesosLogsPort }"

    getMinOffset: =>
        if @length > 0 then @first().getStartOffset() else 0

    getMaxOffset: =>
        if @length > 0 then @last().getEndOffset() else 0

    url: =>
        params =
            path: "#{ @directory }/#{ @path ? ''}"

        "#{ @getSlaveUrlBase() }/files/read.json?#{ $.param params }&jsonp=?"

    fetchEndOffset: =>
        deferred = Q.defer()

        ajaxPromise = $.getJSON @url()

        ajaxPromise.done (result) ->
            deferred.resolve(result.offset)

        ajaxPromise.fail (jqXHR, status, error) ->
            deferred.reject(error)
        
        return deferred.promise

    parse: (result) =>
        offset = result.offset

        # split on newlines
        lines = result.data.split @delimiter
        
        # omit the last element, since it'll either be a blank or incomplete line
        lines = _.initial(lines)

        # omit the first (incomplete) element unless we're at the beginning of the file
        if offset > 0 and lines.length > 0
            offset += lines[0].length + 1
            lines = _.rest(lines)

        # create the LogLine models
        lines.map (data) ->
            obj = new LogLine {data, offset}

            offset += data.length + 1

            return obj

module.exports = LogLines