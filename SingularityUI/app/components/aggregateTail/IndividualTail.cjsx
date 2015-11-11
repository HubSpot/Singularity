
IndividualHeader = require "./IndividualHeader"
Contents = require "./Contents"

TaskHistory = require '../../models/TaskHistory'

IndividualTail = React.createClass
  mixins: [Backbone.React.Component.mixin]

  componentWillMount: ->
    # Get the task info
    @task = new TaskHistory {taskId: @props.taskId}
    @startTaskStatusPoll()

    # Automatically map backbone collections and models to the state of this component
    Backbone.React.Component.mixin.on(@, {
      collections: {
        logLines: @props.logLines
      },
      models: {
        ajaxError: @props.ajaxError
        task: @task
      }
    });

  componentWillUnmount: ->
    Backbone.React.Component.mixin.off(@)
    @stopTaskStatusPoll()

  startTaskStatusPoll: ->
    @task.fetch()
    @taskPoll = setInterval =>
      @task.fetch()
    , 5000

  stopTaskStatusPoll: ->
    clearInterval @taskPoll

  fetchNext: ->
    @props.logLines.fetchNext()

  fetchPrevious: (callback) ->
    @prevLines = @props.logLines.toJSON().length
    @props.logLines.fetchPrevious().done =>
      newLines = @props.logLines.toJSON().length - @prevLines
      console.log 'new', newLines
      @scrollToLine(newLines)
      callback()

  fetchFromStart: ->
    @props.logLines.fetchFromStart()

  scrollToLine: (line) ->
    @refs.contents.scrollToLine(line)

  scrollToTop: ->
    @refs.contents.scrollToTop()

  scrollToBottom: ->
    @refs.contents.scrollToBottom()

  isMoreToFetchAtBeginning: ->
    @props.logLines.state.get('moreToFetchAtBeginning')

  render: ->
    <div>
      <IndividualHeader
        taskId={@props.taskId}
        instanceNumber={@props.instanceNumber}
        scrollToTop={@scrollToTop}
        scrollToBottom={@scrollToBottom}
        closeTail={() => @props.closeTail(@props.taskId)}
        taskState={_.last(@state.task.taskUpdates)?.taskState} />
      <Contents
        ref="contents"
        requestId={@props.requestId}
        taskId={@props.taskId}
        logLines={@state.logLines}
        ajaxError={@state.ajaxError}
        offset={@props.offset}
        fetchNext={@fetchNext}
        fetchPrevious={@fetchPrevious}
        fetchFromStart={@fetchFromStart}
        isMoreToFetchAtBeginning={@isMoreToFetchAtBeginning}
        taskState={_.last(@state.task.taskUpdates)?.taskState} />
    </div>

module.exports = IndividualTail
