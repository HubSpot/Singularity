# ##for testing
# Request = require '../../../models/Request'
ParentViewMixin = require '../../mixins/ParentViewMixin'
BackboneMixin = require '../../mixins/BackboneMixin'

UserInfo = require './UserInfo'
Requests = require './requests'

DashboardMain = React.createClass

  # https://github.com/magalhas/backbone-react-component
  # mixins: [Backbone.React.Component.mixin]
  mixins: [ParentViewMixin]

  getInitialState: ->
      totals: []
      username: ''

  componentDidMount: ->
    console.log 'componentDidMount actual component'

  ## For global refresh and child components
  refresh: ->
    @props.requestsCollection.fetch().done =>
      @setRenderData()

  setRenderData: ->
    @setState
      totals: @props.requestsCollection.getUserRequestsTotals()

  render: ->
    console.log 'render DashboardMainComponent'
    return (
      <div>
        <UserInfo 
          user={@props.user}
          refresh={@refresh}
        />
        <Requests 
          requestTotals={@state.totals} 
          username={@props.user.get('deployUser')}
        />
      </div>
    )

module.exports = DashboardMain