View = require './view'

LogLines = require '../collections/LogLines'

# TODO: misc. cleanup / optimizations

escapeHTML = (string) ->
    if not string
        return ''

    escapes =
      '&': '&amp;'
      '<': '&lt;'
      '>': '&gt;'
      '"': '&quot;'
      "'": '&#x27;'
      '/': '&#x2F;'

    regex = new RegExp '[' + Object.keys(escapes).join('') + ']', 'g'

    return ('' + string).replace regex, (match) -> escapes[match]

class TailerView extends Backbone.View
    initialize: =>
        @paging = false
        @tailing = true

        @lines = new LogLines [], @options

        @lines.on 'tailed', =>
            {scrollTop, height, scrollHeight} = @getScrollInfo()

            # stop tailing if not at bottom anymore
            if scrollTop + height < scrollHeight
                @tailing = false

            @render()

        @lines.on 'paged', =>
            @render()

        @$el.scroll @handleScroll

        @lines.tail()

    handleScroll: =>
        {scrollTop, height, scrollHeight} = @getScrollInfo()

        if scrollTop is 0
            # we're at the top, we should page
            @lines.page()
        else if scrollTop + height >= scrollHeight - 20
            # we're near the bottom, we should tail if not tailing already...
            if not @tailing
                @tailing = true
                @lines.tail()
        else
            # we're in the middle, stop tailing...
            @tailing = false

    getScrollInfo: =>
        scrollTop: @$el.scrollTop()
        height: @$el.height()
        scrollHeight: @$el[0].scrollHeight

    render: =>
        @$el.html (@lines.map (line) -> escapeHTML(line.attributes.data)).join '\n'

module.exports = TailerView