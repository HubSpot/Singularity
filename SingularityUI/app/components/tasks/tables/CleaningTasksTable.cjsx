CleaningTasksTable = React.createClass

  displayName: 'CleaningTasksTable'

  propTypes:
    tasks: React.PropTypes.array.isRequired
    # actions: React.PropTypes.func.isRequired
  
  componentWillMount: ->
    console.log 'CleaningTasksTable will mount'

  render: ->
    <div>
      CleaningTasksTable
    </div>

module.exports = CleaningTasksTable