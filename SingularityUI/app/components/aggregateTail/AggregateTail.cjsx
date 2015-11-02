Header = require "./Header"
Contents = require "./Contents"

AggregateTail = React.createClass
  mixins: [Backbone.React.Component.mixin]

  componentWillMount: =>
    # backboneReact.on(this, {
    #   collections: {
    #     myCollection: collection1
    #   }
    # });

  componentWillUnmount: =>
    # backboneReact.off(this);

  render: ->
    console.log @props
    <div>
      <Header path={@props.path} requestId={@props.requestId} />
      <Contents ajaxError={@props.ajaxError} />
    </div>

module.exports = AggregateTail
