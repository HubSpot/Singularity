React = require 'react'
ColorDropdown = require './ColorDropdown'
SearchDropdown = require './SearchDropdown'
TasksDropdown = require './TasksDropdown'

{ connect } = require 'react-redux'
{ switchViewMode, scrollAllToTop, scrollAllToBottom } = require('../../actions/log')

class Header extends React.Component
  @propTypes:
    requestId: React.PropTypes.string.isRequired
    path: React.PropTypes.string.isRequired
    multipleTasks: React.PropTypes.bool.isRequired
    viewMode: React.PropTypes.string.isRequired

    switchViewMode: React.PropTypes.func.isRequired
    scrollAllToBottom: React.PropTypes.func.isRequired
    scrollAllToTop: React.PropTypes.func.isRequired

  renderBreadcrumbs: ->
    @props.path.split('/').map (subpath, i) ->
      if subpath is '$TASK_ID'
        <li key={i}><span className="label label-info">Task ID</span></li>
      else
        <li key={i}>{subpath}</li>

  renderViewButtons: ->
    if @props.multipleTasks
      <div className="btn-group" role="group" title="Select View Type">
        <button type="button" className="btn btn-sm btn-default no-margin #{if @props.viewMode is 'unified' then 'active'}" onClick={=> @props.switchViewMode('unified')}>Unified</button>
        <button type="button" className="btn btn-sm btn-default no-margin #{if @props.viewMode is 'split' then 'active'}" onClick={=> @props.switchViewMode('split')}>Split</button>
      </div>

  renderAnchorButtons: ->
    if @props.taskGroupCount > 1
      <span>
        <a className="btn btn-default btn-sm tail-bottom-button" onClick={@props.scrollAllToBottom} title="Scroll All to Bottom">
          <span className="glyphicon glyphicon-chevron-down"></span>
        </a>
        <a className="btn btn-default btn-sm tail-top-button" onClick={@props.scrollAllToTop} title="Scroll All to Top">
          <span className="glyphicon glyphicon-chevron-up"></span>
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
            <SearchDropdown />
            <TasksDropdown />
            <ColorDropdown />
            {@renderViewButtons()}
            {@renderAnchorButtons()}
          </div>
        </div>
      </div>

mapStateToProps = (state) ->
  taskGroupCount: state.taskGroups.length
  multipleTasks: state.taskGroups.length > 1 or (state.taskGroups.length > 0 and state.taskGroups[0].taskIds.length > 1)
  path: state.path
  viewMode: state.viewMode
  requestId: state.activeRequest.requestId

mapDispatchToProps = { switchViewMode, scrollAllToBottom, scrollAllToTop }

module.exports = connect(mapStateToProps, mapDispatchToProps)(Header)
