React = require 'react'
Interval = require 'react-interval'
Header = require './Header'
TaskGroupContainer = require './TaskGroupContainer'

{ connect } = require 'react-redux'

{ updateGroups } = require '../../actions/log'

class LogContainer extends React.Component
  @propTypes:
    taskGroups: React.PropTypes.array.isRequired
    ready: React.PropTypes.bool.isRequired

    updateGroups: React.PropTypes.func.isRequired

  renderTaskGroups: ->
    @props.taskGroups.map (taskGroup, i) ->
      <TaskGroupContainer key={i} taskGroupId={i}/>

  render: ->
    <div>
      <Interval enabled={@props.ready} timeout={1000} callback={@props.updateGroups} />
      <Header />
      <div className="row tail-row">
        {@renderTaskGroups()}
      </div>
    </div>

mapStateToProps = (state) ->
  taskGroups: state.taskGroups
  ready: _.all(_.pluck(state.taskGroups, 'ready'))

mapDispatchToProps = { updateGroups }

module.exports = connect(mapStateToProps, mapDispatchToProps)(LogContainer)
