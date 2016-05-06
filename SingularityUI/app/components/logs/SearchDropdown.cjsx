React = require 'react'
ReactDOM = require 'react-dom'

{ connect } = require 'react-redux'
{ setCurrentSearch } = require '../../actions/log'

class SearchDropdown extends React.Component
  @propTypes:
    search: React.PropTypes.string.isRequired

  constructor: (props) ->
    super(props)
    @state = {
      searchValue: props.search
    }

  handleSearchToggle: =>
    ReactDOM.findDOMNode(@refs.searchInput).focus()

  handleSearchUpdate: =>
    @props.setCurrentSearch(@state.searchValue)

  toggleSearchDropdown: =>
    $(ReactDOM.findDOMNode(@refs.searchButton)).dropdown("toggle")

  handleSearchKeyDown: (event) =>
    if event.keyCode is 13 # Enter: commit search and close
      @handleSearchUpdate()
      @toggleSearchDropdown()
    else if event.keyCode is 27 # Escape: clear search and commit
      @setState
        searchValue: @props.search
      @toggleSearchDropdown()

  render: ->
    <div className="btn-group" title="Grep">
      <button ref="searchButton" id="searchDDToggle" type="button" className="btn btn-#{if @props.search is '' then 'default' else 'info'} btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" onClick={@handleSearchToggle}>
        <span className="glyphicon glyphicon-search"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu dropdown-menu-right">
        <li>
          <div className="input-group log-search">
            <input ref="searchInput" type="text" className="form-control" placeholder="Grep Logs" value={@state.searchValue} onKeyDown={@handleSearchKeyDown} onChange={(e) => @setState({searchValue: e.target.value})} />
            <span className="input-group-btn">
              <button className="btn btn-info no-margin" type="button" onClick={@handleSearchUpdate}><span className="glyphicon glyphicon-search"></span></button>
            </span>
          </div>
        </li>
      </ul>
    </div>

mapStateToProps = (state) ->
  search: state.search

mapDispatchToProps = { setCurrentSearch }

module.exports = connect(mapStateToProps, mapDispatchToProps)(SearchDropdown)