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
            changeTable={@props.actions().changeTable}
            buttons= {[
              { label: 'Active', id: 'active', nolink: true, link: "#{config.appRoot}/tasks" }
              { label: 'Scheduled', id: 'scheduled', link: "#{config.appRoot}/tasks/scheduled" }
              { label: 'Cleaning', id: 'cleaning', link: "#{config.appRoot}/tasks/cleaning" }
              { label: 'LB Cleaning', id: 'lbcleanup', link: "#{config.appRoot}/tasks/lbcleanup" }
              { label: 'Decommissioning', id: 'decommissioning', link: "#{config.appRoot}/tasks/decommissioning" }
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