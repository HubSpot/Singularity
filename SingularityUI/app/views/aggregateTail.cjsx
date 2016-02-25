View = require './view'
React = require 'react'
ReactDOM = require 'react-dom'
AggregateTail = require '../components/aggregateTail/AggregateTail'

class AggregateTailView extends View

    # Single Mode: Backwards compatability mode for the old URL format. Disables all task switching controls.
    initialize: ({@requestId, @path, @ajaxError, @offset, @activeTasks, @logLines, @singleMode, @singleModeTaskId}) ->
      window.addEventListener 'viewChange', @handleViewChange

    handleViewChange: =>
      unmounted = ReactDOM.unmountComponentAtNode @el
      if unmounted
        window.removeEventListener 'viewChange', @handleViewChange

    render: () ->
      $(@el).addClass("tail-root")

      ReactDOM.render(
        <AggregateTail
          requestId={@requestId}
          path={@path}
          initialOffset={@offset}
          ajaxError={@ajaxError}
          logLines={@logLines}
          activeTasks={@activeTasks}
          singleMode={@singleMode}
          singleModeTaskId={@singleModeTaskId}
        />,
        @el);

module.exports = AggregateTailView
