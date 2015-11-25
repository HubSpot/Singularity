
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

  renderTasksDropdown: ->
    <div className="btn-group">
      <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        <span className="glyphicon glyphicon-cog"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu">
        {@renderListItems()}
      </ul>
    </div>

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

  renderViewButtons: ->
    if @props.viewingInstances.length > 1
      <div className="btn-group" role="group">
        <button type="button" className="btn btn-sm btn-default no-margin #{if !@props.splitView then 'active'}" onClick={@props.toggleView}>Unified</button>
        <button type="button" className="btn btn-sm btn-default no-margin #{if @props.splitView then 'active'}" onClick={@props.toggleView}>Split</button>
      </div>

  renderColorList: ->
    <div className="btn-group">
      <button type="button" className="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
        <span className="glyphicon glyphicon-adjust"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu">
        <li className={if @props.activeColor is '' then 'active'}>
          <a onClick={() => @props.setLogColor('')}>
            <span>Default</span>
          </a>
        </li>
        <li className={if @props.activeColor is 'midnight' then 'active'}>
          <a onClick={() => @props.setLogColor('midnight')}>
            <span>Midnight</span>
          </a>
        </li>
        <li className={if @props.activeColor is 'solarized' then 'active'}>
          <a onClick={() => @props.setLogColor('solarized')}>
            <span>Solarized</span>
          </a>
        </li>
      </ul>
    </div>

  renderAnchorButtons: ->
    <span>
      <a className="btn btn-default btn-sm tail-bottom-button" onClick={@props.scrollToBottom}>
        All to bottom
      </a>
      <a className="btn btn-default btn-sm tail-top-button" onClick={@props.scrollToTop}>
        All to top
      </a>
    </span>

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
          {@renderColorList()}
          {@renderTasksDropdown()}
          {@renderViewButtons()}
          {@renderAnchorButtons()}
        </div>
      </div>
    </div>

module.exports = Header
