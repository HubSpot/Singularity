InfiniteScroll  = require '../../utils/mixins/InfiniteScroll'
Helpers         = require '../../utils/helpers'  
TableRowAction  = require '../../lib/TableRowAction'

Table       = ReactBootstrap.Table
Row         = ReactBootstrap.Row
Col         = ReactBootstrap.Col
Label       = ReactBootstrap.Label
Glyphicon   = ReactBootstrap.Glyphicon
Symbol      = require '../../lib/Symbol'


ActiveTasksTable = React.createClass

  displayName: 'ActiveTasksTable'

  propTypes:
    tasks: React.PropTypes.array.isRequired
    # renderAtOnce: React.PropTypes.number.isRequired
    # actions: React.PropTypes.func.isRequired

  mixins: [InfiniteScroll]

  render: ->
    @tasksToRender = @props.tasks.slice(@state.lastRender, @state.renderProgress)

    pendingTask = (task) ->
      if task.pendingTask?
        if Helpers.isTimestampInPast task.pendingTask.pendingTaskId.nextRunAt
          <Label bsStyle='danger'>OVERDUE</Label>

    decommTask = (task, decommissioning_hosts) ->
      if Helpers.isInSubFilter task.host, decommissioning_hosts
        <Label bsStyle='warning'>DECOM</Label>

    tbody = @tasksToRender.map (task) =>
      taskLink = "#{config.appRoot}/task/#{task.taskId.id }"

      ## TO DO: 
      decommissioning_hosts = "damp-sound"  ## NEED TO ADD SLAVES CHECK...

      return (
        <tr key={task.taskId.id} data-task-id="{ task.taskId.id }" data-task-host="{ task.host }">
            <td className='keep-in-check'>
                <a title="{ taskId.id }" href={taskLink}>
                    {task.taskId.id}
                </a>
            </td>
            <td className="hidden-xs">
              { Helpers.timestampFromNow task.taskId.startedAt }
            </td>
            <td className="hidden-xs">
                { task.host }
                { decommTask(task, decommissioning_hosts) }
            </td>
            <td className="visible-lg">
                { task.taskId.rackId }
            </td>
            <td className="visible-lg">
                { task.cpus }
            </td>
            <td className="hidden-xs">
                { task.memoryMb } MB
            </td>
            <td>
                { pendingTask(task) }
            </td>
            <td className="actions-column hidden-xs">
                <TableRowAction id={ task.taskId.id } action='remove' glyph='remove' title='Kill task' />
                <TableRowAction id={ task.taskId.id } action='viewJSON' title='JSON' symbol='code' />
            </td>
        </tr>
      )
    
    ## cache new rows
    @tasksRowsCache = @tasksRowsCache.concat tbody

    return(
      <Row>
        <Col md={12} className="table-staged">
          <Table striped>
            <thead>
                <tr>
                  <th data-sort-attribute="taskId.id">
                    Name
                  </th>
                  <th data-sort-attribute="taskId.startedAt" className="hidden-xs">
                    Started
                  </th>
                  <th data-sort-attribute="host" className="hidden-xs">
                      Host
                  </th>
                  <th data-sort-attribute="taskId.rackId" className="hidden-xs">
                    Rack
                  </th>
                  <th data-sort-attribute="cpus" className="visible-lg">
                    CPUs
                  </th>
                  <th data-sort-attribute="memoryMb" className="visible-lg">
                    Memory
                  </th>
                  <th>
                  </th>
                  <th className="hidden-xs">
                  </th>
                </tr>
            </thead>
            <tbody>
              {@tasksRowsCache}
            </tbody>
          </Table>
        </Col>
      </Row>
    )

module.exports = ActiveTasksTable