LogLine = require '../models/LogLine'

class LogLines extends Backbone.Collection
    @instances: {}
    @getInstance: ({taskId, path}) ->
        key = "#{taskId}-#{path}"

        # create if necessary
        if not LogLines.instances[key]
            LogLines.instances[key] = new LogLines [],
                taskId: taskId
                path: path

        LogLines.instances[key]

    model: LogLine
    comparator: 'offset'
    delimiter: /\n/

    initialize: (models, {@taskId, @path}) ->

    getMinOffset: =>
        if @length > 0 then @first().getStartOffset() else 0

    getMaxOffset: =>
        if @length > 0 then @last().getEndOffset() else 0

    url: =>
        params =
            path: @path

        "#{ config.apiRoot }/sandbox/#{ @taskId }/read?#{ $.param params }"

    fetchEndOffset: =>
        Q($.getJSON @url()).then (result) ->
            result.offset
        , (xhr, status, error) ->
            error

    parse: (result) =>
        @offset = offset = result.offset

        # split on newlines
        lines = result.data.split @delimiter

        # omit the last element only if it's blank
        if lines[lines.length - 1] is ''
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