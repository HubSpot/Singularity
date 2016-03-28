React = require 'react'
{ toggleTaskLog } = require '../../actions/log'

{ connect } = require 'react-redux'

class TasksDropdown extends React.Component
  handleTasksKeyDown: ->
    # TODO

  renderListItems: ->
    if @props.activeTasks and @props.taskIds
      tasks = _.sortBy(@props.activeTasks, (t) => t.taskId.instanceNo).map (task, i) =>
        <li key={i}>
          <a onClick={() => @props.onToggleViewingInstance(task.taskId.id)}>
            <span className="glyphicon glyphicon-#{if @props.taskIds[task.taskId.id] then 'check' else 'unchecked'}"></span>
            <span> Instance {task.taskId.instanceNo}</span>
          </a>
        </li>
      if tasks.length > 0
        return tasks
      else
        return <li><a className="disabled">No running instances</a></li>
    else
      <li><a className="disabled">Loading active tasks...</a></li>

  render: ->
    <div className="btn-group" title="Select Instances">
      <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" onKeyDown={@handleTasksKeyDown}>
        <span className="glyphicon glyphicon-tasks"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu">
        {@renderListItems()}
      </ul>
    </div>

mapStateToProps = (state) ->
  activeTasks: state.activeRequest.activeTasks
  taskIds: state.tasks

mapDispatchToProps = (dispatch) ->
  onToggleViewingInstance: (taskId) -> dispatch(toggleTaskLog(taskId))

module.exports = connect(mapStateToProps, mapDispatchToProps)(TasksDropdown)