React = require 'react'
BackboneReactComponent = require 'backbone-react-component'
Header = require './Header'
IndividualTail = require './IndividualTail'
InterleavedTail = require './InterleavedTail'
Utils = require '../../utils'
Help = require './Help'

AggregateTail = React.createClass
  mixins: [BackboneReactComponent.mixin]

  # ============================================================================
  # Lifecycle Methods                                                          |
  # ============================================================================

  # Single Mode: Backwards compatability mode for the old URL format. Disables all task switching controls.
  getInitialState: ->
    params = Utils.getQueryParams()
    viewingInstances: if @props.singleMode then [@props.singleModeTaskId] else (if params.taskIds then params.taskIds.split(',').slice(0, 6) else [])
    color: @getActiveColor()
    splitView: !(params.view is 'unified')
    search: if params.grep then params.grep else ''
    offset: @props.initialOffset

  componentWillMount: ->
    # Automatically map backbone collections and models to the state of this component
    BackboneReactComponent.mixin.on(@, {
      collections: {
        activeTasks: @props.activeTasks
      }
    });

    $(window).on("blur", @onWindowBlur)
    $(window).on("focus", @onWindowFocus)

  componentDidMount: ->
    if @state.viewingInstances.length is 1
      document.title = "Tail of #{@props.path.replace('$TASK_ID', @state.viewingInstances[0])}"
    else
      document.title = "Tail of #{@props.path.replace('$TASK_ID', 'Task Directory')}"

  initialViewingInstancesOnUpdate: ->
    runningTasks = (task for task in @state.activeTasks when task.lastTaskState == 'TASK_RUNNING')
    if runningTasks.length > 0
      return runningTasks
    else
      return @state.activeTasks

  componentDidUpdate: (prevProps, prevState) ->
    if prevState.activeTasks.length is 0 and @state.activeTasks.length > 0 and not Utils.getQueryParams()?.taskIds and !@props.singleMode
      @setState
        viewingInstances: _.pluck(@initialViewingInstancesOnUpdate(), 'id').slice(0, 6)

  componentWillUnmount: ->
    BackboneReactComponent.mixin.off(@);
    $(window).off("blur", @onWindowBlur)
    $(window).off("focus", @onWindowFocus)

  # ============================================================================
  # Event Handlers                                                             |
  # ============================================================================

  handleOffsetLink: (offset) ->
    this.setState {offset}

  onWindowBlur: ->
    @blurTimer = _.delay( =>
      for k, tail of @refs
        if tail.isTailing()
          tail.stopTailing()
          $(window).one("focus", =>
            tail.startTailing()
          )
    , 900000) # 15 minutes

  onWindowFocus: ->
    clearTimeout(@blurTimer)

  toggleViewingInstance: (taskId) ->
    if @props.singleMode
      return

    if taskId in @state.viewingInstances
      viewing = _.without @state.viewingInstances, taskId
    else
      viewing = @state.viewingInstances.concat(taskId)

    if 0 < viewing.length <= 6
      @setState
        viewingInstances: viewing
      history.replaceState @state, '', location.href.replace(location.search, "?taskIds=#{viewing.join(',')}&view=#{@getViewString(@state.splitView)}&grep=#{@state.search}")
      if viewing.length is 1
        document.title = "Tail of #{@props.path.replace('$TASK_ID', viewing[0])}"
      else
        document.title = "Tail of #{@props.path.replace('$TASK_ID', 'Task Directory')}"

  showOnlyInstance: (taskId) ->
    if @props.singleMode
      return

    @setState
      viewingInstances: [taskId]
    history.replaceState @state, '', location.href.replace(location.search, "?taskIds=#{taskId}&view=#{@getViewString(@state.splitView)}&grep=#{@state.search}")

  selectTasks: (selectFuncion) ->
    if @props.singleMode
      return

    viewing = _.pluck(selectFuncion(_.sortBy(@state.activeTasks, (task) => task.taskId.instanceNo)), 'id')
    @setState
      viewingInstances: viewing
    history.replaceState @state, '', location.href.replace(location.search, "?taskIds=#{viewing.join(',')}&view=#{@getViewString(@state.splitView)}&grep=#{@state.search}")

  setSearch: (search) ->
    @setState
      search: search
    history.replaceState @state, '', location.href.replace(location.search, "?taskIds=#{@state.viewingInstances.join(',')}&view=#{@getViewString(@state.splitView)}&grep=#{search}")

  scrollAllTop: ->
    for tail of @refs
      @refs[tail].scrollToTop()

  scrollAllBottom: ->
    for tail of @refs
      @refs[tail].scrollToBottom()

  getColumnWidth: ->
    instances = @state.viewingInstances.length
    if instances is 1
      return 12
    else if instances in [2, 4]
      return 6
    else if instances in [3, 5, 6]
      return 4
    else
      return 1

  setLogColor: (color) ->
    localStorage.setItem('singularityLogColor', color)
    @setState
      color: color

  getActiveColor: ->
    localStorage.getItem('singularityLogColor')

  toggleView: ->
    splitView = !@state.splitView
    @setState
      splitView: splitView
    viewString = @getViewString(splitView)
    history.replaceState @state, '', location.href.replace(location.search, "?taskIds=#{@state.viewingInstances.join(',')}&view=#{viewString}&grep=#{@state.search}")

  getViewString: (splitView) ->
    if splitView then 'split' else 'unified'

  toggleHelp: ->
    vex.open
      content: '<div id="help-target"></div>'
      contentClassName: 'help-dialog'
    ReactDOM.render(<Help/>, $('#help-target').get()[0])

  # ============================================================================
  # Rendering                                                                  |
  # ============================================================================

  getRowType: ->
    if !@state.splitView
      return 'unified'

    if @state.viewingInstances.length > 3 then 'tail-row-half' else 'tail-row'

  getInstanceNumber: (taskId) ->
    @state.activeTasks.filter((t) =>
      t.id is taskId
    )[0]?.taskId.instanceNo

  renderTail: ->
    if @state.splitView
      return @renderIndividualTails()
    else
      return @renderInterleavedTail()

  renderIndividualTails: ->
    @state.viewingInstances.sort((a, b) =>
      @getInstanceNumber(a) > @getInstanceNumber(b)
    ).map((taskId, i) =>
      if @props.logLines[taskId]
        <div key={taskId} id="tail-#{taskId}" className="col-md-#{@getColumnWidth()} tail-column">
          <IndividualTail
            ref="tail_#{i}"
            path={@props.path}
            requestId={@props.requestId}
            taskId={taskId}
            instanceNumber={@getInstanceNumber(taskId)}
            offset={@state.offset}
            handleOffsetLink={@handleOffsetLink}
            logLines={@props.logLines[taskId]}
            ajaxError={@props.ajaxError[taskId]}
            activeTasks={@props.activeTasks}
            closeTail={@toggleViewingInstance}
            expandTail={@showOnlyInstance}
            activeColor={@state.color}
            search={@state.search}
            onlyTask={@state.viewingInstances.length is 1} />
        </div>
    )

  renderInterleavedTail: ->
    logLines = @state.viewingInstances.map((taskId) =>
      @props.logLines[taskId]
    )
    ajaxErrors = @state.viewingInstances.map((taskId) =>
      @props.ajaxError[taskId]
    )
    if _.filter(logLines, (l) => l isnt undefined).length is @state.viewingInstances.length
      <div className="col-md-12 tail-column">
        <InterleavedTail
          ref="tail"
          path={@props.path}
          requestId={@props.requestId}
          taskId={@state.viewingInstances[0]}
          offset={@state.offset}
          logLines={logLines}
          ajaxErrors={ajaxErrors}
          activeTasks={@props.activeTasks}
          viewingInstances={@state.viewingInstances}
          search={@state.search} />
      </div>

  render: ->
    <div>
      <Header
        path={@props.path}
        requestId={@props.requestId}
        scrollToTop={@scrollAllTop}
        scrollToBottom={@scrollAllBottom}
        activeTasks={@state.activeTasks}
        viewingInstances={@state.viewingInstances}
        toggleViewingInstance={@toggleViewingInstance}
        setLogColor={@setLogColor}
        activeColor={@state.color}
        splitView={@state.splitView}
        toggleView={@toggleView}
        setSearch={@setSearch}
        search={@state.search}
        toggleHelp={@toggleHelp}
        selectTasks={@selectTasks}
        singleMode={@props.singleMode} />
      <div className="row #{@getRowType()}">
        {@renderTail()}
      </div>
    </div>

module.exports = AggregateTail
