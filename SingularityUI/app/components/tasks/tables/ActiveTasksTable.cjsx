TaskTableContainer = require './TaskTableContainer'
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
    decommissioning_hosts: React.PropTypes.array.isRequired

  mixins: [InfiniteScroll]

  render: ->
    @tasksToRender = @props.tasks.slice(@state.lastRender, @state.renderProgress)

    tbody = @tasksToRender.map (task) =>

      taskLink = "#{config.appRoot}/task/#{task.taskId.id }"

      return (
        <tr key={_.uniqueId('taskrow_')} data-task-id={ task.taskId.id } data-task-host={ task.host }>
            <td className='keep-in-check'>
                <a title={ task.taskId.id } href={taskLink}>
                    {task.taskId.id}
                </a>
            </td>
            <td className="hidden-xs">
              { Helpers.timestampFromNow task.taskId.startedAt }
            </td>
            <td className="hidden-xs">
                { task.host }
                { if Helpers.isInSubFilter(task.host, @props.decommissioning_hosts) then <Label bsStyle='warning'>DECOM</Label> }
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
                { @props.pendingTask(task) }
            </td>
            <td className="actions-column hidden-xs">
                <TableRowAction id={ task.taskId.id } action='remove' glyph='remove' title='Kill task' />
                <TableRowAction id={ task.taskId.id } action='viewJSON' title='JSON' symbol='code' />
            </td>
        </tr>
      )
    
    ## cache new rows
    @tableBodyRows = @tableBodyRows.concat tbody

    return(
      <Row>
        <Col md={12}>
          <Table striped className='table-staged'>
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
              {@tableBodyRows}
            </tbody>
          </Table>
        </Col>
      </Row>
    )

module.exports = TaskTableContainer(ActiveTasksTable)