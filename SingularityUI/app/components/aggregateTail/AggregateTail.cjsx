# BackboneReact = require "backbone-react-component"

Header = require "./Header"
Contents = require "./Contents"

AggregateTail = React.createClass
  mixins: [Backbone.React.Component.mixin]

  componentWillMount: ->
    console.log @props.activeTasks, @props.logLines
    if @props.activeTasks and @props.logLines
      Backbone.React.Component.mixin.on(@, {
        models: {
          taskHistory: @props.taskHistory
        },
        collections: {
          logLines: @props.logLines
        }
      });

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@);

  render: ->
    console.log @state
    <div>
      <Header path={@props.path} requestId={@props.requestId} />
      <Contents ajaxError={@props.ajaxError} />
      {@state.taskHistory}
    </div>

module.exports = AggregateTail
