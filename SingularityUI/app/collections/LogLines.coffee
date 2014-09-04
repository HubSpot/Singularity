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

    fetchInitialData: =>
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

            @trigger 'initialdata'

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

        request

    parse: (result) =>
        offset = result.offset

        whiteSpace = /^\s*$/

        # Return empty list if all we got is white space
        return [] if result.data.match whiteSpace

        # We have more stuff to fetch if we got `requestLength` data back
        @moreToFetch = result.data.length is @requestLength
        # And (we're going forwards or we're at the start)
        @moreToFetch = @moreToFetch and (offset >= @getMaxOffset() or @getMinOffset() is 0)

        # split on newlines
        lines = result.data.split @delimiter
            
        # always omit last element (either it's blank or an incomplete line)
        lines = _.initial(lines) unless not @moreToFetch

        # omit the first (incomplete) element unless we're at the beginning of the file
        if offset > 0 and lines.length > 0
            offset += lines[0].length + 1
            lines = _.rest lines

        # remove last line if empty, or if it only has whitespace
        if lines[lines.length - 1].match whiteSpace or not lines[lines.length - 1]
            lines.splice lines.length - 1

        # create the objects for LogLine models
        lines.map (data) ->
            line = {data, offset}
            offset += data.length + 1

            line

module.exports = LogLines
