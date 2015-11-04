View = require './view'

AggregateTail = require '../components/aggregateTail/AggregateTail'

class AggregateTailView extends View

    events: ->

    initialize: ({@requestId, @path, @ajaxError, @offset, @activeTasks, @logLines}) ->
      window.addEventListener 'viewChange', @handleViewChange

    handleViewChange: =>
      unmounted = React.unmountComponentAtNode @el
      if unmounted
        window.removeEventListener 'viewChange', @handleViewChange

    render: ->
      React.render(
        <AggregateTail
          requestId={@requestId}
          path={@path}
          offset={@offset}
          ajaxError={@ajaxError}
          logLines={@logLines}
          activeTasks={@activeTasks}
        />,
        @el);

module.exports = AggregateTailView
