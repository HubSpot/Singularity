React = require 'react'
ColorDropdown = require './ColorDropdown'
SearchDropdown = require './SearchDropdown'
TasksDropdown = require './TasksDropdown'

{ connect } = require 'react-redux'
{ switchViewMode } = require '../../actions/log'

scrollToBottom = ->
scrollToTop = ->

class Header extends React.Component
  @propTypes:
    requestId: React.PropTypes.string.isRequired
    path: React.PropTypes.string.isRequired
    taskIdCount: React.PropTypes.number.isRequired
    viewMode: React.PropTypes.string.isRequired

    scrollToBottom: React.PropTypes.func.isRequired
    scrollToTop: React.PropTypes.func.isRequired

  toggleHelp: ->
    # TODO

  renderBreadcrumbs: ->
    @props.path.split('/').map (subpath, i) ->
      <li key={i}>{subpath}</li>

  renderViewButtons: ->
    if @props.taskIdCount > 1
      <div className="btn-group" role="group" title="Select View Type">
        <button type="button" className="btn btn-sm btn-default no-margin #{if @props.viewMode is 'unified' then 'active'}" onClick={=> @props.switchViewMode('unified')}>Unified</button>
        <button type="button" className="btn btn-sm btn-default no-margin #{if @props.viewMode is 'split' then 'active'}" onClick={=> @props.switchViewMode('split')}>Split</button>
      </div>

  renderAnchorButtons: ->
    <span>
      <a className="btn btn-default btn-sm tail-bottom-button" onClick={@props.scrollToBottom} title="Scroll All to Bottom">
        <span className="glyphicon glyphicon-chevron-down"></span>
      </a>
      <a className="btn btn-default btn-sm tail-top-button" onClick={@props.scrollToTop} title="Scroll All to Top">
        <span className="glyphicon glyphicon-chevron-up"></span>
      </a>
    </span>

  renderHelpButton: ->
    <a className="help-link" onClick={@props.toggleHelp} title="Show Help">
      <span className="glyphicon glyphicon-question-sign"></span>
    </a>

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
            {@renderHelpButton()}
          </div>
        </div>
      </div>

mapStateToProps = (state) ->
  taskIdCount: Object.keys(state.tasks).length
  path: state.path
  viewMode: state.viewMode
  requestId: state.activeRequest.requestId

mapDispatchToProps = (dispatch) ->
  switchViewMode: (viewMode) -> dispatch(switchViewMode(viewMode))
  scrollToBottom: -> dispatch(scrollToBottom())
  scrollToTop: -> dispatch(scrollToTop())

module.exports = connect(mapStateToProps, mapDispatchToProps)(Header)
