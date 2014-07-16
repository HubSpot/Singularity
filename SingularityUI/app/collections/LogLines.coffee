Collection = require './collection'

LogLine = require '../models/LogLine'

orZero = (value) => if value < 0 then 0 else value

class LogLines extends Collection

    model: LogLine
    comparator: 'offset'

    delimiter: /\n/
    # How much we request at a time
    requestLength: 30000

    # Did we fetch all `requestLength` last time? If it is it likely means
    # there's more to fetch
    moreToFetch: false

    url: => "#{ config.apiRoot }/sandbox/#{ @taskId }/read"

    initialize: (models, {@taskId, @path}) ->

    getMinOffset: =>
        if @length > 0 then @first().getStartOffset() else 0

    getMaxOffset: =>
        if @length > 0 then @last().getEndOffset() else 0

    fetchInitialData: (callback) =>
        # When we request `read` without passing an offset, we get given
        # back just the end offset of the file
        $.ajax
            url: @url()
            data: {@path, length: @requestLength}
        .done (response) =>
            offset = response.offset - @requestLength
            offset = orZero offset

            request = @fetch data:
                path: @path
                offset: offset
            request.done callback

    fetchPrevious: ->
        @fetch data:
            offset: orZero @getMinOffset() - @requestLength

    fetchNext: =>
        @fetch data:
            offset: @getMaxOffset()

    fetchFromStart: =>
        @fetch data:
            offset: 0

    # Overwrite default fetch
    fetch: (params = {}) ->
        defaultParams = 
            remove: false
            data: _.extend {@path, length: @requestLength}, params.data

        request = super _.extend params, defaultParams
        
        request.done ({data}) =>
            @sort()
            @moreToFetch = data.length is @requestLength

        request

    parse: (result) =>
        offset = result.offset

        # split on newlines
        lines = result.data.split @delimiter
            
        # always omit last element (either it's blank or an incomplete line)
        lines = _.initial(lines)

        # omit the first (incomplete) element unless we're at the beginning of the file
        if offset > 0 and lines.length > 0
            offset += lines[0].length + 1
            lines = _.rest lines

        # create the objects for LogLine models
        lines.map (data) ->
            line = {data, offset}
            offset += data.length + 1

            line

module.exports = LogLines
