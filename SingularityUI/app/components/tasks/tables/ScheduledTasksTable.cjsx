TaskTableContainer = require './TaskTableContainer'
Helpers         = require '../../utils/helpers'  
TableRowAction  = require '../../lib/TableRowAction'
EmptyTableMsg = require '../../lib/EmptyTableMsg'

Table       = ReactBootstrap.Table
Row         = ReactBootstrap.Row
Col         = ReactBootstrap.Col
Label       = ReactBootstrap.Label
Glyphicon   = ReactBootstrap.Glyphicon
Symbol      = require '../../lib/Symbol'


ScheduledTasksTable = React.createClass

  displayName: 'ScheduledTasksTable'

  render: ->

    if @props.tasks.length is 0
      return <EmptyTableMsg msg='No scheduled tasks' />

    tbody = @props.tasks.map (task) =>
      return (
        <tr key={_.uniqueId('taskrow_')}>
            <td className='keep-in-check'>
              {task.id}
            </td>
            <td className="hidden-xs">
              { Helpers.timestampFromNow task.pendingTask?.pendingTaskId.nextRunAt }
              { @props.pendingTask(task) }
            </td>
            <td className="actions-column hidden-xs">
                <TableRowAction id={ task.id } action='run' glyph='flash' title='Run now' />
                <TableRowAction id={ task.id } action='viewJSON' title='JSON' symbol='code' />
            </td>
        </tr>
      )

    return(
      <Row>
        <Col md={12}>
          <Table striped className='table-staged'>
            <thead>
                <tr>
                  <th data-sort-attribute="taskId.id">
                    Name
                  </th>
                  <th className="hidden-xs">
                    Next Run
                  </th>
                  <th className="hidden-xs">
                  </th>
                </tr>
            </thead>
            <tbody>
              {tbody}
            </tbody>
          </Table>
        </Col>
      </Row>
    )


module.exports = TaskTableContainer(ScheduledTasksTable)