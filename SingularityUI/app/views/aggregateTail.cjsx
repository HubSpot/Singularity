View = require './view'

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

      # Some stuff in the app can change this stuff. We wanna reset it.
      $('html, body').css 'min-height', '0px'
      $('#global-zeroclipboard-html-bridge').css 'top', '1px'

      ReactDOM.render(
        <AggregateTail
          requestId={@requestId}
          path={@path}
          offset={@offset}
          ajaxError={@ajaxError}
          logLines={@logLines}
          activeTasks={@activeTasks}
          singleMode={@singleMode}
          singleModeTaskId={@singleModeTaskId}
        />,
        @el);

module.exports = AggregateTailView
