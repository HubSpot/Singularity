DashboardMain = require '../components/dashboard/DashboardMain'
ReactBaseView = require './ReactBaseView'

class DashboardView extends ReactBaseView

  initialize:  =>        
    @listenTo @collection, 'change', @renderReact
    @listenTo app.user, 'change', @renderReact

    @refresh()

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

  getRenderData: ->
    totals: @collection.getUserRequestsTotals(app.user.get('deployUser'))
    starredItems: @starredItems()
    user: app.user
    username: app.user.get('deployUser')
    refresh: @refresh
    sortedAsc: @collection.sortAscending

  starredItems: ->
    if @collection.isSorted and not @hasUnstarred
      @sortedItems
    else
      _.pluck @collection.getStarredOnly(), "attributes"

  unstar: (id) =>
    @hasUnstarred = true
    @collection.toggleStar id
    @renderReact()
    @hasUnstarred = false

  sortTable: (attribute) =>
    @sortedItems = @collection.sortCollection attribute
    @renderReact()

  actions: =>
    unstar: @unstar
    sortTable: @sortTable

module.exports = DashboardView