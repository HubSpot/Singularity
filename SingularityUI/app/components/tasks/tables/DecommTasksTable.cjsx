DecommTasksTable = React.createClass

  displayName: 'DecommTasksTable'

  propTypes:
    tasks: React.PropTypes.array.isRequired
    # actions: React.PropTypes.func.isRequired
  
  componentWillMount: ->
    console.log 'DecommTasksTable will mount'

  render: ->
    <div>
      DecommTasksTable
    </div>

module.exports = DecommTasksTable