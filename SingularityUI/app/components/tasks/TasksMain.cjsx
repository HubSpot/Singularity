Row = ReactBootstrap.Row
Col = ReactBootstrap.Col

FilterButtons = require '../lib/FilterButtons'
FilterSearch = require '../lib/FilterSearch'
TasksTable = require './TasksTable'

TasksMain = React.createClass

  displayName: 'TasksMain'

  propTypes:
    data: React.PropTypes.object.isRequired
    actions: React.PropTypes.func.isRequired

  render: ->
    <div>

      <Row>
        <Col md=12>
          <FilterButtons
            data={@props.data}
            buttons= {[
              { label: 'Active', id: 'active', nolink: true }
              { label: 'Scheduled', id: 'scheduled' }
              { label: 'Cleaning', id: 'cleaning' }
              { label: 'LB Cleaning', id: 'lbcleanup' }
              { label: 'Decommissioning', id: 'decommissioning' }
            ]}
          />
        </Col>
      </Row>



      <FilterSearch />
      <TasksTable
        data={@props.data}
        actions={@props.actions}
      />
    
    </div>

module.exports = TasksMain