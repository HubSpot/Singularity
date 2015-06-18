LBCleanupTasksTable = React.createClass

  displayName: 'LBCleanupTasksTable'

  propTypes:
    tasks: React.PropTypes.array.isRequired
    # actions: React.PropTypes.func.isRequired
  
  componentWillMount: ->
    console.log 'LBCleanupTasksTable will mount'

  render: ->
    <div>
      LBCleanupTasksTable
    </div>

module.exports = LBCleanupTasksTable