# BackboneReact = require "backbone-react-component"

Header = require "./Header"
Contents = require "./Contents"

AggregateTail = React.createClass
  mixins: [Backbone.React.Component.mixin]

  componentWillMount: ->
    if @props.activeTasks and @props.logLines
      Backbone.React.Component.mixin.on(@, {
        collections: {
          logLines: @props.logLines,
          taskHistory: @props.activeTasks
        }
      });

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@);

  componentDidMount: ->
    @props.activeTasks.fetch().done =>
      console.log @props.activeTasks
      console.log @state

  render: ->
    console.log @state
    <div>
      <Header path={@props.path} requestId={@props.requestId} />
      <Contents ajaxError={@props.ajaxError} />
    </div>

module.exports = AggregateTail
