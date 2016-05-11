React = require 'react'
OverlayTrigger = require 'react-bootstrap/lib/OverlayTrigger'
ToolTip = require 'react-bootstrap/lib/Tooltip'
{ toggleTaskLog } = require '../../actions/log'

{ getHostFromTaskId } = require '../../utils'

{ connect } = require 'react-redux'

class TasksDropdown extends React.Component

  getTaskListTooltip: (task) ->
    <ToolTip id={task.taskId.id}>Host: {task.taskId.host}</ToolTip>

  renderListItems: ->
    if @props.activeTasks and @props.taskIds
      tasks = _.sortBy(@props.activeTasks, (t) => t.taskId.instanceNo).map (task, i) =>
        <li key={i}>
          <OverlayTrigger placement='left' overlay={@getTaskListTooltip task}>
            <a onClick={() => @props.toggleTaskLog(task.taskId.id)}>
              <span className="glyphicon glyphicon-#{if task.taskId.id in @props.taskIds then 'check' else 'unchecked'}"></span>
              <span> Instance {task.taskId.instanceNo}</span>
            </a>
          </OverlayTrigger>
        </li>
      if tasks.length > 0
        return tasks
      else
        return <li><a className="disabled">No running instances</a></li>
    else
      <li><a className="disabled">Loading active tasks...</a></li>

  render: ->
    <div className="btn-group" title="Select Instances">
      <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        <span className="glyphicon glyphicon-tasks"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu dropdown-menu-right">
        {@renderListItems()}
      </ul>
    </div>

mapStateToProps = (state) ->
  activeTasks: state.activeRequest.activeTasks
  taskIds: _.flatten(_.pluck(state.taskGroups, 'taskIds'))

mapDispatchToProps = { toggleTaskLog }

module.exports = connect(mapStateToProps, mapDispatchToProps)(TasksDropdown)
