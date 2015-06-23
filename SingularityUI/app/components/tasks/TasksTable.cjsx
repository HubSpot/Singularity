Helpers = require '../utils/helpers'
EmptyTableMsg = require '../lib/EmptyTableMsg'
SectionLoader = require '../lib/SectionLoader'

Tables =
  'active'    : require './tables/ActiveTasksTable'
  'scheduled' : require './tables/ScheduledTasksTable'
  'cleaning'  : require './tables/CleaningTasksTable'
  'lbcleanup' : require './tables/LBCleanupTasksTable'
  'decommissioning' : require './tables/DecommTasksTable'

TasksTable = React.createClass

  displayName: 'TasksTable'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions: React.PropTypes.func.isRequired

  getInitialState: ->
    { loading: true }

  componentWillReceiveProps: (nextProps) ->
    if nextProps.data.synced is true
      @setState { loading: false }

  render: ->
    filter = @props.data.filterState || @props.data.initialFilterState
    table = Tables[filter]

    if @state.loading and not @props.data.synced
      return <SectionLoader />

    if not @state.loading and @props.data.synced and @props.data.tasks.length is 0
      return <EmptyTableMsg msg="No #{Helpers.titleCase filter} Tasks..." />

    React.createElement(table, {tasks: @props.data.tasks, loading: @state.loading} )

module.exports = TasksTable