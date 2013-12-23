LogLine = require '../models/LogLine'

# heavily borrowed from https://github.com/apache/mesos/blob/master/src/webui/master/static/js/jquery.pailer.js
# cheers, @ssorallen !

# TODO: delete unnecessary data (ex. you scroll all the way from top to bottom -- dont need to store the lines at the to)
# TODO: misc. cleanup / optimization

class LogLines extends Backbone.Collection
    model: LogLine

    initialize: (models, @options) =>
        {@offerHostname, @directory, @path, @offset, @readLength} = @options
        @offset ?= 0
        @readLength ?= 7000

        @headBuffer = ''
        @tailBuffer = ''
        @headOffset = @offset
        @tailOffset = @offset

        @tailing = false
        @paging = false

    getSlaveUrlBase: =>
        if constants.mesosLogsPortHttps?
            "https://#{ @offerHostname }:#{ constants.mesosLogsPortHttps }"
        else
            "http://#{ @offerHostname }:#{ constants.mesosLogsPort }"

    url: (offset=@offset, length=@readLength) =>
        baseUrl = @getSlaveUrlBase()
        fullPath = "#{ @directory }/#{ @path ? ''}"
        "#{ baseUrl }/files/read.json?path=#{ escape fullPath }&offset=#{ offset }&length=#{ length }&jsonp=?"

    getCurrentOffset: (cb) =>
        promise = $.getJSON @url(-1, 0)
        promise.success (data) =>
            cb?(data.offset)

    page: =>
        # one page at a time...
        if @paging
            return

        # don't keep paging if we're at the beginning
        if @headOffset <= 0
            return

        @paging = true

        offset = Math.max(@headOffset - @readLength, 0)
        length = if @headOffset > @readLength then @readLength else @headOffset

        ajax = $.getJSON @url(offset, length)

        ajax.success (data) =>
            if not data.data
                @paging = false
                return

            @headBuffer = data.data + @headBuffer

            lines = []

            index = @headBuffer.lastIndexOf '\n'

            while index > -1
                line = @headBuffer.substring index + 1
                @headOffset -= line.length

                lines.push new LogLine
                    data: line
                    offset: @headOffset

                @headBuffer = @headBuffer.substring 0, index
                
                index = @headBuffer.lastIndexOf '\n'

            if offset is 0
                lines.push new LogLine
                    data: @headBuffer.substring 0, index
                    offset: 0

                @headBuffer = ''
                @headOffset = 0

            @unshift line for line in lines

            @paging = false

            @trigger 'paged', lines

        ajax.fail =>
            @paging = false
            # TODO: notify in some way...

    tail: =>
        # one tail at a time...
        if @tailing
            return

        @tailing = true

        ajax = $.getJSON @url(@tailOffset)

        ajax.success (data) =>
            if not data.data
                @tailing = false
                return

            @tailBuffer += data.data
            @tailOffset += data.data.length

            lines = []

            index = @tailBuffer.indexOf '\n'
            while index > -1
                line = @tailBuffer.substring 0, index

                lines.push new LogLine
                    data: line
                    offset: @tailOffset - @tailBuffer.length

                @tailBuffer = @tailBuffer.substring(index + 1)
                @tailOffset += line.length

                index = @tailBuffer.indexOf '\n'

            @add lines

            @trigger 'tailed', lines

            @tailing = false

        ajax.fail =>
            @tailing = false
            # TODO: notify in some way...

    fetch: =>

module.exports = LogLines