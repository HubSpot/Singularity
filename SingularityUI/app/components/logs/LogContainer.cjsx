React = require 'react'
Interval = require 'react-interval'
Header = require './Header'
TaskGroupContainer = require './TaskGroupContainer'

{ connect } = require 'react-redux'

{ updateGroups, updateTaskStatuses } = require('../../actions/log')

class LogContainer extends React.Component
  @propTypes:
    taskGroupsCount: React.PropTypes.number.isRequired
    ready: React.PropTypes.bool.isRequired

    updateGroups: React.PropTypes.func.isRequired
    updateTaskStatuses: React.PropTypes.func.isRequired

  renderTaskGroups: ->
    rows = []

    tasksPerRow = if @props.taskGroupsCount is 4 then 2 else 3

    row = []
    for i in [1..Math.min(@props.taskGroupsCount, tasksPerRow)]
      row.push <TaskGroupContainer key={i - 1} taskGroupId={i - 1} taskGroupContainerCount={Math.min(@props.taskGroupsCount, tasksPerRow)} />

    rows.push row

    if @props.taskGroupsCount > tasksPerRow
      row = []
      for i in [tasksPerRow+1..Math.min(@props.taskGroupsCount, 6)]
        row.push <TaskGroupContainer key={i - 1} taskGroupId={i - 1} taskGroupContainerCount={Math.min(@props.taskGroupsCount, 6) - tasksPerRow} />
      rows.push row

    rowClassName = 'row tail-row'

    if rows.length > 1
      rowClassName = 'row tail-row-half'

    rows.map (row, i) -> <div key={i} className={rowClassName}>{row}</div>

  render: ->
    <div>
      <Interval enabled={@props.ready} timeout={2000} callback={@props.updateGroups} />
      <Interval enabled={true} timeout={10000} callback={@props.updateTaskStatuses} />
      <Header />
      {@renderTaskGroups()}
    </div>

mapStateToProps = (state) ->
  taskGroupsCount: state.taskGroups.length
  ready: _.all(_.pluck(state.taskGroups, 'ready'))

mapDispatchToProps = { updateGroups, updateTaskStatuses }

module.exports = connect(mapStateToProps, mapDispatchToProps)(LogContainer)
