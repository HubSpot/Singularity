# BackboneReact = require "backbone-react-component"

Header = require "./Header"
Contents = require "./Contents"

AggregateTail = React.createClass
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

  fetchPrevious: (currentScrollHeight) ->
    console.log 'prev', currentScrollHeight
    @props.logLines.fetchPrevious().done =>
      # if currentScrollHeight
      #   @setContentScroll(currentScrollHeight)

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
      <Header
        path={@props.path}
        requestId={@props.requestId}
        scrollToTop={@scrollToTop}
        scrollToBottom={@scrollToBottom} />
      <Contents
        ref="contents"
        logLines={@state.logLines}
        ajaxError={@state.ajaxError}
        offset={@props.offset}
        fetchNext={@fetchNext}
        fetchPrevious={@fetchPrevious}
        fetchFromStart={@fetchFromStart} />
    </div>

module.exports = AggregateTail
