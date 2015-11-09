
Header = require './Header'
IndividualTail = require './IndividualTail'
Utils = require '../../utils'

AggregateTail = React.createClass
  mixins: [Backbone.React.Component.mixin]

  getInitialState: ->
    params = Utils.getQueryParams()
    viewingInstances: if params.taskIds then params.taskIds.split(',').slice(0, 6) else []

  componentWillMount: ->
    # Automatically map backbone collections and models to the state of this component
    Backbone.React.Component.mixin.on(@, {
      collections: {
        activeTasks: @props.activeTasks
      }
    });

  componentDidUpdate: (prevProps, prevState) ->
    if prevState.activeTasks.length is 0 and @state.activeTasks.length > 0 and not Utils.getQueryParams()?.taskIds
      @setState
        viewingInstances: _.pluck(@state.activeTasks, 'id').slice(0, 6)

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@);

  toggleViewingInstance: (taskId) ->
    if taskId in @state.viewingInstances
      viewing = _.without @state.viewingInstances, taskId
    else
      viewing = @state.viewingInstances.concat(taskId)
    @setState
      viewingInstances: viewing

    history.replaceState @state, '', location.href.replace(location.search, "?taskIds=#{viewing.join(',')}")

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

  getRowType: ->
    if @state.viewingInstances.length > 3 then 'tail-row-half' else 'tail-row'

  getInstanceNumber: (taskId) ->
    @state.activeTasks.filter((t) =>
      t.id is taskId
    )[0].taskId.instanceNo

  renderIndividualTails: ->
    if @state.activeTasks.length > 0
      @state.viewingInstances.sort((a, b) =>
        @getInstanceNumber(a) > @getInstanceNumber(b)
      ).map((taskId, i) =>
        <div key={taskId} id="tail-#{taskId}" className="col-md-#{@getColumnWidth()} tail-column">
          <IndividualTail
            ref="tail_#{i}"
            path={@props.path}
            requestId={@props.requestId}
            taskId={taskId}
            instanceNumber={@getInstanceNumber(taskId)}
            offset={@props.offset}
            logLines={@props.logLines[taskId]}
            ajaxError={@props.ajaxError[taskId]}
            activeTasks={@props.activeTasks}
            closeTail={@toggleViewingInstance} />
        </div>
      )

  render: ->
    <div>
      <Header
       path={@props.path}
       requestId={@props.requestId}
       scrollToTop={@scrollAllTop}
       scrollToBottom={@scrollAllBottom}
       activeTasks={@state.activeTasks}
       viewingInstances={@state.viewingInstances}
       toggleViewingInstance={@toggleViewingInstance} />
      <div className="row #{@getRowType()}">
        {@renderIndividualTails()}
      </div>
    </div>

module.exports = AggregateTail
