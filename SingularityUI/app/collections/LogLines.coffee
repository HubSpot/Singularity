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

    # Store collection state on a model so it can be observed by others (events)
    state: new Backbone.Model
        # Did we fetch all `requestLength` last time? If it is it likely means
        # there's more to fetch
        moreToFetch: undefined
        moreToFetchAtBeginning: undefined

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
            offset: orZero @getMinOffset() - @state.get('currentRequestLength')

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
            data: _.extend {@path, length: @state.get('currentRequestLength')}, params.data

        request = super _.extend params, defaultParams

        request

    reset: ->
        # Reset the state too
        @state.set
            moreToFetch: undefined
            moreToFetchAtBeginning: undefined

        super

    parse: (result, options) =>
        offset = result.offset
        whiteSpace = /^\s*$/

        # Return empty list if all we got is white space
        return [] if result.data.match whiteSpace

        # We have more stuff to fetch if we got `requestLength` data back
        # and (we're going forwards or we're at the start)
        requestedLength = options.data.length

        isMovingForward = offset >= @getMaxOffset()
        isMovingBackward = offset <= @getMinOffset()

        moreToFetch = result.data.length is requestedLength
        moreToFetch = moreToFetch and (isMovingForward or @getMinOffset() is 0)
        @state.set('moreToFetch', moreToFetch)

        # Determine if we still need to fetch more at the top of the file
        if offset is 0
            @state.set('moreToFetchAtBeginning', false)
        else if @state.get('moreToFetchAtBeginning') is undefined
            @state.set('moreToFetchAtBeginning', true)

        # console.log "isMovingForward", isMovingForward
        # console.log "isMovingBackward", isMovingBackward


        # split on newlines
        lines = result.data.split @delimiter
            
        # always omit last element (either it's blank or an incomplete line)
        lines = _.initial(lines) unless not @state.get('moreToFetch')

        # omit the first (incomplete) element unless we're at the beginning of the file
        if offset > 0 and lines.length > 0
            offset += lines[0].length + 1
            lines = _.rest lines

        # remove last line if empty, or if it only has whitespace
        if lines[lines.length - 1].match whiteSpace or not lines[lines.length - 1]
            lines = _.initial lines

        # create the objects for LogLine models
        lines.map (data) ->
            line = {data, offset}
            offset += data.length + 1

            line

module.exports = LogLines
