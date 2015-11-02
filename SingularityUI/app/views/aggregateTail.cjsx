View = require './view'

AggregateTail = require '../components/aggregateTail/AggregateTail'

class AggregateTailView extends View

    events: ->

    initialize: ({@requestId, @path, @ajaxError, @offset}) ->


    render: =>
      # Factory = React.createFactory(AggregateTail)
      # root = Factory({
      #   requestId: @requestId,
      #   path: @path,
      #   offset: @offset
      #   })
      React.render(
        <AggregateTail
          requestId={@requestId}
          path={@path}
          offset={@offset}
          ajaxError={@ajaxError}
        />,
        @el);

module.exports = AggregateTailView
