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
          logLines: @props.logLines,
          taskHistory: @props.activeTasks
        }
      });

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@);

  fetchNext: ->
    @props.logLines.fetchNext()

  render: ->
    console.log @state
    <div>
      <Header path={@props.path} requestId={@props.requestId} />
      <Contents
        logLines={@state.logLines}
        ajaxError={@props.ajaxError}
        offset={@props.offset}
        fetchNext={@fetchNext} />
    </div>

module.exports = AggregateTail
