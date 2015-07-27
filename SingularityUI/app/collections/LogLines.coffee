Collection = require './collection'

LogLine = require '../models/LogLine'

orZero = (value) => if value < 0 then 0 else value

class LogLines extends Collection

    model: LogLine
    comparator: 'offset'

    delimiter: /\n/

    # How much we request at a time (before growing it)
    baseRequestLength: 30000

    # TODO, hope to get rid of this
    serverMax: 65536

    requestLengthGrowthFactor: 1.75
    maxRequestLength: @::serverMax # @::baseRequestLength * 100

    # Request a larger chunk at start
    initialRequestLength: @::baseRequestLength * 3

    # Store collection state on a model so it can be observed by others (events)
    state: new Backbone.Model
        # Did we fetch all `requestLength` last time? If it is it likely means
        # there's more to fetch
        moreToFetch: undefined
        moreToFetchAtBeginning: undefined

        currentRequestLength: @::baseRequestLength



    url: => "#{ config.apiRoot }/sandbox/#{ @taskId }/read"

    initialize: (models, {@taskId, @path, @ajaxError}) ->

    getMinOffset: =>
        if @length > 0 then @first().getStartOffset() else 0

    getMaxOffset: =>
        if @length > 0 then @last().getEndOffset() else 0

    getStartOffsetOfLastLine: =>
        # Get the offest of the beginning of the last line, not the end of the last line
        if @length > 0 then @last().getStartOffset() else 0

    fetchInitialData: =>
        # When we request `read` without passing an offset, we get given
        # back just the end offset of the file
        $.ajax
            url: @url()
            data: {@path, length: @baseRequestLength}
        .done (response) =>
            offset = response.offset - @baseRequestLength
            offset = orZero offset
            @ajaxError.set present: false

            request = @fetch data:
                path: @path
                offset: offset
                length: @initialRequestLength

            @trigger 'initialdata'
        .error (response) =>
            # If we get a 400, the file has likely not been generated
            # yet, so we'll pass a message to the view
            if response.status in [400, 404, 500]
                app.caughtError()
                @ajaxError.setFromErrorResponse response
    
    fetchPrevious: ->
        @fetch data:
            offset: orZero @getMinOffset() - @state.get('currentRequestLength')

    fetchNext: =>
        @fetch data:
            offset: @getMaxOffset()

    fetchFromStart: =>
        @fetch data:
            offset: 0

    fetchOffset: (offset) =>
        @fetch data: 
            offset: offset - 1
            done: => @trigger 'initialOffsetData'

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

        @_previousParseTimestamp = @_parseTimestamp
        @_parseTimestamp = (new Date).getTime()

        # Return empty list if all we got is white space
        if result.data.match whiteSpace
            @shrinkRequestLength()
            return []

        # We have more stuff to fetch if we got `requestLength` data back
        # and (we're going forwards or we're at the start)
        requestedLength = options.data.length

        isMovingForward = offset >= @getMaxOffset()
        isMovingBackward = offset <= @getMinOffset()

        moreToFetch = result.data.length is requestedLength or result.data.length is @serverMax
        moreToFetch = moreToFetch and (isMovingForward or @getMinOffset() is 0)
        @state.set('moreToFetch', moreToFetch)

        # Determine if we still need to fetch more at the top of the file
        if offset is 0
            @state.set('moreToFetchAtBeginning', false)
        else if @state.get('moreToFetchAtBeginning') is undefined
            @state.set('moreToFetchAtBeginning', true)

        # Grow the request length (page size) if we are not tailing
        if (@state.get('moreToFetch') and isMovingForward) or (@state.set('moreToFetchAtBeginning') and isMovingBackward)
            @growRequestLength(@_previousParseTimestamp, @_parseTimestamp)
        else
            @shrinkRequestLength()


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

    growRequestLength: (previousParseTimestamp, lastParseTimestamp) ->
        return if !previousParseTimestamp? or !lastParseTimestamp?

        # Only grow the request length if we request quickly in succession
        delta = lastParseTimestamp - previousParseTimestamp

        if delta < 5000 and @state.get('currentRequestLength') <= @maxRequestLength
            newRequestLength = parseInt(@state.get('currentRequestLength') * @requestLengthGrowthFactor, 10)
            newRequestLength = @maxRequestLength if newRequestLength > @maxRequestLength

            @state.set('currentRequestLength', newRequestLength)

    shrinkRequestLength: ->
        return if @state.get('currentRequestLength') <= @baseRequestLength

        newRequestLength = parseInt(@state.get('currentRequestLength') / @requestLengthGrowthFactor, 10)
        newRequestLength = @baseRequestLength if newRequestLength < @baseRequestLength

        @state.set('currentRequestLength', newRequestLength)

module.exports = LogLines
