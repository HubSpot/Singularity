ActiveTasksTable = React.createClass

  displayName: 'ActiveTasksTable'

  propTypes:
    tasks: React.PropTypes.array.isRequired
    # actions: React.PropTypes.func.isRequired

  componentWillMount: ->
    console.log 'ActiveTasksTable will mount'

  render: ->
    <div>
      ActiveTasksTable
    </div>

module.exports = ActiveTasksTable