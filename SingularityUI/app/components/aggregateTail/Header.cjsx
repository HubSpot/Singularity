
Header = React.createClass

  renderBreadcrumbs: ->
    segments = @props.path.split('/')
    return segments.map (s, i) =>
      path = segments.slice(0, i + 1).join('/')
      if i < segments.length - 1
        return (
          <li key={i}>
            {s}
          </li>
        )
      else
        return (
          <li key={i}>
            <strong>
                {s}
            </strong>
          </li>
        )

  renderListItems: ->
    tasks = _.sortBy(@props.activeTasks, (t) => t.taskId.instanceNo).map (task, i) =>
        taskId = task.id
        <li key={i}>
          <a onClick={() => @props.toggleViewingInstance(taskId)}>
            <span className="glyphicon glyphicon-#{if taskId in @props.viewingInstances then 'check' else 'unchecked'}"></span>
            <span> Instance {task.taskId.instanceNo}</span>
          </a>
        </li>
    if tasks.length > 0
      return tasks
    else
      return <li><a className="disabled">No running instances</a></li>

  render: ->
    <div className="tail-header">
      <div className="row">
        <div className="col-md-3">
          <ul className="breadcrumb breadcrumb-request">
            <li>
              Request&nbsp;
              <a href="#{config.appRoot}/request/#{@props.requestId}">
                {@props.requestId}
              </a>
            </li>
          </ul>
        </div>
        <div className="col-md-6">
          <ul className="breadcrumb">
            {@renderBreadcrumbs()}
          </ul>
        </div>
        <div className="col-md-3 hidden-xs tail-buttons">
          <div className="btn-group">
            <button type="button" className="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
              <span className="glyphicon glyphicon-cog"></span> <span className="caret"></span>
            </button>
            <ul className="dropdown-menu">
              {@renderListItems()}
            </ul>
          </div>
          <a className="btn btn-default tail-bottom-button" onClick={@props.scrollToBottom}>
            All to bottom
          </a>
          <a className="btn btn-default tail-top-button" onClick={@props.scrollToTop}>
            All to top
          </a>
        </div>
      </div>
    </div>

module.exports = Header
