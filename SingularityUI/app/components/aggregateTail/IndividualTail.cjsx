
Header = require "./Header"
Contents = require "./Contents"

IndividualTail = React.createClass
  mixins: [Backbone.React.Component.mixin]

  componentWillMount: ->
    # Automatically map backbone collections and models to the state of this component
    if @props.activeTasks and @props.logLines
      Backbone.React.Component.mixin.on(@, {
        collections: {
          logLines: @props.logLines
        },
        models: {
          ajaxError: @props.ajaxError
        }
      });

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@);

  fetchNext: ->
    @props.logLines.fetchNext()

  fetchPrevious: ->
    @prevLines = @props.logLines.toJSON().length
    @props.logLines.fetchPrevious().done =>
      newLines = @props.logLines.toJSON().length - @prevLines
      console.log 'new', newLines
      if newLines > 2
        @setContentScroll((newLines) * 20)

  fetchFromStart: ->
    @props.logLines.fetchFromStart()

  setContentScroll: (position) ->
    @refs.contents.setScrollHeight(position)

  scrollToTop: ->
    @refs.contents.scrollToTop()

  scrollToBottom: ->
    @refs.contents.scrollToBottom()

  render: ->
    <div>
      <Contents
        ref="contents"
        requestId={@props.requestId}
        taskId={@props.taskId}
        logLines={@state.logLines}
        ajaxError={@state.ajaxError}
        offset={@props.offset}
        fetchNext={@fetchNext}
        fetchPrevious={@fetchPrevious}
        fetchFromStart={@fetchFromStart} />
    </div>

module.exports = IndividualTail
