React = require 'react'
Header = require './Header'
TaskGroupContainer = require './TaskGroupContainer'
BackboneReactComponent = require 'backbone-react-component'
MergedLogLines = require '../../collections/MergedLogLines'

{ connect } = require 'react-redux'

class LogContainer extends React.Component
  mixins: [Backbone.React.Component.mixin]

  @propTypes:
    taskGroups: React.PropTypes.array.isRequired
    tasks: React.PropTypes.object.isRequired
    path: React.PropTypes.string.isRequired

  renderTaskIdGroups: ->
    componentProps = @props
    @props.taskGroups.map (taskGroup, i) ->
      <TaskGroupContainer
        key={i}
        taskGroupId={i}
        {...componentProps} />

  render: ->
    <div>
      <Header {...@props} taskIdCount={Object.keys(@props.tasks).length} />
      <div className="row tail-row">
        {@renderTaskIdGroups()}
      </div>
    </div>

mapStateToProps = (state, ownProps) ->
  colors: state.colors
  activeColor: state.activeColor
  requestId: state.requestId
  taskGroups: state.taskGroups
  tasks: state.tasks
  path: state.path

module.exports = connect(mapStateToProps)(LogContainer)
