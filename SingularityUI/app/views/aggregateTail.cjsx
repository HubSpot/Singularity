View = require './view'

AggregateTail = require '../components/aggregateTail/AggregateTail'

class AggregateTailView extends View

    events: ->

    initialize: ({@requestId, @path, @ajaxError, @offset, @activeTasks, @logLines}) ->
      window.addEventListener 'viewChange', @handleViewChange.bind(@)

    handleViewChange: =>
      console.log @app
      # if @app.views.current isnt @
      #   return
      console.log 'change'
      window.removeEventListener 'viewChange', @handleViewChange
      if @el
        console.log 'unmounted', React.unmountComponentAtNode @el

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
