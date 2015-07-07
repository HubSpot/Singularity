EmptyTableMsg = require '../../lib/EmptyTableMsg'

CleaningTasksTable = React.createClass

  displayName: 'CleaningTasksTable'

  propTypes:
    tasks: React.PropTypes.array.isRequired
    # actions: React.PropTypes.func.isRequired
  
  componentWillMount: ->
    console.log 'CleaningTasksTable will mount'

  render: ->

    if @props.tasks.length is 0
      return <EmptyTableMsg msg='No cleaning tasks' />

    return (
      <div>
        CleaningTasksTable
      </div>
    )

module.exports = CleaningTasksTable