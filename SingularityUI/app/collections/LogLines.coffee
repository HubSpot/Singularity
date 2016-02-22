Collection = require './collection'

LogLine = require '../models/LogLine'

orZero = (value) => if value < 0 then 0 else value

class LogLines extends Collection

    model: LogLine
    comparator: 'offset'

    delimiter: /\n/

    @lastTimestamp = null
    @timestampIndex = 0

    @grep = ''
    @nextOffset = null

    # How much we request at a time (before growing it)
    baseRequestLength: 30000

    # Store collection state on a model so it can be observed by others (events)
    state: new Backbone.Model
        # Did we fetch all `requestLength` last time? If it is it likely means
        # there's more to fetch
        moreToFetch: undefined
        moreToFetchAtBeginning: undefined

    url: => "#{ config.apiRoot }/sandbox/#{ @taskId }/read"

    initialize: (models, {@taskId, @path, @ajaxError}) ->

    getMinOffset: =>
        if @length > 0 then @first().getStartOffset() else 0

    getMaxOffset: =>
        if @length > 0 then @last().getEndOffset() else 0

    getStartOffsetOfLastLine: =>
        # Get the offest of the beginning of the last line, not the end of the last line
        if @length > 0 then @last().getStartOffset() else 0

    fetchInitialData: (callback = _.noop)=>
        # When we request `read` without passing an offset, we get given
        # back just the end offset of the file
        promise = $.ajax
            url: @url()
            data: {@path, length: @baseRequestLength}

        promise.done (response) =>
            offset = response.offset - @baseRequestLength
            offset = orZero offset
            @ajaxError.set present: false

            request = @fetch data:
                path: @path
                offset: offset
                length: @initialRequestLength

            @trigger 'initialdata'
            request.done callback

        promise.error (response) =>
            # If we get a 400, the file has likely not been generated
            # yet, so we'll pass a message to the view
            if response.status in [400, 404, 500]
                app.caughtError()
                @ajaxError.setFromErrorResponse response

        promise

    fetchPrevious: ->
        @fetch(
            data:
                offset: orZero @getMinOffset() - @baseRequestLength
        ).error (error) =>
          app.caughtError() if error.status is 404
    fetchNext: =>
        @fetch(data:
            offset: @nextOffset or @getMaxOffset()
        ).error (error) =>
          # Don't throw an error if the task ends while we're tailing
          app.caughtError() if error.status is 404
        @nextOffset = null

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
            data: _.extend {@path, length: @baseRequestLength, grep: @grep}, params.data

        request = super(_.extend params, defaultParams)

        request

    reset: ->
        # Reset the state too
        @state.set
            moreToFetch: undefined
            moreToFetchAtBeginning: undefined

        super

    parse: (result, options) =>
        @nextOffset = result.nextOffset

        # bail early if no new data
        if result.data.length is 0
            return []

        @state.set('moreToFetch', (result.data.length is options.data.length) and ((offset >= @getMaxOffset()) or @getMinOffset() is 0))
        @state.set('moreToFetchAtBeginning', offset > 0 and @getMinOffset() > 0)

        # split on newlines
        lines = _.initial(result.data.match /[^\n]*(\n|$)/g)

        # If out batch lines up with the end and the last line doesn't end with a newline, append the first line
        if result.offset > 0 and result.offset is @getMaxOffset()
            origLine = @at(-1)
            unless origLine.get('data').endsWith '\n'
                origLine.set
                    data: origLine.get('data') + lines[0]
                lines = _.rest(lines)

        # If our batch lines up with the beginning and doesn't end with a newline, prepend the last line
        if result.offset + result.data.length is @getMinOffset()
            origLine = @at(0)
            lastLine = _.last(lines)
            unless lastLine.endsWith '\n'
                origLine.set
                    data: lastLine + origLine.get('data')
                    offset: origLine.get('offset') - lastLine.length
                lines = _.initial(lines)

        # create the objects for LogLine models
        @lastTimestamp = null
        @firstTimestamp = null
        offset = result.offset
        res = lines.map (data) =>
          tryTimestamp = moment data # Try builtin ISO 8601 timetamp strings
          tryTimestampCustom = moment data, 'HH:mm:ss.SSS' # Try custom format
          if tryTimestamp.isValid() or tryTimestampCustom.isValid()
            timestamp = if tryTimestamp.isValid() then tryTimestamp else tryTimestampCustom
            if not @lastTimestamp
                @firstTimestamp = timestamp
            @lastTimestamp = timestamp
            @timestampIndex = 0
          else
            timestamp = @lastTimestamp
            if @lastTimestamp
              @timestampIndex++

          line = {data, offset, timestamp, @timestampIndex, @taskId}
          offset += data.length

          line

        if @firstTimestamp
          for l in res
            if not res.timestamp
              res.timestamp = @firstTimestamp.subtract(1, 'ms')

        res


    # Static Methods -----------------------------------------------------------

    # Merge an array of multiple LogLines collections ordered by timestamp
    @merge: (collections) ->
      collection = [].concat.apply [], collections
      collection = collection.sort (a, b) =>
        if a.timestamp and b.timestamp and !a.timestamp.isSame(b.timestamp)
          return if a.timestamp.isBefore(b.timestamp) then -1 else 1
        else if a.taskId isnt b.taskId
          return if a.taskId > b.taskId then -1 else 1
        else if a.timestampIndex isnt b.timestampIndex
          return if a.timestampIndex > b.timestampIndex then -1 else 1
        else
          return 0

      collection

module.exports = LogLines
