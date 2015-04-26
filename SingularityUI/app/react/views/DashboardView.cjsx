DashboardMain = require '../components/dashboard/DashboardMainCmpt'
View = require './BaseView'

class DashboardView extends View

  # Set this view as app.currentView so app.globalRefresh
  # can trigger this view to refresh().
  # Listen to events and kick off the initial fetch.
  initialize:  =>        
    @listenTo @collection, 'change', @renderReact
    @listenTo app.user, 'change', @renderReact

    @refresh()

  ## Fetch data and then pass it to components as
  ## as plain objects. This keeps backbone's business
  ## out of react's business.
  refresh: =>
    @collection.fetch().done =>
      @renderReact()
    
  ## Pass data into the parent component as plain objects 
  renderReact: ->
    totals = @collection.getUserRequestsTotals()
    user = app.user
    username = app.user.get('deployUser')
    
    React.render(
        <DashboardMain
            totals={totals}
            starredRequests={@getStarredRequests()}
            user={user}
            username={username}
            refresh={@refresh}
            unstar={@unstar}
            sortStarredRequests={@sortStarredRequests}
        />, 
        app.pageEl
      )

  getStarredRequests: ->
    requests = _.pluck @collection.getStarredOnly(), "attributes"
    return requests


  ##
  ## Starred Request Table Handlers
  ##
  unstar: (id) =>
    @collection.toggleStar id
    @refresh()

  sortStarredRequests: (attribute) =>
    console.log 'sort by: ', attribute
    @renderReact()

module.exports = DashboardView