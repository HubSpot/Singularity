Glyphicon = ReactBootstrap.Glyphicon

TableContainer = (Component) ->

  Table = React.createClass

    propTypes:
      sortTable: React.PropTypes.func.isRequired
      unstar: React.PropTypes.func.isRequired

    getInitialState: ->
      { sortedAttribute: null }

    handleUnstar: (e) ->
      id = e.currentTarget.getAttribute('data-id')
      @props.unstar id

    handleSort: (e) ->
      attribute = e.currentTarget.getAttribute('data-sort-attribute')
      @setState { sortedAttribute: attribute}
      @props.sortTable attribute

    sortDirection: (attr) ->
      if @state.sortedAttribute is attr
        <Glyphicon glyph="chevron-#{if @props.data.sortedAsc then 'up' else 'down' }" />

    render: ->
      return (
        <Component 
          {...@props} 
          {...@state} 
          handleUnstar={@handleUnstar}
          handleSort={@handleSort}
          sortDirection={@sortDirection}
        />
      )


module.exports = TableContainer