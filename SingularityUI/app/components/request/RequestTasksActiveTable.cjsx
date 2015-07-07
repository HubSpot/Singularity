Helpers = require '../utils/helpers'
Table = ReactBootstrap.Table

RequestTasksActiveTable = React.createClass

  displayName: 'RequestTasksActiveTable'

  propTypes:
    activeTasks: React.PropTypes.array.isRequired
    getModel: React.PropTypes.func.isRequired

  ## To do: move this out to a container component
  handleShowJSON: (e) ->
    id = e.currentTarget.getAttribute('data-id')
    utils.viewJSON @props.getModel 'activeTasks', id

  render: ->
    tbody = @props.activeTasks.map (request) =>
      requestLink = "#{config.appRoot}/task/#{request.id}"
      logLink = "#{config.appRoot}/task/#{request.id}/tail/#{Helpers.substituteTaskID(config.runningTaskLogPath, request.taskId.id)}"
      return(
        <tr key={request.id}>
          <td>
            <span title={ request.id }>
              <a href=requestLink>
                { request.id }
              </a>
            </span>
          </td>
          <td>{ Helpers.humanizeText request.lastTaskState }</td>
          <td>{ request.taskId.deployId }</td>
          <td>{ Helpers.timestampFromNow request.taskId.startedAt }</td>
          <td>{ Helpers.timestampFromNow request.updatedAt }</td>
          <td className="actions-column hidden-xs">
            <a href=logLink title="Log">&#8230;</a>
          </td>
          <td className="actions-column hidden-xs">
            <a title="JSON" data-id={ request.id } onClick={@handleShowJSON}>&#123; &#125;</a>
          </td>
        </tr>
      )

    return (
      <Table striped>
        <thead>
          <tr>
            <th>Name</th>
            <th>Status</th>
            <th>Deploy ID</th>
            <th>Started</th>
            <th>Updated</th>
            <th></th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {tbody}
        </tbody>
      </Table>
    )

module.exports = RequestTasksActiveTable
