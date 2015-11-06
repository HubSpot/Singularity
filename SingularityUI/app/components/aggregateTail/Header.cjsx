
Header = React.createClass

  renderBreadcrumbs: ->
    segments = @props.path.split('/')
    return segments.map (s, i) =>
      path = segments.slice(0, i + 1).join('/')
      if i < segments.length - 1
        return (
          <li key={i}>
            <a href="#{config.appRoot}/request/#{@props.requestId}/tail/#{path}">
                {s}
            </a>
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
          <div className="col-md-7">
              <ul className="breadcrumb">
                  {@renderBreadcrumbs()}
              </ul>
          </div>
          <div className="col-md-2 hidden-xs tail-buttons">
              <a className="btn btn-default tail-top-button" onClick={@props.scrollToTop}>
                  All to top
              </a>
              <a className="btn btn-default tail-bottom-button" onClick={@props.scrollToBottom}>
                  All to bottom
              </a>
          </div>
      </div>
    </div>

module.exports = Header
