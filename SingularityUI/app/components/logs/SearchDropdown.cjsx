React = require 'react'

{ connect } = require 'react-redux'
{ setCurrentSearch } = require '../../actions/log'

class SearchDropdown extends React.Component
  @propTypes:
    currentSearch: React.PropTypes.string.isRequired

  constructor: (props) ->
    super(props)
    @state = {
      searchValue: 'xxx'
    }

  handleSearchToggle: ->
    # TODO

  handleSearchUpdate: =>
    console.log @props.setCurrentSearch
    @props.setCurrentSearch(@state.searchValue)

  render: ->
    <div className="btn-group" title="Grep">
      <button id="searchDDToggle" type="button" className="btn btn-#{if @props.currentSearch is '' then 'default' else 'info'} btn-sm dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" onClick={@handleSearchToggle}>
        <span className="glyphicon glyphicon-search"></span> <span className="caret"></span>
      </button>
      <ul className="dropdown-menu">
        <li>
          <div className="input-group log-search">
            <input ref="searchInput" type="text" className="form-control" placeholder="Grep Logs" value={@state.searchValue} onChange={(e) => @setState({searchValue: e.target.value})} />
            <span className="input-group-btn">
              <button className="btn btn-info no-margin" type="button" onClick={@handleSearchUpdate}><span className="glyphicon glyphicon-search"></span></button>
            </span>
          </div>
        </li>
      </ul>
    </div>

mapStateToProps = (state) ->
  currentSearch: state.currentSearch

mapDispatchToProps = (dispatch) ->
  setCurrentSearch: (value) -> dispatch(setCurrentSearch(value))

module.exports = connect(mapStateToProps, mapDispatchToProps)(SearchDropdown)