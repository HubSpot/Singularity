DashboardMain = require '../components/dashboard/DashboardMainCmpt'
View = require './ReactBaseView'

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
    
  renderReact: ->
    React.render(
        <DashboardMain
          data={@getRenderData()}
          actions={@actions}            
        />, 
        app.pageEl
      )

  ##
  ## Render Data
  ##
  getRenderData: ->
    totals: @collection.getUserRequestsTotals(app.user.get('deployUser'))
    starredRequests: @starredRequests()
    user: app.user
    username: app.user.get('deployUser')
    refresh: @refresh
    sortedAsc: @collection.sortAscending

  starredRequests: ->
    if @collection.isSorted
      @sortedRequests
    else
      _.pluck @collection.getStarredOnly(), "attributes"

  ##
  ## Actions
  ##
  actions: =>
    unstar: @unstar
    sortStarredRequests: @sortStarredRequests

  unstar: (id) =>
    @collection.toggleStar id
    @renderReact()

  sortStarredRequests: (attribute) =>
    @sortedRequests = @collection.sortCollection attribute
    @renderReact()


module.exports = DashboardView